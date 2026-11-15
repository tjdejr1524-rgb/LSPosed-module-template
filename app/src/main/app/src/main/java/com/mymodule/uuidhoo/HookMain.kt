package com.mymodule.uuidhoo

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.io.File

class HookMain : IXposedHookLoadPackage {
    private var fakeUuid: String = "00000000-0000-0000-0000-000000000000"
    private var fakeAndroidId: String = "0000000000000000"

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        val targetPackages = listOf(
            "com.ss.android.ugc.trill",
            "com.ss.android.ugc.tiktok.lite",
            "com.eltavine.duckdetector"
        )
        if (lpparam.packageName !in targetPackages) return

        loadFakeValues(lpparam.packageName)

        // 1) UUID.randomUUID() 후킹 (TikTok이 실제로 사용하는 경로)
        XposedHelpers.findAndHookMethod(
            "java.util.UUID", lpparam.classLoader, "randomUUID",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    param.result = java.util.UUID.fromString(fakeUuid)
                }
            }
        )

        // 2) Settings.Secure.getString(cr, "android_id") 후킹
        XposedHelpers.findAndHookMethod(
            "android.provider.Settings\$Secure", lpparam.classLoader,
            "getString", android.content.ContentResolver::class.java, String::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (param.args[1] == "android_id") {
                        param.result = fakeAndroidId
                    }
                }
            }
        )
    }

    private fun loadFakeValues(packageName: String) {
        val paths = listOf(
            "/data/data/$packageName/files/fake_uuid.txt",  // 앱 데이터 디렉터리 우선
            "/data/local/tmp/fake_uuid.txt"                  // 폴백 (SELinux 막힐 수 있음)
        )
        for (path in paths) {
            try {
                val file = File(path)
                if (!file.exists() || !file.canRead()) continue
                val line = file.readText().trim()
                val parts = line.split("\\s+".toRegex())
                if (parts.size >= 2 && parts[0].length == 16 && parts[1].length == 16) {
                    val hex32 = parts[0] + parts[1]
                    fakeUuid = "${hex32.substring(0,8)}-${hex32.substring(8,12)}-" +
                               "${hex32.substring(12,16)}-${hex32.substring(16,20)}-" +
                               "${hex32.substring(20,32)}"
                    fakeAndroidId = parts[0]  // android_id는 보통 16자 hex
                    return
                }
            } catch (_: Exception) { }
        }
    }
}

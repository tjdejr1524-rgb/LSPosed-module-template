package com.mymodule.uuidhoo

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.io.File

class HookMain : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        // 🟢 덕디텍터 패키지 추가
        val targetPackages = listOf(
            "com.ss.android.ugc.trill",
            "com.ss.android.ugc.tiktok.lite",
            "com.eltavine.duckdetector" // 👈 덕디텍터 추가
        )
        if (lpparam.packageName !in targetPackages) return

fun getFakeUuid(): String {
    return try {
        val file = File("/data/local/tmp/fake_uuid.txt")
        if (file.exists() && file.canRead()) {
            val content = file.readText().trim()
            // "UUID:Inode" 형식에서 UUID 부분만 추출
            val uuid = content.split(":")[0]
            uuid
        } else {
            "00000000-0000-0000-0000-000000000000"
        }
    } catch (e: Exception) {
        "00000000-0000-0000-0000-000000000000"
    }
}

        XposedHelpers.findAndHookMethod(
            "android.os.storage.StorageVolume",
            lpparam.classLoader,
            "getUuid",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    param.result = getFakeUuid()
                }
            }
        )
    }
}

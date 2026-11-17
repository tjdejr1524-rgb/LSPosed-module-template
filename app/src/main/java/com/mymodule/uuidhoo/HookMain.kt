package com.mymodule.uuidhoo

import android.util.Log
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.io.File

class HookMain : IXposedHookLoadPackage {

    companion object {
        private const val TAG = "UUIDHoo"
        private const val FALLBACK_VOLUME_UUID = "0000-0000"
        private const val FALLBACK_ANDROID_ID = "0000000000000000"

        private val TARGET_PACKAGES = listOf(
            "com.ss.android.ugc.trill",
            "com.ss.android.ugc.tiktok.lite",
            "com.eltavine.duckdetector"
        )
    }

    init {
        Log.d(TAG, "HookMain init")
        XposedBridge.log("$TAG: HookMain init")
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName !in TARGET_PACKAGES) return

        Log.d(TAG, "attach ${lpparam.packageName}")
        XposedBridge.log("$TAG: attach ${lpparam.packageName}")

        val pkg = lpparam.packageName

        try {
            XposedHelpers.findAndHookMethod(
                "android.provider.Settings\$Secure",
                null,
                "getString",
                android.content.ContentResolver::class.java,
                String::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        if (param.args.size >= 2 && param.args[1] == "android_id") {
                            param.result = loadFakeValues(pkg).second
                        }
                    }
                }
            )
        } catch (e: Throwable) {
            XposedBridge.log("$TAG: getString hook failed: ${e.message}")
        }

        try {
            XposedHelpers.findAndHookMethod(
                "android.provider.Settings\$Secure",
                null,
                "getStringForUser",
                android.content.ContentResolver::class.java,
                String::class.java,
                Int::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        if (param.args.size >= 2 && param.args[1] == "android_id") {
                            param.result = loadFakeValues(pkg).second
                        }
                    }
                }
            )
        } catch (e: Throwable) {
            XposedBridge.log("$TAG: getStringForUser hook failed: ${e.message}")
        }

        try {
            XposedHelpers.findAndHookMethod(
                "android.os.storage.StorageVolume",
                null,
                "getUuid",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        param.result = loadFakeValues(pkg).first
                    }
                }
            )
        } catch (e: Throwable) {
            XposedBridge.log("$TAG: getUuid hook failed: ${e.message}")
        }
    }

    /** 호출마다 다시 읽음. 반환은 (볼륨 UUID "XXXX-XXXX", android_id 16hex) */
    private fun loadFakeValues(packageName: String): Pair<String, String> {
        val paths = listOf(
            "/data/data/$packageName/files/fake_uuid.txt",
            "/data/local/tmp/fake_uuid.txt"
        )

        for (path in paths) {
            try {
                val file = File(path)
                if (!file.exists() || !file.canRead()) continue

                val parts = file.readText().trim().split("\\s+".toRegex())
                if (parts.size < 2) continue

                val fsidHex = parts[0].padStart(16, '0').takeLast(16)
                val volumeUuid = (fsidHex.substring(0, 4) + "-" + fsidHex.substring(4, 8)).uppercase()
                val androidId = fsidHex

                return Pair(volumeUuid, androidId)
            } catch (e: Throwable) {
                Log.e(TAG, "$path: ${e.message}")
            }
        }

        return Pair(FALLBACK_VOLUME_UUID, FALLBACK_ANDROID_ID)
    }
}

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

        val pkg = lpparam.packageName
        Log.d(TAG, "attach $pkg")
        XposedBridge.log("$TAG: attach $pkg")

        try {
            XposedHelpers.findAndHookMethod(
                "android.os.storage.StorageVolume",
                null,
                "getUuid",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        param.result = loadVolumeUuid(pkg)
                    }
                }
            )
        } catch (e: Throwable) {
            XposedBridge.log("$TAG: getUuid hook failed: ${e.message}")
        }
    }

    /** 호출마다 다시 읽음. fsid 앞 8자 → "XXXX-XXXX" */
    private fun loadVolumeUuid(packageName: String): String {
        val paths = listOf(
            "/data/data/$packageName/files/fake_uuid.txt",
            "/data/local/tmp/fake_uuid.txt"
        )

        for (path in paths) {
            try {
                val file = File(path)
                if (!file.exists() || !file.canRead()) continue

                val parts = file.readText().trim().split("\\s+".toRegex())
                if (parts.isEmpty() || parts[0].isEmpty()) continue

                val fsidHex = parts[0].padStart(16, '0').takeLast(16)
                return (fsidHex.substring(0, 4) + "-" + fsidHex.substring(4, 8)).uppercase()
            } catch (e: Throwable) {
                Log.e(TAG, "$path: ${e.message}")
            }
        }

        return FALLBACK_VOLUME_UUID
    }
}

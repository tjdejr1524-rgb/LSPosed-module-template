package com.mymodule.uuidhoo

import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam
import java.io.File

class HookMain : XposedModule() {

    companion object {
        private const val FALLBACK = "0000-0000"
        private val TARGETS = setOf(
            "com.ss.android.ugc.trill",
            "com.ss.android.ugc.tiktok.lite",
            "com.eltavine.duckdetector"
        )
    }

override fun onPackageLoaded(param: PackageLoadedParam) {
    if (!param.isFirstPackage) return
    val packageName = param.packageName
    if (packageName !in TARGETS) return

    try {
        val method = Class.forName("android.os.storage.StorageVolume")
            .getDeclaredMethod("getUuid")
        deoptimize(method)
        hook(method).intercept(object : XposedInterface.Hooker {
            override fun intercept(chain: XposedInterface.Chain): Any {
                return load(packageName)
            }
        })
    } catch (t: Throwable) {
        log(android.util.Log.ERROR, "UUIDHoo", "uuid hook failed: $t")
    }
}
    private fun load(packageName: String): String {
        val paths = listOf(
            "/data/user/0/$packageName/files/fake_uuid.txt",
            "/data/data/$packageName/files/fake_uuid.txt"
        )
        for (path in paths) {
            try {
                val file = File(path)
                if (!file.exists() || !file.canRead()) continue
                val hex = file.readText().trim().split("\\s+".toRegex())
                    .firstOrNull()?.padStart(16, '0')?.takeLast(16)
                    ?: continue
                return (hex.substring(0, 4) + "-" + hex.substring(4, 8)).uppercase()
            } catch (_: Throwable) {
            }
        }
        return FALLBACK
    }
}

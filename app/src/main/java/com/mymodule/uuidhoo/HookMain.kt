package com.mymodule.uuidhoo

import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam
import java.io.File

class HookMain(base: XposedInterface, param: ModuleLoadedParam) : XposedModule(base, param) {

    companion object {
        private const val FALLBACK = "0000-0000"
        private val TARGETS = setOf(
            "com.ss.android.ugc.trill",
            "com.ss.android.ugc.tiktok.lite",
            "com.eltavine.duckdetector"
        )
        @Volatile
        var pkg: String = ""
    }

    override fun onPackageLoaded(param: PackageLoadedParam) {
        if (!param.isFirstPackage) return
        val name = param.packageName
        if (name !in TARGETS) return
        pkg = name
        val method = Class.forName("android.os.storage.StorageVolume")
            .getDeclaredMethod("getUuid")
        hook(method, UuidHooker::class.java)
    }

    class UuidHooker : XposedInterface.Hooker {
        companion object {
            @JvmStatic
            @XposedInterface.AfterInvocation
            fun after(callback: XposedInterface.AfterHookCallback) {
                callback.result = load(pkg)
            }

            private fun load(packageName: String): String {
                val paths = listOf(
                    "/data/data/$packageName/files/fake_uuid.txt",
                    "/data/user/0/$packageName/files/fake_uuid.txt"
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
    }
}

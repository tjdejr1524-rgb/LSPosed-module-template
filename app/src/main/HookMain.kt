package com.mymodule.uuidhook

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class HookMain : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != "com.ss.android.ugc.trill" && 
            lpparam.packageName != "com.ss.android.ugc.tiktok.lite") return

        XposedHelpers.findAndHookMethod(
            "android.os.storage.StorageVolume",
            lpparam.classLoader,
            "getUuid",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    // 여기에 KPM이 생성한 가짜 UUID를 파일에서 읽어와 반환
                    param.result = "00000000-0000-0000-0000-000000000000"
                }
            }
        )
    }
}

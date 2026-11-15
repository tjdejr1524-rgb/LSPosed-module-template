package com.mymodule.uuidhoo

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.io.File

class HookMain : IXposedHookLoadPackage {

    companion object {
        private const val TAG = "UUIDHook"
        private const val FALLBACK_UUID = "00000000-0000-0000-0000-000000000000"
        private const val FALLBACK_ANDROID_ID = "0000000000000000"

        private val TARGET_PACKAGES = listOf(
            "com.ss.android.ugc.trill",
            "com.ss.android.ugc.tiktok.lite",
            "com.eltavine.duckdetector"
        )
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName !in TARGET_PACKAGES) return

        XposedBridge.log("$TAG: attaching to ${lpparam.packageName}")

        // ── KPM fake 값 로드 ──
        val (fakeUuid, fakeAndroidId) = loadFakeValues(lpparam.packageName)
        XposedBridge.log("$TAG: fakeUuid=$fakeUuid, fakeAndroidId=$fakeAndroidId")

        // ══════════════════════════════════════════════
        // 1) java.util.UUID.randomUUID() 후킹
        // ══════════════════════════════════════════════
        try {
            XposedHelpers.findAndHookMethod(
                "java.util.UUID",
                null,  // 부트 클래스패스 (시스템 클래스)
                "randomUUID",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        try {
                            param.result = java.util.UUID.fromString(fakeUuid)
                        } catch (e: Throwable) {
                            XposedBridge.log("$TAG: UUID.fromString 실패: ${e.message}")
                        }
                    }
                }
            )
            XposedBridge.log("$TAG: ✓ UUID.randomUUID 후킹 성공")
        } catch (e: Throwable) {
            XposedBridge.log("$TAG: ✗ UUID.randomUUID 후킹 실패: ${e.message}")
        }

        // ══════════════════════════════════════════════
        // 2) Settings.Secure.getString(cr, "android_id") 후킹
        // ══════════════════════════════════════════════
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
                            param.result = fakeAndroidId
                        }
                    }
                }
            )
            XposedBridge.log("$TAG: ✓ Settings.Secure.getString 후킹 성공")
        } catch (e: Throwable) {
            XposedBridge.log("$TAG: ✗ Settings.Secure.getString 후킹 실패: ${e.message}")
        }

        // ══════════════════════════════════════════════
        // 3) StorageVolume.getUuid() 후킹 (원래 요청 코드 유지, 효과는 불확실)
        // ══════════════════════════════════════════════
        try {
            XposedHelpers.findAndHookMethod(
                "android.os.storage.StorageVolume",
                null,
                "getUuid",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        param.result = fakeUuid
                    }
                }
            )
            XposedBridge.log("$TAG: ✓ StorageVolume.getUuid 후킹 성공")
        } catch (e: Throwable) {
            XposedBridge.log("$TAG: ✗ StorageVolume.getUuid 후킹 실패: ${e.message}")
        }
    }

    /**
     * KPM이 쓴 fake_uuid.txt를 읽어서 (UUID, android_id) 쌍으로 반환
     *
     * 파일 형식: "<fsid_hex_16> <ino_hex_16>\n"
     * 예:       "a641841865136930 1cacc7e77e7099a9\n"
     */
    private fun loadFakeValues(packageName: String): Pair<String, String> {
        val paths = listOf(
            "/data/data/$packageName/files/fake_uuid.txt",  // 앱 데이터 (SELinux 통과)
            "/data/local/tmp/fake_uuid.txt"                 // 폴백 (SELinux 차단될 수 있음)
        )

        for (path in paths) {
            try {
                val file = File(path)
                if (!file.exists() || !file.canRead()) continue

                val content = file.readText().trim()
                if (content.isEmpty()) continue

                // 공백 구분 (스페이스, 탭, 개행 등)
                val parts = content.split("\\s+".toRegex())
                if (parts.size < 2) continue

                val fsidHex = parts[0]
                val inoHex = parts[1]

                // 16자 hex 검증
                if (fsidHex.length != 16 || inoHex.length != 16) {
                    XposedBridge.log("$TAG: hex 길이 이상 (fsid=${fsidHex.length}, ino=${inoHex.length})")
                    continue
                }
                if (!fsidHex.matches(Regex("[0-9a-fA-F]+")) ||
                    !inoHex.matches(Regex("[0-9a-fA-F]+"))) {
                    XposedBridge.log("$TAG: hex 문자 이상")
                    continue
                }

                // 32자 hex → UUID 형식 (8-4-4-4-12)
                val hex32 = fsidHex + inoHex
                val uuid = buildString {
                    append(hex32.substring(0, 8)); append('-')
                    append(hex32.substring(8, 12)); append('-')
                    append(hex32.substring(12, 16)); append('-')
                    append(hex32.substring(16, 20)); append('-')
                    append(hex32.substring(20, 32))
                }

                // android_id는 보통 16자 hex. fsid 그대로 사용.
                val androidId = fsidHex

                XposedBridge.log("$TAG: $path 읽기 성공 → uuid=$uuid, androidId=$androidId")
                return Pair(uuid, androidId)

            } catch (e: Throwable) {
                XposedBridge.log("$TAG: $path 읽기 실패: ${e.message}")
            }
        }

        XposedBridge.log("$TAG: fake_uuid.txt를 찾지 못함. fallback 사용")
        return Pair(FALLBACK_UUID, FALLBACK_ANDROID_ID)
    }
}

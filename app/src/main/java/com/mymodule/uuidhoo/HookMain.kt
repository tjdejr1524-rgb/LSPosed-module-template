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
        private const val FALLBACK_UUID = "00000000-0000-0000-0000-000000000000"
        private const val FALLBACK_ANDROID_ID = "0000000000000000"

        private val TARGET_PACKAGES = listOf(
            "com.ss.android.ugc.trill",
            "com.ss.android.ugc.tiktok.lite",
            "com.eltavine.duckdetector"
        )
    }

    // 클래스가 로드되자마자 찍히는 로그
    init {
        Log.d(TAG, "★ HookMain 클래스 로드됨 (init block)")
        XposedBridge.log("$TAG: ★ HookMain 클래스 로드됨 (init block)")
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        // 함수 호출 즉시 찍히는 로그
        Log.d(TAG, "★ handleLoadPackage 호출됨: ${lpparam.packageName}")

        if (lpparam.packageName !in TARGET_PACKAGES) return

        Log.d(TAG, "★ 타겟 패키지 매칭됨! 후킹을 시작합니다: ${lpparam.packageName}")
        XposedBridge.log("$TAG: attaching to ${lpparam.packageName}")

        // ── KPM fake 값 로드 ──
        val (fakeUuid, fakeAndroidId) = loadFakeValues(lpparam.packageName)
        Log.d(TAG, "로드된 가짜 값 -> UUID: $fakeUuid, AndroidID: $fakeAndroidId")

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
                            Log.d(TAG, "→ UUID.randomUUID() 호출 가로챔: $fakeUuid 반환")
                        } catch (e: Throwable) {
                            Log.e(TAG, "UUID.fromString 실패: ${e.message}")
                            XposedBridge.log("$TAG: UUID.fromString 실패: ${e.message}")
                        }
                    }
                }
            )
            Log.d(TAG, "✓ UUID.randomUUID 후킹 성공")
            XposedBridge.log("$TAG: ✓ UUID.randomUUID 후킹 성공")
        } catch (e: Throwable) {
            Log.e(TAG, "✗ UUID.randomUUID 후킹 실패: ${e.message}")
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
                            Log.d(TAG, "→ Settings.Secure android_id 호출 가로챔: $fakeAndroidId 반환")
                        }
                    }
                }
            )
            Log.d(TAG, "✓ Settings.Secure.getString 후킹 성공")
            XposedBridge.log("$TAG: ✓ Settings.Secure.getString 후킹 성공")
        } catch (e: Throwable) {
            Log.e(TAG, "✗ Settings.Secure.getString 후킹 실패: ${e.message}")
            XposedBridge.log("$TAG: ✗ Settings.Secure.getString 후킹 실패: ${e.message}")
        }

        // ══════════════════════════════════════════════
        // 3) StorageVolume.getUuid() 후킹
        // ══════════════════════════════════════════════
        try {
            XposedHelpers.findAndHookMethod(
                "android.os.storage.StorageVolume",
                null,
                "getUuid",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        param.result = fakeUuid
                        Log.d(TAG, "→ StorageVolume.getUuid 호출 가로챔: $fakeUuid 반환")
                    }
                }
            )
            Log.d(TAG, "✓ StorageVolume.getUuid 후킹 성공")
            XposedBridge.log("$TAG: ✓ StorageVolume.getUuid 후킹 성공")
        } catch (e: Throwable) {
            Log.e(TAG, "✗ StorageVolume.getUuid 후킹 실패: ${e.message}")
            XposedBridge.log("$TAG: ✗ StorageVolume.getUuid 후킹 실패: ${e.message}")
        }
    }

    /**
     * KPM이 쓴 fake_uuid.txt를 읽어서 (UUID, android_id) 쌍으로 반환
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

                val parts = content.split("\\s+".toRegex())
                if (parts.size < 2) continue

                val fsidHex = parts[0]
                val inoHex = parts[1]

                // 16자 hex 검증
                if (fsidHex.length != 16 || inoHex.length != 16) {
                    Log.w(TAG, "hex 길이 이상 (fsid=${fsidHex.length}, ino=${inoHex.length})")
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

                val androidId = fsidHex

                Log.d(TAG, "✓ $path 읽기 성공 -> uuid=$uuid, androidId=$androidId")
                XposedBridge.log("$TAG: $path 읽기 성공 → uuid=$uuid, androidId=$androidId")
                return Pair(uuid, androidId)

            } catch (e: Throwable) {
                Log.e(TAG, "✗ $path 읽기 실패: ${e.message}")
                XposedBridge.log("$TAG: $path 읽기 실패: ${e.message}")
            }
        }

        Log.w(TAG, "fake_uuid.txt를 찾지 못함. fallback 사용")
        return Pair(FALLBACK_UUID, FALLBACK_ANDROID_ID)
    }
}

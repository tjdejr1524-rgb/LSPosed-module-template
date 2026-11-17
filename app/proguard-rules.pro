-adaptresourcefilecontents META-INF/xposed/java_init.list

-keep,allowobfuscation,allowoptimization public class * extends io.github.libxposed.api.XposedModule {
    public void onPackageLoaded(...);
    public void onPackageReady(...);
    public void onSystemServerStarting(...);
}
-keep,allowshrinking,allowoptimization,allowobfuscation class ** implements io.github.libxposed.api.XposedInterface$Hooker
-keepclassmembers,allowoptimization class ** implements io.github.libxposed.api.XposedInterface$Hooker {
    public java.lang.Object intercept(io.github.libxposed.api.XposedInterface$Chain);
}
-keep class com.mymodule.uuidhoo.HookMain { *; }
-keep class com.mymodule.uuidhoo.HookMain$UuidHooker { *; }
-keepclassmembers class * {
    @io.github.libxposed.api.XposedInterface$AfterInvocation <methods>;
}

-repackageclasses
-allowaccessmodification

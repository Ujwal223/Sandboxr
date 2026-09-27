# ==============================================================================
# Sandboxr Launcher ProGuard / R8 Optimization & Obfuscation Rules
# Ported from GrapheneOS Launcher3 (proguard.flags) and adapted for Sandboxr
# ==============================================================================

# Improves inlining by allowing ProGuard / R8 to make fields/methods accessed
# by the inlined method visible.
-allowaccessmodification

# Selectively allow shrinking and optimization for key launcher packages
-keep,allowshrinking,allowoptimization,allowaccessmodification class com.sandboxr.launcher.** { *; }
-keep,allowshrinking,allowoptimization,allowaccessmodification class com.android.launcher3.** { *; }
-keep,allowshrinking,allowoptimization class com.android.systemui.shared.** { *; }
-keepclasseswithmembernames class com.android.systemui.shared.** { *; }
-keep,allowshrinking,allowoptimization class com.android.quickstep.** { *; }
-keepclasseswithmembernames class com.android.quickstep.** { *; }

# System UI & Window Manager settings
-keep class android.gui.BoxShadowSettings
-keep class android.gui.BorderSettings

# Proguard will strip methods required for talkback to properly scroll to
# next row when focus is on the last item of last row when using a RecyclerView
-keep class androidx.recyclerview.widget.RecyclerView { *; }

# Fragments constructors
-keep class ** extends androidx.fragment.app.Fragment {
    public <init>(...);
}
-keep class ** extends android.app.Fragment {
    public <init>(...);
}

# Protocol Buffers (lite) rules
-keep class com.google.protobuf.** { *; }
-keep class * extends com.google.protobuf.GeneratedMessageLite { *; }
-keep interface com.android.launcher3.userevent.nano.LauncherLogProto.** { *; }
-keep interface com.android.launcher3.model.nano.LauncherDumpProto.** { *; }
-keep interface com.android.internal.protolog.common.IProtoLogGroup { *; }

# Dagger Dependency Injection Keep Rules
-keep class * extends dagger.Component { *; }
-keep @dagger.Component class * { *; }
-keep @dagger.Subcomponent class * { *; }
-keep @dagger.Module class * { *; }
-keepclassmembers class * {
    @javax.inject.Inject <init>(...);
    @javax.inject.Inject <fields>;
    @javax.inject.Inject <methods>;
    @dagger.Provides <methods>;
    @dagger.Binds <methods>;
}
-keep class com.sandboxr.launcher.dagger.** { *; }

# Animation methods accessed via reflection / object animator
-keep class com.android.launcher3.allapps.DiscoveryBounce$VerticalProgressWrapper {
    public void setProgress(float);
    public float getProgress();
}
-keep class com.sandboxr.launcher.allapps.DiscoveryBounce$VerticalProgressWrapper {
    public void setProgress(float);
    public float getProgress();
}

# Tooling and Compose internal library warnings
-dontwarn androidx.compose.animation.tooling.**

# Suppress warnings for framework-hidden APIs accessed via reflection
-dontwarn android.app.**
-dontwarn android.graphics.**
-dontwarn android.net.**
-dontwarn android.os.**
-dontwarn android.view.**
-dontwarn android.window.**
-dontwarn android.support.**
-dontwarn org.apache.http.**

# Testing & Heap analysis keep rules
-keep class com.android.launcher3.util.BaseContext { *; }
-keep class com.android.launcher3.util.LifecycleRegistryWrapper { *; }
-keep class androidx.lifecycle.LifecycleRegistry { *; }

# App Functions serialization
-keep @androidx.appfunctions.AppFunctionSerializable class * {
    <fields>;
    <init>(...);
}

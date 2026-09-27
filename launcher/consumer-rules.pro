# ==============================================================================
# Consumer ProGuard Rules for Sandboxr Launcher Library
# Exported to consumer modules (e.g. :app)
# ==============================================================================

-keep,allowshrinking,allowoptimization,allowaccessmodification class com.sandboxr.launcher.** { *; }
-keep,allowshrinking,allowoptimization,allowaccessmodification class com.android.launcher3.** { *; }
-keep,allowshrinking,allowoptimization class com.android.systemui.shared.** { *; }
-keepclasseswithmembernames class com.android.systemui.shared.** { *; }
-keep,allowshrinking,allowoptimization class com.android.quickstep.** { *; }
-keepclasseswithmembernames class com.android.quickstep.** { *; }

-keep class androidx.recyclerview.widget.RecyclerView { *; }
-keep class com.google.protobuf.** { *; }
-keep class * extends com.google.protobuf.GeneratedMessageLite { *; }

-keep class com.sandboxr.launcher.dagger.** { *; }
-keep class * extends dagger.Component { *; }
-keep @dagger.Component class * { *; }
-keep @dagger.Subcomponent class * { *; }
-keep @dagger.Module class * { *; }

-dontwarn androidx.compose.animation.tooling.**
-dontwarn android.app.**
-dontwarn android.graphics.**
-dontwarn android.net.**
-dontwarn android.os.**
-dontwarn android.view.**
-dontwarn android.window.**
-dontwarn android.support.**
-dontwarn org.apache.http.**

package com.sandboxr.virtual.server.pm

import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.ProviderInfo
import android.content.pm.ServiceInfo
import java.io.File

/**
 * Representation of a parsed guest package APK.
 */
data class VPackage(
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val apkFile: File,
    val packageInfo: PackageInfo,
    val applicationInfo: ApplicationInfo,
    val activities: List<ActivityInfo> = emptyList(),
    val services: List<ServiceInfo> = emptyList(),
    val providers: List<ProviderInfo> = emptyList(),
    val requestedPermissions: List<String> = emptyList()
)

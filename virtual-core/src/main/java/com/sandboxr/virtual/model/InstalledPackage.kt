package com.sandboxr.virtual.model

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import java.io.File

/**
 * Model representing an application installed inside a virtual environment.
 */
data class InstalledPackage(
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val apkFile: File,
    val envId: String,
    val installTime: Long = System.currentTimeMillis(),
    val applicationInfo: ApplicationInfo,
    val packageInfo: PackageInfo
)

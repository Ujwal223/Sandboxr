package com.sandboxr.virtual.server.gms

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.util.Log
import java.util.concurrent.ConcurrentHashMap

/**
 * Intercepts Google Play Services (GMS) requests on a per-environment basis.
 *
 * When GMS is disabled for an environment (default privacy mode):
 * - Intercepts queries for "com.google.android.gms", "com.android.vending", and "com.google.android.gsf".
 * - Cleanly returns SERVICE_MISSING (code 1) or API_UNAVAILABLE (code 16) without throwing
 *   unhandled exceptions or crashing guest applications.
 * - Suppresses background GMS binding intents and telemetry beacons.
 *
 * Allows guest applications to execute standard fallback flows (e.g. OpenStreetMap,
 * microG, local storage, non-GMS push notifications) with graceful degradation.
 */
class GmsInterceptor private constructor() {

    companion object {
        private const val TAG = "GmsInterceptor"

        const val GMS_PACKAGE = "com.google.android.gms"
        const val PLAY_STORE_PACKAGE = "com.android.vending"
        const val GSF_PACKAGE = "com.google.android.gsf"

        // Standard ConnectionResult status codes recognized by GoogleApiAvailability
        const val SERVICE_SUCCESS = 0
        const val SERVICE_MISSING = 1
        const val SERVICE_VERSION_UPDATE_REQUIRED = 2
        const val SERVICE_DISABLED = 3
        const val SERVICE_INVALID = 9
        const val API_UNAVAILABLE = 16

        val GMS_PACKAGES = setOf(
            GMS_PACKAGE,
            PLAY_STORE_PACKAGE,
            GSF_PACKAGE
        )

        @Volatile
        private var instance: GmsInterceptor? = null

        fun get(): GmsInterceptor {
            return instance ?: synchronized(this) {
                instance ?: GmsInterceptor().also { instance = it }
            }
        }

        fun createForTesting(): GmsInterceptor {
            return GmsInterceptor()
        }
    }

    /**
     * Map of environment UUID to GMS enabled boolean flag.
     * Environments not explicitly set default to false (blocked).
     */
    private val environmentGmsPolicy = ConcurrentHashMap<String, Boolean>()

    /**
     * Configures GMS policy for a specific environment.
     */
    fun setGmsEnabled(envId: String, enabled: Boolean) {
        environmentGmsPolicy[envId] = enabled
        Log.d(TAG, "Environment '$envId' GMS policy set to enabled=$enabled")
    }

    /**
     * Checks if GMS is enabled for an environment. Defaults to false (blocked).
     */
    fun isGmsEnabled(envId: String): Boolean {
        return environmentGmsPolicy[envId] ?: false
    }

    /**
     * Checks if the target package is a Google Play Services ecosystem component.
     */
    fun isGmsPackage(packageName: String): Boolean {
        return GMS_PACKAGES.contains(packageName) ||
                packageName.startsWith("com.google.android.gms.")
    }

    /**
     * Evaluates whether a package query should be blocked for the given environment.
     */
    fun shouldBlockPackageQuery(packageName: String, envId: String): Boolean {
        if (!isGmsPackage(packageName)) {
            return false
        }
        return !isGmsEnabled(envId)
    }

    /**
     * Intercepts IPackageManager.getPackageInfo for GMS queries.
     *
     * @return null if GMS is disabled (simulating missing package), or throws NameNotFoundException
     *         if requireException is requested.
     */
    fun interceptPackageInfo(packageName: String, envId: String, requireException: Boolean = false): PackageInfo? {
        if (shouldBlockPackageQuery(packageName, envId)) {
            Log.d(TAG, "Intercepted getPackageInfo for '$packageName' in env '$envId' -> Returning SERVICE_MISSING")
            if (requireException) {
                throw PackageManager.NameNotFoundException("Google Play Services disabled for environment $envId")
            }
            return null
        }
        return null // Not intercepted, proceed with normal resolution
    }

    /**
     * Intercepts IPackageManager.getApplicationInfo for GMS queries.
     */
    fun interceptApplicationInfo(packageName: String, envId: String): ApplicationInfo? {
        if (shouldBlockPackageQuery(packageName, envId)) {
            Log.d(TAG, "Intercepted getApplicationInfo for '$packageName' in env '$envId' -> Blocked")
            return null
        }
        return null
    }

    /**
     * Intercepts outgoing service bind/start intents targeting GMS.
     *
     * @return true if the intent targets GMS and was blocked; false to allow standard dispatch.
     */
    fun interceptServiceIntent(intent: Intent?, envId: String): Boolean {
        if (intent == null) return false
        val targetPkg = intent.`package` ?: intent.component?.packageName
        if (targetPkg != null && shouldBlockPackageQuery(targetPkg, envId)) {
            Log.d(TAG, "Intercepted and suppressed GMS Service Intent targeting '$targetPkg' in env '$envId'")
            return true
        }
        return false
    }

    /**
     * Computes the availability status code for an environment.
     * Compatible with GoogleApiAvailability.isGooglePlayServicesAvailable().
     */
    fun checkAvailability(envId: String, hostGmsInstalled: Boolean = false): Int {
        val enabled = isGmsEnabled(envId)
        return when {
            !enabled -> SERVICE_MISSING // Code 1: Cleanly signals to guest app that GMS is not installed
            hostGmsInstalled -> SERVICE_SUCCESS // Code 0: GMS is present and enabled
            else -> SERVICE_MISSING
        }
    }

    /**
     * Clears all environment policies (used in tests or environment teardown).
     */
    fun clear() {
        environmentGmsPolicy.clear()
    }
}

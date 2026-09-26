package com.sandboxr.virtual.client.stub

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import androidx.activity.BackEventCompat
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.sandboxr.virtual.VirtualCore
import com.sandboxr.virtual.core.VClassLoader
import com.sandboxr.virtual.core.VContextImpl
import com.sandboxr.virtual.core.VEnvironment
import com.sandboxr.virtual.server.am.VActivityManagerService
import com.sandboxr.virtual.server.pm.VPackageManagerService
import java.io.File
import java.lang.reflect.Method

import androidx.fragment.app.FragmentActivity

/**
 * Host Activity container that loads and hosts guest Activity components in-process without system installation.
 * Implements Android 16 (API 36) edge-to-edge enforcement, fragment hosting, and predictive back gesture dispatch.
 */
open class StubActivity : FragmentActivity() {

    companion object {
        private const val TAG = "StubActivity"
    }

    class SingleTop : StubActivity()
    class SingleTask : StubActivity()
    class SingleInstance : StubActivity()

    private var guestActivity: Activity? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // Enforce Edge-to-Edge unconditionally (API 36+ requirement, windowOptOutEdgeToEdgeEnforcement removed)
        try {
            enableEdgeToEdge()
        } catch (t: Throwable) {
            Log.w(TAG, "enableEdgeToEdge fallback on current runtime environment", t)
        }
        super.onCreate(savedInstanceState)

        // Setup Predictive Back Dispatcher callback for API 34-36+ predictive back gesture
        setupPredictiveBackHandler()

        // Apply system window insets listener to ensure no status/nav bar overlap
        setupEdgeToEdgeInsets()

        val targetIntent = androidx.core.content.IntentCompat.getParcelableExtra(
            intent,
            VActivityManagerService.EXTRA_TARGET_INTENT,
            Intent::class.java
        )
        val envId = intent.getStringExtra(VActivityManagerService.EXTRA_ENV_ID)
        val targetPkg = intent.getStringExtra(VActivityManagerService.EXTRA_TARGET_PKG)
        val targetActivityClass = intent.getStringExtra(VActivityManagerService.EXTRA_TARGET_ACTIVITY)

        if (targetPkg == null || targetActivityClass == null || envId == null) {
            Log.e(TAG, "Missing virtual execution parameters. Finishing.")
            finish()
            return
        }

        try {
            val vCore = VirtualCore.get()
            val vpm = VPackageManagerService.get(this)
            val installedPkg = vpm.getPackageInfo(targetPkg, 0, envId)
            val appInfo = vpm.getApplicationInfo(targetPkg, 0, envId)

            if (installedPkg == null || appInfo == null) {
                Log.e(TAG, "Guest package $targetPkg not found in environment $envId")
                finish()
                return
            }

            val apkFile = File(appInfo.sourceDir)
            val env = vCore.getEnvironment(envId) ?: VEnvironment.create(this, envId, "Default", 0L)
            val packageDataDir = env.getPackageDataDir(targetPkg)

            // Dynamic ClassLoader for guest APK
            val classLoader = VClassLoader.create(apkFile, packageDataDir, classLoader)
            val vContext = VContextImpl(
                base = baseContext,
                environment = env,
                guestPackageName = targetPkg,
                guestClassLoader = classLoader,
                guestAppInfo = appInfo
            )

            // Instantiate guest activity
            val clazz = classLoader.loadClass(targetActivityClass)
            val activityInstance = clazz.getDeclaredConstructor().newInstance() as Activity
            guestActivity = activityInstance

            // Register active activity container with VActivityManagerService
            VActivityManagerService.get(this).registerActiveActivity(envId, targetPkg, this)

            // Attach context and transfer core activity tokens via reflection
            attachGuestActivity(activityInstance, vContext, targetIntent ?: intent)

            // Invoke guest onCreate
            val onCreateMethod: Method = Activity::class.java.getDeclaredMethod("onCreate", Bundle::class.java)
            onCreateMethod.isAccessible = true
            onCreateMethod.invoke(activityInstance, savedInstanceState)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch guest activity $targetActivityClass", e)
            finish()
        }
    }

    private fun setupEdgeToEdgeInsets() {
        try {
            val rootView = window?.decorView?.findViewById<ViewGroup>(android.R.id.content) ?: window?.decorView
            if (rootView != null) {
                ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
                    val bars = insets.getInsets(
                        WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
                    )
                    v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
                    insets
                }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to attach edge-to-edge window insets listener", t)
        }
    }

    private fun setupPredictiveBackHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackStarted(backEvent: BackEventCompat) {
                Log.d(TAG, "Predictive back started (progress: ${backEvent.progress})")
            }

            override fun handleOnBackProgressed(backEvent: BackEventCompat) {
                Log.d(TAG, "Predictive back progressed: ${backEvent.progress}")
            }

            override fun handleOnBackPressed() {
                val guest = guestActivity
                if (guest != null) {
                    try {
                        if (guest is ComponentActivity) {
                            guest.onBackPressedDispatcher.onBackPressed()
                        } else {
                            val onBackPressedMethod = Activity::class.java.getDeclaredMethod("onBackPressed")
                            onBackPressedMethod.isAccessible = true
                            onBackPressedMethod.invoke(guest)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Exception invoking onBackPressed on guest activity", e)
                        finish()
                    }
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }

            override fun handleOnBackCancelled() {
                Log.d(TAG, "Predictive back cancelled")
            }
        })
    }

    private fun attachGuestActivity(guest: Activity, vContext: VContextImpl, targetIntent: Intent) {
        try {
            // Transfer base Context
            val attachBaseContextMethod = Activity::class.java.getDeclaredMethod("attachBaseContext", android.content.Context::class.java)
            attachBaseContextMethod.isAccessible = true
            attachBaseContextMethod.invoke(guest, vContext)

            // Transfer intent
            val setIntentMethod = Activity::class.java.getDeclaredMethod("setIntent", Intent::class.java)
            setIntentMethod.isAccessible = true
            setIntentMethod.invoke(guest, targetIntent)

            // Transfer Window and WindowManager so setContentView and view operations succeed
            setField(Activity::class.java, guest, "mWindow", this.window)
            setField(Activity::class.java, guest, "mWindowManager", this.windowManager)

            try {
                val appField = Activity::class.java.getDeclaredField("mApplication")
                appField.isAccessible = true
                appField.set(guest, application)
            } catch (_: Throwable) {}

            try {
                val threadField = Activity::class.java.getDeclaredField("mMainThread")
                threadField.isAccessible = true
                threadField.set(guest, threadField.get(this))
            } catch (_: Throwable) {}

            try {
                val instrField = Activity::class.java.getDeclaredField("mInstrumentation")
                instrField.isAccessible = true
                instrField.set(guest, instrField.get(this))
            } catch (_: Throwable) {}

            try {
                val tokenField = Activity::class.java.getDeclaredField("mToken")
                tokenField.isAccessible = true
                tokenField.set(guest, tokenField.get(this))
            } catch (_: Throwable) {}

            // Copy window and title if available
            guest.title = title
        } catch (e: Exception) {
            Log.w(TAG, "Partial attachment of guest activity context", e)
        }
    }

    private fun setField(clazz: Class<*>, instance: Any, fieldName: String, value: Any?) {
        try {
            val field = clazz.getDeclaredField(fieldName)
            field.isAccessible = true
            field.set(instance, value)
        } catch (_: Throwable) {}
    }

    override fun onStart() {
        super.onStart()
        invokeGuestLifecycle("onStart")
    }

    override fun onResume() {
        super.onResume()
        invokeGuestLifecycle("onResume")
    }

    override fun onPause() {
        invokeGuestLifecycle("onPause")
        super.onPause()
    }

    override fun onStop() {
        invokeGuestLifecycle("onStop")
        super.onStop()
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        guestActivity?.let { guest ->
            try {
                guest.onConfigurationChanged(newConfig)
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to dispatch onConfigurationChanged to guest activity", t)
            }
        }
    }

    override fun onDestroy() {
        val envId = intent.getStringExtra(VActivityManagerService.EXTRA_ENV_ID)
        val targetPkg = intent.getStringExtra(VActivityManagerService.EXTRA_TARGET_PKG)
        if (envId != null && targetPkg != null) {
            VActivityManagerService.get(this).unregisterActiveActivity(envId, targetPkg, this)
        }
        invokeGuestLifecycle("onDestroy")
        super.onDestroy()
    }

    private fun invokeGuestLifecycle(methodName: String) {
        guestActivity?.let {
            try {
                val method = Activity::class.java.getDeclaredMethod(methodName)
                method.isAccessible = true
                method.invoke(it)
            } catch (e: Exception) {
                Log.w(TAG, "Failed lifecycle dispatch $methodName on guest", e)
            }
        }
    }
}

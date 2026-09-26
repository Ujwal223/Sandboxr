package com.sandboxr.virtual.client.hook

import android.content.Context
import android.content.Intent
import com.sandboxr.virtual.VirtualCore
import com.sandboxr.virtual.server.am.VActivityManagerService
import java.lang.reflect.Method

/**
 * Intercepts calls to IActivityManager and IActivityTaskManager to redirect component lifecycle to StubActivity.
 */
class ActivityManagerHook(private val hostContext: Context) : BinderHook("activity") {

    override fun onIntercept(method: Method, args: Array<out Any>): HookResult? {
        val vCore = VirtualCore.get()
        val currentEnvId = vCore.currentEnvironmentId ?: return null
        val vam = VActivityManagerService.get(hostContext)

        if (method.name.startsWith("startActivity")) {
            // Find Intent among arguments
            for (i in args.indices) {
                val arg = args[i]
                if (arg is Intent) {
                    val targetPkg = arg.component?.packageName ?: arg.`package`
                    if (targetPkg != null && targetPkg == hostContext.packageName &&
                        arg.component?.className != "com.sandboxr.virtual.client.stub.StubActivity") {
                        throw SecurityException("Sandbox escape: guest application is not permitted to launch host activities ($targetPkg)")
                    }
                    val stubIntent = vam.createStubIntent(arg, currentEnvId)
                    if (stubIntent != null) {
                        // Rewritten intent directed to StubActivity container
                        val mutableArgs = args.toMutableList()
                        mutableArgs[i] = stubIntent
                        val res = method.invoke(originalInterface, *mutableArgs.toTypedArray())
                        return HookResult.handled(res)
                    }
                }
            }
        }
        return null
    }
}

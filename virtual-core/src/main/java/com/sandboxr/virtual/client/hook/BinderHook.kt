package com.sandboxr.virtual.client.hook

import android.os.IBinder
import android.os.IInterface
import android.util.Log
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

/**
 * Base abstract class for Binder IPC proxy interception.
 */
abstract class BinderHook(val serviceName: String) : InvocationHandler {

    companion object {
        private const val TAG = "BinderHook"
    }

    var originalBinder: IBinder? = null
        protected set
    var originalInterface: Any? = null
        protected set
    var proxiedInterface: Any? = null
        protected set

    /**
     * Initializes the hook by wrapping the existing IBinder interface with dynamic proxy.
     */
    fun install(baseBinder: IBinder, interfaceClass: Class<*>): IBinder {
        this.originalBinder = baseBinder

        // Extract original IInterface through asInterface method
        val stubClass = try {
            Class.forName("${interfaceClass.name}\$Stub")
        } catch (e: Exception) {
            null
        }

        if (stubClass != null) {
            val asInterfaceMethod = stubClass.getMethod("asInterface", IBinder::class.java)
            originalInterface = asInterfaceMethod.invoke(null, baseBinder)
        }

        // Create proxy for the interface
        proxiedInterface = Proxy.newProxyInstance(
            interfaceClass.classLoader,
            arrayOf(interfaceClass),
            this
        )

        // Return a proxy IBinder whose queryLocalInterface returns our proxied interface only for matching descriptor
        return Proxy.newProxyInstance(
            IBinder::class.java.classLoader,
            arrayOf(IBinder::class.java),
            InvocationHandler { _, method, args ->
                if (method.name == "queryLocalInterface") {
                    val descriptor = args?.getOrNull(0) as? String
                    if (descriptor == null || descriptor == interfaceClass.name) {
                        return@InvocationHandler proxiedInterface
                    }
                    return@InvocationHandler null
                }
                method.invoke(baseBinder, *(args ?: emptyArray()))
            }
        ) as IBinder
    }

    override fun invoke(proxy: Any?, method: Method, args: Array<out Any>?): Any? {
        val safeArgs = args ?: emptyArray()
        try {
            val intercepted = onIntercept(method, safeArgs)
            if (intercepted != null) {
                return intercepted.value
            }
            return method.invoke(originalInterface, *safeArgs)
        } catch (e: Exception) {
            val cause = e.cause ?: e
            Log.w(TAG, "Exception during hook invocation for ${method.name}", cause)
            throw cause
        }
    }

    /**
     * Override to intercept specific Binder calls. Return HookResult.handled(value) if handled.
     */
    open fun onIntercept(method: Method, args: Array<out Any>): HookResult? = null

    data class HookResult(val value: Any?) {
        companion object {
            fun handled(value: Any?) = HookResult(value)
        }
    }
}

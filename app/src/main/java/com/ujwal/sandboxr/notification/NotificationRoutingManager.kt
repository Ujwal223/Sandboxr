package com.ujwal.sandboxr.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Routing decision for an intercepted notification.
 */
enum class NotificationRouteDecision {
    /**
     * Allowed to post directly to the Android system notification shade.
     */
    POST_TO_SHADE,

    /**
     * Suppressed from the Android system shade.
     * Buffered internally and increments the environment launcher badge dot.
     */
    SUPPRESS_AND_BADGE,

    /**
     * Completely dropped.
     */
    BLOCKED
}

/**
 * Intercepted notification metadata record.
 */
data class InterceptedNotification(
    val id: Int,
    val tag: String?,
    val packageName: String,
    val envId: String,
    val notification: Notification,
    val postTime: Long = System.currentTimeMillis()
)

/**
 * Result of the notification routing engine.
 */
data class RoutingResult(
    val decision: NotificationRouteDecision,
    val namespacedChannelId: String,
    val unreadBadgeCount: Int
)

/**
 * Manages per-environment notification routing, isolation, and badge dot state.
 *
 * Enforces PRD Section 7.2 (notifIsolation):
 * When notifIsolation is enabled, notifications from an inactive environment are suppressed
 * from the Android system notification shade and held in an in-memory queue while
 * incrementing launcher badge dots. When the user switches to that environment, notifications
 * can be seamlessly delivered.
 */
class NotificationRoutingManager(
    private val hostContext: Context,
    private val notificationManager: NotificationManager? = hostContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
) {

    companion object {
        private const val TAG = "NotificationRouting"
        const val CHANNEL_PREFIX = "sandboxr_env_"

        @Volatile
        private var instance: NotificationRoutingManager? = null

        fun get(context: Context): NotificationRoutingManager {
            return instance ?: synchronized(this) {
                instance ?: NotificationRoutingManager(context.applicationContext ?: context).also { instance = it }
            }
        }

        fun createForTesting(context: Context, notificationManager: NotificationManager? = null): NotificationRoutingManager {
            return NotificationRoutingManager(context, notificationManager)
        }
    }

    // Current active environment UUID
    @Volatile
    var activeEnvironmentId: String = "system_default_environment"
        private set

    // Key: envId -> Boolean (default: true per PRD Section 7.2)
    private val isolationPolicies = ConcurrentHashMap<String, Boolean>()

    // Key: envId -> List<InterceptedNotification>
    private val bufferedNotifications = ConcurrentHashMap<String, CopyOnWriteArrayList<InterceptedNotification>>()

    // Key: envId -> List<InterceptedNotification> of currently active shade notifications
    private val activeShadeNotifications = ConcurrentHashMap<String, CopyOnWriteArrayList<InterceptedNotification>>()

    // Reactive unread count badge per environment: envId -> unread count
    private val _unreadCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val unreadCounts: StateFlow<Map<String, Int>> = _unreadCounts.asStateFlow()

    fun setNotificationIsolation(envId: String, enabled: Boolean) {
        isolationPolicies[envId] = enabled
    }

    fun isNotificationIsolationEnabled(envId: String): Boolean {
        return isolationPolicies[envId] ?: true
    }

    fun setActiveEnvironment(newEnvId: String) {
        if (activeEnvironmentId == newEnvId) return
        val previousEnvId = activeEnvironmentId
        activeEnvironmentId = newEnvId

        Log.i(TAG, "Active environment changed from '$previousEnvId' to '$newEnvId'")
        onActiveEnvironmentChanged(previousEnvId, newEnvId)
    }

    /**
     * Intercepts and routes an incoming guest application notification.
     */
    fun routeNotification(
        envId: String,
        packageName: String,
        id: Int,
        tag: String?,
        notification: Notification
    ): RoutingResult {
        val isCurrentEnv = (envId == activeEnvironmentId)
        val isolationEnabled = isNotificationIsolationEnabled(envId)

        val intercepted = InterceptedNotification(
            id = id,
            tag = tag,
            packageName = packageName,
            envId = envId,
            notification = notification
        )

        val namespacedChannel = getNamespacedChannelId(envId, notification.channelId)

        return if (isCurrentEnv || !isolationEnabled) {
            // Post to system shade
            val activeList = activeShadeNotifications.computeIfAbsent(envId) { CopyOnWriteArrayList() }
            activeList.removeIf { it.id == id && it.tag == tag }
            activeList.add(intercepted)

            // Ensure channel exists on API 26+
            ensureNotificationChannel(namespacedChannel, envId)

            Log.d(TAG, "Routing notification from '$packageName' in active env '$envId' -> POST_TO_SHADE")
            RoutingResult(
                decision = NotificationRouteDecision.POST_TO_SHADE,
                namespacedChannelId = namespacedChannel,
                unreadBadgeCount = getUnreadCount(envId)
            )
        } else {
            // Inactive environment with isolation enabled: Suppress and buffer
            val buffer = bufferedNotifications.computeIfAbsent(envId) { CopyOnWriteArrayList() }
            buffer.removeIf { it.id == id && it.tag == tag }
            buffer.add(intercepted)

            updateUnreadCount(envId)

            Log.i(TAG, "Suppressed notification from '$packageName' in inactive env '$envId' -> SUPPRESS_AND_BADGE")
            RoutingResult(
                decision = NotificationRouteDecision.SUPPRESS_AND_BADGE,
                namespacedChannelId = namespacedChannel,
                unreadBadgeCount = getUnreadCount(envId)
            )
        }
    }

    /**
     * Handles environment transition: suppresses previous environment's active notifications
     * if isolated, and flushes buffered notifications for the new environment.
     */
    private fun onActiveEnvironmentChanged(previousEnvId: String, newEnvId: String) {
        // 1. Suppress previous environment if isolation is enabled
        if (isNotificationIsolationEnabled(previousEnvId)) {
            val activeList = activeShadeNotifications.remove(previousEnvId)
            if (activeList != null && activeList.isNotEmpty()) {
                // Cancel them from the Android system shade
                activeList.forEach { item ->
                    notificationManager?.cancel(item.tag, item.id)
                }
                // Move them to buffered
                val buffer = bufferedNotifications.computeIfAbsent(previousEnvId) { CopyOnWriteArrayList() }
                buffer.addAll(activeList)
                updateUnreadCount(previousEnvId)
                Log.d(TAG, "Suppressed ${activeList.size} notifications for inactive env '$previousEnvId'")
            }
        }

        // 2. Deliver buffered notifications for newly active environment
        val pending = bufferedNotifications.remove(newEnvId)
        if (pending != null && pending.isNotEmpty()) {
            val activeList = activeShadeNotifications.computeIfAbsent(newEnvId) { CopyOnWriteArrayList() }
            pending.forEach { item ->
                activeList.add(item)
                val channelId = getNamespacedChannelId(newEnvId, item.notification.channelId)
                ensureNotificationChannel(channelId, newEnvId)
                notificationManager?.notify(item.tag, item.id, item.notification)
            }
            updateUnreadCount(newEnvId)
            Log.d(TAG, "Flushed ${pending.size} buffered notifications to shade for active env '$newEnvId'")
        }
    }

    fun getUnreadCount(envId: String): Int {
        return bufferedNotifications[envId]?.size ?: 0
    }

    fun getBufferedNotifications(envId: String): List<InterceptedNotification> {
        return bufferedNotifications[envId]?.toList() ?: emptyList()
    }

    fun getActiveShadeNotifications(envId: String): List<InterceptedNotification> {
        return activeShadeNotifications[envId]?.toList() ?: emptyList()
    }

    fun clearAllForEnvironment(envId: String) {
        val active = activeShadeNotifications.remove(envId)
        active?.forEach { notificationManager?.cancel(it.tag, it.id) }
        bufferedNotifications.remove(envId)
        updateUnreadCount(envId)
    }

    private fun updateUnreadCount(envId: String) {
        val current = _unreadCounts.value.toMutableMap()
        val count = getUnreadCount(envId)
        if (count > 0) {
            current[envId] = count
        } else {
            current.remove(envId)
        }
        _unreadCounts.value = current
    }

    private fun getNamespacedChannelId(envId: String, originalChannelId: String?): String {
        val channelSuffix = originalChannelId ?: "default"
        return "${CHANNEL_PREFIX}${envId}_$channelSuffix"
    }

    private fun ensureNotificationChannel(channelId: String, envId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && notificationManager != null) {
            val existing = notificationManager.getNotificationChannel(channelId)
            if (existing == null) {
                val channel = NotificationChannel(
                    channelId,
                    "Sandboxr ($envId)",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Notifications isolated for environment $envId"
                }
                notificationManager.createNotificationChannel(channel)
            }
        }
    }
}

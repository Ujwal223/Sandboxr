package com.ujwal.sandboxr.notification

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NotificationRoutingManagerTest {

    private lateinit var context: Context
    private lateinit var notificationManager: NotificationManager
    private lateinit var routingManager: NotificationRoutingManager

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        routingManager = NotificationRoutingManager.createForTesting(context, notificationManager)
    }

    private fun createDummyNotification(title: String, text: String): Notification {
        return Notification.Builder(context, "default_channel")
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
    }

    @Test
    fun testActiveEnvironmentNotificationPostedToShade() {
        val activeEnv = "env-uuid-work"
        routingManager.setActiveEnvironment(activeEnv)

        val notif = createDummyNotification("Work Slack", "New message from colleague")
        val result = routingManager.routeNotification(
            envId = activeEnv,
            packageName = "com.work.slack",
            id = 101,
            tag = null,
            notification = notif
        )

        // Must be routed directly to system notification shade
        assertEquals(NotificationRouteDecision.POST_TO_SHADE, result.decision)
        assertTrue(result.namespacedChannelId.startsWith("sandboxr_env_env-uuid-work"))
        assertEquals(0, result.unreadBadgeCount)

        val shadeNotifs = routingManager.getActiveShadeNotifications(activeEnv)
        assertEquals(1, shadeNotifs.size)
        assertEquals("com.work.slack", shadeNotifs[0].packageName)
    }

    @Test
    fun testInactiveEnvironmentWithIsolationSuppressedFromShadeAndBadged() {
        val currentActiveEnv = "env-uuid-personal"
        val inactiveEnv = "env-uuid-secret"

        routingManager.setActiveEnvironment(currentActiveEnv)
        // Ensure isolation is enabled for secret environment
        assertTrue(routingManager.isNotificationIsolationEnabled(inactiveEnv))

        val secretNotif1 = createDummyNotification("Signal Secret", "Encrypted message 1")
        val result1 = routingManager.routeNotification(
            envId = inactiveEnv,
            packageName = "org.thoughtcrime.securesms",
            id = 201,
            tag = null,
            notification = secretNotif1
        )

        // 1. Must NOT appear on system shade; must be suppressed and badged
        assertEquals(NotificationRouteDecision.SUPPRESS_AND_BADGE, result1.decision)
        assertEquals(1, result1.unreadBadgeCount)
        assertEquals(1, routingManager.getUnreadCount(inactiveEnv))

        // 2. Active shade for inactive env must be empty
        assertEquals(0, routingManager.getActiveShadeNotifications(inactiveEnv).size)

        // 3. Must be held in buffer
        val buffered = routingManager.getBufferedNotifications(inactiveEnv)
        assertEquals(1, buffered.size)
        assertEquals("org.thoughtcrime.securesms", buffered[0].packageName)

        // 4. Second notification increments badge count
        val secretNotif2 = createDummyNotification("Signal Secret", "Encrypted message 2")
        val result2 = routingManager.routeNotification(
            envId = inactiveEnv,
            packageName = "org.thoughtcrime.securesms",
            id = 202,
            tag = null,
            notification = secretNotif2
        )
        assertEquals(2, result2.unreadBadgeCount)
        assertEquals(2, routingManager.getUnreadCount(inactiveEnv))
    }

    @Test
    fun testInactiveEnvironmentWithIsolationDisabledPostsToShade() {
        val currentActiveEnv = "env-uuid-alpha"
        val inactiveEnv = "env-uuid-beta"

        routingManager.setActiveEnvironment(currentActiveEnv)
        routingManager.setNotificationIsolation(inactiveEnv, false)

        val notif = createDummyNotification("Utility Alert", "Battery saver update")
        val result = routingManager.routeNotification(
            envId = inactiveEnv,
            packageName = "com.android.utility",
            id = 301,
            tag = null,
            notification = notif
        )

        // Allowed to post to shade because isolation was explicitly disabled
        assertEquals(NotificationRouteDecision.POST_TO_SHADE, result.decision)
    }

    @Test
    fun testSwitchingActiveEnvironmentFlushesBufferedNotifications() {
        val envA = "env-uuid-A"
        val envB = "env-uuid-B"

        routingManager.setActiveEnvironment(envA)

        // Post notification to inactive envB
        val notifB = createDummyNotification("Bank App", "Transaction Alert")
        routingManager.routeNotification(
            envId = envB,
            packageName = "com.banking.app",
            id = 401,
            tag = null,
            notification = notifB
        )

        assertEquals(1, routingManager.getUnreadCount(envB))
        assertEquals(0, routingManager.getActiveShadeNotifications(envB).size)

        // Now switch active environment to envB
        routingManager.setActiveEnvironment(envB)

        // Buffered notifications must now be flushed to active shade
        assertEquals(0, routingManager.getUnreadCount(envB))
        val activeNotifs = routingManager.getActiveShadeNotifications(envB)
        assertEquals(1, activeNotifs.size)
        assertEquals("com.banking.app", activeNotifs[0].packageName)
    }

    @Test
    fun testClearAllForEnvironment() {
        val env = "env-uuid-clear"
        routingManager.setActiveEnvironment(env)

        val notif = createDummyNotification("Test", "Test notification")
        routingManager.routeNotification(env, "com.test", 501, null, notif)
        assertEquals(1, routingManager.getActiveShadeNotifications(env).size)

        routingManager.clearAllForEnvironment(env)
        assertEquals(0, routingManager.getActiveShadeNotifications(env).size)
        assertEquals(0, routingManager.getUnreadCount(env))
    }
}

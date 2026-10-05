/*
 * Copyright (C) 2026 The Android Open Source Project
 * Copyright (C) 2026 Sandboxr Platform
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sandboxr.launcher.notification

import android.app.Notification
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.os.Process
import android.os.UserHandle
import android.service.notification.StatusBarNotification
import com.sandboxr.launcher.dot.DotInfo
import com.sandboxr.launcher.dot.DotRenderer
import com.sandboxr.launcher.dot.FolderDotInfo
import com.sandboxr.launcher.settings.NotificationDotsPreference
import com.sandboxr.launcher.util.PackageUserKey
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NotificationSystemTest {

    private lateinit var context: Context
    private val myUser: UserHandle = Process.myUserHandle()

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        NotificationListener.clearListenersForTesting()
        NotificationListener.setInstanceForTesting(null, false)
        NotificationRepository.setInstanceForTesting(NotificationRepository())
    }

    @After
    fun tearDown() {
        NotificationListener.clearListenersForTesting()
        NotificationListener.setInstanceForTesting(null, false)
    }

    @Test
    fun testDotInfoBasicOperations() {
        val dotInfo = DotInfo()
        assertFalse(dotInfo.hasDot())
        assertEquals(0, dotInfo.getNotificationCount())

        val key1 = NotificationKeyData(
            notificationKey = "key_1",
            shortcutId = "shortcut_1",
            count = 3
        )
        val added = dotInfo.addOrUpdateNotificationKey(key1)
        assertTrue(added)
        assertTrue(dotInfo.hasDot())
        assertEquals(3, dotInfo.getNotificationCount())

        // Update count of same key
        val key1Updated = NotificationKeyData(
            notificationKey = "key_1",
            shortcutId = "shortcut_1",
            count = 5
        )
        val updated = dotInfo.addOrUpdateNotificationKey(key1Updated)
        assertTrue(updated)
        assertEquals(5, dotInfo.getNotificationCount())

        // Add second key
        val key2 = NotificationKeyData(
            notificationKey = "key_2",
            shortcutId = null,
            count = 2
        )
        dotInfo.addOrUpdateNotificationKey(key2)
        assertEquals(7, dotInfo.getNotificationCount())

        // Remove key1
        val removed1 = dotInfo.removeNotificationKey(key1)
        assertTrue(removed1)
        assertEquals(2, dotInfo.getNotificationCount())
        assertTrue(dotInfo.hasDot())

        // Remove key2
        val removed2 = dotInfo.removeNotificationKey(key2)
        assertTrue(removed2)
        assertEquals(0, dotInfo.getNotificationCount())
        assertFalse(dotInfo.hasDot())
    }

    @Test
    fun testDotInfoCapMaxCount() {
        val dotInfo = DotInfo()
        val keyLarge = NotificationKeyData(
            notificationKey = "huge_key",
            count = 5000
        )
        dotInfo.addOrUpdateNotificationKey(keyLarge)
        assertEquals(DotInfo.MAX_COUNT, dotInfo.getNotificationCount())
    }

    @Test
    fun testFolderDotInfoAggregation() {
        val folderDot = FolderDotInfo()
        assertFalse(folderDot.hasDot())
        assertEquals(0, folderDot.getNotificationCount())

        val item1 = DotInfo().apply {
            addOrUpdateNotificationKey(NotificationKeyData("item1_key1", count = 2))
            addOrUpdateNotificationKey(NotificationKeyData("item1_key2", count = 3))
        }

        val item2 = DotInfo().apply {
            addOrUpdateNotificationKey(NotificationKeyData("item2_key1", count = 4))
        }

        folderDot.addDotInfo(item1)
        assertTrue(folderDot.hasDot())
        assertEquals(5, folderDot.getNotificationCount())

        folderDot.addDotInfo(item2)
        assertEquals(9, folderDot.getNotificationCount())

        folderDot.subtractDotInfo(item1)
        assertTrue(folderDot.hasDot())
        assertEquals(4, folderDot.getNotificationCount())

        folderDot.subtractDotInfo(item2)
        assertFalse(folderDot.hasDot())
        assertEquals(0, folderDot.getNotificationCount())
    }

    @Test
    fun testDotRendererDrawing() {
        val renderer = DotRenderer(size = 24)
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val params = DotRenderer.DrawParams().apply {
            iconBounds.set(10, 10, 90, 90)
            dotColor = Color.RED
            scale = 1.0f
        }

        // Draw normal dot with count
        renderer.draw(canvas, params, count = 5)

        // Draw with scale <= 0 should do nothing
        params.scale = 0f
        renderer.draw(canvas, params, count = 5)

        // Draw 99+ count
        params.scale = 1.0f
        renderer.draw(canvas, params, count = 150)
        assertNotNull(bitmap)
    }

    @Test
    fun testNotificationKeyDataEqualityAndExtraction() {
        val keyData1 = NotificationKeyData("notif_1", "sc1", 2, arrayOf("personA"))
        val keyData2 = NotificationKeyData("notif_1", "sc1", 5, arrayOf("personB"))
        val keyData3 = NotificationKeyData("notif_2", "sc1", 2, arrayOf("personA"))

        // Equality matches by notificationKey
        assertEquals(keyData1, keyData2)
        assertEquals(keyData1.hashCode(), keyData2.hashCode())
        assertFalse(keyData1 == keyData3)

        // Extraction from StatusBarNotification
        val extras = Bundle().apply {
            putStringArray(Notification.EXTRA_PEOPLE, arrayOf("person://key_z", "person://key_a"))
        }
        val notif = Notification.Builder(context, "test_channel")
            .setContentTitle("Test Notification")
            .setContentText("Details")
            .setNumber(3)
            .setShortcutId("shortcut_direct")
            .setExtras(extras)
            .build()

        val sbn = StatusBarNotification(
            "com.example.chat",
            "com.example.chat",
            101,
            "tag",
            Process.myUid(),
            1001,
            0,
            notif,
            myUser,
            System.currentTimeMillis()
        )

        val extracted = NotificationKeyData.fromNotification(sbn)
        assertEquals(sbn.key, extracted.notificationKey)
        assertEquals("shortcut_direct", extracted.shortcutId)
        assertEquals(3, extracted.count)
        assertEquals(2, extracted.personKeys.size)
        // Person keys should be sorted
        assertEquals("person://key_a", extracted.personKeys[0])
        assertEquals("person://key_z", extracted.personKeys[1])
    }

    @Test
    fun testNotificationRepositoryUpdateStream() {
        val repo = NotificationRepository.getInstance()
        var lastUpdatedKey: PackageUserKey? = null

        val closeable = repo.updateStream.forEach(Runnable::run) { predicate ->
            val pkg = PackageUserKey("com.example.test", myUser)
            if (predicate.test(pkg)) {
                lastUpdatedKey = pkg
            }
        }

        val testKey = PackageUserKey("com.example.test", myUser)
        val dotInfo = DotInfo().apply {
            addOrUpdateNotificationKey(NotificationKeyData("k1", count = 1))
        }

        repo.dispatchUpdate(mapOf(testKey to dotInfo)) { it == testKey }
        assertEquals(testKey, lastUpdatedKey)
        assertEquals(dotInfo, repo.packageUserToDotInfos[testKey])

        closeable.close()
    }

    @Test
    fun testNotificationListenerFiltering() {
        val ongoingNotif = Notification.Builder(context, "test_channel")
            .setContentTitle("Ongoing Music")
            .setFlag(Notification.FLAG_ONGOING_EVENT, true)
            .build()

        val ongoingSbn = StatusBarNotification(
            "com.example.music",
            "com.example.music",
            1,
            null,
            Process.myUid(),
            101,
            0,
            ongoingNotif,
            myUser,
            System.currentTimeMillis()
        )

        assertTrue(NotificationListener.shouldBeFiltered(ongoingSbn))

        val regularNotif = Notification.Builder(context, "test_channel")
            .setContentTitle("New Message")
            .build()

        val regularSbn = StatusBarNotification(
            "com.example.msg",
            "com.example.msg",
            2,
            null,
            Process.myUid(),
            102,
            0,
            regularNotif,
            myUser,
            System.currentTimeMillis()
        )

        assertFalse(NotificationListener.shouldBeFiltered(regularSbn))
    }

    @Test
    fun testNotificationListenerCallbacksAndPosting() {
        var postedReceived: NotificationKeyData? = null
        var removedReceived: NotificationKeyData? = null
        var fullRefreshReceived: Map<PackageUserKey, DotInfo>? = null

        val listener = object : NotificationListener.NotificationsChangedListener {
            override fun onNotificationPosted(packageUserKey: PackageUserKey, notificationKey: NotificationKeyData) {
                postedReceived = notificationKey
            }

            override fun onNotificationRemoved(packageUserKey: PackageUserKey, notificationKey: NotificationKeyData) {
                removedReceived = notificationKey
            }

            override fun onNotificationFullRefresh(packageUserToDotInfos: Map<PackageUserKey, DotInfo>) {
                fullRefreshReceived = packageUserToDotInfos
            }
        }

        NotificationListener.addNotificationsChangedListener(listener)

        val service = NotificationListener()
        NotificationListener.setInstanceForTesting(service, connected = true)
        assertTrue(NotificationListener.isConnected())
        assertEquals(service, NotificationListener.getInstanceIfConnected())

        val notif = Notification.Builder(context, "channel")
            .setContentTitle("Message")
            .setNumber(1)
            .build()

        val sbn = StatusBarNotification(
            "com.test.app",
            "com.test.app",
            1,
            null,
            Process.myUid(),
            1,
            0,
            notif,
            myUser,
            System.currentTimeMillis()
        )

        // Post notification
        service.onNotificationPosted(sbn)
        ShadowLooper.idleMainLooper()

        assertNotNull(postedReceived)
        assertEquals(sbn.key, postedReceived?.notificationKey)

        val repo = NotificationRepository.getInstance()
        val pkgKey = PackageUserKey("com.test.app", myUser)
        assertTrue(repo.packageUserToDotInfos.containsKey(pkgKey))
        assertEquals(1, repo.packageUserToDotInfos[pkgKey]?.getNotificationCount())

        // Remove notification
        service.onNotificationRemoved(sbn)
        ShadowLooper.idleMainLooper()

        assertNotNull(removedReceived)
        assertEquals(sbn.key, removedReceived?.notificationKey)
        assertFalse(repo.packageUserToDotInfos.containsKey(pkgKey))

        NotificationListener.removeNotificationsChangedListener(listener)
    }

    @Test
    fun testNotificationDotsPreference() {
        val pref = NotificationDotsPreference(context)
        assertNotNull(pref.title)
        assertNotNull(pref.summary)

        // Verify helper methods exist and operate safely
        pref.isNotificationListenerGranted()
        pref.isBadgingEnabled()
        pref.updateSummary()

        // Calling handlePreferenceClick should safely resolve intent
        pref.handlePreferenceClick()
    }
}

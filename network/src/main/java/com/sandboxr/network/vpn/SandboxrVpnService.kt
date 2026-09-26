package com.sandboxr.network.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.sandboxr.network.FirestackEngine

/**
 * Headless RethinkDNS-derived Local VpnService.
 *
 * Establishes a local TUN virtual interface (tun0) with zero external servers by default.
 * Captures outgoing IP packets, passes the native TUN file descriptor to [FirestackEngine],
 * and routes packets per-environment (Direct, SOCKS5, WireGuard, Blocked).
 *
 * All RethinkDNS UI and user-facing activities are stripped; the service is managed
 * entirely programmatically via [VpnController].
 */
class SandboxrVpnService : VpnService() {

    companion object {
        private const val TAG = "SandboxrVpnService"
        const val ACTION_START = "com.sandboxr.network.vpn.START"
        const val ACTION_STOP = "com.sandboxr.network.vpn.STOP"
        const val ACTION_PAUSE = "com.sandboxr.network.vpn.PAUSE"
        const val ACTION_RESUME = "com.sandboxr.network.vpn.RESUME"

        const val CHANNEL_ID = "sandboxr_network_channel"
        const val NOTIFICATION_ID = 2001

        const val LOCAL_IP_V4 = "10.111.222.1"
        const val LOCAL_IP_V6 = "fd00::1"
        const val VPN_MTU = 1500
        const val SESSION_NAME = "Sandboxr Local Firewall"

        @Volatile
        var isServiceActive: Boolean = false
            private set

        @Volatile
        var isPaused: Boolean = false
            private set
    }

    private var tunInterface: ParcelFileDescriptor? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        Log.i(TAG, "onStartCommand received action: $action")

        when (action) {
            ACTION_START -> startVpn()
            ACTION_STOP -> stopVpn()
            ACTION_PAUSE -> pauseVpn()
            ACTION_RESUME -> resumeVpn()
        }

        return START_STICKY
    }

    /**
     * Establishes the tun0 interface and connects it to FirestackEngine.
     */
    @Synchronized
    fun startVpn(): Boolean {
        if (isServiceActive && !isPaused) {
            Log.d(TAG, "VPN is already active")
            return true
        }

        try {
            // Start foreground notification to prevent OEM killer termination
            startForeground(NOTIFICATION_ID, buildForegroundNotification())

            // Build TUN interface
            val builder = Builder()
                .setSession(SESSION_NAME)
                .setMtu(VPN_MTU)
                .addAddress(LOCAL_IP_V4, 32)
                .addRoute("0.0.0.0", 0)
                .addDnsServer(LOCAL_IP_V4)

            // Prevent capturing host application in its own VPN tunnel
            try {
                builder.addDisallowedApplication(packageName)
            } catch (e: Exception) {
                Log.w(TAG, "Could not disallow host package from VPN: ${e.message}")
            }

            // IPv6 support
            try {
                builder.addAddress(LOCAL_IP_V6, 128)
                builder.addRoute("::", 0)
            } catch (e: Exception) {
                Log.w(TAG, "IPv6 address assignment ignored by host OS", e)
            }

            builder.setBlocking(true)

            val pfd = builder.establish()
            if (pfd == null) {
                Log.e(TAG, "Failed to establish tun interface: builder.establish() returned null")
                stopSelf()
                return false
            }

            tunInterface = pfd
            val fd = pfd.fd

            // Pass native TUN file descriptor to Firestack Go Engine
            val engineStarted = FirestackEngine.start(fd, VPN_MTU)
            if (!engineStarted) {
                Log.w(TAG, "FirestackEngine start returned false; continuing in headless mode")
            }

            isServiceActive = true
            isPaused = false
            Log.i(TAG, "Sandboxr TUN interface established successfully with fd: $fd")
            return true

        } catch (e: Exception) {
            Log.e(TAG, "Exception establishing VPN service", e)
            stopVpn()
            return false
        }
    }

    /**
     * Pauses the TUN interface to yield network socket control to an external VPN
     * (used by Pro Auto-VPN handoff protocol).
     */
    @Synchronized
    fun pauseVpn() {
        if (!isServiceActive || isPaused) return
        Log.i(TAG, "Pausing Sandboxr local VPN tun0 interface")

        FirestackEngine.stop()
        closeTun()
        isPaused = true
    }

    /**
     * Resumes the local TUN interface after external VPN disconnects.
     */
    @Synchronized
    fun resumeVpn() {
        if (!isServiceActive || !isPaused) return
        Log.i(TAG, "Resuming Sandboxr local VPN tun0 interface")
        isPaused = false
        startVpn()
    }

    /**
     * Halts the VPN service and closes the TUN interface.
     */
    @Synchronized
    fun stopVpn() {
        Log.i(TAG, "Stopping Sandboxr VPN service")
        FirestackEngine.stop()
        closeTun()
        isServiceActive = false
        isPaused = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onRevoke() {
        Log.w(TAG, "VPN permission revoked by system or user")
        stopVpn()
        super.onRevoke()
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }

    private fun closeTun() {
        tunInterface?.let {
            try {
                it.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error closing tun interface", e)
            }
        }
        tunInterface = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SANDBOXR Network Firewall",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Active userspace privacy firewall and ad blocking"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle("SANDBOXR Privacy Network")
            .setContentText("Local userspace firewall active")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }
}

package com.sandboxr.virtual.client.stub

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * Host Service container for dispatching guest background operations.
 */
class StubService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }
}

package com.ujwal.sandboxr.ui.model

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.ujwal.sandboxr.data.db.EnvironmentEntity
import com.ujwal.sandboxr.data.db.NetworkConfig
import com.sandboxr.launcher.ui.EnvironmentCardData
import com.sandboxr.launcher.ui.EnvironmentIconType
import com.sandboxr.network.model.NetworkMode

/**
 * Converts an Android [Drawable] into a Compose [ImageBitmap] safely.
 */
fun Drawable.toImageBitmap(): ImageBitmap {
    return try {
        if (this is BitmapDrawable && this.bitmap != null && !this.bitmap.isRecycled) {
            return this.bitmap.asImageBitmap()
        }
        val width = if (intrinsicWidth > 0) intrinsicWidth else 144
        val height = if (intrinsicHeight > 0) intrinsicHeight else 144
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        setBounds(0, 0, canvas.width, canvas.height)
        draw(canvas)
        bitmap.asImageBitmap()
    } catch (_: Throwable) {
        val fallback = Bitmap.createBitmap(144, 144, Bitmap.Config.ARGB_8888)
        fallback.asImageBitmap()
    }
}

/**
 * Maps a Room [EnvironmentEntity] to UI [EnvironmentCardData].
 */
fun EnvironmentEntity.toCardData(
    appCount: Int,
    isActive: Boolean,
    notificationCount: Int = 0
): EnvironmentCardData {
    val netMode = when (networkConfig) {
        NetworkConfig.DIRECT -> NetworkMode.DIRECT
        NetworkConfig.SOCKS5 -> NetworkMode.SOCKS5
        NetworkConfig.WIREGUARD -> NetworkMode.WIREGUARD
        NetworkConfig.BLOCKED -> NetworkMode.BLOCKED
    }

    val iconType = try {
        if (isSystem) {
            EnvironmentIconType.LOCK
        } else if (!iconEmoji.isNullOrBlank()) {
            EnvironmentIconType.valueOf(iconEmoji)
        } else {
            EnvironmentIconType.SHIELD
        }
    } catch (_: Exception) {
        if (isSystem) EnvironmentIconType.LOCK else EnvironmentIconType.SHIELD
    }

    return EnvironmentCardData(
        id = id,
        name = displayName,
        color = Color(colorTag),
        isSystem = isSystem,
        appCount = appCount,
        networkMode = netMode,
        hasNotification = notificationCount > 0,
        notificationCount = notificationCount,
        isActive = isActive,
        iconType = iconType
    )
}

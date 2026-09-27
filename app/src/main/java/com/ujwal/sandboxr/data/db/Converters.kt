package com.ujwal.sandboxr.data.db

import androidx.room.TypeConverter

/**
 * Room TypeConverters for Environment enums.
 */
class Converters {
    @TypeConverter
    fun fromNetworkConfig(config: NetworkConfig?): String? {
        return config?.name
    }

    @TypeConverter
    fun toNetworkConfig(value: String?): NetworkConfig? {
        return value?.let {
            try {
                NetworkConfig.valueOf(it)
            } catch (e: IllegalArgumentException) {
                NetworkConfig.DIRECT
            }
        }
    }

    @TypeConverter
    fun fromClipboardMode(mode: ClipboardMode?): String? {
        return mode?.name
    }

    @TypeConverter
    fun toClipboardMode(value: String?): ClipboardMode? {
        return value?.let {
            try {
                ClipboardMode.valueOf(it)
            } catch (e: IllegalArgumentException) {
                ClipboardMode.ISOLATED
            }
        }
    }
}

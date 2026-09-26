package com.sandboxr.virtual.core

import android.content.SharedPreferences
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Properties
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Isolated, file-backed SharedPreferences implementation for virtual guest packages.
 * Guarantees zero leakage into the host application SharedPreferences directory.
 */
class VSharedPreferences(private val file: File) : SharedPreferences {

    private val lock = Any()
    private val values = ConcurrentHashMap<String, Any>()
    private val listeners = CopyOnWriteArrayList<SharedPreferences.OnSharedPreferenceChangeListener>()

    init {
        loadFromFile()
    }

    private fun loadFromFile() {
        synchronized(lock) {
            if (!file.exists() || file.length() == 0L) return
            try {
                val props = Properties()
                FileInputStream(file).use { props.load(it) }
                for (key in props.stringPropertyNames()) {
                    val raw = props.getProperty(key) ?: continue
                    values[key] = deserializeValue(raw)
                }
            } catch (_: Exception) {}
        }
    }

    private fun saveToFile() {
        synchronized(lock) {
            try {
                file.parentFile?.mkdirs()
                val props = Properties()
                for ((k, v) in values) {
                    props.setProperty(k, serializeValue(v))
                }
                FileOutputStream(file).use { props.store(it, null) }
            } catch (_: Exception) {}
        }
    }

    private fun serializeValue(value: Any): String {
        return when (value) {
            is String -> "S:$value"
            is Int -> "I:$value"
            is Long -> "L:$value"
            is Float -> "F:$value"
            is Boolean -> "B:$value"
            is Set<*> -> "SET:" + value.filterIsInstance<String>().joinToString("\u0000")
            else -> "S:$value"
        }
    }

    private fun deserializeValue(raw: String): Any {
        if (raw.length < 2 || raw[1] != ':') return raw
        val type = raw.substring(0, raw.indexOf(':'))
        val data = raw.substring(raw.indexOf(':') + 1)
        return when (type) {
            "S" -> data
            "I" -> data.toIntOrNull() ?: 0
            "L" -> data.toLongOrNull() ?: 0L
            "F" -> data.toFloatOrNull() ?: 0f
            "B" -> data.toBoolean()
            "SET" -> if (data.isEmpty()) emptySet<String>() else data.split("\u0000").toSet()
            else -> data
        }
    }

    override fun getAll(): MutableMap<String, *> {
        return HashMap(values)
    }

    override fun getString(key: String?, defValue: String?): String? {
        val v = if (key != null) values[key] else null
        return (v as? String) ?: defValue
    }

    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? {
        val v = if (key != null) values[key] else null
        return ((v as? Set<String>)?.toMutableSet()) ?: defValues
    }

    override fun getInt(key: String?, defValue: Int): Int {
        val v = if (key != null) values[key] else null
        return (v as? Int) ?: defValue
    }

    override fun getLong(key: String?, defValue: Long): Long {
        val v = if (key != null) values[key] else null
        return (v as? Long) ?: defValue
    }

    override fun getFloat(key: String?, defValue: Float): Float {
        val v = if (key != null) values[key] else null
        return (v as? Float) ?: defValue
    }

    override fun getBoolean(key: String?, defValue: Boolean): Boolean {
        val v = if (key != null) values[key] else null
        return (v as? Boolean) ?: defValue
    }

    override fun contains(key: String?): Boolean {
        return key != null && values.containsKey(key)
    }

    override fun edit(): SharedPreferences.Editor {
        return EditorImpl()
    }

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener)
        }
    }

    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
        if (listener != null) {
            listeners.remove(listener)
        }
    }

    private inner class EditorImpl : SharedPreferences.Editor {
        private val modified = HashMap<String, Any?>()
        private var clear = false

        override fun putString(key: String?, value: String?): SharedPreferences.Editor {
            if (key != null) modified[key] = value
            return this
        }

        override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor {
            if (key != null) modified[key] = values?.toSet()
            return this
        }

        override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
            if (key != null) modified[key] = value
            return this
        }

        override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
            if (key != null) modified[key] = value
            return this
        }

        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
            if (key != null) modified[key] = value
            return this
        }

        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
            if (key != null) modified[key] = value
            return this
        }

        override fun remove(key: String?): SharedPreferences.Editor {
            if (key != null) modified[key] = this
            return this
        }

        override fun clear(): SharedPreferences.Editor {
            clear = true
            return this
        }

        override fun commit(): Boolean {
            apply()
            return true
        }

        override fun apply() {
            val notifyKeys = mutableListOf<String>()
            synchronized(lock) {
                if (clear) {
                    values.clear()
                }
                for ((k, v) in modified) {
                    if (v === this) {
                        values.remove(k)
                    } else if (v != null) {
                        values[k] = v
                    }
                    notifyKeys.add(k)
                }
                saveToFile()
            }
            for (key in notifyKeys) {
                for (listener in listeners) {
                    listener.onSharedPreferenceChanged(this@VSharedPreferences, key)
                }
            }
        }
    }
}

/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.sandboxr.launcher.logging

import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.util.Log
import android.util.Pair
import androidx.annotation.VisibleForTesting
import com.sandboxr.launcher.util.IOUtils
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.PrintWriter
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Wrapper around [Log] to allow writing to a file asynchronously from any thread.
 */
object FileLog {
    const val ENABLED: Boolean = true
    private const val FILE_NAME_PREFIX = "log-"
    /** 8 MB maximum per log file */
    private const val MAX_LOG_FILE_SIZE = (8 shl 20).toLong()
    const val LOG_DAYS: Int = 4

    private val DATE_FORMAT: DateFormat =
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)

    private var sHandlerThread: HandlerThread? = null
    private var sHandler: Handler? = null
    private var sLogsDirectory: File? = null

    @JvmStatic
    fun setDir(logsDir: File) {
        if (ENABLED) {
            synchronized(DATE_FORMAT) {
                // If the target directory changes, stop any active thread.
                if (sHandler != null && logsDir != sLogsDirectory) {
                    sHandlerThread?.quit()
                    sHandlerThread = null
                    sHandler = null
                }
            }
        }
        sLogsDirectory = logsDir
    }

    @JvmStatic
    fun d(tag: String?, msg: String?, e: Throwable? = null) {
        Log.d(tag, msg, e)
        print(tag, msg, e)
    }

    @JvmStatic
    fun d(tag: String?, msg: String) {
        Log.d(tag, msg)
        print(tag, msg)
    }

    @JvmStatic
    fun i(tag: String?, msg: String?, e: Throwable? = null) {
        Log.i(tag, msg, e)
        print(tag, msg, e)
    }

    @JvmStatic
    fun i(tag: String?, msg: String) {
        Log.i(tag, msg)
        print(tag, msg)
    }

    @JvmStatic
    fun w(tag: String?, msg: String?, e: Throwable? = null) {
        Log.w(tag, msg, e)
        print(tag, msg, e)
    }

    @JvmStatic
    fun w(tag: String?, msg: String) {
        Log.w(tag, msg)
        print(tag, msg)
    }

    @JvmStatic
    fun e(tag: String?, msg: String?, e: Throwable? = null) {
        Log.e(tag, msg, e)
        print(tag, msg, e)
    }

    @JvmStatic
    fun e(tag: String, msg: String?) {
        Log.e(tag, msg, null)
        print(tag, msg)
    }

    @JvmOverloads
    @JvmStatic
    fun print(tag: String?, msg: String?, e: Throwable? = null) {
        if (!ENABLED) {
            return
        }
        var out = "${DATE_FORMAT.format(Date())} $tag $msg"
        if (e != null) {
            out += "\n" + Log.getStackTraceString(e)
        }
        Message.obtain(handler, LogWriterCallback.MSG_WRITE, out).sendToTarget()
    }

    @get:VisibleForTesting
    val handler: Handler
        get() {
            synchronized(DATE_FORMAT) {
                var handler = sHandler
                if (handler == null) {
                    val thread = HandlerThread("sandboxr-file-logger")
                    thread.start()
                    sHandlerThread = thread
                    handler = Handler(thread.looper, LogWriterCallback())
                    sHandler = handler
                }
                return handler
            }
        }

    /**
     * Blocks until all pending logs are written to disk.
     *
     * @param out if not null, all persisted logs are copied to the writer.
     */
    @Throws(InterruptedException::class)
    @JvmStatic
    fun flushAll(out: PrintWriter?): Boolean {
        if (!ENABLED) {
            return false
        }
        val latch = CountDownLatch(1)
        Message.obtain(
            handler,
            LogWriterCallback.MSG_FLUSH,
            Pair.create<PrintWriter?, CountDownLatch?>(out, latch)
        ).sendToTarget()

        latch.await(2, TimeUnit.SECONDS)
        return latch.count == 0L
    }

    private fun dumpFile(out: PrintWriter, fileName: String) {
        val logFile = File(sLogsDirectory, fileName)
        if (logFile.exists()) {
            var reader: BufferedReader? = null
            try {
                reader = BufferedReader(FileReader(logFile))
                out.println()
                out.println("--- logfile: $fileName ---")
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    out.println(line)
                }
            } catch (_: Exception) {
                // Ignore file read errors during dump
            } finally {
                IOUtils.closeSilently(reader)
            }
        }
    }

    /**
     * Writes logs to rotating log files.
     */
    private class LogWriterCallback : Handler.Callback {
        private var mCurrentFileName: String? = null
        private var mCurrentWriter: PrintWriter? = null

        fun closeWriter() {
            IOUtils.closeSilently(mCurrentWriter)
            mCurrentWriter = null
        }

        override fun handleMessage(msg: Message): Boolean {
            if (sLogsDirectory == null || !ENABLED) {
                return true
            }
            when (msg.what) {
                MSG_WRITE -> {
                    val cal = Calendar.getInstance()
                    val fileName = FILE_NAME_PREFIX + (cal.get(Calendar.DAY_OF_YEAR) % LOG_DAYS)

                    if (fileName != mCurrentFileName) {
                        closeWriter()
                    }

                    try {
                        if (mCurrentWriter == null) {
                            mCurrentFileName = fileName
                            var append = false
                            val logsDir = sLogsDirectory ?: return true
                            if (!logsDir.exists()) {
                                logsDir.mkdirs()
                            }
                            val logFile = File(logsDir, fileName)
                            if (logFile.exists()) {
                                val modifiedTime = Calendar.getInstance()
                                modifiedTime.timeInMillis = logFile.lastModified()
                                modifiedTime.add(Calendar.HOUR, 36)
                                append = cal.before(modifiedTime) && logFile.length() < MAX_LOG_FILE_SIZE
                            }
                            mCurrentWriter = PrintWriter(FileWriter(logFile, append))
                        }

                        mCurrentWriter?.println(msg.obj as? String)
                        mCurrentWriter?.flush()

                        sHandler?.removeMessages(MSG_CLOSE)
                        sHandler?.sendEmptyMessageDelayed(MSG_CLOSE, CLOSE_DELAY)
                    } catch (e: Exception) {
                        Log.e("FileLog", "Error writing logs to file: ${e.message}", e)
                        closeWriter()
                    }
                    return true
                }

                MSG_CLOSE -> {
                    closeWriter()
                    return true
                }

                MSG_FLUSH -> {
                    closeWriter()
                    @Suppress("UNCHECKED_CAST")
                    (msg.obj as? Pair<PrintWriter?, CountDownLatch?>)?.let { pair ->
                        pair.first?.let { printWriter ->
                            for (i in 0 until LOG_DAYS) {
                                dumpFile(printWriter, "$FILE_NAME_PREFIX$i")
                            }
                        }
                        pair.second?.countDown()
                    }
                    return true
                }
            }
            return true
        }

        companion object {
            private const val CLOSE_DELAY: Long = 5000L
            const val MSG_WRITE = 1
            const val MSG_CLOSE = 2
            const val MSG_FLUSH = 3
        }
    }
}

/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.sandboxr.launcher

import android.content.Context
import android.graphics.Rect
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Log
import android.view.DragEvent
import android.view.KeyEvent
import android.view.View.OnFocusChangeListener
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.widget.AppCompatEditText
import com.sandboxr.launcher.views.ActivityContext

/**
 * An enhanced [AppCompatEditText] providing back-key IME event interception,
 * programmatic keyboard show/hide, multi-listener focus handling, and drag isolation.
 */
open class ExtendedEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.editTextStyle
) : AppCompatEditText(context, attrs, defStyleAttr) {

    fun interface OnBackKeyListener {
        fun onBackKey(): Boolean
    }

    private val onFocusChangeListeners = HashSet<OnFocusChangeListener>()
    private var backKeyListener: OnBackKeyListener? = null
    private var forceDisableSuggestions = false
    private var allowRequestFocusWithoutWindow = false

    fun setOnBackKeyListener(listener: OnBackKeyListener?) {
        backKeyListener = listener
    }

    override fun onKeyPreIme(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && event?.action == KeyEvent.ACTION_UP) {
            val consumed = backKeyListener?.onBackKey() == true
            if (consumed) return true
        }
        return super.onKeyPreIme(keyCode, event)
    }

    override fun onDragEvent(event: DragEvent?): Boolean {
        // Prevent interfering with Launcher workspace/drawer drag and drop
        return false
    }

    /**
     * Synchronously requests focus and shows the soft keyboard.
     */
    open fun showKeyboard(): Boolean {
        return requestFocusExplicitly() && showSoftInputInternal()
    }

    /**
     * Explicitly requests focus without requiring an active window focus.
     */
    open fun requestFocusExplicitly(): Boolean {
        allowRequestFocusWithoutWindow = true
        return try {
            requestFocus()
        } finally {
            allowRequestFocusWithoutWindow = false
        }
    }

    override fun requestFocus(direction: Int, previouslyFocusedRect: Rect?): Boolean {
        if (!isFocused && !hasWindowFocus() && !allowRequestFocusWithoutWindow) {
            return false
        }
        return super.requestFocus(direction, previouslyFocusedRect)
    }

    @JvmOverloads
    open fun hideKeyboard(clearFocus: Boolean = true) {
        try {
            val activityContext = ActivityContext.lookupContext<ActivityContext>(context)
            activityContext.hideKeyboard()
        } catch (_: Exception) {
            val imm = context.getSystemService(InputMethodManager::class.java)
            imm?.hideSoftInputFromWindow(windowToken, 0)
        }
        if (clearFocus) {
            clearFocus()
        }
    }

    private fun showSoftInputInternal(): Boolean {
        val imm = context.getSystemService(InputMethodManager::class.java)
        return if (imm != null) {
            imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
        } else {
            Log.w(TAG, "Failed to retrieve InputMethodManager from system.")
            false
        }
    }

    open fun dispatchBackKey() {
        hideKeyboard()
        backKeyListener?.onBackKey()
    }

    open fun forceDisableSuggestions(disable: Boolean) {
        forceDisableSuggestions = disable
    }

    override fun isSuggestionsEnabled(): Boolean {
        return !forceDisableSuggestions && super.isSuggestionsEnabled()
    }

    open fun reset() {
        if (!TextUtils.isEmpty(text)) {
            setText("")
        }
    }

    override fun setText(text: CharSequence?, type: BufferType?) {
        super.setText(text, type)
        text?.let {
            setSelection(it.length)
        }
    }

    fun addOnFocusChangeListener(listener: OnFocusChangeListener) {
        onFocusChangeListeners.add(listener)
    }

    fun removeOnFocusChangeListener(listener: OnFocusChangeListener) {
        onFocusChangeListeners.remove(listener)
    }

    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect)
        for (listener in ArrayList(onFocusChangeListeners)) {
            listener.onFocusChange(this, focused)
        }
    }

    open fun saveFocusedStateAndUpdateToUnfocusedState() {}
    open fun restoreToFocusedState() {}

    companion object {
        private const val TAG = "ExtendedEditText"
    }
}

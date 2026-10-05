/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.sandboxr.launcher.folder;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;
import com.sandboxr.launcher.ExtendedEditText;

/**
 * Text editor for folder titles with automatic select-all on focus,
 * IME action done interception, and title commit callbacks.
 */
public class FolderNameEditText extends ExtendedEditText
        implements TextView.OnEditorActionListener, View.OnFocusChangeListener {

    public interface OnTitleChangeListener {
        void onTitleChanged(CharSequence newTitle);
    }

    private OnTitleChangeListener mTitleChangeListener;

    public FolderNameEditText(Context context) {
        this(context, null, 0);
    }

    public FolderNameEditText(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FolderNameEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setOnEditorActionListener(this);
        setOnFocusChangeListener(this);
        setTextColor(Color.WHITE);
        setHighlightColor(0x6664D2FF);
        setSingleLine(true);
        setImeOptions(EditorInfo.IME_ACTION_DONE);
    }

    public void setOnTitleChangeListener(OnTitleChangeListener listener) {
        mTitleChangeListener = listener;
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (hasFocus) {
            selectAll();
        } else {
            commitTitle();
            hideKeyboard();
        }
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_DONE || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
            clearFocus();
            commitTitle();
            hideKeyboard();
            return true;
        }
        return false;
    }

    private void commitTitle() {
        if (mTitleChangeListener != null && getText() != null) {
            mTitleChangeListener.onTitleChanged(getText().toString().trim());
        }
    }
}

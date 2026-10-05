/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.launcher3;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import androidx.annotation.VisibleForTesting;

/**
 * Self-rescheduling timer alarm for launcher animations, spring-loaded transitions, and timeouts.
 */
public class Alarm implements Runnable {
    private long mAlarmTriggerTime;
    private boolean mWaitingForCallback;
    private final Handler mHandler;
    private OnAlarmListener mAlarmListener;
    private boolean mAlarmPending = false;
    private long mLastSetTimeout;

    public Alarm() {
        this(Looper.myLooper() != null ? Looper.myLooper() : Looper.getMainLooper());
    }

    public Alarm(Looper looper) {
        mHandler = new Handler(looper != null ? looper : Looper.getMainLooper());
    }

    public void setOnAlarmListener(OnAlarmListener alarmListener) {
        mAlarmListener = alarmListener;
    }

    public void setAlarm(long millisecondsInFuture) {
        long currentTime = SystemClock.uptimeMillis();
        mAlarmPending = true;
        long oldTriggerTime = mAlarmTriggerTime;
        mAlarmTriggerTime = currentTime + millisecondsInFuture;
        mLastSetTimeout = millisecondsInFuture;

        if (mWaitingForCallback && oldTriggerTime > mAlarmTriggerTime) {
            mHandler.removeCallbacks(this);
            mWaitingForCallback = false;
        }
        if (!mWaitingForCallback) {
            mHandler.postDelayed(this, mAlarmTriggerTime - currentTime);
            mWaitingForCallback = true;
        }
    }

    public void cancelAlarm() {
        mAlarmPending = false;
        mWaitingForCallback = false;
        mHandler.removeCallbacks(this);
    }

    @Override
    public void run() {
        mWaitingForCallback = false;
        if (mAlarmPending) {
            long currentTime = SystemClock.uptimeMillis();
            if (mAlarmTriggerTime > currentTime) {
                mHandler.postDelayed(this, Math.max(0, mAlarmTriggerTime - currentTime));
                mWaitingForCallback = true;
            } else {
                mAlarmPending = false;
                if (mAlarmListener != null) {
                    mAlarmListener.onAlarm(this);
                }
            }
        }
    }

    public boolean alarmPending() {
        return mAlarmPending;
    }

    public long getLastSetTimeout() {
        return mLastSetTimeout;
    }

    @VisibleForTesting
    public void finishAlarm() {
        if (!mAlarmPending) return;
        mAlarmPending = false;
        mWaitingForCallback = false;
        mHandler.removeCallbacks(this);
        if (mAlarmListener != null) {
            mAlarmListener.onAlarm(this);
        }
    }
}

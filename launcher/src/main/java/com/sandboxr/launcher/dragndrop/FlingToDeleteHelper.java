/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.sandboxr.launcher.dragndrop;

import android.graphics.PointF;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import com.sandboxr.launcher.ButtonDropTarget;
import com.sandboxr.launcher.DropTarget;
import com.sandboxr.launcher.Launcher;
import com.sandboxr.launcher.R;
import com.sandboxr.launcher.util.FlingAnimation;

/**
 * Utility helper managing fling gestures to delete/uninstall dropped items.
 */
public class FlingToDeleteHelper {

    private static final float MAX_FLING_DEGREES = 35f;

    private final Launcher mLauncher;
    private ButtonDropTarget mDropTarget;
    private VelocityTracker mVelocityTracker;

    public FlingToDeleteHelper(Launcher launcher) {
        mLauncher = launcher;
    }

    public void recordMotionEvent(MotionEvent ev) {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(ev);
    }

    public void releaseVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    public DropTarget getDropTarget() {
        return mDropTarget;
    }

    public void setDropTarget(ButtonDropTarget target) {
        mDropTarget = target;
    }

    public Runnable getFlingAnimation(DropTarget.DragObject dragObject, DragOptions options) {
        if (options == null) {
            return null;
        }
        PointF vel = isFlingingToDelete();
        options.isFlingToDelete = vel != null;
        if (!options.isFlingToDelete) {
            return null;
        }
        return new FlingAnimation(dragObject, vel, mDropTarget, mLauncher, options);
    }

    /**
     * Determines whether user flung the currently dragged item upward towards delete drop target.
     */
    public PointF isFlingingToDelete() {
        if (mVelocityTracker == null) return null;
        if (mDropTarget == null) {
            mDropTarget = mLauncher.findViewById(R.id.delete_target_text);
        }
        if (mDropTarget == null || !mDropTarget.isDropEnabled()) {
            return null;
        }
        ViewConfiguration config = ViewConfiguration.get(mLauncher);
        mVelocityTracker.computeCurrentVelocity(1000, config.getScaledMaximumFlingVelocity());
        PointF vel = new PointF(mVelocityTracker.getXVelocity(), mVelocityTracker.getYVelocity());

        // Dot product to ensure upward fling
        PointF upVec = new PointF(0f, -1f);
        float theta = getAngleBetweenVectors(vel, upVec);

        if (theta <= Math.toRadians(MAX_FLING_DEGREES)) {
            return vel;
        }
        return null;
    }

    private float getAngleBetweenVectors(PointF vec1, PointF vec2) {
        float length1 = vec1.length();
        float length2 = vec2.length();
        if (length1 == 0 || length2 == 0) return (float) Math.PI;
        float dot = (vec1.x * vec2.x) + (vec1.y * vec2.y);
        float cosVal = Math.max(-1f, Math.min(1f, dot / (length1 * length2)));
        return (float) Math.acos(cosVal);
    }
}

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

import android.graphics.Point;
import com.sandboxr.launcher.DropTarget;

/**
 * Set of options to control drag-and-drop behavior across workspace, apps, and targets.
 */
public class DragOptions {

    public boolean isAccessibleDrag = false;
    public boolean isKeyboardDrag = false;
    public boolean isMouseDrag = false;

    public Point simulatedDndStartPoint = null;

    public PreDragCondition preDragCondition = null;

    public float preDragEndScale = 0f;
    public boolean deferDragToPreDragEnd = false;
    public float intrinsicIconScaleFactor = 1f;

    public boolean isSystemDrag = false;
    public boolean isFlingToDelete = false;

    /**
     * Interface defining conditions that must be met before drag initiation is committed.
     */
    public interface PreDragCondition {
        boolean shouldStartDrag(double distanceDragged);

        void onPreDragStart(DropTarget.DragObject dragObject);

        void onPreDragEnd(DropTarget.DragObject dragObject, boolean dragStarted);

        default Point getDragOffset() {
            return new Point(0, 0);
        }
    }
}

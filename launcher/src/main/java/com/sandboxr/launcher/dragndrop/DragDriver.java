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

package com.sandboxr.launcher.dragndrop;

import android.os.SystemClock;
import android.view.DragEvent;
import android.view.MotionEvent;

import java.util.function.Consumer;

/**
 * Base class for driving a drag/drop operation via touch or system DnD events.
 */
public abstract class DragDriver {

    protected final EventListener mEventListener;
    protected final Consumer<MotionEvent> mSecondaryEventConsumer;

    public interface EventListener {
        void onDriverDragMove(float x, float y);
        void onDriverDragEnterWindow();
        void onDriverDragExitWindow();
        void onDriverDragEnd(float x, float y);
        void onDriverDragCancel();
    }

    public DragDriver(EventListener eventListener, Consumer<MotionEvent> sec) {
        mEventListener = eventListener;
        mSecondaryEventConsumer = sec;
    }

    public abstract boolean isDragWithinWindow();

    public boolean onTouchEvent(MotionEvent ev) {
        return false;
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return false;
    }

    public boolean onDragEvent(DragEvent event) {
        return false;
    }

    public static DragDriver create(DragController dragController, DragOptions options,
            Consumer<MotionEvent> sec) {
        if (options.simulatedDndStartPoint != null) {
            if (options.isAccessibleDrag) {
                return null;
            }
            return new SystemDragDriver(dragController, sec);
        } else {
            return new InternalDragDriver(dragController, sec);
        }
    }

    /**
     * Driver for system framework drag-and-drop operations.
     */
    static class SystemDragDriver extends DragDriver {
        private final long mDragStartTime;
        private boolean mIsDragWithinWindow;
        float mLastX = 0;
        float mLastY = 0;

        SystemDragDriver(DragController dragController, Consumer<MotionEvent> sec) {
            super(dragController, sec);
            mDragStartTime = SystemClock.uptimeMillis();
        }

        @Override
        public boolean isDragWithinWindow() {
            return mIsDragWithinWindow;
        }

        @Override
        public boolean onInterceptTouchEvent(MotionEvent ev) {
            return false;
        }

        private void simulateSecondaryMotionEvent(DragEvent event) {
            final int motionAction;
            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED:
                    motionAction = MotionEvent.ACTION_DOWN;
                    break;
                case DragEvent.ACTION_DRAG_LOCATION:
                    motionAction = MotionEvent.ACTION_MOVE;
                    break;
                case DragEvent.ACTION_DRAG_ENDED:
                    motionAction = MotionEvent.ACTION_UP;
                    break;
                default:
                    return;
            }
            MotionEvent emulatedEvent = MotionEvent.obtain(mDragStartTime,
                    SystemClock.uptimeMillis(), motionAction, event.getX(), event.getY(), 0);
            if (mSecondaryEventConsumer != null) {
                mSecondaryEventConsumer.accept(emulatedEvent);
            }
            emulatedEvent.recycle();
        }

        @Override
        public boolean onDragEvent(DragEvent event) {
            simulateSecondaryMotionEvent(event);
            final int action = event.getAction();

            switch (action) {
                case DragEvent.ACTION_DRAG_STARTED:
                    mLastX = event.getX();
                    mLastY = event.getY();
                    return true;

                case DragEvent.ACTION_DRAG_ENTERED:
                    mIsDragWithinWindow = true;
                    mEventListener.onDriverDragEnterWindow();
                    return true;

                case DragEvent.ACTION_DRAG_LOCATION:
                    mLastX = event.getX();
                    mLastY = event.getY();
                    mEventListener.onDriverDragMove(event.getX(), event.getY());
                    return true;

                case DragEvent.ACTION_DROP:
                    mLastX = event.getX();
                    mLastY = event.getY();
                    mEventListener.onDriverDragMove(event.getX(), event.getY());
                    mEventListener.onDriverDragEnd(mLastX, mLastY);
                    return true;

                case DragEvent.ACTION_DRAG_EXITED:
                    mIsDragWithinWindow = false;
                    mEventListener.onDriverDragExitWindow();
                    return true;

                case DragEvent.ACTION_DRAG_ENDED:
                    mEventListener.onDriverDragCancel();
                    return true;

                default:
                    return false;
            }
        }
    }

    /**
     * Driver for internal touch-driven drag operations.
     */
    static class InternalDragDriver extends DragDriver {
        private final DragController mDragController;

        InternalDragDriver(DragController dragController, Consumer<MotionEvent> sec) {
            super(dragController, sec);
            mDragController = dragController;
        }

        @Override
        public boolean isDragWithinWindow() {
            return true;
        }

        @Override
        public boolean onTouchEvent(MotionEvent ev) {
            if (mSecondaryEventConsumer != null) {
                mSecondaryEventConsumer.accept(ev);
            }
            final int action = ev.getAction();

            switch (action) {
                case MotionEvent.ACTION_MOVE:
                    mEventListener.onDriverDragMove(mDragController.getX(ev), mDragController.getY(ev));
                    break;
                case MotionEvent.ACTION_UP:
                    mEventListener.onDriverDragMove(mDragController.getX(ev), mDragController.getY(ev));
                    mEventListener.onDriverDragEnd(mDragController.getX(ev), mDragController.getY(ev));
                    break;
                case MotionEvent.ACTION_CANCEL:
                    mEventListener.onDriverDragCancel();
                    break;
            }
            return true;
        }

        @Override
        public boolean onInterceptTouchEvent(MotionEvent ev) {
            if (mSecondaryEventConsumer != null) {
                mSecondaryEventConsumer.accept(ev);
            }
            final int action = ev.getAction();

            switch (action) {
                case MotionEvent.ACTION_UP:
                    mEventListener.onDriverDragEnd(mDragController.getX(ev), mDragController.getY(ev));
                    break;
                case MotionEvent.ACTION_CANCEL:
                    mEventListener.onDriverDragCancel();
                    break;
            }
            return true;
        }
    }
}

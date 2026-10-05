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

import android.graphics.Rect;
import com.sandboxr.launcher.Launcher;
import com.sandboxr.launcher.dragndrop.DragLayer;

/**
 * High-level coordinator managing folder opening and closing animation states,
 * bounding geometry calculation, and scrim fade transitions.
 */
public class FolderAnimationManager {

    private final Folder mFolder;
    private final boolean mIsOpen;
    private FolderSpringAnimatorSet mCurrentAnimator;

    public FolderAnimationManager(Folder folder, boolean isOpen) {
        mFolder = folder;
        mIsOpen = isOpen;
    }

    public boolean isAnimating() {
        return mCurrentAnimator != null && mCurrentAnimator.isRunning();
    }

    public void animate(FolderIcon folderIcon, Runnable onComplete) {
        if (folderIcon == null || mFolder == null) {
            if (onComplete != null) onComplete.run();
            return;
        }

        Launcher launcher = mFolder.getLauncher();
        DragLayer dragLayer = launcher != null ? launcher.getDragLayer() : null;

        Rect startRect = new Rect();
        Rect endRect = new Rect();

        if (dragLayer != null) {
            dragLayer.getDescendantRectRelativeToSelf(folderIcon, startRect);
            dragLayer.getDescendantRectRelativeToSelf(mFolder, endRect);
        } else {
            folderIcon.getHitRect(startRect);
            mFolder.getHitRect(endRect);
        }

        if (endRect.isEmpty() && mFolder.getWidth() > 0 && mFolder.getHeight() > 0) {
            endRect.set(mFolder.getLeft(), mFolder.getTop(), mFolder.getRight(), mFolder.getBottom());
        }

        float startRadius = folderIcon.getPreviewBackground().getRadius();
        float endRadius = 32f;

        ClipRevealData clipData = new ClipRevealData(startRect, endRect, startRadius, endRadius);

        if (mCurrentAnimator != null && mCurrentAnimator.isRunning()) {
            mCurrentAnimator.cancel();
        }

        if (launcher != null) {
            FolderScrimAnimationListener scrimListener = new FolderScrimAnimationListener(launcher, mIsOpen);
            // Drive scrim listener
        }

        if (mIsOpen) {
            mCurrentAnimator = FolderAnimationCreator.createFolderOpenAnimation(mFolder, clipData, onComplete);
        } else {
            mCurrentAnimator = FolderAnimationCreator.createFolderCloseAnimation(mFolder, clipData, onComplete);
        }

        mCurrentAnimator.start();
    }
}

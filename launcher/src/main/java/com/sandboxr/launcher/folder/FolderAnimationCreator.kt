/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.sandboxr.launcher.folder

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.Rect
import android.view.View

/**
 * Creates physics-based spring animations for opening and closing folders,
 * coordinating clip-reveal, scale, alpha, and coordinate translation.
 */
class FolderAnimationCreator {

    companion object {
        const val FOLDER_OPEN_DURATION = 350L
        const val FOLDER_CLOSE_DURATION = 250L

        @JvmStatic
        fun createFolderOpenAnimation(
            folder: View,
            clipData: ClipRevealData,
            onComplete: Runnable? = null
        ): FolderSpringAnimatorSet {
            val animatorSet = FolderSpringAnimatorSet()
            val springInterpolator = FolderAnimationSpringBuilderManager.createFolderSpringInterpolator()

            val startWidth = clipData.startRect.width().toFloat()
            val startHeight = clipData.startRect.height().toFloat()
            val endWidth = clipData.endRect.width().toFloat().coerceAtLeast(1f)
            val endHeight = clipData.endRect.height().toFloat().coerceAtLeast(1f)

            val initialScaleX = (startWidth / endWidth).coerceIn(0.1f, 1.0f)
            val initialScaleY = (startHeight / endHeight).coerceIn(0.1f, 1.0f)

            val iconCenterX = clipData.startRect.centerX().toFloat()
            val iconCenterY = clipData.startRect.centerY().toFloat()
            val folderCenterX = clipData.endRect.centerX().toFloat()
            val folderCenterY = clipData.endRect.centerY().toFloat()

            val initialTransX = iconCenterX - folderCenterX
            val initialTransY = iconCenterY - folderCenterY

            folder.pivotX = endWidth / 2f
            folder.pivotY = endHeight / 2f
            folder.scaleX = initialScaleX
            folder.scaleY = initialScaleY
            folder.translationX = initialTransX
            folder.translationY = initialTransY
            folder.alpha = 0f
            folder.visibility = View.VISIBLE

            val scaleXAnim = ObjectAnimator.ofFloat(folder, View.SCALE_X, initialScaleX, 1.0f).apply {
                interpolator = springInterpolator
            }
            val scaleYAnim = ObjectAnimator.ofFloat(folder, View.SCALE_Y, initialScaleY, 1.0f).apply {
                interpolator = springInterpolator
            }
            val transXAnim = ObjectAnimator.ofFloat(folder, View.TRANSLATION_X, initialTransX, 0f).apply {
                interpolator = springInterpolator
            }
            val transYAnim = ObjectAnimator.ofFloat(folder, View.TRANSLATION_Y, initialTransY, 0f).apply {
                interpolator = springInterpolator
            }
            val alphaAnim = ObjectAnimator.ofFloat(folder, View.ALPHA, 0f, 1.0f).apply {
                interpolator = FolderAnimationSpringBuilderManager.FOLDER_FADE_INTERPOLATOR
            }

            animatorSet.playTogether(scaleXAnim, scaleYAnim, transXAnim, transYAnim, alphaAnim)
            animatorSet.setDuration(FOLDER_OPEN_DURATION)
            animatorSet.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    folder.scaleX = 1.0f
                    folder.scaleY = 1.0f
                    folder.translationX = 0f
                    folder.translationY = 0f
                    folder.alpha = 1.0f
                    onComplete?.run()
                }
            })

            return animatorSet
        }

        @JvmStatic
        fun createFolderCloseAnimation(
            folder: View,
            clipData: ClipRevealData,
            onComplete: Runnable? = null
        ): FolderSpringAnimatorSet {
            val animatorSet = FolderSpringAnimatorSet()
            val springInterpolator = FolderAnimationSpringBuilderManager.createFolderSpringInterpolator(
                dampingRatio = 0.9f,
                frequency = 12f
            )

            val startWidth = clipData.startRect.width().toFloat()
            val startHeight = clipData.startRect.height().toFloat()
            val endWidth = clipData.endRect.width().toFloat().coerceAtLeast(1f)
            val endHeight = clipData.endRect.height().toFloat().coerceAtLeast(1f)

            val targetScaleX = (startWidth / endWidth).coerceIn(0.1f, 1.0f)
            val targetScaleY = (startHeight / endHeight).coerceIn(0.1f, 1.0f)

            val iconCenterX = clipData.startRect.centerX().toFloat()
            val iconCenterY = clipData.startRect.centerY().toFloat()
            val folderCenterX = clipData.endRect.centerX().toFloat()
            val folderCenterY = clipData.endRect.centerY().toFloat()

            val targetTransX = iconCenterX - folderCenterX
            val targetTransY = iconCenterY - folderCenterY

            val scaleXAnim = ObjectAnimator.ofFloat(folder, View.SCALE_X, 1.0f, targetScaleX).apply {
                interpolator = springInterpolator
            }
            val scaleYAnim = ObjectAnimator.ofFloat(folder, View.SCALE_Y, 1.0f, targetScaleY).apply {
                interpolator = springInterpolator
            }
            val transXAnim = ObjectAnimator.ofFloat(folder, View.TRANSLATION_X, 0f, targetTransX).apply {
                interpolator = springInterpolator
            }
            val transYAnim = ObjectAnimator.ofFloat(folder, View.TRANSLATION_Y, 0f, targetTransY).apply {
                interpolator = springInterpolator
            }
            val alphaAnim = ObjectAnimator.ofFloat(folder, View.ALPHA, 1.0f, 0f).apply {
                interpolator = FolderAnimationSpringBuilderManager.FOLDER_FADE_INTERPOLATOR
            }

            animatorSet.playTogether(scaleXAnim, scaleYAnim, transXAnim, transYAnim, alphaAnim)
            animatorSet.setDuration(FOLDER_CLOSE_DURATION)
            animatorSet.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    folder.visibility = View.GONE
                    folder.scaleX = 1.0f
                    folder.scaleY = 1.0f
                    folder.alpha = 0f
                    onComplete?.run()
                }
            })

            return animatorSet
        }
    }
}

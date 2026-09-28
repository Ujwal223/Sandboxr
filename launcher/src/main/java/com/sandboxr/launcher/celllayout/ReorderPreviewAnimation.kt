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

package com.sandboxr.launcher.celllayout

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.animation.ValueAnimator.areAnimatorsEnabled
import android.util.ArrayMap
import android.view.View
import android.view.animation.DecelerateInterpolator
import com.sandboxr.launcher.CellLayout
import com.sandboxr.launcher.Reorderable
import com.sandboxr.launcher.Workspace
import com.sandboxr.launcher.util.MultiTranslateDelegate.Companion.INDEX_REORDER_BOUNCE_OFFSET
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.sign
import kotlin.math.sin

/**
 * Class which represents the reorder preview animations. These animations show that an item is in a
 * temporary state, and hint at where the item will return to.
 */
class ReorderPreviewAnimation<T>(
    val child: T,
    val mode: Int,
    cellX0: Int,
    cellY0: Int,
    cellX1: Int,
    cellY1: Int,
    spanX: Int,
    spanY: Int,
    reorderMagnitude: Float,
    cellLayout: CellLayout,
    private val shakeAnimators: ArrayMap<Reorderable, ReorderPreviewAnimation<*>>
) : ValueAnimator.AnimatorUpdateListener where T : View, T : Reorderable {

    private var finalDeltaX = 0f
    private var finalDeltaY = 0f
    private var initDeltaX =
        child.getTranslateDelegate().getTranslationX(INDEX_REORDER_BOUNCE_OFFSET).value
    private var initDeltaY =
        child.getTranslateDelegate().getTranslationY(INDEX_REORDER_BOUNCE_OFFSET).value
    private var initScale = child.getReorderBounceScale()
    private val finalScale = CellLayout.DEFAULT_SCALE - CHILD_DIVIDEND / child.width.coerceAtLeast(1) * initScale

    private val dir = if (mode == MODE_HINT) -1 else 1
    var animator: ValueAnimator =
        ObjectAnimator.ofFloat(0f, 1f).also {
            it.addUpdateListener(this)
            it.duration = (if (mode == MODE_HINT) HINT_DURATION else PREVIEW_DURATION).toLong()
            it.startDelay = (Math.random() * 60).toLong()
            if (areAnimatorsEnabled() && mode == MODE_PREVIEW) {
                it.repeatCount = ValueAnimator.INFINITE
                it.repeatMode = ValueAnimator.REVERSE
            }
        }

    init {
        val tmpRes = intArrayOf(0, 0)
        cellLayout.regionToCenterPoint(cellX0, cellY0, spanX, spanY, tmpRes)
        val x0 = tmpRes[0]
        val y0 = tmpRes[1]
        cellLayout.regionToCenterPoint(cellX1, cellY1, spanX, spanY, tmpRes)
        val x1 = tmpRes[0]
        val y1 = tmpRes[1]
        val dX = x1 - x0
        val dY = y1 - y0

        if (dX != 0 || dY != 0) {
            if (dY == 0) {
                finalDeltaX = -dir * sign(dX.toFloat()) * reorderMagnitude
            } else if (dX == 0) {
                finalDeltaY = -dir * sign(dY.toFloat()) * reorderMagnitude
            } else {
                val angle = atan((dY.toFloat() / dX).toDouble())
                finalDeltaX = (-dir * sign(dX.toFloat()) * abs(cos(angle).toFloat() * reorderMagnitude))
                finalDeltaY = (-dir * sign(dY.toFloat()) * abs(sin(angle).toFloat() * reorderMagnitude))
            }
        }
    }

    private fun setInitialAnimationValuesToBaseline() {
        initScale = CellLayout.DEFAULT_SCALE
        initDeltaX = 0f
        initDeltaY = 0f
    }

    fun animate() {
        val noMovement = finalDeltaX == 0f && finalDeltaY == 0f
        if (shakeAnimators.containsKey(child)) {
            val oldAnimation: ReorderPreviewAnimation<*>? = shakeAnimators.remove(child)
            if (noMovement) {
                oldAnimation?.finishAnimation()
                return
            } else {
                oldAnimation?.cancel()
            }
        }
        if (noMovement) {
            return
        }
        shakeAnimators[child] = this
        animator.start()
    }

    override fun onAnimationUpdate(updatedAnimation: ValueAnimator) {
        val progress = updatedAnimation.animatedValue as Float
        child
            .getTranslateDelegate()
            .setTranslation(
                INDEX_REORDER_BOUNCE_OFFSET,
                progress * finalDeltaX + (1 - progress) * initDeltaX,
                progress * finalDeltaY + (1 - progress) * initDeltaY
            )
        child.setReorderBounceScale(progress * finalScale + (1 - progress) * initScale)
    }

    fun cancel() {
        animator.cancel()
    }

    fun finishAnimation() {
        animator.cancel()
        setInitialAnimationValuesToBaseline()
        val currentVal = (animator.animatedValue as? Float) ?: 0f
        animator = ObjectAnimator.ofFloat(currentVal, 0f)
        animator.addUpdateListener(this)
        animator.interpolator = DecelerateInterpolator(1.5f)
        animator.duration = CellLayout.REORDER_ANIMATION_DURATION.toLong()
        animator.start()
    }

    companion object {
        const val PREVIEW_DURATION = 300
        const val HINT_DURATION = Workspace.REORDER_TIMEOUT
        private const val CHILD_DIVIDEND = 4.0f
        const val MODE_HINT = 0
        const val MODE_PREVIEW = 1
    }
}

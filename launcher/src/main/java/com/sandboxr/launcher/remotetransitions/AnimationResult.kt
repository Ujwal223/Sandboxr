/*
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

package com.sandboxr.launcher.remotetransitions

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.content.Context
import android.view.SurfaceControl
import android.window.IRemoteTransitionFinishedCallback
import android.window.WindowContainerTransaction
import androidx.annotation.UiThread
import com.sandboxr.launcher.util.Executors
import java.util.concurrent.Executor

/**
 * Extends [IRemoteTransitionFinishedCallback] and used by remote transition
 * implementations to run animations and manage their lifecycle callbacks.
 */
class AnimationResult(
    private val syncFinishRunnable: Runnable,
    private val asyncFinishRunnable: Runnable,
    private val bgExecutor: Executor = Executors.ORDERED_BG_EXECUTOR,
    private val mainExecutor: Executor = Executors.MAIN_EXECUTOR,
) : IRemoteTransitionFinishedCallback.Stub() {

    private var onCompleteCallback: Runnable? = null
    private var finished: Boolean = false
    private var initialized: Boolean = false

    @UiThread
    fun finish() {
        if (!finished) {
            syncFinishRunnable.run()
            bgExecutor.execute {
                asyncFinishRunnable.run()
                onCompleteCallback?.let { mainExecutor.execute(it) }
            }
            finished = true
        }
    }

    @UiThread
    fun setAnimation(animation: AnimatorSet?, context: Context?) {
        setAnimation(
            animation = animation,
            context = context,
            onCompleteCallback = null,
            skipFirstFrame = false,
        )
    }

    @UiThread
    fun setAnimation(
        animation: AnimatorSet?,
        context: Context?,
        onCompleteCallback: Runnable?,
        skipFirstFrame: Boolean = false,
    ) {
        if (initialized) {
            throw IllegalStateException("Animation already initialized")
        }
        initialized = true
        this.onCompleteCallback = onCompleteCallback

        if (animation == null) {
            finish()
        } else if (finished) {
            animation.start()
            animation.end()
            onCompleteCallback?.run()
        } else {
            animation.addListener(
                object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        finish()
                    }
                }
            )
            animation.start()
        }
    }

    override fun onTransitionFinished(
        wct: WindowContainerTransaction?,
        transaction: SurfaceControl.Transaction?,
    ) {
        asyncFinishRunnable.run()
    }
}

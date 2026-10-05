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

import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.RemoteException
import android.util.Log
import android.view.SurfaceControl
import android.window.IRemoteTransitionFinishedCallback
import android.window.RemoteTransitionStub
import android.window.TransitionInfo

/**
 * Standard implementation of [RemoteTransitionStub] that coordinates transition lifecycle,
 * merges, and finish callbacks with a [RemoteAnimationRunner].
 */
class LauncherTransition(
    private val runner: RemoteAnimationRunner,
    private val mainHandler: Handler = Handler(Looper.getMainLooper())
) : RemoteTransitionStub() {

    override fun startAnimation(
        token: IBinder?,
        info: TransitionInfo?,
        t: SurfaceControl.Transaction?,
        finishCallback: IRemoteTransitionFinishedCallback?
    ) {
        mainHandler.post {
            runner.onAnimationStart(token, info, t) {
                try {
                    finishCallback?.onTransitionFinished(null, null)
                } catch (e: RemoteException) {
                    Log.e(TAG, "Failed to call finishCallback", e)
                }
            }
        }
    }

    override fun mergeAnimation(
        token: IBinder?,
        info: TransitionInfo?,
        t: SurfaceControl.Transaction?,
        mergeTarget: IBinder?,
        finishCallback: IRemoteTransitionFinishedCallback?
    ) {
        mainHandler.post {
            runner.onAnimationMerged(token, info, t, mergeTarget)
            try {
                finishCallback?.onTransitionFinished(null, null)
            } catch (e: RemoteException) {
                Log.e(TAG, "Failed to call finishCallback on merge", e)
            }
        }
    }

    override fun onTransitionConsumed(token: IBinder?, aborted: Boolean) {
        mainHandler.post {
            runner.onAnimationCancelled()
        }
    }

    companion object {
        private const val TAG = "LauncherTransition"
    }
}

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

package com.sandboxr.launcher.cuebar.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import com.sandboxr.launcher.cuebar.ui.utils.AmbientCueAnimationState

/**
 * CUJ type constants mirroring [com.android.internal.jank.Cuj] for CueBar animations.
 *
 * These integer identifiers are passed to [AmbientCueJankMonitor.onAnimationStateChange] so the
 * jank monitor can distinguish which animation is being tracked. Because Sandboxr does not link
 * against the hidden [com.android.internal.jank] API, we define our own constants here.
 */
object CueBarCuj {
    const val AMBIENT_CUE_SHOW: Int = 1
    const val AMBIENT_CUE_HIDE: Int = 2
    const val AMBIENT_CUE_EXPAND: Int = 3
    const val AMBIENT_CUE_COLLAPSE: Int = 4
}

/**
 * Side-effect composable that tracks CueBar animation lifecycle and forwards
 * [AmbientCueAnimationState] events to [onAnimationStateChange].
 *
 * This mirrors the upstream `AmbientCueJankMonitorComposable` but replaces the
 * `com.android.internal.jank.Cuj` constants with [CueBarCuj] so the code compiles
 * against the public SDK.
 *
 * @param visibleTargetState Whether the pill is transitioning to visible (`true`) or hidden.
 * @param enterProgress Progress of the enter/exit animation in `[0f, 1f]`.
 * @param expanded Whether the action list is expanded.
 * @param expansionAlpha Alpha used to track collapse/expand progress (`0f` = expanded).
 * @param showAnimationInProgress Mutable flag tracking the show animation.
 * @param hideAnimationInProgress Mutable flag tracking the hide animation.
 * @param expandAnimationInProgress Mutable flag tracking the expand animation.
 * @param collapseAnimationInProgress Mutable flag tracking the collapse animation.
 * @param onAnimationStateChange Callback invoked with (cujType, [AmbientCueAnimationState]).
 */
@Composable
fun AmbientCueJankMonitorComposable(
    visibleTargetState: Boolean,
    enterProgress: Float,
    expanded: Boolean,
    expansionAlpha: Float,
    showAnimationInProgress: MutableState<Boolean>,
    hideAnimationInProgress: MutableState<Boolean>,
    expandAnimationInProgress: MutableState<Boolean>,
    collapseAnimationInProgress: MutableState<Boolean>,
    onAnimationStateChange: (Int, AmbientCueAnimationState) -> Unit,
) {
    // Show / hide lifecycle
    LaunchedEffect(visibleTargetState, enterProgress) {
        if (visibleTargetState) {
            when (enterProgress) {
                0f -> {
                    showAnimationInProgress.value = true
                    onAnimationStateChange(CueBarCuj.AMBIENT_CUE_SHOW, AmbientCueAnimationState.BEGIN)
                }
                1f -> {
                    showAnimationInProgress.value = false
                    onAnimationStateChange(CueBarCuj.AMBIENT_CUE_SHOW, AmbientCueAnimationState.END)
                }
            }
        } else {
            when (enterProgress) {
                0f -> {
                    hideAnimationInProgress.value = false
                    onAnimationStateChange(CueBarCuj.AMBIENT_CUE_HIDE, AmbientCueAnimationState.END)
                }
                1f -> {
                    hideAnimationInProgress.value = true
                    onAnimationStateChange(CueBarCuj.AMBIENT_CUE_HIDE, AmbientCueAnimationState.BEGIN)
                }
            }
        }
    }

    // Expand / collapse lifecycle
    LaunchedEffect(expanded, expansionAlpha) {
        if (expanded) {
            when (expansionAlpha) {
                0f -> {
                    if (expandAnimationInProgress.value) {
                        expandAnimationInProgress.value = false
                        onAnimationStateChange(
                            CueBarCuj.AMBIENT_CUE_EXPAND,
                            AmbientCueAnimationState.END,
                        )
                    }
                }
                1f -> {
                    expandAnimationInProgress.value = true
                    onAnimationStateChange(
                        CueBarCuj.AMBIENT_CUE_EXPAND,
                        AmbientCueAnimationState.BEGIN,
                    )
                }
            }
        } else {
            when (expansionAlpha) {
                0f -> {
                    collapseAnimationInProgress.value = true
                    onAnimationStateChange(
                        CueBarCuj.AMBIENT_CUE_COLLAPSE,
                        AmbientCueAnimationState.BEGIN,
                    )
                }
                1f -> {
                    if (collapseAnimationInProgress.value) {
                        collapseAnimationInProgress.value = false
                        onAnimationStateChange(
                            CueBarCuj.AMBIENT_CUE_COLLAPSE,
                            AmbientCueAnimationState.END,
                        )
                    }
                }
            }
        }
    }

    // Cancel any in-flight animations when the composable leaves composition
    DisposableEffect(Unit) {
        onDispose {
            if (showAnimationInProgress.value) {
                onAnimationStateChange(CueBarCuj.AMBIENT_CUE_SHOW, AmbientCueAnimationState.CANCEL)
            }
            if (hideAnimationInProgress.value) {
                onAnimationStateChange(CueBarCuj.AMBIENT_CUE_HIDE, AmbientCueAnimationState.CANCEL)
            }
            if (expandAnimationInProgress.value) {
                onAnimationStateChange(CueBarCuj.AMBIENT_CUE_EXPAND, AmbientCueAnimationState.CANCEL)
            }
            if (collapseAnimationInProgress.value) {
                onAnimationStateChange(
                    CueBarCuj.AMBIENT_CUE_COLLAPSE,
                    AmbientCueAnimationState.CANCEL,
                )
            }
        }
    }
}

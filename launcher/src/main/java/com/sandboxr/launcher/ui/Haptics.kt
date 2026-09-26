package com.sandboxr.launcher.ui

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

/**
 * Haptic feedback types for SANDBOXR adhering to DESIGN.md Section 9.
 * Used exclusively for action confirmation, never decoration.
 */
enum class SandboxrHapticType {
    ENVIRONMENT_SWITCH,
    CARD_PRESS,
    DESTRUCTIVE_CONFIRM,
    UNLOCK_SUCCESS,
    UNLOCK_FAIL,
    DRAG_START,
    DROP
}

/**
 * Helper class for dispatching tactile feedback via Android HapticFeedbackConstants.
 */
class SandboxrHaptics(private val view: View?) {

    fun perform(type: SandboxrHapticType) {
        val view = this.view ?: return

        val feedbackConstant = when (type) {
            SandboxrHapticType.ENVIRONMENT_SWITCH -> {
                HapticFeedbackConstants.CONTEXT_CLICK
            }
            SandboxrHapticType.CARD_PRESS -> {
                HapticFeedbackConstants.KEYBOARD_TAP
            }
            SandboxrHapticType.DESTRUCTIVE_CONFIRM -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    HapticFeedbackConstants.REJECT
                } else {
                    HapticFeedbackConstants.LONG_PRESS
                }
            }
            SandboxrHapticType.UNLOCK_SUCCESS -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    HapticFeedbackConstants.CONFIRM
                } else {
                    HapticFeedbackConstants.CONTEXT_CLICK
                }
            }
            SandboxrHapticType.UNLOCK_FAIL -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    HapticFeedbackConstants.REJECT
                } else {
                    HapticFeedbackConstants.LONG_PRESS
                }
            }
            SandboxrHapticType.DRAG_START -> {
                HapticFeedbackConstants.LONG_PRESS
            }
            SandboxrHapticType.DROP -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    HapticFeedbackConstants.GESTURE_END
                } else {
                    HapticFeedbackConstants.CONTEXT_CLICK
                }
            }
        }

        try {
            view.performHapticFeedback(
                feedbackConstant,
                HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
            )
        } catch (_: Exception) {
            // Graceful fallback on devices with disabled or missing haptic vibrators
        }
    }

    fun onCardPress() = perform(SandboxrHapticType.CARD_PRESS)
    fun onEnvironmentSwitch() = perform(SandboxrHapticType.ENVIRONMENT_SWITCH)
    fun onDestructiveConfirm() = perform(SandboxrHapticType.DESTRUCTIVE_CONFIRM)
    fun onUnlockSuccess() = perform(SandboxrHapticType.UNLOCK_SUCCESS)
    fun onUnlockFail() = perform(SandboxrHapticType.UNLOCK_FAIL)
    fun onDragStart() = perform(SandboxrHapticType.DRAG_START)
    fun onDrop() = perform(SandboxrHapticType.DROP)
}

/**
 * Composable returning a memoized SandboxrHaptics instance tied to LocalView.
 */
@Composable
fun rememberSandboxrHaptics(): SandboxrHaptics {
    val view = LocalView.current
    return remember(view) { SandboxrHaptics(view) }
}

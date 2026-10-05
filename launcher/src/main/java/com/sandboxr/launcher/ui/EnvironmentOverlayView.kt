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

package com.sandboxr.launcher.ui

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import com.sandboxr.launcher.theme.SandboxrTheme
import com.sandboxr.launcher.virtual.VirtualEnvironmentBridge
import com.sandboxr.network.model.NetworkMode

/**
 * Android View container embedding the Jetpack Compose Liquid Glass [EnvironmentLazyRow]
 * directly into the ported GrapheneOS Launcher3 View hierarchy (DragLayer / Workspace).
 *
 * Implements seamless coexistence between View-based gesture physics and Compose material glass cards.
 */
class EnvironmentOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val bridge = VirtualEnvironmentBridge.get(context)
    private var onEnvironmentSelectedListener: ((EnvironmentCardData) -> Unit)? = null
    private var onEnvironmentLongClickListener: ((EnvironmentCardData) -> Unit)? = null

    init {
        val composeView = ComposeView(context).apply {
            setContent {
                SandboxrTheme(isDark = true) {
                    EnvironmentOverlayContent()
                }
            }
        }
        addView(
            composeView,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        )
    }

    fun setOnEnvironmentSelectedListener(listener: (EnvironmentCardData) -> Unit) {
        this.onEnvironmentSelectedListener = listener
    }

    fun setOnEnvironmentLongClickListener(listener: (EnvironmentCardData) -> Unit) {
        this.onEnvironmentLongClickListener = listener
    }

    @Composable
    private fun EnvironmentOverlayContent() {
        val envSummaries by bridge.environmentsFlow.collectAsState()

        val cardItems = envSummaries.map { summary ->
            EnvironmentCardData(
                id = summary.id,
                name = summary.name,
                color = Color(summary.color),
                isSystem = summary.isSystem,
                appCount = summary.appCount,
                networkMode = NetworkMode.DIRECT,
                hasNotification = false,
                notificationCount = 0,
                isActive = summary.id == "system" // Defaults to system until switched
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            EnvironmentLazyRow(
                environments = cardItems,
                onEnvironmentSelected = { card ->
                    onEnvironmentSelectedListener?.invoke(card)
                },
                onEnvironmentLongClick = { card ->
                    onEnvironmentLongClickListener?.invoke(card)
                }
            )
        }
    }
}

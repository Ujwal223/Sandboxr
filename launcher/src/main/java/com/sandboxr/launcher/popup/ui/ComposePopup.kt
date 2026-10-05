/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.sandboxr.launcher.popup.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.sandboxr.launcher.R
import com.sandboxr.launcher.model.data.ItemInfoWithIcon
import com.sandboxr.launcher.popup.ui.PopupMenuItemDimens.popupMenuItemHeight
import com.sandboxr.launcher.popup.ui.PopupMenuItemDimens.popupMenuItemHorizontalPadding
import com.sandboxr.launcher.popup.ui.PopupMenuItemDimens.popupMenuItemWidth
import com.android.launcher3.testing.shared.TestProtocol.DEEP_SHORTCUTS_CONTAINER
import com.android.launcher3.testing.shared.TestProtocol.SYSTEM_SHORTCUTS_CONTAINER
import com.sandboxr.launcher.util.compose.testTag
import com.sandboxr.launcher.util.compose.testTagContainer
import kotlin.math.max

/**
 * The main composable for displaying the Launcher3 popup menu, built with Jetpack Compose.
 */
@Composable
fun ComposePopup(
    viewModel: PopupViewModel,
    onClickListener: (PopupClickEvent) -> Unit,
    onAddIconClick: ((ItemInfoWithIcon) -> Unit)?,
    onDeepShortcutLongPress: (ItemInfoWithIcon, Offset) -> Unit,
    onMaxHeightMeasured: ((Int) -> Unit)?,
) {
    val state = viewModel.state

    Box(
        modifier = Modifier.bottomAlignAndAllowOverflow().testTagContainer(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        ComposePopupContent(
            viewModel,
            state,
            onClickListener,
            onAddIconClick,
            onDeepShortcutLongPress,
            onMaxHeightMeasured,
        )
    }
}

private fun Modifier.bottomAlignAndAllowOverflow() =
    this.layout { measurable, _ ->
        val placeable = measurable.measure(Constraints()) // unbounded
        layout(placeable.width, placeable.height) { placeable.placeRelative(0, 0) }
    }

@Composable
private fun ComposePopupContent(
    viewModel: PopupViewModel,
    targetState: PopupUiState,
    onClickListener: (PopupClickEvent) -> Unit,
    onAddIconClick: ((ItemInfoWithIcon) -> Unit)?,
    onDeepShortcutLongPress: (ItemInfoWithIcon, Offset) -> Unit,
    onMaxHeightMeasured: ((Int) -> Unit)?,
) {
    val density = LocalDensity.current

    val maxHeight =
        remember(targetState, density) {
            with(density) {
                val spacerHeight = ComposePopupDimens.popupContentSpacerHeight.toPx()

                if (targetState.mainSegmentsStyle == MainSegmentsStyle.ACCORDION) {
                    val expandPopupMenuButtonHeight = popupMenuItemHeight.toPx()
                    val systemShortcutsHeight: Float =
                        if (targetState.compactSystemShortcuts.isEmpty()) {
                            targetState.standardSystemShortcuts.size * popupMenuItemHeight.toPx()
                        } else {
                            ((targetState.standardSystemShortcuts.size + 1) *
                                popupMenuItemHeight.toPx()) +
                                ComposePopupDimens.systemShortcutsDividerHeight.toPx()
                        }

                    val deepShortcutsHeight =
                        (targetState.deepShortcuts.size * popupMenuItemHeight.toPx())

                    val heightDeepExpanded =
                        expandPopupMenuButtonHeight + deepShortcutsHeight + spacerHeight
                    val heightSystemExpanded =
                        expandPopupMenuButtonHeight + systemShortcutsHeight + spacerHeight
                    max(heightDeepExpanded, heightSystemExpanded).toInt()
                } else { // MainSegmentsStyle.LIST
                    var systemShortcutsHeight = 0f
                    if (targetState.compactSystemShortcuts.isNotEmpty()) {
                        systemShortcutsHeight += popupMenuItemHeight.toPx()
                    }
                    if (
                        targetState.compactSystemShortcuts.isNotEmpty() &&
                            targetState.standardSystemShortcuts.isNotEmpty()
                    ) {
                        systemShortcutsHeight += ComposePopupDimens.popupContentSpacerHeight.toPx()
                    }
                    systemShortcutsHeight +=
                        targetState.standardSystemShortcuts.size * popupMenuItemHeight.toPx()

                    val deepShortcutsHeight =
                        targetState.deepShortcuts.size * popupMenuItemHeight.toPx()

                    if (deepShortcutsHeight > 0) {
                        systemShortcutsHeight +=
                            ComposePopupDimens.systemShortcutsDividerHeight.toPx()
                    }

                    (systemShortcutsHeight + deepShortcutsHeight + spacerHeight).toInt()
                }
            }
        }

    val lastReportedMaxHeight = remember { mutableIntStateOf(-1) }

    SideEffect {
        if (onMaxHeightMeasured != null && lastReportedMaxHeight.intValue != maxHeight) {
            onMaxHeightMeasured(maxHeight)
            lastReportedMaxHeight.intValue = maxHeight
        }
    }

    if (targetState.mainSegmentsStyle == MainSegmentsStyle.ACCORDION) {
        val expandedSection = viewModel.expandedSection

        val isSystemShortcutsExpanded = expandedSection == ExpandedSection.SYSTEM
        val isDeepShortcutsExpanded = expandedSection == ExpandedSection.DEEP

        ExpandableHybridPopup(
            compactSystemShortcuts = targetState.compactSystemShortcuts,
            standardSystemShortcuts = targetState.standardSystemShortcuts,
            standardDeepShortcuts = targetState.deepShortcuts,
            isSystemShortcutsExpanded = isSystemShortcutsExpanded,
            isDeepShortcutsExpanded = isDeepShortcutsExpanded,
            onToggle = viewModel::expandSection,
            onClickListener = onClickListener,
            onAddButtonClick = onAddIconClick,
            onDeepShortcutLongPress = onDeepShortcutLongPress,
        )
    } else { // MainSegmentsStyle.LIST
        ExpandableHybridPopup(
            compactSystemShortcuts = targetState.compactSystemShortcuts,
            standardSystemShortcuts = targetState.standardSystemShortcuts,
            standardDeepShortcuts = targetState.deepShortcuts,
            isSystemShortcutsExpanded = true,
            isDeepShortcutsExpanded = true,
            onToggle = {},
            onClickListener = onClickListener,
            onAddButtonClick = onAddIconClick,
            onDeepShortcutLongPress = onDeepShortcutLongPress,
        )
    }
}

@Composable
fun ExpandableContainer(
    collapsedContent: @Composable () -> Unit,
    expandedContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    transition: Transition<Boolean>,
) {
    val animationProgress by
        transition.animateFloat(
            transitionSpec = { ComposePopupAnimations.popupContentSpringSpec },
            label = "ExpandCollapseProgress",
        ) { isExpanded ->
            if (isExpanded) 1f else 0f
        }

    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(ComposePopupDimens.popupCornerRadius))
                .background(colorResource(R.color.materialColorSurfaceContainer))
    ) {
        AnimatedContent(
            targetState = transition.targetState,
            modifier = Modifier,
            label = "ExpandableContent",
            transitionSpec = {
                fadeIn(animationSpec = ComposePopupAnimations.popupContentSpringSpec) togetherWith
                    fadeOut(animationSpec = ComposePopupAnimations.popupContentSpringSpec) using
                    SizeTransform(
                        clip = true,
                        sizeAnimationSpec = { _, _ ->
                            ComposePopupAnimations.popupContainerSpringSpec
                        },
                    )
            },
        ) { isExpanded ->
            Box(
                modifier =
                    Modifier.graphicsLayer {
                        alpha =
                            if (isExpanded) {
                                ((animationProgress -
                                        ComposePopupAnimations
                                            .EXPANDED_CONTENT_FADE_START_PROGRESS) /
                                        ComposePopupAnimations.EXPANDED_CONTENT_FADE_DURATION_RATIO)
                                    .coerceIn(0f, 1f)
                            } else {
                                (1f -
                                        (animationProgress /
                                            ComposePopupAnimations
                                                .COLLAPSED_CONTENT_FADE_DURATION_RATIO))
                                    .coerceIn(0f, 1f)
                            }
                    }
            ) {
                if (isExpanded) {
                    expandedContent()
                } else {
                    collapsedContent()
                }
            }
        }
    }
}

@Composable
fun ExpandableHybridPopup(
    modifier: Modifier = Modifier,
    compactSystemShortcuts: List<PopupItem>,
    standardSystemShortcuts: List<PopupItem>,
    standardDeepShortcuts: List<ItemInfoWithIcon?>,
    isSystemShortcutsExpanded: Boolean,
    isDeepShortcutsExpanded: Boolean,
    onToggle: (ExpandedSection) -> Unit,
    onClickListener: (PopupClickEvent) -> Unit,
    onAddButtonClick: ((ItemInfoWithIcon) -> Unit)?,
    onDeepShortcutLongPress: (ItemInfoWithIcon, Offset) -> Unit,
) {
    val systemTransitionState = remember { MutableTransitionState(isSystemShortcutsExpanded) }
    LaunchedEffect(isSystemShortcutsExpanded) {
        systemTransitionState.targetState = isSystemShortcutsExpanded
    }
    val systemTransition =
        rememberTransition(systemTransitionState, label = "SystemShortcutsTransition")

    val deepTransitionState = remember { MutableTransitionState(isDeepShortcutsExpanded) }
    LaunchedEffect(isDeepShortcutsExpanded) {
        deepTransitionState.targetState = isDeepShortcutsExpanded
    }
    val deepTransition = rememberTransition(deepTransitionState, label = "DeepShortcutsTransition")

    Column(
        verticalArrangement = Arrangement.Bottom,
        modifier = modifier.fillMaxWidth().wrapContentHeight(align = Alignment.Bottom),
    ) {
        ExpandableContainer(
            collapsedContent = {
                ExpandSystemShortcutsMenuButton(
                    onShowSystemShortcuts = { onToggle(ExpandedSection.SYSTEM) }
                )
            },
            expandedContent = {
                SystemShortcutsSection(
                    compactSystemShortcuts,
                    standardSystemShortcuts,
                    onClickListener,
                )
            },
            transition = systemTransition,
        )
        if (standardSystemShortcuts.isNotEmpty() && standardDeepShortcuts.isNotEmpty()) {
            Spacer(modifier = Modifier.height(ComposePopupDimens.popupContentSpacerHeight))
        }
        if (standardDeepShortcuts.isNotEmpty()) {
            ExpandableContainer(
                collapsedContent = {
                    ExpandDeepShortcutsMenuButton(
                        onShowDeepShortcuts = { onToggle(ExpandedSection.DEEP) }
                    )
                },
                expandedContent = {
                    DeepShortcutsContent(
                        standardDeepShortcuts,
                        onClickListener,
                        onAddButtonClick,
                        onDeepShortcutLongPress,
                    )
                },
                transition = deepTransition,
            )
        }
    }
}

@Composable
private fun SystemShortcutsSection(
    compactShortcuts: List<PopupItem>,
    standardShortcuts: List<PopupItem>,
    onClick: (PopupClickEvent) -> Unit,
) {
    Column(modifier = Modifier.testTag(SYSTEM_SHORTCUTS_CONTAINER)) {
        if (compactShortcuts.isNotEmpty()) {
            Row(
                modifier =
                    Modifier.size(popupMenuItemWidth, popupMenuItemHeight)
                        .padding(vertical = 0.dp, horizontal = popupMenuItemHorizontalPadding),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                compactShortcuts.forEach { shortcut ->
                    SystemShortcutMenuItem(
                        shortcut = shortcut,
                        onClick = onClick,
                        isIconOnly = true,
                    )
                }
            }
        }

        if (compactShortcuts.isNotEmpty() && standardShortcuts.isNotEmpty()) {
            Box(
                modifier =
                    Modifier.size(
                        popupMenuItemWidth,
                        ComposePopupDimens.systemShortcutsDividerHeight,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Spacer(
                    modifier =
                        Modifier.size(
                                ComposePopupDimens.systemShortcutsDividerWidth,
                                ComposePopupDimens.systemShortcutsDividerHeight,
                            )
                            .background(colorResource(R.color.materialColorOutlineVariant))
                )
            }
        }

        if (standardShortcuts.isNotEmpty()) {
            standardShortcuts.forEach { shortcut ->
                SystemShortcutMenuItem(shortcut = shortcut, onClick = onClick)
            }
        }
    }
}

@Composable
private fun DeepShortcutsContent(
    deepShortcuts: List<ItemInfoWithIcon?>,
    onClick: (PopupClickEvent) -> Unit,
    onAddButtonClick: ((ItemInfoWithIcon) -> Unit)?,
    onDeepShortcutLongPress: (ItemInfoWithIcon, Offset) -> Unit,
) {
    Column(modifier = Modifier.testTag(DEEP_SHORTCUTS_CONTAINER)) {
        deepShortcuts.forEach { shortcut ->
            DeepShortcutMenuItem(
                shortcut = shortcut,
                onClick = onClick,
                onAddButtonClick = onAddButtonClick,
                onLongClick = { item, offset -> onDeepShortcutLongPress(item, offset) },
            )
        }
    }
}

object ComposePopupDimens {
    val popupCornerRadius = 24.dp
    val popupContentSpacerHeight = 2.dp
    val systemShortcutsDividerHeight = 1.dp
    val systemShortcutsDividerWidth = 180.dp
}

private object ComposePopupAnimations {
    val popupContentSpringSpec = spring<Float>(dampingRatio = 1f, stiffness = 300f)
    val popupContainerSpringSpec = spring<IntSize>(dampingRatio = 0.7f, stiffness = 300f)
    const val EXPANDED_CONTENT_FADE_START_PROGRESS = 0.2f
    const val EXPANDED_CONTENT_FADE_DURATION_RATIO = 0.8f
    const val COLLAPSED_CONTENT_FADE_DURATION_RATIO = 0.2f
}

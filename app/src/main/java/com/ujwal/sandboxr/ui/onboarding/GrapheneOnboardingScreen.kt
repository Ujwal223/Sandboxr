package com.ujwal.sandboxr.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sandboxr.launcher.theme.SandboxrFontFamilies
import com.sandboxr.launcher.theme.SandboxrTheme
import com.sandboxr.launcher.ui.PhosphorIcon
import com.sandboxr.launcher.ui.PhosphorIconView
import com.sandboxr.launcher.ui.rememberSandboxrHaptics
import kotlinx.coroutines.launch

/**
 * Data structure representing a single step in the user-friendly onboarding tour.
 */
data class OnboardingStep(
    val stepIndex: Int,
    val badge: String,
    val title: String,
    val subtitle: String,
    val icon: PhosphorIcon,
    val highlights: List<StepHighlight>,
    val helpfulTip: String? = null
)

data class StepHighlight(
    val title: String,
    val description: String,
    val icon: PhosphorIcon
)

/**
 * Full-screen GrapheneOS-style onboarding and tutorial flow for SANDBOXR.
 * Written in clear, non-technical language for the general public:
 * 1. Welcome & zero-tracking promise
 * 2. Separate spaces (profiles) for work, personal, and dual apps
 * 3. Simple navigation via drawer tabs and 1-tap switching
 * 4. Automatic privacy protection & tracker blocking
 * 5. Simple internet and network controls per space
 * 6. Setting as default home launcher
 */
@Composable
fun GrapheneOnboardingScreen(
    isDefaultLauncher: Boolean,
    onRequestDefaultLauncher: () -> Unit,
    onCompleteOnboarding: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberSandboxrHaptics()
    val coroutineScope = rememberCoroutineScope()

    val steps = listOf(
        OnboardingStep(
            stepIndex = 1,
            badge = "STEP 1 OF 6 // GETTING STARTED",
            title = "Welcome to SANDBOXR",
            subtitle = "A clean, fast, and completely private home screen designed to give you total control over your phone.",
            icon = PhosphorIcon.SHIELD,
            highlights = listOf(
                StepHighlight(
                    title = "Completely Private",
                    description = "No tracking, no analytics, and no ads. Everything you do stays strictly on your phone.",
                    icon = PhosphorIcon.LOCK
                ),
                StepHighlight(
                    title = "Fast and Battery Friendly",
                    description = "Engineered to run smoothly without draining your battery or slowing down your device.",
                    icon = PhosphorIcon.CPU
                ),
                StepHighlight(
                    title = "Clean and Distraction-Free",
                    description = "A simple, elegant design modeled after GrapheneOS for a calm, organized experience.",
                    icon = PhosphorIcon.GRID
                )
            ),
            helpfulTip = "You can customize your wallpaper, grid layout, and icon shapes anytime in Home Settings."
        ),
        OnboardingStep(
            stepIndex = 2,
            badge = "STEP 2 OF 6 // SEPARATE SPACES",
            title = "Separate Spaces for Your Apps",
            subtitle = "Keep your personal life, work apps, and private tools in their own dedicated spaces.",
            icon = PhosphorIcon.BOX,
            highlights = listOf(
                StepHighlight(
                    title = "Two Copies of Any App",
                    description = "Log into two different accounts on WhatsApp, Telegram, or social media simultaneously.",
                    icon = PhosphorIcon.PLUS
                ),
                StepHighlight(
                    title = "Separated Photos and Files",
                    description = "Files, photos, and messages in one space are completely hidden from other spaces.",
                    icon = PhosphorIcon.DATABASE
                ),
                StepHighlight(
                    title = "No Technical Setup",
                    description = "Create a new space in seconds with a custom name and color. Everything works right away.",
                    icon = PhosphorIcon.SHIELD
                )
            ),
            helpfulTip = "Tap '+ New Profile' in your app drawer anytime to create a fresh space."
        ),
        OnboardingStep(
            stepIndex = 3,
            badge = "STEP 3 OF 6 // EASY NAVIGATION",
            title = "Move Easily Between Spaces",
            subtitle = "Switch between your profiles with a single tap, right from your app drawer or home screen.",
            icon = PhosphorIcon.GRID,
            highlights = listOf(
                StepHighlight(
                    title = "Tabs at the Top of Your Drawer",
                    description = "Swipe up to see all your apps, then tap tabs like 'Personal', 'Work', or your custom spaces.",
                    icon = PhosphorIcon.USER
                ),
                StepHighlight(
                    title = "One-Tap Switcher Pill",
                    description = "The top bar shows which space is currently active. Tap it anytime to switch or add spaces.",
                    icon = PhosphorIcon.CARET_DOWN
                ),
                StepHighlight(
                    title = "Long-Press for Quick Actions",
                    description = "Press and hold any app icon to quickly copy it to another space or pin it to your home screen.",
                    icon = PhosphorIcon.SPARKLE
                )
            ),
            helpfulTip = "Swipe up anywhere on your home screen to open the app drawer and see your profile tabs."
        ),
        OnboardingStep(
            stepIndex = 4,
            badge = "STEP 4 OF 6 // PRIVACY SHIELD",
            title = "Stop Apps from Tracking You",
            subtitle = "SANDBOXR automatically shields your device identifiers so companies cannot link your profiles together.",
            icon = PhosphorIcon.LOCK,
            highlights = listOf(
                StepHighlight(
                    title = "Unique Identity per Space",
                    description = "Each profile appears as a completely different device to apps, keeping your accounts separate.",
                    icon = PhosphorIcon.LOCK
                ),
                StepHighlight(
                    title = "Blocks Ad Tracking",
                    description = "Prevents advertising networks and social apps from recognizing your physical phone.",
                    icon = PhosphorIcon.SHIELD
                ),
                StepHighlight(
                    title = "Works Automatically",
                    description = "Protection is active by default. You don't have to configure any complicated settings.",
                    icon = PhosphorIcon.CHECK
                )
            ),
            helpfulTip = "Apps inside private spaces can never see your real device serial number."
        ),
        OnboardingStep(
            stepIndex = 5,
            badge = "STEP 5 OF 6 // INTERNET CONTROL",
            title = "Control How Apps Connect",
            subtitle = "Decide whether a space connects normally, uses a secure VPN or proxy, or stays completely offline.",
            icon = PhosphorIcon.GLOBE,
            highlights = listOf(
                StepHighlight(
                    title = "VPN & Proxy Options",
                    description = "Direct your work apps through a secure VPN while personal apps use normal WiFi or mobile data.",
                    icon = PhosphorIcon.GLOBE
                ),
                StepHighlight(
                    title = "Completely Offline Mode",
                    description = "Turn off internet access for sensitive apps so they can never send your data anywhere.",
                    icon = PhosphorIcon.LOCK
                ),
                StepHighlight(
                    title = "Private Web Lookups",
                    description = "Protects the websites your phone connects to from being monitored by your internet provider.",
                    icon = PhosphorIcon.SHIELD
                )
            ),
            helpfulTip = "You can change the internet mode of any profile anytime from the profile settings."
        ),
        OnboardingStep(
            stepIndex = 6,
            badge = "STEP 6 OF 6 // READY TO GO",
            title = "Make SANDBOXR Your Home",
            subtitle = "Set SANDBOXR as your default launcher to enjoy smooth gestures and full privacy every time you use your phone.",
            icon = PhosphorIcon.HOME,
            highlights = listOf(
                StepHighlight(
                    title = "Smooth Daily Gestures",
                    description = "Swipe up for all apps, swipe down for notifications, and double-tap empty space to sleep.",
                    icon = PhosphorIcon.ARROW_UP
                ),
                StepHighlight(
                    title = "Helpful Clock and Date",
                    description = "Tap the time to open your alarm clock, or tap the date to jump straight to your calendar.",
                    icon = PhosphorIcon.CLOUD_SUN
                ),
                StepHighlight(
                    title = "Revisit Anytime",
                    description = "You can re-open this guide whenever you like by pressing and holding the home screen wallpaper.",
                    icon = PhosphorIcon.GEAR
                )
            ),
            helpfulTip = "Tap 'Set as Default Home Launcher' below, then tap 'Get Started' to begin!"
        )
    )

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { steps.size })

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(SandboxrTheme.colors.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                // Top Header: Step Counter & Skip Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(SandboxrTheme.colors.primaryAccent.copy(alpha = 0.15f))
                                .border(1.dp, SandboxrTheme.colors.primaryAccent.copy(alpha = 0.35f), RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            PhosphorIconView(
                                icon = PhosphorIcon.SHIELD,
                                color = SandboxrTheme.colors.primaryAccent,
                                size = 14.dp
                            )
                        }

                        Text(
                            text = "SANDBOXR // STEP ${pagerState.currentPage + 1} OF ${steps.size}",
                            fontFamily = SandboxrFontFamilies.JetBrainsMono,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.0.sp,
                            color = SandboxrTheme.colors.primaryAccent
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(SandboxrTheme.colors.surface2)
                            .clickable {
                                haptics.onCardPress()
                                onCompleteOnboarding()
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "SKIP",
                            fontFamily = SandboxrFontFamilies.JetBrainsMono,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.0.sp,
                            color = SandboxrTheme.colors.textSecondary
                        )
                    }
                }

                // Horizontal Pager for the 6 setup steps
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) { page ->
                    val step = steps[page]
                    OnboardingStepCard(
                        step = step,
                        isLastPage = page == steps.size - 1,
                        isDefaultLauncher = isDefaultLauncher,
                        onRequestDefaultLauncher = onRequestDefaultLauncher
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Navigation Bar: Back, Indicators, and Next/Finish
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back Button (or spacer on first page)
                    if (pagerState.currentPage > 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(SandboxrTheme.colors.surface2)
                                .clickable {
                                    haptics.onCardPress()
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = "BACK",
                                fontFamily = SandboxrFontFamilies.JetBrainsMono,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SandboxrTheme.colors.textPrimary
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(64.dp))
                    }

                    // Dot Page Indicators
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(steps.size) { index ->
                            val isSelected = pagerState.currentPage == index
                            Box(
                                modifier = Modifier
                                    .width(if (isSelected) 20.dp else 6.dp)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(
                                        if (isSelected) SandboxrTheme.colors.primaryAccent
                                        else SandboxrTheme.colors.surface2
                                    )
                                    .clickable {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(index)
                                        }
                                    }
                            )
                        }
                    }

                    // Next or Get Started Button
                    val isLastPage = pagerState.currentPage == steps.size - 1
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(SandboxrTheme.colors.primaryAccent)
                            .clickable {
                                haptics.onCardPress()
                                if (isLastPage) {
                                    onCompleteOnboarding()
                                } else {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                    }
                                }
                            }
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = if (isLastPage) "GET STARTED" else "NEXT",
                            fontFamily = SandboxrFontFamilies.JetBrainsMono,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.0.sp,
                            color = SandboxrTheme.colors.surface1
                        )
                    }
                }
            }
        }
    }
}

/**
 * Slide card layout for a single onboarding tutorial page.
 */
@Composable
private fun OnboardingStepCard(
    step: OnboardingStep,
    isLastPage: Boolean,
    isDefaultLauncher: Boolean,
    onRequestDefaultLauncher: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Card with Icon & Title
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(SandboxrTheme.colors.surface1)
                .border(1.dp, SandboxrTheme.colors.glassBorder, RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SandboxrTheme.colors.surface2)
                            .border(1.dp, SandboxrTheme.colors.glassBorder, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        PhosphorIconView(
                            icon = step.icon,
                            color = SandboxrTheme.colors.primaryAccent,
                            size = 22.dp
                        )
                    }

                    Text(
                        text = step.badge,
                        fontFamily = SandboxrFontFamilies.JetBrainsMono,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        color = SandboxrTheme.colors.textSecondary
                    )
                }

                Text(
                    text = step.title,
                    fontFamily = SandboxrFontFamilies.Inter,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = SandboxrTheme.colors.textPrimary
                )

                Text(
                    text = step.subtitle,
                    fontFamily = SandboxrFontFamilies.Inter,
                    fontSize = 13.sp,
                    color = SandboxrTheme.colors.textSecondary,
                    lineHeight = 18.sp
                )
            }
        }

        // Feature Highlights
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            step.highlights.forEach { highlight ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SandboxrTheme.colors.surface2.copy(alpha = 0.7f))
                        .border(1.dp, SandboxrTheme.colors.glassBorder.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(SandboxrTheme.colors.surface1),
                        contentAlignment = Alignment.Center
                    ) {
                        PhosphorIconView(
                            icon = highlight.icon,
                            color = SandboxrTheme.colors.primaryAccent,
                            size = 14.dp
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = highlight.title,
                            fontFamily = SandboxrFontFamilies.Inter,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SandboxrTheme.colors.textPrimary
                        )

                        Text(
                            text = highlight.description,
                            fontFamily = SandboxrFontFamilies.Inter,
                            fontSize = 12.sp,
                            color = SandboxrTheme.colors.textSecondary,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        // Helpful Tip Banner
        step.helpfulTip?.let { tip ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SandboxrTheme.colors.surface2)
                    .border(1.dp, SandboxrTheme.colors.glassBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(SandboxrTheme.colors.surface1),
                        contentAlignment = Alignment.Center
                    ) {
                        PhosphorIconView(
                            icon = PhosphorIcon.CHECK,
                            color = SandboxrTheme.colors.primaryAccent,
                            size = 10.dp
                        )
                    }

                    Text(
                        text = tip,
                        fontFamily = SandboxrFontFamilies.Inter,
                        fontSize = 11.sp,
                        color = SandboxrTheme.colors.textSecondary,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        // Last Page Special: Set Default Launcher Action
        if (isLastPage) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isDefaultLauncher) SandboxrTheme.colors.surface2 else SandboxrTheme.colors.primaryAccent.copy(alpha = 0.12f))
                    .border(
                        1.dp,
                        if (isDefaultLauncher) SandboxrTheme.colors.glassBorder else SandboxrTheme.colors.primaryAccent,
                        RoundedCornerShape(14.dp)
                    )
                    .clickable { onRequestDefaultLauncher() }
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isDefaultLauncher) "SANDBOXR is your default launcher" else "Set as Default Home Launcher",
                            fontFamily = SandboxrFontFamilies.Inter,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDefaultLauncher) SandboxrTheme.colors.textPrimary else SandboxrTheme.colors.primaryAccent
                        )
                        Text(
                            text = if (isDefaultLauncher) "All system gestures and home buttons are routed to SANDBOXR." else "Tap here to make SANDBOXR your main home screen.",
                            fontFamily = SandboxrFontFamilies.Inter,
                            fontSize = 11.sp,
                            color = SandboxrTheme.colors.textSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (isDefaultLauncher) SandboxrTheme.colors.primaryAccent else SandboxrTheme.colors.surface1),
                        contentAlignment = Alignment.Center
                    ) {
                        PhosphorIconView(
                            icon = if (isDefaultLauncher) PhosphorIcon.CHECK else PhosphorIcon.HOME,
                            color = if (isDefaultLauncher) SandboxrTheme.colors.surface1 else SandboxrTheme.colors.primaryAccent,
                            size = 14.dp
                        )
                    }
                }
            }
        }
    }
}

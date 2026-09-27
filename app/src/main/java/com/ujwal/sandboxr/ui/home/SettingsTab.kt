package com.ujwal.sandboxr.ui.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sandboxr.launcher.theme.SandboxrColors
import com.sandboxr.launcher.theme.SandboxrFontFamilies
import com.sandboxr.launcher.theme.SandboxrSpacingTokens
import com.sandboxr.launcher.theme.SandboxrTheme
import com.sandboxr.launcher.ui.PhosphorIcon
import com.sandboxr.launcher.ui.PhosphorIconView

/**
 * Settings and Platform Security configuration screen.
 */
@Composable
fun SettingsTab(
    onNotice: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = SandboxrSpacingTokens.Normal)
            .padding(bottom = 100.dp) // clear bottom navigation bar
    ) {
        // Section Header
        Text(
            text = "PLATFORM CONFIGURATION",
            style = SandboxrTheme.typography.label.copy(letterSpacing = 1.2.sp),
            color = SandboxrColors.PrimaryAccent
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Sandboxr Settings",
            style = SandboxrTheme.typography.title,
            color = SandboxrColors.TextPrimaryDark
        )
        Text(
            text = "Kernel evasion, clipboard sandboxing, and system launcher preferences.",
            style = SandboxrTheme.typography.body.copy(fontSize = 13.sp),
            color = SandboxrColors.TextSecondaryDark,
            modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
        )

        // 1. Default Home App Card
        val cardShape = RoundedCornerShape(16.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(cardShape)
                .background(SandboxrColors.SurfaceLevel1Dark)
                .border(1.dp, Color.White.copy(alpha = 0.08f), cardShape)
                .padding(16.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(SandboxrColors.PrimaryAccent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        PhosphorIconView(icon = PhosphorIcon.BOX, color = SandboxrColors.PrimaryAccent, size = 20.dp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Default System Launcher",
                            style = SandboxrTheme.typography.body.copy(fontWeight = FontWeight.SemiBold),
                            color = SandboxrColors.TextPrimaryDark
                        )
                        Text(
                            text = "Set Sandboxr as your primary Android home screen",
                            fontSize = 12.sp,
                            color = SandboxrColors.TextSecondaryDark
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        try {
                            val intent = Intent(Settings.ACTION_HOME_SETTINGS)
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            try {
                                val intent = Intent(Settings.ACTION_SETTINGS)
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                onNotice("Could not open Home Settings: ${e.message}")
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SandboxrColors.PrimaryAccent),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(42.dp)
                ) {
                    Text("Configure Default Home App", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 2. Engine & Isolation Telemetry
        Text(
            text = "SYSTEM ENGINE TELEMETRY",
            style = SandboxrTheme.typography.label.copy(letterSpacing = 1.2.sp),
            color = SandboxrColors.TextSecondaryDark
        )
        Spacer(modifier = Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingTelemetryRow(
                title = "16KB Page Alignment",
                subtitle = "Android 15+ ELF LOAD segment alignment",
                badgeText = "VERIFIED (16384 B)",
                badgeColor = SandboxrColors.Success
            )

            SettingTelemetryRow(
                title = "Native Hooking Engine",
                subtitle = "ShadowHook (PLT/Inline) + ByteHook (libc/fs)",
                badgeText = "ACTIVE",
                badgeColor = SandboxrColors.Success
            )

            SettingTelemetryRow(
                title = "Hidden API Bypass",
                subtitle = "AndroidHiddenApiBypass Unsafe memory unreflection",
                badgeText = "ACTIVE (API 29–37)",
                badgeColor = SandboxrColors.Success
            )

            SettingTelemetryRow(
                title = "GMS Sandbox Interceptor",
                subtitle = "Google Play Services dynamic stub code dispatch",
                badgeText = "SERVICE_MISSING (Code 1)",
                badgeColor = SandboxrColors.PrimaryAccent
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 3. About & License
        Text(
            text = "ABOUT SANDBOXR",
            style = SandboxrTheme.typography.label.copy(letterSpacing = 1.2.sp),
            color = SandboxrColors.TextSecondaryDark
        )
        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(SandboxrColors.SurfaceLevel1Dark)
                .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(14.dp))
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Version",
                        fontSize = 13.sp,
                        color = SandboxrColors.TextSecondaryDark
                    )
                    Text(
                        text = "1.0.0 (Production)",
                        fontFamily = SandboxrFontFamilies.JetBrainsMono,
                        fontSize = 13.sp,
                        color = SandboxrColors.TextPrimaryDark
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "License",
                        fontSize = 13.sp,
                        color = SandboxrColors.TextSecondaryDark
                    )
                    Text(
                        text = "GNU General Public License v3.0",
                        fontFamily = SandboxrFontFamilies.JetBrainsMono,
                        fontSize = 12.sp,
                        color = SandboxrColors.PrimaryAccent
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Privacy Guarantee",
                        fontSize = 13.sp,
                        color = SandboxrColors.TextSecondaryDark
                    )
                    Text(
                        text = "Zero Telemetry / Zero Root",
                        fontFamily = SandboxrFontFamilies.JetBrainsMono,
                        fontSize = 12.sp,
                        color = SandboxrColors.Success
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingTelemetryRow(
    title: String,
    subtitle: String,
    badgeText: String,
    badgeColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SandboxrColors.SurfaceLevel1Dark)
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = SandboxrTheme.typography.body.copy(fontWeight = FontWeight.Medium),
                    color = SandboxrColors.TextPrimaryDark
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = SandboxrColors.TextSecondaryDark
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(badgeColor.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = badgeText,
                    fontFamily = SandboxrFontFamilies.JetBrainsMono,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = badgeColor
                )
            }
        }
    }
}

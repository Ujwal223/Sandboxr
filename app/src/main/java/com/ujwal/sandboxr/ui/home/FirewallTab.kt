package com.ujwal.sandboxr.ui.home

import android.content.Context
import android.content.Intent
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ujwal.sandboxr.data.db.EnvironmentEntity
import com.ujwal.sandboxr.data.db.NetworkConfig
import com.sandboxr.launcher.theme.SandboxrColors
import com.sandboxr.launcher.theme.SandboxrFontFamilies
import com.sandboxr.launcher.theme.SandboxrSpacingTokens
import com.sandboxr.launcher.theme.SandboxrTheme
import com.sandboxr.launcher.ui.PhosphorIcon
import com.sandboxr.launcher.ui.PhosphorIconView
import com.ujwal.sandboxr.ui.components.PrivateDnsMode
import com.ujwal.sandboxr.ui.components.PrivateDnsWarningBanner

/**
 * Firewall & Network Routing tab displaying per-container routing and DNS ad-blocking status.
 */
@Composable
fun FirewallTab(
    environments: List<EnvironmentEntity>,
    onUpdateNetworkMode: (envId: String, mode: NetworkConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var adBlockGlobal by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = SandboxrSpacingTokens.Normal)
            .padding(bottom = 100.dp) // clear bottom navigation bar
    ) {
        // Section Header
        Text(
            text = "LOCAL DNS FIREWALL",
            style = SandboxrTheme.typography.label.copy(letterSpacing = 1.2.sp),
            color = SandboxrColors.PrimaryAccent
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Network & Routing Control",
            style = SandboxrTheme.typography.title,
            color = SandboxrColors.TextPrimaryDark
        )
        Text(
            text = "Per-environment socket isolation and local RethinkDNS firestack ad-blocking.",
            style = SandboxrTheme.typography.body.copy(fontSize = 13.sp),
            color = SandboxrColors.TextSecondaryDark,
            modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
        )

        // Private DNS Warning Banner
        PrivateDnsWarningBanner(
            onOpenSettings = {
                try {
                    context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
                } catch (_: Exception) {
                    try {
                        context.startActivity(Intent(Settings.ACTION_SETTINGS))
                    } catch (_: Exception) {}
                }
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Card 1: Local In-Process DNS Engine Status
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PhosphorIconView(
                            icon = PhosphorIcon.SHIELD,
                            color = SandboxrColors.Success,
                            size = 22.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Headless Firestack Engine",
                                style = SandboxrTheme.typography.body.copy(fontWeight = FontWeight.SemiBold),
                                color = SandboxrColors.TextPrimaryDark
                            )
                            Text(
                                text = "Steven Black In-RAM Blocklist (~184,000 domains)",
                                style = SandboxrTheme.typography.label.copy(fontSize = 11.sp),
                                color = SandboxrColors.TextSecondaryDark
                            )
                        }
                    }

                    Switch(
                        checked = adBlockGlobal,
                        onCheckedChange = { adBlockGlobal = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = SandboxrColors.PrimaryAccent,
                            checkedTrackColor = SandboxrColors.PrimaryAccent.copy(alpha = 0.4f),
                            uncheckedThumbColor = SandboxrColors.TextSecondaryDark,
                            uncheckedTrackColor = SandboxrColors.SurfaceLevel2Dark
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SandboxrColors.SurfaceLevel2Dark)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "STATUS",
                            fontSize = 10.sp,
                            color = SandboxrColors.TextSecondaryDark
                        )
                        Text(
                            text = if (adBlockGlobal) "ACTIVE" else "BYPASS",
                            fontFamily = SandboxrFontFamilies.JetBrainsMono,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (adBlockGlobal) SandboxrColors.Success else SandboxrColors.Warning
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "QUERIES",
                            fontSize = 10.sp,
                            color = SandboxrColors.TextSecondaryDark
                        )
                        Text(
                            text = "0 ms avg",
                            fontFamily = SandboxrFontFamilies.JetBrainsMono,
                            fontSize = 12.sp,
                            color = SandboxrColors.TextMonospaceDark
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "TUN0 INTERFACE",
                            fontSize = 10.sp,
                            color = SandboxrColors.TextSecondaryDark
                        )
                        Text(
                            text = "10.111.222.1",
                            fontFamily = SandboxrFontFamilies.JetBrainsMono,
                            fontSize = 12.sp,
                            color = SandboxrColors.TextMonospaceDark
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Card 2: Per-Environment Routing Matrix
        Text(
            text = "CONTAINER ROUTING MATRIX",
            style = SandboxrTheme.typography.label.copy(letterSpacing = 1.2.sp),
            color = SandboxrColors.TextSecondaryDark
        )
        Spacer(modifier = Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            environments.forEach { env ->
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(Color(env.colorTag), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = env.displayName,
                                    style = SandboxrTheme.typography.body.copy(fontWeight = FontWeight.Medium),
                                    color = SandboxrColors.TextPrimaryDark
                                )
                                Text(
                                    text = if (env.isSystem) "Unrouted host system" else "Userspace container",
                                    fontSize = 11.sp,
                                    color = SandboxrColors.TextSecondaryDark
                                )
                            }
                        }

                        // Mode Selector Pills
                        if (!env.isSystem) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                NetworkConfig.entries.forEach { mode ->
                                    val isSelected = env.networkConfig == mode
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                if (isSelected) SandboxrColors.PrimaryAccent.copy(alpha = 0.25f)
                                                else SandboxrColors.SurfaceLevel2Dark
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (isSelected) SandboxrColors.PrimaryAccent else Color.Transparent,
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                            .clickable { onUpdateNetworkMode(env.id, mode) }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = mode.name.take(4),
                                            fontFamily = SandboxrFontFamilies.JetBrainsMono,
                                            fontSize = 10.sp,
                                            color = if (isSelected) SandboxrColors.PrimaryAccent else SandboxrColors.TextSecondaryDark
                                        )
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = "DIRECT",
                                fontFamily = SandboxrFontFamilies.JetBrainsMono,
                                fontSize = 11.sp,
                                color = SandboxrColors.Success
                            )
                        }
                    }
                }
            }
        }
    }
}

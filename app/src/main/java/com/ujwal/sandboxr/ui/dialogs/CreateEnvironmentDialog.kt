package com.ujwal.sandboxr.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ujwal.sandboxr.data.db.ClipboardMode
import com.ujwal.sandboxr.data.db.NetworkConfig
import com.sandboxr.launcher.theme.SandboxrColors
import com.sandboxr.launcher.theme.SandboxrSpacingTokens
import com.sandboxr.launcher.theme.SandboxrTheme
import com.sandboxr.launcher.ui.EnvironmentIconType
import com.sandboxr.launcher.ui.EnvironmentVectorGlyph

private val PALETTE_COLORS = listOf(
    0xFF3A4A6BL to "Slate",
    0xFF4A3A6BL to "Violet",
    0xFF6B3A4AL to "Rose",
    0xFF6B5A2AL to "Amber",
    0xFF2A5A5AL to "Teal",
    0xFF3A5A2AL to "Moss",
    0xFF6B2A2AL to "Crimson",
    0xFF3A3A3AL to "Graphite"
)

/**
 * High-precision Obsidian + Liquid Glass dialog for provisioning new virtual containers.
 */
@Composable
fun CreateEnvironmentDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (
        name: String,
        colorTag: Long,
        iconType: EnvironmentIconType,
        networkConfig: NetworkConfig,
        gmsEnabled: Boolean,
        clipboardMode: ClipboardMode
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedColor by remember { mutableLongStateOf(PALETTE_COLORS[1].first) }
    var selectedIcon by remember { mutableStateOf(EnvironmentIconType.SHIELD) }
    var selectedNetwork by remember { mutableStateOf(NetworkConfig.DIRECT) }
    var gmsEnabled by remember { mutableStateOf(false) }
    var clipboardMode by remember { mutableStateOf(ClipboardMode.ISOLATED) }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(24.dp)
                ),
            color = SandboxrColors.SurfaceLevel1Dark
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Text(
                    text = "NEW VIRTUAL SANDBOX",
                    style = SandboxrTheme.typography.label.copy(letterSpacing = 1.5.sp),
                    color = SandboxrColors.PrimaryAccent
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Provision Container",
                    style = SandboxrTheme.typography.title,
                    color = SandboxrColors.TextPrimaryDark
                )
                Text(
                    text = "Zero-root userspace sandbox with hardware ID spoofing and network isolation.",
                    style = SandboxrTheme.typography.body.copy(fontSize = 13.sp),
                    color = SandboxrColors.TextSecondaryDark,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                // Name Input
                Text(
                    text = "Container Name",
                    style = SandboxrTheme.typography.label,
                    color = SandboxrColors.TextPrimaryDark
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 24) name = it },
                    placeholder = { Text("e.g. Work, Untrusted, Finance", color = SandboxrColors.TextTertiaryDark) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SandboxrColors.PrimaryAccent,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                        focusedTextColor = SandboxrColors.TextPrimaryDark,
                        unfocusedTextColor = SandboxrColors.TextPrimaryDark,
                        focusedContainerColor = SandboxrColors.SurfaceLevel2Dark,
                        unfocusedContainerColor = SandboxrColors.SurfaceLevel2Dark
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Color Selection
                Text(
                    text = "Container Color Tag",
                    style = SandboxrTheme.typography.label,
                    color = SandboxrColors.TextPrimaryDark
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(PALETTE_COLORS) { (colorLong, _) ->
                        val isSelected = selectedColor == colorLong
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(colorLong))
                                .clickable { selectedColor = colorLong }
                                .then(
                                    if (isSelected) {
                                        Modifier.border(2.dp, Color.White, CircleShape)
                                    } else {
                                        Modifier
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(Color.White, CircleShape)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Icon Glyph Selection
                Text(
                    text = "Vector Glyph",
                    style = SandboxrTheme.typography.label,
                    color = SandboxrColors.TextPrimaryDark
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    EnvironmentIconType.entries.forEach { iconType ->
                        val isSelected = selectedIcon == iconType
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) SandboxrColors.PrimaryAccent.copy(alpha = 0.25f)
                                    else SandboxrColors.SurfaceLevel2Dark
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) SandboxrColors.PrimaryAccent else Color.White.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedIcon = iconType },
                            contentAlignment = Alignment.Center
                        ) {
                            EnvironmentVectorGlyph(
                                type = iconType,
                                color = if (isSelected) SandboxrColors.PrimaryAccent else SandboxrColors.TextSecondaryDark
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Network Routing Mode
                Text(
                    text = "Network Routing",
                    style = SandboxrTheme.typography.label,
                    color = SandboxrColors.TextPrimaryDark
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    NetworkConfig.entries.forEach { mode ->
                        val isSelected = selectedNetwork == mode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) SandboxrColors.SurfaceLevel3Dark
                                    else SandboxrColors.SurfaceLevel2Dark
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) SandboxrColors.PrimaryAccent.copy(alpha = 0.6f)
                                    else Color.White.copy(alpha = 0.05f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedNetwork = mode }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = mode.name,
                                    style = SandboxrTheme.typography.body.copy(fontWeight = FontWeight.Medium),
                                    color = SandboxrColors.TextPrimaryDark
                                )
                                val desc = when (mode) {
                                    NetworkConfig.DIRECT -> "Direct OS connection via local DNS filter"
                                    NetworkConfig.WIREGUARD -> "Tunnel via active WireGuard config"
                                    NetworkConfig.SOCKS5 -> "Route via configured SOCKS5 proxy"
                                    NetworkConfig.BLOCKED -> "Air-gapped: all socket connections killed"
                                }
                                Text(
                                    text = desc,
                                    style = SandboxrTheme.typography.label.copy(fontSize = 11.sp),
                                    color = SandboxrColors.TextSecondaryDark
                                )
                            }
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(SandboxrColors.PrimaryAccent, CircleShape)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // GMS Cutoff Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SandboxrColors.SurfaceLevel2Dark)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Google Play Services (GMS)",
                            style = SandboxrTheme.typography.body.copy(fontWeight = FontWeight.Medium),
                            color = SandboxrColors.TextPrimaryDark
                        )
                        Text(
                            text = if (gmsEnabled) "Allowed: Play Services calls pass through"
                            else "Cutoff: Cleanly returns SERVICE_MISSING (Stops telemetry)",
                            style = SandboxrTheme.typography.label.copy(fontSize = 11.sp),
                            color = if (gmsEnabled) SandboxrColors.Warning else SandboxrColors.Success
                        )
                    }
                    Switch(
                        checked = gmsEnabled,
                        onCheckedChange = { gmsEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = SandboxrColors.PrimaryAccent,
                            checkedTrackColor = SandboxrColors.PrimaryAccent.copy(alpha = 0.4f),
                            uncheckedThumbColor = SandboxrColors.TextSecondaryDark,
                            uncheckedTrackColor = SandboxrColors.SurfaceLevel1Dark
                        )
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("Cancel", color = SandboxrColors.TextSecondaryDark)
                    }

                    Button(
                        onClick = {
                            val finalName = if (name.isBlank()) "Sandbox" else name.trim()
                            onConfirm(
                                finalName,
                                selectedColor,
                                selectedIcon,
                                selectedNetwork,
                                gmsEnabled,
                                clipboardMode
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SandboxrColors.PrimaryAccent),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1.5f).height(48.dp)
                    ) {
                        Text(
                            text = "Create Sandbox",
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

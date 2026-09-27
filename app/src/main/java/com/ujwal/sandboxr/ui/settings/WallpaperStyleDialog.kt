package com.ujwal.sandboxr.ui.settings

import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.sandboxr.launcher.theme.SandboxrFontFamilies
import com.sandboxr.launcher.theme.SandboxrTheme
import com.sandboxr.launcher.theme.getLauncherIconShape
import com.sandboxr.launcher.ui.PhosphorIcon
import com.sandboxr.launcher.ui.PhosphorIconView

/**
 * Curated wallpaper preset options.
 */
data class WallpaperPreset(
    val id: String,
    val title: String,
    val subtitle: String,
    val previewColors: List<Color>
)

val WALLPAPER_PRESETS = listOf(
    WallpaperPreset(
        id = "system",
        title = "System Live",
        subtitle = "Current OS device wallpaper",
        previewColors = listOf(Color(0xFF1E293B), Color(0xFF0F172A))
    ),
    WallpaperPreset(
        id = "pixel_dark",
        title = "Pixel Feather Dark",
        subtitle = "Pixel 9 Pro textured dark feather",
        previewColors = listOf(Color(0xFF121418), Color(0xFF1F242B), Color(0xFF0D0E11))
    ),
    WallpaperPreset(
        id = "amoled_obsidian",
        title = "OLED Obsidian",
        subtitle = "Zero battery drain pure pitch black",
        previewColors = listOf(Color.Black, Color(0xFF050505))
    ),
    WallpaperPreset(
        id = "midnight_aurora",
        title = "Midnight Aurora",
        subtitle = "Deep cosmic indigo gradient",
        previewColors = listOf(Color(0xFF0B1021), Color(0xFF1A1B4B), Color(0xFF090A0F))
    ),
    WallpaperPreset(
        id = "cyber_grid",
        title = "Cyber Titanium",
        subtitle = "Tactical privacy carbon texture",
        previewColors = listOf(Color(0xFF1A1D20), Color(0xFF0F1113))
    )
)

/**
 * Wallpaper & Style Dialog modeled after Android 14/15 Pixel & GrapheneOS customization skins.
 * Allows changing:
 * - Wallpaper preset, device photo gallery wallpaper, or system wallpaper picker
 * - Dimming level scrim slider
 * - Icon shapes (Circle, Squircle, Rounded Square, Teardrop, Pebble)
 * - Themed icons toggle (Material You monochrome)
 * - Grid density (4x4, 4x5, 5x5, 6x5)
 */
@Composable
fun WallpaperStyleDialog(
    currentSettings: LauncherSettings,
    onSaveSettings: (LauncherSettings) -> Unit,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    var settings by remember { mutableStateOf(currentSettings) }

    // System Image Picker Launcher for custom gallery wallpaper
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            settings = settings.copy(
                wallpaperPreset = "custom",
                customWallpaperUri = uri.toString()
            )
        }
    }

    Dialog(onDismissRequest = {
        onSaveSettings(settings)
        onDismissRequest()
    }) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(SandboxrTheme.colors.surface1)
                .border(1.dp, SandboxrTheme.colors.glassBorder, RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "WALLPAPER & STYLE",
                            fontFamily = SandboxrFontFamilies.JetBrainsMono,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = SandboxrTheme.colors.primaryAccent
                        )
                        Text(
                            text = "Customize Home screen appearance",
                            fontFamily = SandboxrFontFamilies.Inter,
                            fontSize = 12.sp,
                            color = SandboxrTheme.colors.textSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(SandboxrTheme.colors.surface2)
                            .clickable {
                                onSaveSettings(settings)
                                onDismissRequest()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        PhosphorIconView(
                            icon = PhosphorIcon.X,
                            color = SandboxrTheme.colors.textSecondary,
                            size = 14.dp
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(480.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // ── Section 1: Wallpaper Presets & Picker ──────────────────
                    item {
                        Text(
                            text = "WALLPAPER",
                            fontFamily = SandboxrFontFamilies.JetBrainsMono,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.0.sp,
                            color = SandboxrTheme.colors.primaryAccent
                        )
                    }

                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(WALLPAPER_PRESETS) { preset ->
                                val isSelected = settings.wallpaperPreset == preset.id
                                Box(
                                    modifier = Modifier
                                        .width(105.dp)
                                        .height(150.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Brush.verticalGradient(preset.previewColors))
                                        .border(
                                            width = if (isSelected) 2.5.dp else 1.dp,
                                            color = if (isSelected) SandboxrTheme.colors.primaryAccent else SandboxrTheme.colors.glassBorder,
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .clickable {
                                            settings = settings.copy(
                                                wallpaperPreset = preset.id,
                                                customWallpaperUri = null
                                            )
                                        }
                                        .padding(10.dp),
                                    contentAlignment = Alignment.BottomStart
                                ) {
                                    Column {
                                        Text(
                                            text = preset.title,
                                            fontFamily = SandboxrFontFamilies.Inter,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = if (isSelected) "Active" else "Select",
                                            fontFamily = SandboxrFontFamilies.JetBrainsMono,
                                            fontSize = 9.sp,
                                            color = if (isSelected) SandboxrTheme.colors.primaryAccent else Color.White.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // External / Gallery Wallpaper Buttons
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Pick from Photos
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SandboxrTheme.colors.surface2)
                                    .border(1.dp, SandboxrTheme.colors.glassBorder, RoundedCornerShape(12.dp))
                                    .clickable { imagePickerLauncher.launch("image/*") }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    PhosphorIconView(
                                        icon = PhosphorIcon.PALETTE,
                                        color = SandboxrTheme.colors.primaryAccent,
                                        size = 15.dp
                                    )
                                    Text(
                                        text = "Choose Photo",
                                        fontFamily = SandboxrFontFamilies.Inter,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = SandboxrTheme.colors.textPrimary
                                    )
                                }
                            }

                            // System Wallpaper Settings
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SandboxrTheme.colors.surface2)
                                    .border(1.dp, SandboxrTheme.colors.glassBorder, RoundedCornerShape(12.dp))
                                    .clickable {
                                        try {
                                            val intent = Intent(Intent.ACTION_SET_WALLPAPER).apply {
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            context.startActivity(Intent.createChooser(intent, "Set System Wallpaper"))
                                        } catch (_: Exception) {}
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    PhosphorIconView(
                                        icon = PhosphorIcon.GEAR,
                                        color = SandboxrTheme.colors.textSecondary,
                                        size = 15.dp
                                    )
                                    Text(
                                        text = "System Picker",
                                        fontFamily = SandboxrFontFamilies.Inter,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = SandboxrTheme.colors.textPrimary
                                    )
                                }
                            }
                        }
                    }

                    // Wallpaper Dimming
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SandboxrTheme.colors.surface2)
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Wallpaper Dimming",
                                    fontFamily = SandboxrFontFamilies.Inter,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = SandboxrTheme.colors.textPrimary
                                )
                                Text(
                                    text = "${(settings.wallpaperDimming * 100).toInt()}%",
                                    fontFamily = SandboxrFontFamilies.JetBrainsMono,
                                    fontSize = 12.sp,
                                    color = SandboxrTheme.colors.primaryAccent
                                )
                            }
                            Slider(
                                value = settings.wallpaperDimming,
                                onValueChange = { settings = settings.copy(wallpaperDimming = it) },
                                valueRange = 0.0f..0.85f,
                                colors = SliderDefaults.colors(
                                    thumbColor = SandboxrTheme.colors.primaryAccent,
                                    activeTrackColor = SandboxrTheme.colors.primaryAccent,
                                    inactiveTrackColor = SandboxrTheme.colors.surface1
                                )
                            )
                        }
                    }

                    // ── Section 2: Icon Shapes ────────────────────────────────
                    item {
                        Text(
                            text = "ICON SHAPE",
                            fontFamily = SandboxrFontFamilies.JetBrainsMono,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.0.sp,
                            color = SandboxrTheme.colors.primaryAccent
                        )
                    }

                    item {
                        val shapes = listOf("Circle", "Squircle", "Rounded Square", "Teardrop", "Pebble")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            shapes.forEach { shapeName ->
                                val isSelected = settings.iconShape.equals(shapeName, ignoreCase = true)
                                val previewShape = getLauncherIconShape(shapeName)
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) SandboxrTheme.colors.primaryAccent.copy(alpha = 0.15f) else SandboxrTheme.colors.surface2)
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) SandboxrTheme.colors.primaryAccent else SandboxrTheme.colors.glassBorder,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { settings = settings.copy(iconShape = shapeName) }
                                        .padding(vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(previewShape)
                                            .background(if (isSelected) SandboxrTheme.colors.primaryAccent else SandboxrTheme.colors.textSecondary)
                                    )
                                    Text(
                                        text = shapeName.split(" ").first(),
                                        fontFamily = SandboxrFontFamilies.Inter,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isSelected) SandboxrTheme.colors.primaryAccent else SandboxrTheme.colors.textPrimary
                                    )
                                }
                            }
                        }
                    }

                    // Themed Icons Toggle
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SandboxrTheme.colors.surface2)
                                .clickable { settings = settings.copy(themedIcons = !settings.themedIcons) }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Themed icons",
                                    fontFamily = SandboxrFontFamilies.Inter,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = SandboxrTheme.colors.textPrimary
                                )
                                Text(
                                    text = "Monochrome icons matching accent color",
                                    fontFamily = SandboxrFontFamilies.Inter,
                                    fontSize = 11.sp,
                                    color = SandboxrTheme.colors.textSecondary
                                )
                            }
                            Switch(
                                checked = settings.themedIcons,
                                onCheckedChange = { settings = settings.copy(themedIcons = it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = SandboxrTheme.colors.surface1,
                                    checkedTrackColor = SandboxrTheme.colors.primaryAccent,
                                    uncheckedThumbColor = SandboxrTheme.colors.textSecondary,
                                    uncheckedTrackColor = SandboxrTheme.colors.surface1
                                )
                            )
                        }
                    }

                    // ── Section 3: App Grid ───────────────────────────────────
                    item {
                        Text(
                            text = "APP GRID",
                            fontFamily = SandboxrFontFamilies.JetBrainsMono,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.0.sp,
                            color = SandboxrTheme.colors.primaryAccent
                        )
                    }

                    item {
                        val gridOptions = listOf(
                            Pair(4, 4),
                            Pair(4, 5),
                            Pair(5, 5),
                            Pair(6, 5)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            gridOptions.forEach { (cols, rows) ->
                                val isSelected = settings.gridColumns == cols && settings.gridRows == rows
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) SandboxrTheme.colors.primaryAccent else SandboxrTheme.colors.surface2)
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) SandboxrTheme.colors.primaryAccent else SandboxrTheme.colors.glassBorder,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable { settings = settings.copy(gridColumns = cols, gridRows = rows) }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${cols}x${rows}",
                                        fontFamily = SandboxrFontFamilies.JetBrainsMono,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) SandboxrTheme.colors.surface1 else SandboxrTheme.colors.textPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                // Apply Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SandboxrTheme.colors.primaryAccent)
                        .clickable {
                            onSaveSettings(settings)
                            onDismissRequest()
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Apply Wallpaper & Style",
                        fontFamily = SandboxrFontFamilies.Inter,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SandboxrTheme.colors.surface1
                    )
                }
            }
        }
    }
}

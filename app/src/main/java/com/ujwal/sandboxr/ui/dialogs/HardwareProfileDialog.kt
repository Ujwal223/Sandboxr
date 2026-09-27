package com.ujwal.sandboxr.ui.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.window.Dialog
import com.ujwal.sandboxr.data.db.EnvironmentEntity
import com.sandboxr.launcher.theme.SandboxrColors
import com.sandboxr.launcher.theme.SandboxrFontFamilies
import com.sandboxr.launcher.theme.SandboxrTheme

/**
 * High-security dialog displaying spoofed hardware identifiers in JetBrains Mono.
 * Strictly adheres to DESIGN.md Sections 3, 10 & 14.
 */
@Composable
fun HardwareProfileDialog(
    environment: EnvironmentEntity,
    onDismissRequest: () -> Unit,
    onCopyNotice: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val hw = environment.hardwareIds

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
                    text = "VIRTUAL HARDWARE IDENTITY",
                    style = SandboxrTheme.typography.label.copy(letterSpacing = 1.5.sp),
                    color = SandboxrColors.PrimaryAccent
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = environment.displayName,
                    style = SandboxrTheme.typography.title,
                    color = SandboxrColors.TextPrimaryDark
                )
                Text(
                    text = if (environment.isSystem) {
                        "Host system hardware identity. Unmodified physical hardware IDs."
                    } else {
                        "Dynamic userspace spoofed identity hooked via ShadowHook & ByteHook. Apps in this sandbox only perceive these synthesized values."
                    },
                    style = SandboxrTheme.typography.body.copy(fontSize = 13.sp),
                    color = SandboxrColors.TextSecondaryDark,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                // Identifiers
                HardwareRow(
                    label = "IMEI (Luhn-compliant)",
                    value = if (environment.isSystem) "System Native IMEI" else hw.imei,
                    context = context,
                    onCopied = onCopyNotice
                )

                HardwareRow(
                    label = "ANDROID ID (64-bit Hex)",
                    value = if (environment.isSystem) "System Native Android ID" else hw.androidId,
                    context = context,
                    onCopied = onCopyNotice
                )

                HardwareRow(
                    label = "WiFi MAC Address",
                    value = if (environment.isSystem) "System Native MAC" else hw.macAddress,
                    context = context,
                    onCopied = onCopyNotice
                )

                HardwareRow(
                    label = "Build Serial Number",
                    value = if (environment.isSystem) "System Native Serial" else hw.serial,
                    context = context,
                    onCopied = onCopyNotice
                )

                HardwareRow(
                    label = "Google Advertising ID (GAID)",
                    value = if (environment.isSystem) "System GAID" else hw.advertisingId,
                    context = context,
                    onCopied = onCopyNotice
                )

                HardwareRow(
                    label = "WiFi BSSID",
                    value = if (environment.isSystem) "System Native BSSID" else hw.wifiBssid,
                    context = context,
                    onCopied = onCopyNotice
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismissRequest,
                    colors = ButtonDefaults.buttonColors(containerColor = SandboxrColors.PrimaryAccent),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Close", fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun HardwareRow(
    label: String,
    value: String,
    context: Context,
    onCopied: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(SandboxrColors.SurfaceLevel2Dark)
            .clickable {
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                cm?.setPrimaryClip(ClipData.newPlainText(label, value))
                onCopied("Copied $label")
            }
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = SandboxrColors.TextSecondaryDark
            )
            Text(
                text = "TAP TO COPY",
                fontSize = 9.sp,
                color = SandboxrColors.PrimaryAccent
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontFamily = SandboxrFontFamilies.JetBrainsMono,
            fontSize = 13.sp,
            color = SandboxrColors.TextMonospaceDark
        )
    }
}

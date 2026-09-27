package com.ujwal.sandboxr.ui.components

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

// ─── Private DNS Conflict Detection Banner ────────────────────────────────────
//
// System Android Private DNS (Settings.Global.PRIVATE_DNS_MODE) intercepts
// all DNS traffic at the OS level BEFORE our local tun0 interface receives it.
// This means our DNS ad-blocking and per-environment upstream DNS configuration
// are silently bypassed when Private DNS is set to "strict" or "opportunistic".
//
// This banner detects that condition on Android 10+ (API 29) and prompts the
// user to either:
//  • Set Private DNS to "Off" to let SANDBOXR handle DNS completely.
//  • Set Private DNS to a compatible upstream (e.g., dns.sandbox.com) if the
//    user wants to use their DoH server through our local VPN tunnel.
//
// Aligns with PRD Section 7.3 and PROJECT_EXECUTION_PLAN Phase 5, Task P05-T05.

/**
 * Represents the current Android system Private DNS configuration state.
 */
enum class PrivateDnsMode {
    OFF,          // DNS handled entirely by SANDBOXR local tun0
    OPPORTUNISTIC,// TLS if available, otherwise plaintext — partially conflicts
    STRICT;       // Forces system DoH/DoT — SANDBOXR DNS completely bypassed

    companion object {
        /**
         * Reads the current Private DNS mode from [Settings.Global] on API 29+.
         */
        fun fromSystem(context: Context): PrivateDnsMode {
            return try {
                val rawMode = Settings.Global.getString(
                    context.contentResolver,
                    "private_dns_mode" // Settings.Global.PRIVATE_DNS_MODE
                ) ?: return OFF
                when (rawMode) {
                    "opportunistic" -> OPPORTUNISTIC
                    "hostname" -> STRICT
                    else -> OFF
                }
            } catch (_: Exception) {
                OFF
            }
        }

        /**
         * Returns whether SANDBOXR DNS functionality is affected by the current mode.
         */
        fun isConflicting(mode: PrivateDnsMode): Boolean =
            mode == STRICT || mode == OPPORTUNISTIC
    }
}

/**
 * Animated warning banner displayed when Android's system Private DNS
 * configuration conflicts with SANDBOXR's local VPN-based DNS filtering.
 *
 * Detects conflicts on each resume lifecycle event to account for settings
 * changes while the app is backgrounded.
 *
 * @param modifier Modifier for layout customization.
 * @param onDismiss Optional callback when the user dismisses the banner.
 * @param onOpenSettings Callback to open Android Private DNS settings deep link.
 */
@Composable
fun PrivateDnsWarningBanner(
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var conflictMode by remember { mutableStateOf(PrivateDnsMode.OFF) }
    var dismissed by remember { mutableStateOf(false) }

    // Re-check on each lifecycle resume (user may have changed settings in background)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                conflictMode = PrivateDnsMode.fromSystem(context)
                dismissed = false // Re-show banner if they didn't fix the issue
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val isVisible = PrivateDnsMode.isConflicting(conflictMode) && !dismissed

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        PrivateDnsWarningCard(
            mode = conflictMode,
            modifier = modifier,
            onDismiss = {
                dismissed = true
                onDismiss?.invoke()
            },
            onOpenSettings = onOpenSettings
        )
    }
}

@Composable
private fun PrivateDnsWarningCard(
    mode: PrivateDnsMode,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onOpenSettings: (() -> Unit)?
) {
    val warningAmber = Color(0xFFFFB74D)
    val warningBackground = Color(0xFF1A1200)
    val borderColor = Color(0xFF7A5500)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF1C1200), Color(0xFF0F0D00))
                )
            )
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Warning icon
            Text(
                text = "⚠",
                fontSize = 20.sp,
                color = warningAmber
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Private DNS Conflict Detected",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = warningAmber
                    )
                )

                val bodyText = when (mode) {
                    PrivateDnsMode.STRICT ->
                        "System Private DNS (Strict mode) is overriding SANDBOXR's local DNS filter. Ad-blocking and per-environment DNS routing are inactive."
                    PrivateDnsMode.OPPORTUNISTIC ->
                        "System Private DNS (Opportunistic) may partially bypass SANDBOXR's DNS filter. Ad-blocking may not apply for all connections."
                    PrivateDnsMode.OFF -> ""
                }

                Text(
                    text = bodyText,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFFD4C070),
                        lineHeight = 16.sp
                    ),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Dismiss",
                    color = Color(0xFF888888),
                    fontSize = 12.sp
                )
            }

            if (onOpenSettings != null) {
                Spacer(modifier = Modifier.width(4.dp))
                TextButton(onClick = onOpenSettings) {
                    Text(
                        text = "Fix in Settings",
                        color = warningAmber,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

/**
 * Programmatically checks whether the system Private DNS conflicts with SANDBOXR.
 * Use this in ViewModels or background checks (not Compose UI context).
 */
fun checkPrivateDnsConflict(context: Context): PrivateDnsMode {
    return PrivateDnsMode.fromSystem(context)
}

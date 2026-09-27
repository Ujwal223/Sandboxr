package com.ujwal.sandboxr.ui.dialogs

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ujwal.sandboxr.data.db.EnvironmentEntity
import com.sandboxr.launcher.theme.SandboxrColors
import com.sandboxr.launcher.theme.SandboxrFontFamilies
import com.sandboxr.launcher.theme.SandboxrSpacingTokens
import com.sandboxr.launcher.theme.SandboxrTheme
import com.sandboxr.launcher.theme.SuperellipseShape
import com.ujwal.sandboxr.ui.model.toImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class CloneableApp(
    val packageName: String,
    val label: String,
    val iconBitmap: androidx.compose.ui.graphics.ImageBitmap? = null
)

/**
 * Bottom sheet allowing user to clone any host application into an isolated sandbox.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloneAppSheet(
    targetEnvironment: EnvironmentEntity,
    onDismissRequest: () -> Unit,
    onCloneApp: (packageName: String) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var apps by remember { mutableStateOf<List<CloneableApp>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val list = pm.queryIntentActivities(intent, 0).mapNotNull { ri ->
                val pkg = ri.activityInfo?.packageName ?: return@mapNotNull null
                if (pkg == context.packageName) return@mapNotNull null
                val label = ri.loadLabel(pm).toString()
                val icon = try { ri.loadIcon(pm).toImageBitmap() } catch (_: Exception) { null }
                CloneableApp(pkg, label, icon)
            }.sortedBy { it.label.lowercase() }
            apps = list
            isLoading = false
        }
    }

    val filtered = remember(apps, searchQuery) {
        if (searchQuery.isBlank()) apps
        else apps.filter {
            it.label.contains(searchQuery, ignoreCase = true) ||
            it.packageName.contains(searchQuery, ignoreCase = true)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = RoundedCornerShape(
            topStart = SandboxrSpacingTokens.BottomSheetRadius,
            topEnd = SandboxrSpacingTokens.BottomSheetRadius
        ),
        containerColor = SandboxrTheme.colors.surface1,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(SandboxrSpacingTokens.BottomSheetHandleWidth)
                    .height(SandboxrSpacingTokens.BottomSheetHandleHeight)
                    .background(SandboxrTheme.colors.textSecondary.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            Text(
                text = "Clone Application",
                style = SandboxrTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = SandboxrTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Import to ${targetEnvironment.displayName}",
                style = SandboxrTheme.typography.title,
                color = SandboxrTheme.colors.primaryAccent
            )
            Text(
                text = "Copies app into this isolated sandbox profile with separate storage.",
                style = SandboxrTheme.typography.body,
                color = SandboxrTheme.colors.textSecondary,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
            )

            // Search filter
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search installed applications...", color = SandboxrTheme.colors.textSecondary, fontSize = 13.sp) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SandboxrTheme.colors.primaryAccent,
                    unfocusedBorderColor = SandboxrTheme.colors.glassBorder,
                    focusedTextColor = SandboxrTheme.colors.textPrimary,
                    unfocusedTextColor = SandboxrTheme.colors.textPrimary,
                    focusedContainerColor = SandboxrTheme.colors.surface2,
                    unfocusedContainerColor = SandboxrTheme.colors.surface2
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Scanning host apps...", color = SandboxrTheme.colors.textSecondary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered, key = { it.packageName }) { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(SandboxrTheme.colors.surface2)
                                .border(1.dp, SandboxrTheme.colors.glassBorder, RoundedCornerShape(16.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val squircle = remember { SuperellipseShape(exponent = 4f) }
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(squircle)
                                        .background(SandboxrTheme.colors.surface3),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (app.iconBitmap != null) {
                                        Image(
                                            bitmap = app.iconBitmap,
                                            contentDescription = app.label,
                                            modifier = Modifier.size(44.dp)
                                        )
                                    } else {
                                        Text(
                                            text = app.label.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                            color = SandboxrTheme.colors.textPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = app.label,
                                        style = SandboxrTheme.typography.body.copy(fontWeight = FontWeight.Medium),
                                        color = SandboxrTheme.colors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = app.packageName,
                                        fontFamily = SandboxrFontFamilies.Inter,
                                        fontSize = 11.sp,
                                        color = SandboxrTheme.colors.textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Button(
                                onClick = { onCloneApp(app.packageName) },
                                colors = ButtonDefaults.buttonColors(containerColor = SandboxrTheme.colors.primaryAccent),
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("Clone", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

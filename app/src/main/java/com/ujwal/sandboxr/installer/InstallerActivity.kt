package com.ujwal.sandboxr.installer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.lifecycleScope
import com.ujwal.sandboxr.data.db.EnvironmentEntity
import com.ujwal.sandboxr.data.db.SandboxrDatabase
import com.ujwal.sandboxr.installer.ui.InstallerInstallState
import com.ujwal.sandboxr.installer.ui.InstallerScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * UI State for the APK Installer and Staging flow.
 */
sealed class InstallerUiState {
    data class Staging(val progressMessage: String = "Streaming APK to isolated staging vault...") : InstallerUiState()
    data class Ready(val apkInfo: StagedApkInfo) : InstallerUiState()
    data class Error(val message: String, val details: String? = null) : InstallerUiState()
}

/**
 * Universal APK Installer Intent Receiver & Staging Activity.
 *
 * Intercepts `application/vnd.android.package-archive` intents and immediately copies
 * the raw byte stream into the private staging directory to guarantee immunity against
 * Android content:// URI permission timeouts.
 */
class InstallerActivity : ComponentActivity() {

    companion object {
        private const val TAG = "InstallerActivity"
    }

    var uiState by mutableStateOf<InstallerUiState>(InstallerUiState.Staging())
        internal set

    var installState by mutableStateOf<InstallerInstallState>(InstallerInstallState.Idle)
        internal set

    private val database by lazy { SandboxrDatabase.getInstance(this) }

    private val installerEngine by lazy { InstallerEngine() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Process incoming intent immediately upon creation
        handleInstallerIntent(intent)

        setContent {
            val environments by database.environmentDao().getAllEnvironmentsFlow()
                .collectAsState(initial = emptyList())

            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0A0A0F) // Obsidian dark background per DESIGN.md
                ) {
                    when (val current = uiState) {
                        is InstallerUiState.Ready -> {
                            InstallerScreen(
                                apkInfo = current.apkInfo,
                                environments = environments,
                                installState = installState,
                                onInstallToSystem = { info ->
                                    installState = InstallerInstallState.Installing("Host System")
                                    lifecycleScope.launch {
                                        val result = installerEngine.install(
                                            this@InstallerActivity,
                                            info,
                                            InstallDestination.System
                                        )
                                        installState = when (result) {
                                            is InstallResult.Success -> {
                                                InstallerInstallState.Success("Host System", result.details)
                                            }
                                            is InstallResult.Error -> {
                                                InstallerInstallState.Error(result.message)
                                            }
                                        }
                                    }
                                },
                                onInstallToVirtual = { info, env ->
                                    installState = InstallerInstallState.Installing(env.displayName)
                                    lifecycleScope.launch {
                                        val result = installerEngine.install(
                                            this@InstallerActivity,
                                            info,
                                            InstallDestination.Virtual(env.id, env.displayName)
                                        )
                                        installState = when (result) {
                                            is InstallResult.Success -> {
                                                InstallerInstallState.Success(env.displayName, result.details)
                                            }
                                            is InstallResult.Error -> {
                                                InstallerInstallState.Error(result.message)
                                            }
                                        }
                                    }
                                },
                                onCreateNewEnvironment = { name, color ->
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        val newEnv = EnvironmentEntity(
                                            displayName = name,
                                            colorTag = color,
                                            isSystem = false
                                        )
                                        database.environmentDao().insert(newEnv)
                                    }
                                },
                                onDismiss = { finish() }
                            )
                        }
                        else -> {
                            InstallerContainer(
                                state = current,
                                onDismiss = { finish() },
                                onRetry = { handleInstallerIntent(intent) }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleInstallerIntent(intent)
    }

    /**
     * Immediately grabs the URI and initiates streaming to the staging directory.
     */
    fun handleInstallerIntent(intent: Intent?) {
        if (intent == null) {
            uiState = InstallerUiState.Error("No intent received.")
            return
        }

        val uri: Uri? = intent.data ?: intent.clipData?.let {
            if (it.itemCount > 0) it.getItemAt(0).uri else null
        }

        if (uri == null) {
            uiState = InstallerUiState.Error(
                message = "Invalid installation request",
                details = "No APK data URI was provided in the intent."
            )
            return
        }

        // Try to take persistable URI permission if offered
        try {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Exception) {
            // Non-persistable URIs will be immediately copied before expiration
        }

        uiState = InstallerUiState.Staging()

        lifecycleScope.launch(Dispatchers.IO) {
            val result = ApkStagingManager.stageApkSync(this@InstallerActivity, uri)
            withContext(Dispatchers.Main) {
                uiState = when (result) {
                    is StagedApkResult.Success -> {
                        InstallerUiState.Ready(result.info)
                    }
                    is StagedApkResult.Error -> {
                        InstallerUiState.Error(
                            message = result.message,
                            details = result.cause?.localizedMessage
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InstallerContainer(
    state: InstallerUiState,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        when (state) {
            is InstallerUiState.Staging -> {
                StagingView(message = state.progressMessage)
            }
            is InstallerUiState.Ready -> {
                StagedApkHeaderCard(
                    info = state.apkInfo,
                    onDismiss = onDismiss
                )
            }
            is InstallerUiState.Error -> {
                ErrorView(
                    message = state.message,
                    details = state.details,
                    onRetry = onRetry,
                    onDismiss = onDismiss
                )
            }
        }
    }
}

@Composable
fun StagingView(message: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            color = Color(0xFF6C5CE7), // Brand Purple
            strokeWidth = 3.dp,
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Staging APK Package",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            color = Color(0xFFA0A0AB),
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun StagedApkHeaderCard(
    info: StagedApkInfo,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF161622))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Icon Badge
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Color(0xFF232336)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = info.label.take(1).uppercase(),
                color = Color(0xFF6C5CE7),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = info.label,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = info.packageName,
            color = Color(0xFF8E8EA0),
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Technical specs chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SpecChip(label = "Version", value = info.versionName)
            SpecChip(
                label = "Size",
                value = "${info.fileSizeBytes / (1024 * 1024)} MB"
            )
            SpecChip(label = "Target SDK", value = "API ${info.targetSdk}")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "APK isolated in userspace staging. Ready for target environment dispatch.",
            color = Color(0xFF00D2D3), // Cyber cyan status
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF28283C),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(text = "Close")
            }
        }
    }
}

@Composable
fun SpecChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label.uppercase(),
            color = Color(0xFF71717A),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            color = Color.White,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun ErrorView(
    message: String,
    details: String?,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1E1418))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Staging Error",
            color = Color(0xFFFF5252),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = message,
            color = Color.White,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        if (details != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = details,
                color = Color(0xFFA0A0AB),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6C5CE7)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(text = "Retry")
            }
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF28283C)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(text = "Close")
            }
        }
    }
}

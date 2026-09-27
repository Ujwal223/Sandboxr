package com.ujwal.sandboxr

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ujwal.sandboxr.data.db.SandboxrDatabase
import com.ujwal.sandboxr.data.repository.EnvironmentRepository
import com.ujwal.sandboxr.domain.SystemEnvironmentManager
import com.ujwal.sandboxr.installer.InstallerEngine
import com.sandboxr.launcher.loader.LawnchairAppLoader
import com.sandboxr.launcher.theme.SandboxrTheme
import com.ujwal.sandboxr.ui.home.HomeScreen
import com.ujwal.sandboxr.ui.settings.LauncherThemeMode
import com.ujwal.sandboxr.ui.viewmodel.LauncherViewModel

/**
 * Main Android Home Launcher Activity for SANDBOXR.
 * Registers as the device's primary launcher (CATEGORY_HOME) to replace OEM home apps (OneUI Home, Pixel Launcher).
 * Supports live system wallpaper backdrop, edge-to-edge transparent system bars, and home intent handling.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val db = SandboxrDatabase.getInstance(applicationContext)
                val repository = EnvironmentRepository(db.environmentDao())
                val systemEnvManager = SystemEnvironmentManager()
                val installerEngine = InstallerEngine()
                val appLoader = LawnchairAppLoader(applicationContext)
                return LauncherViewModel(repository, systemEnvManager, installerEngine, appLoader) as T
            }
        }
    }

    /**
     * Modern Activity Result API launcher for the RoleManager HOME role request dialog.
     * Replaces the deprecated onActivityResult(requestCode=1001) pattern.
     * Re-checks default launcher status after the user returns from the system dialog.
     */
    private val defaultLauncherResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        // Re-check launcher status regardless of result code — user may have set it
        viewModel.checkDefaultLauncher(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Show live system wallpaper behind the window for a genuine home launcher experience
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        enableEdgeToEdge()

        // Check default launcher status on startup
        viewModel.checkDefaultLauncher(this)

        // Wire the ActivityResultLauncher into the ViewModel so it can launch the
        // RoleManager/Settings dialog without holding an Activity reference directly.
        viewModel.setDefaultLauncherResultLauncher(defaultLauncherResultLauncher)

        // Intercept back presses to behave like a true home screen (never exit)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!viewModel.handleBackPress()) {
                    // Already at root home screen workspace: do not exit launcher
                }
            }
        })

        setContent {
            val settings by viewModel.launcherSettings.collectAsState()
            val isDark = when (settings.themeMode) {
                LauncherThemeMode.SYSTEM -> isSystemInDarkTheme()
                LauncherThemeMode.DARK -> true
                LauncherThemeMode.LIGHT -> false
            }
            SandboxrTheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Transparent
                ) {
                    HomeScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkDefaultLauncher(this)
        viewModel.refreshApps(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) {
            // User pressed physical or gesture Home button: reset to home workspace
            viewModel.onHomeIntent()
        }
    }
}

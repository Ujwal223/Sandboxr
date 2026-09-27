package com.ujwal.sandboxr.ui.viewmodel

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.AlarmClock
import android.provider.Settings
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ujwal.sandboxr.data.db.ClipboardMode
import com.ujwal.sandboxr.data.db.EnvironmentEntity
import com.ujwal.sandboxr.data.db.NetworkConfig
import com.ujwal.sandboxr.data.repository.EnvironmentRepository
import com.ujwal.sandboxr.domain.LaunchResult
import com.ujwal.sandboxr.domain.SystemEnvironmentManager
import com.ujwal.sandboxr.installer.ApkStagingManager
import com.ujwal.sandboxr.installer.InstallerEngine
import com.ujwal.sandboxr.installer.StagedApkResult
import com.sandboxr.launcher.loader.LawnchairAppLoader
import com.sandboxr.launcher.theme.SandboxrColors
import com.sandboxr.launcher.ui.AppItem
import com.sandboxr.launcher.ui.EnvironmentIconType
import com.sandboxr.launcher.ui.NavTab
import com.ujwal.sandboxr.ui.model.toImageBitmap
import com.ujwal.sandboxr.ui.settings.LauncherPreferencesManager
import com.ujwal.sandboxr.ui.settings.LauncherSettings
import com.sandboxr.virtual.VirtualCore
import com.sandboxr.virtual.model.SpoofProfile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Primary ViewModel driving the SANDBOXR Launcher and Privacy Platform UI.
 * Built on Lawnchair / AOSP Launcher3 architecture to serve as a complete
 * Android default Home app replacement (e.g., OneUI Home replacement).
 */
class LauncherViewModel(
    private val environmentRepository: EnvironmentRepository,
    private val systemEnvManager: SystemEnvironmentManager = SystemEnvironmentManager(),
    private val installerEngine: InstallerEngine = InstallerEngine(),
    private var appLoader: LawnchairAppLoader? = null
) : ViewModel() {

    companion object {
        private const val TAG = "LauncherViewModel"
    }

    // Wired from MainActivity via setDefaultLauncherResultLauncher().
    // Used to launch the RoleManager/Settings dialog without holding an Activity reference.
    private var defaultLauncherResultLauncher: ActivityResultLauncher<Intent>? = null

    /**
     * Called from MainActivity.onCreate() to provide the ActivityResultLauncher
     * needed for the RoleManager HOME role request dialog.
     */
    fun setDefaultLauncherResultLauncher(launcher: ActivityResultLauncher<Intent>) {
        defaultLauncherResultLauncher = launcher
    }

    val environments: StateFlow<List<EnvironmentEntity>> = environmentRepository.environmentsState

    private val _activeEnvironmentId = MutableStateFlow(EnvironmentEntity.SYSTEM_ENV_ID)
    val activeEnvironmentId: StateFlow<String> = _activeEnvironmentId.asStateFlow()

    val activeEnvironment: StateFlow<EnvironmentEntity?> = combine(
        environments,
        _activeEnvironmentId
    ) { envList, activeId ->
        envList.find { it.id == activeId } ?: envList.find { it.isSystem }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _currentTab = MutableStateFlow(NavTab.ENVIRONMENTS)
    val currentTab: StateFlow<NavTab> = _currentTab.asStateFlow()

    private val _isDefaultLauncher = MutableStateFlow(false)
    val isDefaultLauncher: StateFlow<Boolean> = _isDefaultLauncher.asStateFlow()

    private val _wallpaperDimming = MutableStateFlow(0.55f)
    val wallpaperDimming: StateFlow<Float> = _wallpaperDimming.asStateFlow()

    private val _isDrawerMode = MutableStateFlow(false)
    val isDrawerMode: StateFlow<Boolean> = _isDrawerMode.asStateFlow()

    private val _installedApps = MutableStateFlow<List<AppItem>>(emptyList())
    val installedApps: StateFlow<List<AppItem>> = _installedApps.asStateFlow()

    private val _homeApps = MutableStateFlow<List<AppItem>>(emptyList())
    val homeApps: StateFlow<List<AppItem>> = _homeApps.asStateFlow()

    private val _dockApps = MutableStateFlow<List<AppItem>>(emptyList())
    val dockApps: StateFlow<List<AppItem>> = _dockApps.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedProfileFilter = MutableStateFlow<String?>(null)
    val selectedProfileFilter: StateFlow<String?> = _selectedProfileFilter.asStateFlow()

    private val _launcherSettings = MutableStateFlow(LauncherSettings())
    val launcherSettings: StateFlow<LauncherSettings> = _launcherSettings.asStateFlow()

    private val _showHomeSettings = MutableStateFlow(false)
    val showHomeSettings: StateFlow<Boolean> = _showHomeSettings.asStateFlow()

    private val _showWallpaperStyle = MutableStateFlow(false)
    val showWallpaperStyle: StateFlow<Boolean> = _showWallpaperStyle.asStateFlow()

    private val _showWidgetsDialog = MutableStateFlow(false)
    val showWidgetsDialog: StateFlow<Boolean> = _showWidgetsDialog.asStateFlow()

    private val _showManageScreensDialog = MutableStateFlow(false)
    val showManageScreensDialog: StateFlow<Boolean> = _showManageScreensDialog.asStateFlow()

    private val _currentPageIndex = MutableStateFlow(0)
    val currentPageIndex: StateFlow<Int> = _currentPageIndex.asStateFlow()

    private val _showProfileSwitcher = MutableStateFlow(false)
    val showProfileSwitcher: StateFlow<Boolean> = _showProfileSwitcher.asStateFlow()

    private val _showHomeMenu = MutableStateFlow(false)
    val showHomeMenu: StateFlow<Boolean> = _showHomeMenu.asStateFlow()

    val filteredApps: StateFlow<List<AppItem>> = combine(
        _installedApps,
        _searchQuery,
        _selectedProfileFilter,
        _launcherSettings
    ) { apps, query, filterEnvId, settings ->
        apps.filter { app ->
            val matchesQuery = query.isBlank() ||
                app.label.contains(query, ignoreCase = true) ||
                app.packageName.contains(query, ignoreCase = true)

            val matchesProfile = filterEnvId == null || app.envId == filterEnvId
            val notHidden = !settings.hiddenPackages.contains(app.packageName)

            matchesQuery && matchesProfile && notHidden
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _appCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val appCounts: StateFlow<Map<String, Int>> = _appCounts.asStateFlow()

    private val _isLoadingApps = MutableStateFlow(false)
    val isLoadingApps: StateFlow<Boolean> = _isLoadingApps.asStateFlow()

    // Dialog & Sheet States
    private val _showCreateDialog = MutableStateFlow(false)
    val showCreateDialog: StateFlow<Boolean> = _showCreateDialog.asStateFlow()

    private val _actionSheetEnv = MutableStateFlow<EnvironmentEntity?>(null)
    val actionSheetEnv: StateFlow<EnvironmentEntity?> = _actionSheetEnv.asStateFlow()

    private val _hardwareProfileEnv = MutableStateFlow<EnvironmentEntity?>(null)
    val hardwareProfileEnv: StateFlow<EnvironmentEntity?> = _hardwareProfileEnv.asStateFlow()

    private val _cloneSheetEnv = MutableStateFlow<EnvironmentEntity?>(null)
    val cloneSheetEnv: StateFlow<EnvironmentEntity?> = _cloneSheetEnv.asStateFlow()

    private val _selectedApp = MutableStateFlow<AppItem?>(null)
    val selectedApp: StateFlow<AppItem?> = _selectedApp.asStateFlow()

    private val _statusNotice = MutableStateFlow<String?>(null)
    val statusNotice: StateFlow<String?> = _statusNotice.asStateFlow()

    fun setTab(tab: NavTab) {
        _currentTab.value = tab
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun openCreateDialog() {
        _showCreateDialog.value = true
    }

    fun closeCreateDialog() {
        _showCreateDialog.value = false
    }

    fun openActionSheet(env: EnvironmentEntity) {
        _actionSheetEnv.value = env
    }

    fun closeActionSheet() {
        _actionSheetEnv.value = null
    }

    fun openHardwareProfile(env: EnvironmentEntity) {
        _hardwareProfileEnv.value = env
    }

    fun closeHardwareProfile() {
        _hardwareProfileEnv.value = null
    }

    fun openCloneSheet(env: EnvironmentEntity) {
        _cloneSheetEnv.value = env
    }

    fun closeCloneSheet() {
        _cloneSheetEnv.value = null
    }

    fun selectAppForDetails(app: AppItem?) {
        _selectedApp.value = app
    }

    fun clearStatusNotice() {
        _statusNotice.value = null
    }

    fun showNotice(msg: String) {
        _statusNotice.value = msg
    }

    fun setDrawerMode(enabled: Boolean) {
        _isDrawerMode.value = enabled
    }

    fun toggleDrawerMode() {
        _isDrawerMode.value = !_isDrawerMode.value
    }

    fun setWallpaperDimming(alpha: Float) {
        _wallpaperDimming.value = alpha.coerceIn(0.1f, 0.95f)
    }

    fun checkDefaultLauncher(context: Context? = null) {
        val loader = appLoader ?: context?.let { LawnchairAppLoader(it).also { l -> appLoader = l } } ?: return
        _isDefaultLauncher.value = loader.isDefaultLauncher()
    }

    fun requestDefaultLauncher(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val roleManager = context.getSystemService(RoleManager::class.java)
                if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                    if (roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
                        _isDefaultLauncher.value = true
                        showNotice("Sandboxr is already your default Home launcher")
                        return
                    }
                    val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
                    // Prefer modern ActivityResultLauncher (wired from MainActivity)
                    val launcher = defaultLauncherResultLauncher
                    if (launcher != null) {
                        try {
                            launcher.launch(intent)
                            return
                        } catch (e: Exception) {
                            Log.w(TAG, "ActivityResultLauncher launch failed, falling back: ${e.message}")
                        }
                    }
                    // Fallback: context.startActivity if launcher not yet wired
                    try {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                        return
                    } catch (e: Exception) {
                        Log.w(TAG, "RoleManager startActivity failed, falling back: ${e.message}")
                    }
                }
            }

            // Fallback 1: Direct Home Settings
            try {
                val homeSettingsIntent = Intent(Settings.ACTION_HOME_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(homeSettingsIntent)
                return
            } catch (_: Exception) {}

            // Fallback 2: Manage Default Apps Settings (Samsung One UI, Pixel, HyperOS)
            try {
                val defaultAppsIntent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(defaultAppsIntent)
                return
            } catch (_: Exception) {}

            // Fallback 3: Application Details Settings
            try {
                val appDetailsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = android.net.Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(appDetailsIntent)
                return
            } catch (_: Exception) {}

            // Fallback 4: Intent Chooser
            val homeChooser = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(homeChooser, "Select Home Launcher").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            showNotice("Could not open default launcher settings: ${e.message}")
        }
    }

    fun onHomeIntent() {
        _searchQuery.value = ""
        _isDrawerMode.value = false
        _showCreateDialog.value = false
        _actionSheetEnv.value = null
        _hardwareProfileEnv.value = null
        _cloneSheetEnv.value = null
        _selectedApp.value = null
        _currentTab.value = NavTab.ENVIRONMENTS
    }

    fun handleBackPress(): Boolean {
        if (_isDrawerMode.value) {
            _isDrawerMode.value = false
            return true
        }
        if (_searchQuery.value.isNotEmpty()) {
            _searchQuery.value = ""
            return true
        }
        if (_actionSheetEnv.value != null) {
            _actionSheetEnv.value = null
            return true
        }
        if (_hardwareProfileEnv.value != null) {
            _hardwareProfileEnv.value = null
            return true
        }
        if (_cloneSheetEnv.value != null) {
            _cloneSheetEnv.value = null
            return true
        }
        if (_showHomeSettings.value) {
            _showHomeSettings.value = false
            return true
        }
        if (_showWallpaperStyle.value) {
            _showWallpaperStyle.value = false
            return true
        }
        if (_showWidgetsDialog.value) {
            _showWidgetsDialog.value = false
            return true
        }
        if (_showManageScreensDialog.value) {
            _showManageScreensDialog.value = false
            return true
        }
        if (_showProfileSwitcher.value) {
            _showProfileSwitcher.value = false
            return true
        }
        if (_showHomeMenu.value) {
            _showHomeMenu.value = false
            return true
        }
        if (_showCreateDialog.value) {
            _showCreateDialog.value = false
            return true
        }
        val defaultPage = _launcherSettings.value.defaultPageIndex
        if (_currentPageIndex.value != defaultPage) {
            _currentPageIndex.value = defaultPage
            return true
        }
        if (_currentTab.value != NavTab.ENVIRONMENTS) {
            _currentTab.value = NavTab.ENVIRONMENTS
            return true
        }
        return false
    }

    fun setSelectedProfileFilter(context: Context, envId: String?) {
        _selectedProfileFilter.value = envId
        if (envId != null) {
            switchEnvironment(context, envId)
        } else {
            loadAllAppsCombined(context)
        }
    }

    private fun loadAllAppsCombined(context: Context) {
        viewModelScope.launch {
            _isLoadingApps.value = true
            try {
                val combined = mutableListOf<AppItem>()
                val envList = environments.value
                val loader = appLoader ?: LawnchairAppLoader(context).also { appLoader = it }
                val systemApps = withContext(Dispatchers.IO) { loader.loadAllSystemApps() }
                combined.addAll(systemApps)

                for (env in envList) {
                    if (!env.isSystem) {
                        val vApps = withContext(Dispatchers.IO) { queryApps(context, env) }
                        combined.addAll(vApps)
                    }
                }
                combined.sortBy { it.label.lowercase() }
                _installedApps.value = combined
                updateDefaultHomeAndDock("merged_all", combined)
            } catch (e: Throwable) {
                Log.e(TAG, "Error loading combined apps", e)
            } finally {
                _isLoadingApps.value = false
            }
        }
    }

    fun openHomeSettings() {
        _showHomeSettings.value = true
    }

    fun closeHomeSettings() {
        _showHomeSettings.value = false
    }

    fun openWallpaperStyle() {
        _showWallpaperStyle.value = true
    }

    fun closeWallpaperStyle() {
        _showWallpaperStyle.value = false
    }

    fun openWidgetsDialog() {
        _showWidgetsDialog.value = true
    }

    fun closeWidgetsDialog() {
        _showWidgetsDialog.value = false
    }

    fun openManageScreensDialog() {
        _showManageScreensDialog.value = true
    }

    fun closeManageScreensDialog() {
        _showManageScreensDialog.value = false
    }

    fun setCurrentPageIndex(index: Int) {
        _currentPageIndex.value = index
    }

    fun setDefaultPage(context: Context, pageIndex: Int) {
        val updated = _launcherSettings.value.copy(defaultPageIndex = pageIndex)
        updateLauncherSettings(context, updated)
        showNotice("Set Screen ${pageIndex + 1} as default Home screen")
    }

    fun addWorkspacePage(context: Context) {
        val currentCount = _launcherSettings.value.pageCount
        val updated = _launcherSettings.value.copy(pageCount = currentCount + 1)
        updateLauncherSettings(context, updated)
        _currentPageIndex.value = currentCount
        showNotice("Added new Home screen")
    }

    fun removeWorkspacePage(context: Context, pageIndex: Int) {
        val currentCount = _launcherSettings.value.pageCount
        if (currentCount <= 1) {
            showNotice("Cannot remove last remaining screen")
            return
        }
        var newDefault = _launcherSettings.value.defaultPageIndex
        if (newDefault >= currentCount - 1) {
            newDefault = (currentCount - 2).coerceAtLeast(0)
        }
        val updated = _launcherSettings.value.copy(
            pageCount = currentCount - 1,
            defaultPageIndex = newDefault
        )
        updateLauncherSettings(context, updated)
        _currentPageIndex.value = newDefault
        showNotice("Removed screen")
    }

    fun openProfileSwitcher() {
        _showProfileSwitcher.value = true
    }

    fun closeProfileSwitcher() {
        _showProfileSwitcher.value = false
    }

    fun openHomeMenu() {
        _showHomeMenu.value = true
    }

    fun closeHomeMenu() {
        _showHomeMenu.value = false
    }

    fun updateLauncherSettings(context: Context, newSettings: LauncherSettings) {
        _launcherSettings.value = newSettings
        LauncherPreferencesManager(context).saveSettings(newSettings)
    }

    fun loadLauncherSettings(context: Context) {
        _launcherSettings.value = LauncherPreferencesManager(context).loadSettings()
    }

    fun openWallpaperPicker(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_SET_WALLPAPER)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(Intent.createChooser(intent, "Set Wallpaper"))
        } catch (_: Exception) {
            try {
                val intent = Intent(Settings.ACTION_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } catch (_: Exception) {}
        }
    }

    fun expandNotifications(context: Context) {
        try {
            val statusBarService = context.getSystemService("statusbar")
            val statusBarManagerClass = Class.forName("android.app.StatusBarManager")
            val expand = statusBarManagerClass.getMethod("expandNotificationsPanel")
            expand.invoke(statusBarService)
        } catch (_: Exception) {
            try {
                val intent = Intent("android.intent.action.EXPAND_STATUS_BAR")
                context.sendBroadcast(intent)
            } catch (_: Exception) {}
        }
    }

    private val _envHomeAppsMap = mutableMapOf<String, List<AppItem>>()
    private val _envDockAppsMap = mutableMapOf<String, List<AppItem>>()

    fun refreshApps(context: Context) {
        val loader = appLoader ?: LawnchairAppLoader(context).also { newLoader ->
            appLoader = newLoader
            newLoader.registerPackageListener {
                refreshApps(context)
            }
        }
        viewModelScope.launch {
            environmentRepository.syncWorkProfile(loader.hasWorkProfile())
            val env = activeEnvironment.value
            if (env != null) {
                loadAppsForEnvironment(context, env)
            }
            refreshAllAppCounts(context)
        }
    }

    /**
     * Switches the active environment and loads its apps.
     */
    fun switchEnvironment(context: Context, envId: String) {
        _activeEnvironmentId.value = envId
        viewModelScope.launch {
            environmentRepository.setActiveEnvironment(envId)
            val env = environmentRepository.getEnvironment(envId)
            if (env != null) {
                loadAppsForEnvironment(context, env)
            }
        }
    }

    private var loadAppsJob: Job? = null

    /**
     * Asynchronously loads applications installed in the target environment.
     */
    fun loadAppsForEnvironment(context: Context, environment: EnvironmentEntity) {
        loadAppsJob?.cancel()
        loadAppsJob = viewModelScope.launch {
            _isLoadingApps.value = true
            try {
                val apps = withContext(Dispatchers.IO) {
                    queryApps(context, environment).distinctBy { "${it.envId}_${it.packageName}" }
                }
                _installedApps.value = apps
                updateDefaultHomeAndDock(environment.id, apps)
                _appCounts.value = _appCounts.value.toMutableMap().apply {
                    put(environment.id, apps.size)
                }
            } catch (e: Throwable) {
                if (e !is CancellationException) {
                    Log.e(TAG, "Error loading apps for ${environment.id}", e)
                }
            } finally {
                _isLoadingApps.value = false
            }
        }
    }

    private fun updateDefaultHomeAndDock(environmentId: String, apps: List<AppItem>) {
        if (apps.isEmpty()) {
            _dockApps.value = emptyList()
            _homeApps.value = emptyList()
            return
        }

        // 1. Detect Dock Apps (Phone, Messages, Browser, Camera, Settings or priority apps)
        val savedDock = _envDockAppsMap[environmentId]
        val finalDock = if (!savedDock.isNullOrEmpty()) {
            savedDock
        } else {
            val dialer = apps.find { it.packageName.contains("dialer", ignoreCase = true) || it.packageName.contains("phone", ignoreCase = true) }
            val messaging = apps.find { it.packageName.contains("messaging", ignoreCase = true) || it.packageName.contains("mms", ignoreCase = true) }
            val browser = apps.find { it.packageName.contains("chrome", ignoreCase = true) || it.packageName.contains("browser", ignoreCase = true) || it.packageName.contains("firefox", ignoreCase = true) }
            val camera = apps.find { it.packageName.contains("camera", ignoreCase = true) }
            val settings = apps.find { it.packageName.contains("settings", ignoreCase = true) }

            val detectedDock = listOfNotNull(dialer, messaging, browser, camera, settings).distinctBy { it.packageName }
            if (detectedDock.size >= 4) {
                detectedDock.take(5)
            } else {
                (detectedDock + apps.filterNot { it in detectedDock }).take(5)
            }
        }
        _dockApps.value = finalDock
        _envDockAppsMap[environmentId] = finalDock

        // 2. Load Home Desktop apps specifically for this environment (switches when profile switches)
        val savedHome = _envHomeAppsMap[environmentId]
        val finalHome = if (!savedHome.isNullOrEmpty()) {
            savedHome
        } else {
            val dockPkgs = finalDock.map { it.packageName }.toSet()
            apps.filterNot { it.packageName in dockPkgs }.take(12)
        }
        _homeApps.value = finalHome
        _envHomeAppsMap[environmentId] = finalHome
    }

    fun addToHome(app: AppItem) {
        if (_homeApps.value.none { it.packageName == app.packageName && it.envId == app.envId }) {
            val updated = _homeApps.value + app
            _homeApps.value = updated
            _envHomeAppsMap[app.envId] = updated
            showNotice("Added ${app.label} to Home screen")
        }
    }

    fun removeFromHome(app: AppItem) {
        val updated = _homeApps.value.filterNot { it.packageName == app.packageName && it.envId == app.envId }
        _homeApps.value = updated
        _envHomeAppsMap[app.envId] = updated
        showNotice("Removed ${app.label} from Home screen")
    }

    fun addToDock(app: AppItem) {
        if (_dockApps.value.size >= 5) {
            showNotice("Dock is full (maximum 5 apps)")
            return
        }
        if (_dockApps.value.none { it.packageName == app.packageName && it.envId == app.envId }) {
            val updated = _dockApps.value + app
            _dockApps.value = updated
            _envDockAppsMap[app.envId] = updated
            showNotice("Added ${app.label} to Dock")
        }
    }

    fun removeFromDock(app: AppItem) {
        val updated = _dockApps.value.filterNot { it.packageName == app.packageName && it.envId == app.envId }
        _dockApps.value = updated
        _envDockAppsMap[app.envId] = updated
        showNotice("Removed ${app.label} from Dock")
    }

    fun openAppInfo(context: Context, app: AppItem) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", app.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            showNotice("Could not open settings: ${e.message}")
        }
    }

    fun openClock(context: Context) {
        val clockIntent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(clockIntent)
        } catch (_: Exception) {
            val clockApp = installedApps.value.find {
                it.packageName.contains("clock", ignoreCase = true) ||
                it.packageName.contains("deskclock", ignoreCase = true)
            }
            if (clockApp != null) {
                launchApp(context, clockApp)
            }
        }
    }

    fun openCalendar(context: Context) {
        val calendarIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_APP_CALENDAR)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(calendarIntent)
        } catch (_: Exception) {
            val calApp = installedApps.value.find { it.packageName.contains("calendar", ignoreCase = true) }
            if (calApp != null) {
                launchApp(context, calApp)
            }
        }
    }

    /**
     * Refreshes app counts across all environments.
     */
    fun refreshAllAppCounts(context: Context) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val envList = environments.value
                val countMap = mutableMapOf<String, Int>()
                val loader = appLoader ?: LawnchairAppLoader(context).also { appLoader = it }

                val myUser = android.os.Process.myUserHandle()
                countMap[EnvironmentEntity.SYSTEM_ENV_ID] = loader.loadAllSystemApps(EnvironmentEntity.SYSTEM_ENV_ID, targetUser = myUser).size

                val workUser = loader.getWorkProfileUser()
                if (workUser != null) {
                    countMap[EnvironmentEntity.WORK_PROFILE_ENV_ID] = loader.loadAllSystemApps(EnvironmentEntity.WORK_PROFILE_ENV_ID, targetUser = workUser).size
                }

                val vc = try { VirtualCore.get() } catch (_: Exception) { null }
                if (vc != null) {
                    for (env in envList) {
                        if (!env.isSystem) {
                            val vApps = vc.packageManagerService.getInstalledPackages(0, env.id)
                            countMap[env.id] = vApps.size
                        }
                    }
                }
                _appCounts.value = countMap
            }
        }
    }

    private suspend fun queryApps(context: Context, environment: EnvironmentEntity): List<AppItem> {
        val loader = appLoader ?: LawnchairAppLoader(context).also { appLoader = it }
        val envColor = Color(environment.colorTag)

        if (environment.id == EnvironmentEntity.WORK_PROFILE_ENV_ID) {
            val workUser = loader.getWorkProfileUser()
            return loader.loadAllSystemApps(environment.id, envColor, workUser)
        } else if (environment.isSystem || environment.id == EnvironmentEntity.SYSTEM_ENV_ID) {
            val personalUser = android.os.Process.myUserHandle()
            return loader.loadAllSystemApps(environment.id, envColor, personalUser)
        } else {
            val result = mutableListOf<AppItem>()
            val vc = try { VirtualCore.get() } catch (_: Exception) { null }
            if (vc != null) {
                val installedPackages = vc.packageManagerService.getInstalledPackages(0, environment.id)
                val pm = context.packageManager

                for (pkg in installedPackages) {
                    val pkgName = pkg.packageName
                    val appInfo = pkg.applicationInfo

                    // Resolve human-readable label:
                    // Priority: APK archive label → host PM label → package name suffix
                    val label: String = try {
                        val apkSourceDir = appInfo?.sourceDir
                        if (apkSourceDir != null && File(apkSourceDir).exists()) {
                            val archiveInfo = pm.getPackageArchiveInfo(apkSourceDir, 0)
                            archiveInfo?.applicationInfo?.apply {
                                sourceDir = apkSourceDir
                                publicSourceDir = apkSourceDir
                            }?.loadLabel(pm)?.toString()
                                ?: appInfo.loadLabel(pm).toString()
                        } else {
                            try {
                                pm.getApplicationInfo(pkgName, 0).loadLabel(pm).toString()
                            } catch (_: Exception) {
                                pkgName.substringAfterLast('.')
                            }
                        }
                    } catch (_: Exception) {
                        pkgName.substringAfterLast('.')
                    }

                    // Resolve icon:
                    val icon = try {
                        val apkSourceDir = appInfo?.sourceDir
                        if (apkSourceDir != null && File(apkSourceDir).exists()) {
                            val archiveInfo = pm.getPackageArchiveInfo(apkSourceDir, 0)
                            val ai = archiveInfo?.applicationInfo?.apply {
                                sourceDir = apkSourceDir
                                publicSourceDir = apkSourceDir
                            }
                            ai?.loadIcon(pm)?.toImageBitmap()
                        } else null
                    } catch (_: Exception) {
                        try {
                            pm.getApplicationIcon(pkgName).toImageBitmap()
                        } catch (_: Exception) { null }
                    }

                    result.add(
                        AppItem(
                            packageName = pkgName,
                            label = label,
                            iconBitmap = icon,
                            envId = environment.id,
                            envColor = envColor,
                            isSystemApp = false
                        )
                    )
                }
            }
            result.sortBy { it.label.lowercase() }
            return result
        }
    }

    /**
     * Launches an app using LawnchairAppLoader for system/work apps or SystemEnvironmentManager for virtual apps.
     */
    fun launchApp(context: Context, app: AppItem) {
        val active = activeEnvironment.value ?: return
        viewModelScope.launch {
            if (app.isSystemApp) {
                val loader = appLoader ?: LawnchairAppLoader(context).also { appLoader = it }
                val targetUser = app.userHandle ?: if (app.envId == EnvironmentEntity.WORK_PROFILE_ENV_ID) loader.getWorkProfileUser() else null
                val launched = loader.launchSystemApp(app.packageName, targetUser = targetUser)
                if (launched) {
                    Log.i(TAG, "Launched ${app.packageName} via LawnchairAppLoader on user $targetUser")
                    return@launch
                }
            }

            val res = systemEnvManager.launchApp(context, app.packageName, active)
            when (res) {
                is LaunchResult.Success -> {
                    Log.i(TAG, "Launched ${app.packageName} successfully via ${res.mode}")
                }
                is LaunchResult.PackageNotFound -> {
                    showNotice("App ${app.label} not found in this environment.")
                }
                is LaunchResult.Error -> {
                    showNotice("Failed to launch ${app.label}: ${res.message}")
                }
            }
        }
    }

    /**
     * Opens voice search scoped to the currently active profile.
     */
    fun openVoiceSearchForActiveProfile(context: Context) {
        val active = activeEnvironment.value ?: return
        viewModelScope.launch {
            if (!active.isSystem) {
                val voiceApp = installedApps.value.find {
                    it.packageName.contains("voice", ignoreCase = true) ||
                    it.label.contains("voice", ignoreCase = true) ||
                    it.packageName.contains("search", ignoreCase = true) ||
                    it.label.contains("search", ignoreCase = true) ||
                    it.packageName.contains("browser", ignoreCase = true) ||
                    it.label.contains("browser", ignoreCase = true)
                }
                if (voiceApp != null) {
                    launchApp(context, voiceApp)
                    return@launch
                }
            } else if (active.id == EnvironmentEntity.WORK_PROFILE_ENV_ID) {
                val loader = appLoader ?: LawnchairAppLoader(context).also { appLoader = it }
                val workUser = loader.getWorkProfileUser()
                if (workUser != null) {
                    val workApp = installedApps.value.find {
                        it.packageName.contains("search", ignoreCase = true) ||
                        it.label.contains("search", ignoreCase = true) ||
                        it.packageName.contains("voice", ignoreCase = true) ||
                        it.label.contains("voice", ignoreCase = true) ||
                        it.packageName.contains("browser", ignoreCase = true) ||
                        it.label.contains("browser", ignoreCase = true)
                    }
                    if (workApp != null) {
                        loader.launchSystemApp(workApp.packageName, targetUser = workUser)
                        return@launch
                    }
                }
            }
            try {
                val voiceIntent = Intent(android.speech.RecognizerIntent.ACTION_WEB_SEARCH).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(voiceIntent)
            } catch (_: Exception) {
                showNotice("No voice search app found for this profile")
            }
        }
    }

    /**
     * Opens camera scoped to the currently active profile.
     */
    fun openCameraForActiveProfile(context: Context) {
        val active = activeEnvironment.value ?: return
        viewModelScope.launch {
            if (!active.isSystem) {
                val cameraApp = installedApps.value.find {
                    it.packageName.contains("camera", ignoreCase = true) ||
                    it.label.contains("camera", ignoreCase = true)
                }
                if (cameraApp != null) {
                    launchApp(context, cameraApp)
                    return@launch
                }
            } else if (active.id == EnvironmentEntity.WORK_PROFILE_ENV_ID) {
                val loader = appLoader ?: LawnchairAppLoader(context).also { appLoader = it }
                val workUser = loader.getWorkProfileUser()
                if (workUser != null) {
                    val workCamera = installedApps.value.find {
                        it.packageName.contains("camera", ignoreCase = true) ||
                        it.label.contains("camera", ignoreCase = true)
                    }
                    if (workCamera != null) {
                        loader.launchSystemApp(workCamera.packageName, targetUser = workUser)
                        return@launch
                    }
                }
            }
            try {
                val camIntent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(camIntent)
            } catch (_: Exception) {
                showNotice("No camera app found for this profile")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        appLoader?.unregisterPackageListener()
    }

    /**
     * Creates a new isolated virtual container environment.
     */
    fun createEnvironment(
        context: Context,
        name: String,
        colorTag: Long,
        iconType: EnvironmentIconType,
        networkConfig: NetworkConfig,
        gmsEnabled: Boolean,
        clipboardMode: ClipboardMode
    ) {
        viewModelScope.launch {
            val newEnv = EnvironmentEntity(
                id = UUID.randomUUID().toString(),
                displayName = name.trim(),
                colorTag = colorTag,
                iconEmoji = iconType.name,
                networkConfig = networkConfig,
                gmsEnabled = gmsEnabled,
                clipboardMode = clipboardMode,
                hardwareIds = SpoofProfile.generate(),
                isSystem = false,
                sortOrder = (environments.value.maxOfOrNull { it.sortOrder } ?: 0) + 1
            )
            environmentRepository.insertEnvironment(newEnv)
            closeCreateDialog()
            switchEnvironment(context, newEnv.id)
            showNotice("Created container '${newEnv.displayName}'")
        }
    }

    /**
     * Deletes a virtual container environment.
     */
    fun deleteEnvironment(context: Context, envId: String) {
        viewModelScope.launch {
            val deleted = environmentRepository.deleteEnvironment(envId)
            if (deleted) {
                closeActionSheet()
                switchEnvironment(context, EnvironmentEntity.SYSTEM_ENV_ID)
                showNotice("Environment deleted.")
            } else {
                showNotice("Cannot delete System environment.")
            }
        }
    }

    /**
     * Updates an environment's network mode.
     */
    fun updateNetworkMode(envId: String, mode: NetworkConfig) {
        viewModelScope.launch {
            environmentRepository.updateNetworkConfig(envId, mode)
            showNotice("Network mode updated to $mode")
        }
    }

    /**
     * Toggles GMS telemetry for an environment.
     */
    fun toggleGms(env: EnvironmentEntity) {
        viewModelScope.launch {
            val newGms = !env.gmsEnabled
            environmentRepository.updateGmsEnabled(env.id, newGms)
            showNotice(if (newGms) "Google Play Services allowed" else "Google Play Services blocked")
        }
    }

    /**
     * Clones an existing system app into a virtual container.
     */
    fun cloneAppToEnvironment(context: Context, packageName: String, targetEnvId: String) {
        viewModelScope.launch {
            _isLoadingApps.value = true
            try {
                val success = withContext(Dispatchers.IO) {
                    try {
                        val pm = context.packageManager
                        val appInfo = pm.getApplicationInfo(packageName, 0)
                        val baseApk = File(appInfo.sourceDir)
                        if (!baseApk.exists()) {
                            Log.e(TAG, "Source APK not found for $packageName at ${baseApk.absolutePath}")
                            return@withContext false
                        }
                        val targetEnv = environmentRepository.getEnvironment(targetEnvId)
                            ?: return@withContext false

                        val vc = try { VirtualCore.get() } catch (_: Throwable) { null }
                        if (vc != null) {
                            try {
                                vc.cloneSystemPackage(packageName, targetEnv.id)
                                Log.i(TAG, "Successfully cloned $packageName to environment ${targetEnv.id}")
                                true
                            } catch (e: Exception) {
                                Log.e(TAG, "VirtualCore clone failed for $packageName, trying fallback", e)
                                // Fallback to staging method if VirtualCore clone fails
                                val stagedResult = ApkStagingManager.stageApkSync(context, android.net.Uri.fromFile(baseApk))
                                if (stagedResult is StagedApkResult.Success) {
                                    val destination = com.ujwal.sandboxr.installer.InstallDestination.Virtual(
                                        environmentId = targetEnv.id,
                                        environmentName = targetEnv.displayName
                                    )
                                    val installResult = installerEngine.installToVirtualEnvironment(
                                        context = context,
                                        stagedApk = stagedResult.info,
                                        destination = destination
                                    )
                                    installResult is com.ujwal.sandboxr.installer.InstallResult.Success
                                } else {
                                    false
                                }
                            }
                        } else {
                            // VirtualCore not available, use staging method
                            val stagedResult = ApkStagingManager.stageApkSync(context, android.net.Uri.fromFile(baseApk))
                            if (stagedResult is StagedApkResult.Success) {
                                val destination = com.ujwal.sandboxr.installer.InstallDestination.Virtual(
                                    environmentId = targetEnv.id,
                                    environmentName = targetEnv.displayName
                                )
                                val installResult = installerEngine.installToVirtualEnvironment(
                                    context = context,
                                    stagedApk = stagedResult.info,
                                    destination = destination
                                )
                                installResult is com.ujwal.sandboxr.installer.InstallResult.Success
                            } else {
                                false
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to clone app $packageName", e)
                        false
                    }
                }

                if (success) {
                    showNotice("Cloned application into container.")
                    val env = environmentRepository.getEnvironment(targetEnvId)
                    if (env != null) {
                        loadAppsForEnvironment(context, env)
                    }
                    refreshAllAppCounts(context)
                } else {
                    showNotice("Failed to clone application.")
                }
            } finally {
                _isLoadingApps.value = false
                closeCloneSheet()
            }
        }
    }

    /**
     * Uninstalls an application from a virtual environment.
     */
    fun uninstallVirtualApp(context: Context, packageName: String, envId: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val vc = VirtualCore.get()
                    vc.packageManagerService.uninstallPackage(packageName, envId)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to uninstall virtual package: $packageName", e)
                }
            }
            selectAppForDetails(null)
            val env = environmentRepository.getEnvironment(envId)
            if (env != null) {
                loadAppsForEnvironment(context, env)
            }
            showNotice("App removed from container.")
        }
    }

    /**
     * Freezes dormant background processes in the container.
     */
    fun freezeContainer(env: EnvironmentEntity) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val vc = VirtualCore.get()
                    vc.activityManagerService.killAllProcessesForEnvironment(env.id)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to freeze container: ${env.id}", e)
                }
            }
            closeActionSheet()
            showNotice("Processes frozen for '${env.displayName}'")
        }
    }
}

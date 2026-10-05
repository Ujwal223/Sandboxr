package com.ujwal.sandboxr.ui.settings

import android.content.Context
import android.content.SharedPreferences

/**
 * Theme mode selection for SANDBOXR Launcher.
 */
enum class LauncherThemeMode(val displayName: String) {
    SYSTEM("Follow device theme"),
    DARK("Always dark"),
    LIGHT("Always light")
}

/**
 * Search engine providers for the app drawer search bar.
 */
enum class SearchEngine(val displayName: String, val searchUrl: String) {
    DUCKDUCKGO("DuckDuckGo", "https://duckduckgo.com/?q="),
    STARTPAGE("Startpage", "https://www.startpage.com/sp/search?query="),
    BRAVE("Brave Search", "https://search.brave.com/search?q="),
    GOOGLE("Google", "https://www.google.com/search?q=")
}

/**
 * Immutable settings state for GrapheneOS Launcher3 features.
 */
data class LauncherSettings(
    // Grid & Layout
    val gridColumns: Int = 4,
    val gridRows: Int = 5,
    val hotseatIconCount: Int = 5,

    // Multi-Screen Workspace (PagedView matching AOSP / GrapheneOS)
    val pageCount: Int = 2,
    val defaultPageIndex: Int = 0,

    // Home Screen, Widgets & At-a-Glance
    val showHomeLabels: Boolean = true,
    val showAtAGlance: Boolean = true,
    val showWidgets: Boolean = true,
    val showDockSearchBar: Boolean = true,
    val autoAddNewAppsToHome: Boolean = true,
    val allowScreenRotation: Boolean = false,
    val wallpaperDimming: Float = 0.20f,
    val wallpaperPreset: String = "system",
    val customWallpaperUri: String? = null,

    // App Drawer
    val showDrawerLabels: Boolean = true,
    val showDrawerSearchBar: Boolean = true,
    val drawerColumns: Int = 5,
    val searchEngine: SearchEngine = SearchEngine.DUCKDUCKGO,

    // Theming & Icons
    val themeMode: LauncherThemeMode = LauncherThemeMode.SYSTEM,
    val themedIcons: Boolean = false,
    val iconShape: String = "Circle",

    // Gestures
    val swipeDownForNotifications: Boolean = true,
    val doubleTapToLock: Boolean = true,
    val swipeUpForDrawer: Boolean = true,

    // Security & Privacy (Signature GrapheneOS features)
    val hiddenPackages: Set<String> = emptySet(),
    val protectedPackages: Set<String> = emptySet(),
    val showSandboxIndicators: Boolean = true,

    // Onboarding & Guided Tour
    val hasCompletedOnboarding: Boolean = false
)

/**
 * SharedPreferences persistence manager for LauncherSettings.
 */
class LauncherPreferencesManager(context: Context) {

    companion object {
        private const val PREFS_NAME = "grapheneos_launcher_preferences"

        private const val KEY_GRID_COLUMNS = "pref_grid_columns"
        private const val KEY_GRID_ROWS = "pref_grid_rows"
        private const val KEY_HOTSEAT_COUNT = "pref_hotseat_count"

        private const val KEY_PAGE_COUNT = "pref_page_count"
        private const val KEY_DEFAULT_PAGE_INDEX = "pref_default_page_index"

        private const val KEY_SHOW_HOME_LABELS = "pref_show_home_labels"
        private const val KEY_SHOW_AT_A_GLANCE = "pref_show_at_a_glance"
        private const val KEY_SHOW_WIDGETS = "pref_show_widgets"
        private const val KEY_SHOW_DOCK_SEARCH_BAR = "pref_show_dock_search_bar"
        private const val KEY_AUTO_ADD_APPS = "pref_add_icon_to_home"
        private const val KEY_ALLOW_ROTATION = "pref_allow_rotation"
        private const val KEY_WALLPAPER_DIMMING = "pref_wallpaper_dimming"
        private const val KEY_WALLPAPER_PRESET = "pref_wallpaper_preset"
        private const val KEY_CUSTOM_WALLPAPER_URI = "pref_custom_wallpaper_uri"

        private const val KEY_SHOW_DRAWER_LABELS = "pref_show_drawer_labels"
        private const val KEY_SHOW_SEARCH_BAR = "pref_show_search_bar"
        private const val KEY_DRAWER_COLUMNS = "pref_drawer_columns"
        private const val KEY_SEARCH_ENGINE = "pref_search_engine"

        private const val KEY_THEME_MODE = "pref_theme_mode"
        private const val KEY_THEMED_ICONS = "pref_themed_icons"
        private const val KEY_ICON_SHAPE = "pref_icon_shape"

        private const val KEY_SWIPE_NOTIFICATIONS = "pref_swipe_notifications"
        private const val KEY_DOUBLE_TAP_LOCK = "pref_double_tap_lock"
        private const val KEY_SWIPE_DRAWER = "pref_swipe_drawer"

        private const val KEY_HIDDEN_PACKAGES = "pref_hidden_packages"
        private const val KEY_PROTECTED_PACKAGES = "pref_protected_packages"
        private const val KEY_SHOW_SANDBOX_INDICATORS = "pref_show_sandbox_indicators"
        private const val KEY_HAS_COMPLETED_ONBOARDING = "pref_has_completed_onboarding"
    }

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadSettings(): LauncherSettings {
        return LauncherSettings(
            gridColumns = prefs.getInt(KEY_GRID_COLUMNS, 4),
            gridRows = prefs.getInt(KEY_GRID_ROWS, 5),
            hotseatIconCount = prefs.getInt(KEY_HOTSEAT_COUNT, 5),

            pageCount = prefs.getInt(KEY_PAGE_COUNT, 2).coerceAtLeast(1),
            defaultPageIndex = prefs.getInt(KEY_DEFAULT_PAGE_INDEX, 0),

            showHomeLabels = prefs.getBoolean(KEY_SHOW_HOME_LABELS, true),
            showAtAGlance = prefs.getBoolean(KEY_SHOW_AT_A_GLANCE, true),
            showWidgets = prefs.getBoolean(KEY_SHOW_WIDGETS, true),
            showDockSearchBar = prefs.getBoolean(KEY_SHOW_DOCK_SEARCH_BAR, true),
            autoAddNewAppsToHome = prefs.getBoolean(KEY_AUTO_ADD_APPS, true),
            allowScreenRotation = prefs.getBoolean(KEY_ALLOW_ROTATION, false),
            wallpaperDimming = prefs.getFloat(KEY_WALLPAPER_DIMMING, 0.20f),
            wallpaperPreset = prefs.getString(KEY_WALLPAPER_PRESET, "system") ?: "system",
            customWallpaperUri = prefs.getString(KEY_CUSTOM_WALLPAPER_URI, null),

            showDrawerLabels = prefs.getBoolean(KEY_SHOW_DRAWER_LABELS, true),
            showDrawerSearchBar = prefs.getBoolean(KEY_SHOW_SEARCH_BAR, true),
            drawerColumns = prefs.getInt(KEY_DRAWER_COLUMNS, 5),
            searchEngine = try {
                SearchEngine.valueOf(prefs.getString(KEY_SEARCH_ENGINE, SearchEngine.DUCKDUCKGO.name) ?: SearchEngine.DUCKDUCKGO.name)
            } catch (_: Exception) {
                SearchEngine.DUCKDUCKGO
            },

            themeMode = try {
                LauncherThemeMode.valueOf(prefs.getString(KEY_THEME_MODE, LauncherThemeMode.SYSTEM.name) ?: LauncherThemeMode.SYSTEM.name)
            } catch (_: Exception) {
                LauncherThemeMode.SYSTEM
            },
            themedIcons = prefs.getBoolean(KEY_THEMED_ICONS, false),
            iconShape = prefs.getString(KEY_ICON_SHAPE, "Circle") ?: "Circle",

            swipeDownForNotifications = prefs.getBoolean(KEY_SWIPE_NOTIFICATIONS, true),
            doubleTapToLock = prefs.getBoolean(KEY_DOUBLE_TAP_LOCK, true),
            swipeUpForDrawer = prefs.getBoolean(KEY_SWIPE_DRAWER, true),

            hiddenPackages = prefs.getStringSet(KEY_HIDDEN_PACKAGES, emptySet()) ?: emptySet(),
            protectedPackages = prefs.getStringSet(KEY_PROTECTED_PACKAGES, emptySet()) ?: emptySet(),
            showSandboxIndicators = prefs.getBoolean(KEY_SHOW_SANDBOX_INDICATORS, true),
            hasCompletedOnboarding = prefs.getBoolean(KEY_HAS_COMPLETED_ONBOARDING, false)
        )
    }

    fun saveSettings(settings: LauncherSettings) {
        prefs.edit()
            .putInt(KEY_GRID_COLUMNS, settings.gridColumns)
            .putInt(KEY_GRID_ROWS, settings.gridRows)
            .putInt(KEY_HOTSEAT_COUNT, settings.hotseatIconCount)
            .putInt(KEY_PAGE_COUNT, settings.pageCount)
            .putInt(KEY_DEFAULT_PAGE_INDEX, settings.defaultPageIndex)
            .putBoolean(KEY_SHOW_HOME_LABELS, settings.showHomeLabels)
            .putBoolean(KEY_SHOW_AT_A_GLANCE, settings.showAtAGlance)
            .putBoolean(KEY_SHOW_WIDGETS, settings.showWidgets)
            .putBoolean(KEY_SHOW_DOCK_SEARCH_BAR, settings.showDockSearchBar)
            .putBoolean(KEY_AUTO_ADD_APPS, settings.autoAddNewAppsToHome)
            .putBoolean(KEY_ALLOW_ROTATION, settings.allowScreenRotation)
            .putFloat(KEY_WALLPAPER_DIMMING, settings.wallpaperDimming)
            .putString(KEY_WALLPAPER_PRESET, settings.wallpaperPreset)
            .putString(KEY_CUSTOM_WALLPAPER_URI, settings.customWallpaperUri)
            .putBoolean(KEY_SHOW_DRAWER_LABELS, settings.showDrawerLabels)
            .putBoolean(KEY_SHOW_SEARCH_BAR, settings.showDrawerSearchBar)
            .putInt(KEY_DRAWER_COLUMNS, settings.drawerColumns)
            .putString(KEY_SEARCH_ENGINE, settings.searchEngine.name)
            .putString(KEY_THEME_MODE, settings.themeMode.name)
            .putBoolean(KEY_THEMED_ICONS, settings.themedIcons)
            .putString(KEY_ICON_SHAPE, settings.iconShape)
            .putBoolean(KEY_SWIPE_NOTIFICATIONS, settings.swipeDownForNotifications)
            .putBoolean(KEY_DOUBLE_TAP_LOCK, settings.doubleTapToLock)
            .putBoolean(KEY_SWIPE_DRAWER, settings.swipeUpForDrawer)
            .putStringSet(KEY_HIDDEN_PACKAGES, settings.hiddenPackages)
            .putStringSet(KEY_PROTECTED_PACKAGES, settings.protectedPackages)
            .putBoolean(KEY_SHOW_SANDBOX_INDICATORS, settings.showSandboxIndicators)
            .putBoolean(KEY_HAS_COMPLETED_ONBOARDING, settings.hasCompletedOnboarding)
            .apply()
    }
}

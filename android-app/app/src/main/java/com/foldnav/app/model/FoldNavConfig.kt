package com.foldnav.app.model

import android.content.Context
import android.content.SharedPreferences

enum class MapTheme(
    val id: String,
    val title: String,
    val tagline: String,
    val accentHex: String,
    val bgHex: String,
    val waterHex: String,
    val parkHex: String,
    val landuseHex: String,
    val minorRoadHex: String,
    val majorRoadHex: String,
    val motorwayHex: String,
    val buildingHex: String,
    val labelHex: String,
    val routeGlowHex: String,
    val routeCoreHex: String
) {
    NEO_TOKYO(
        id = "neo_tokyo",
        title = "Neo-Tokyo 3D",
        tagline = "Obsidian grid, electric cyan highways & extruded 3D monoliths.",
        accentHex = "#22E5FF",
        bgHex = "#060912",
        waterHex = "#071E36",
        parkHex = "#0B1E1B",
        landuseHex = "#0B101D",
        minorRoadHex = "#1E2D47",
        majorRoadHex = "#1BC6E8",
        motorwayHex = "#FF2E93",
        buildingHex = "#15233B",
        labelHex = "#BEEFFF",
        routeGlowHex = "#FF2E93",
        routeCoreHex = "#48F0FF"
    ),
    OBSIDIAN_GOLD(
        id = "obsidian_gold",
        title = "Obsidian & Gold",
        tagline = "Titanium monochrome streets with champagne-gold boulevards.",
        accentHex = "#D6B47B",
        bgHex = "#080A0F",
        waterHex = "#0C1420",
        parkHex = "#121814",
        landuseHex = "#0E121A",
        minorRoadHex = "#272D39",
        majorRoadHex = "#D6B47B",
        motorwayHex = "#FFE099",
        buildingHex = "#191E2A",
        labelHex = "#F5E2B8",
        routeGlowHex = "#D6B47B",
        routeCoreHex = "#FFF3C4"
    ),
    STEALTH_RADAR(
        id = "stealth_radar",
        title = "Stealth Radar",
        tagline = "High-contrast AMOLED emerald & amber tactical night telemetry.",
        accentHex = "#34F5A5",
        bgHex = "#030607",
        waterHex = "#041815",
        parkHex = "#061C12",
        landuseHex = "#070E0D",
        minorRoadHex = "#17322A",
        majorRoadHex = "#27C986",
        motorwayHex = "#FFB838",
        buildingHex = "#0D201B",
        labelHex = "#B3FFE0",
        routeGlowHex = "#27C986",
        routeCoreHex = "#A8FFE0"
    );

    companion object {
        fun fromId(id: String?): MapTheme = entries.firstOrNull { it.id == id } ?: NEO_TOKYO
    }
}

enum class NavCursor(
    val id: String,
    val title: String,
    val subtitle: String,
    val primaryHex: String,
    val glowHex: String
) {
    HYPERCAR_GT(
        id = "hypercar_gt",
        title = "Hypercar GT",
        subtitle = "Matte titanium & neon diffuser",
        primaryHex = "#D6B47B",
        glowHex = "#22E5FF"
    ),
    SONIC_SPINDASH(
        id = "sonic_spindash",
        title = "Sonic Velocity",
        subtitle = "Electric cobalt & gold crescent",
        primaryHex = "#1F8BFF",
        glowHex = "#FFD447"
    ),
    SHADOW_RIDER(
        id = "shadow_rider",
        title = "Dark Rider",
        subtitle = "Crimson streak & obsidian blade",
        primaryHex = "#FF2244",
        glowHex = "#FFB636"
    ),
    STEALTH_JET(
        id = "stealth_jet",
        title = "Stealth Delta",
        subtitle = "Afterburner vector delta jet",
        primaryHex = "#94A3B8",
        glowHex = "#38BDF8"
    ),
    TITANIUM_ARROW(
        id = "titanium_arrow",
        title = "Prism Chevron",
        subtitle = "Precision dual-tone reticle",
        primaryHex = "#F8FAFC",
        glowHex = "#D6B47B"
    );

    companion object {
        fun fromId(id: String?): NavCursor = entries.firstOrNull { it.id == id } ?: HYPERCAR_GT
    }
}

data class QuickDestination(
    val label: String,
    val query: String,
    val lat: Double,
    val lon: Double
)

object DefaultDestinations {
    val presets = listOf(
        QuickDestination("Millennium Park", "Millennium Park Chicago", 41.8826, -87.6226),
        QuickDestination("Navy Pier", "Navy Pier Chicago", 41.8917, -87.6043),
        QuickDestination("Willis Tower", "Willis Tower Chicago", 41.8789, -87.6359),
        QuickDestination("O'Hare (ORD)", "Chicago O'Hare International Airport", 41.9742, -87.9073)
    )
}

data class FoldNavSettings(
    val theme: MapTheme = MapTheme.NEO_TOKYO,
    val cursor: NavCursor = NavCursor.HYPERCAR_GT,
    val useMph: Boolean = true,
    val voiceEnabled: Boolean = true,
    val demoCruise: Boolean = true,
    val autoFlex3D: Boolean = true,
    val speedAlertMph: Int = 75
)

class FoldNavPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): FoldNavSettings {
        return FoldNavSettings(
            theme = MapTheme.fromId(prefs.getString(KEY_THEME, MapTheme.NEO_TOKYO.id)),
            cursor = NavCursor.fromId(prefs.getString(KEY_CURSOR, NavCursor.HYPERCAR_GT.id)),
            useMph = prefs.getBoolean(KEY_USE_MPH, true),
            voiceEnabled = prefs.getBoolean(KEY_VOICE_ENABLED, true),
            demoCruise = prefs.getBoolean(KEY_DEMO_CRUISE, true),
            autoFlex3D = prefs.getBoolean(KEY_AUTO_FLEX_3D, true),
            speedAlertMph = prefs.getInt(KEY_SPEED_ALERT_MPH, 75).coerceIn(35, 130)
        )
    }

    fun save(settings: FoldNavSettings) {
        prefs.edit()
            .putString(KEY_THEME, settings.theme.id)
            .putString(KEY_CURSOR, settings.cursor.id)
            .putBoolean(KEY_USE_MPH, settings.useMph)
            .putBoolean(KEY_VOICE_ENABLED, settings.voiceEnabled)
            .putBoolean(KEY_DEMO_CRUISE, settings.demoCruise)
            .putBoolean(KEY_AUTO_FLEX_3D, settings.autoFlex3D)
            .putInt(KEY_SPEED_ALERT_MPH, settings.speedAlertMph)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "foldnav_prefs"
        private const val KEY_THEME = "map_theme"
        private const val KEY_CURSOR = "nav_cursor"
        private const val KEY_USE_MPH = "use_mph"
        private const val KEY_VOICE_ENABLED = "voice_enabled"
        private const val KEY_DEMO_CRUISE = "demo_cruise"
        private const val KEY_AUTO_FLEX_3D = "auto_flex_3d"
        private const val KEY_SPEED_ALERT_MPH = "speed_alert_mph"
    }
}

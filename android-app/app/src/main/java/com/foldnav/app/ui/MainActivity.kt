package com.foldnav.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.foldnav.app.map.CursorBitmapFactory
import com.foldnav.app.map.MapStyleFactory
import com.foldnav.app.model.DefaultDestinations
import com.foldnav.app.model.FoldNavPreferences
import com.foldnav.app.model.FoldNavSettings
import com.foldnav.app.model.MapTheme
import com.foldnav.app.model.NavCursor
import com.foldnav.app.nav.GoogleMapsLauncher
import com.foldnav.app.nav.PlaceResult
import com.foldnav.app.nav.RouteResult
import com.foldnav.app.nav.RoutingClient
import com.foldnav.app.overlay.FoldHudOverlayService
import com.foldnav.app.overlay.HudOverlayState
import com.foldnav.app.sensor.FoldTelemetryEngine
import com.foldnav.app.sensor.TelemetryBus
import com.foldnav.app.voice.FoldVoiceAnnouncer
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import java.util.Locale
import kotlin.math.roundToInt

private val BgObsidian = Color(0xFF090B10)
private val BgPanel = Color(0xFF10131A)
private val BgControl = Color(0xFF171C26)
private val BorderHairline = Color(0xFF283142)
private val AccentGold = Color(0xFFD6B47B)
private val AccentCyan = Color(0xFF22E5FF)
private val InkPrimary = Color(0xFFF3F6FA)
private val InkMuted = Color(0xFF9AA5B8)

private const val ROUTE_SOURCE_ID = "foldnav-route-source"
private const val ROUTE_GLOW_LAYER_ID = "foldnav-route-glow"
private const val ROUTE_CORE_LAYER_ID = "foldnav-route-core"
private const val CURSOR_SOURCE_ID = "foldnav-cursor-source"
private const val CURSOR_LAYER_ID = "foldnav-cursor-layer"
private const val CURSOR_IMAGE_ID = "foldnav-cursor-img"
private const val EMPTY_FEATURE_COLLECTION = """{"type":"FeatureCollection","features":[]}"""

class MainActivity : ComponentActivity() {
    private lateinit var prefs: FoldNavPreferences
    private lateinit var telemetryEngine: FoldTelemetryEngine
    private lateinit var voiceAnnouncer: FoldVoiceAnnouncer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapLibre.getInstance(applicationContext)

        prefs = FoldNavPreferences(this)
        val initialSettings = prefs.load()
        telemetryEngine = FoldTelemetryEngine(this).apply {
            demoCruiseEnabled = initialSettings.demoCruise
        }
        voiceAnnouncer = FoldVoiceAnnouncer(this)

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = BgObsidian,
                    surface = BgPanel,
                    primary = AccentGold
                )
            ) {
                FoldNavScreen(
                    prefs = prefs,
                    initialSettings = initialSettings,
                    telemetryEngine = telemetryEngine,
                    voiceAnnouncer = voiceAnnouncer
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        telemetryEngine.start(this)
    }

    override fun onStop() {
        super.onStop()
        telemetryEngine.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        voiceAnnouncer.shutdown()
    }
}

@Composable
private fun FoldNavScreen(
    prefs: FoldNavPreferences,
    initialSettings: FoldNavSettings,
    telemetryEngine: FoldTelemetryEngine,
    voiceAnnouncer: FoldVoiceAnnouncer
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    var settings by remember { mutableStateOf(initialSettings) }
    val telemetry by TelemetryBus.state.collectAsState()
    val hudActive by HudOverlayState.running.collectAsState()

    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }
    var loadedStyle by remember { mutableStateOf<Style?>(null) }
    var followGps by remember { mutableStateOf(true) }
    var cameraInitialized by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<PlaceResult>>(emptyList()) }
    var activeRoute by remember { mutableStateOf<RouteResult?>(null) }
    var routeBusy by remember { mutableStateOf(false) }
    var statusBanner by remember { mutableStateOf("Chicago Loop 3D Demo · Ready") }
    var lastSelectedDest by remember {
        mutableStateOf(
            PlaceResult(
                DefaultDestinations.presets.first().label,
                DefaultDestinations.presets.first().lat,
                DefaultDestinations.presets.first().lon
            )
        )
    }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var hasOverlayPermission by remember { mutableStateOf(Settings.canDrawOverlays(context)) }

    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasLocationPermission = granted
        if (granted) {
            telemetryEngine.refreshLocationUpdates()
            statusBanner = "GPS active"
        }
    }

    fun applySettings(next: FoldNavSettings) {
        settings = next
        prefs.save(next)
        telemetryEngine.demoCruiseEnabled = next.demoCruise
    }

    fun routeToPlace(place: PlaceResult) {
        lastSelectedDest = place
        searchResults = emptyList()
        routeBusy = true
        statusBanner = "Routing to ${place.displayName.substringBefore(',')}..."
        coroutineScope.launch {
            val result = RoutingClient.fetchDrivingRoute(
                fromLat = telemetry.lat,
                fromLon = telemetry.lon,
                toLat = place.lat,
                toLon = place.lon,
                destLabel = place.displayName.substringBefore(',')
            )
            routeBusy = false
            if (result != null) {
                activeRoute = result
                val miles = result.distanceMeters / 1609.34
                val mins = (result.durationSeconds / 60.0).roundToInt().coerceAtLeast(1)
                statusBanner = String.format(Locale.US, "%.1f mi · %d min · %s", miles, mins, result.firstInstruction)
                voiceAnnouncer.speak(
                    "Route ready. ${String.format(Locale.US, "%.1f miles", miles)}, $mins minutes. ${result.firstInstruction}",
                    settings.voiceEnabled,
                    force = true
                )
            } else {
                statusBanner = "Offline route failed — tap Launch Google Maps"
            }
        }
    }

    val mapView = remember {
        MapView(context).apply {
            onCreate(null)
            getMapAsync { map ->
                map.uiSettings.isCompassEnabled = false
                map.uiSettings.isAttributionEnabled = false
                map.uiSettings.isLogoEnabled = false
                map.addOnCameraMoveStartedListener { reason ->
                    if (reason == MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE) {
                        followGps = false
                    }
                }
                mapLibreMap = map
            }
        }
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> {
                    mapView.onResume()
                    hasOverlayPermission = Settings.canDrawOverlays(context)
                }
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Apply MapLibre style when theme changes
    LaunchedEffect(mapLibreMap, settings.theme) {
        val map = mapLibreMap ?: return@LaunchedEffect
        loadedStyle = null
        val json = MapStyleFactory.buildStyleJson(settings.theme)
        map.setStyle(Style.Builder().fromJson(json)) { style ->
            style.addImage(CURSOR_IMAGE_ID, CursorBitmapFactory.buildBitmap(settings.cursor))

            val routeInitial = activeRoute?.geoJsonFeatureCollection ?: EMPTY_FEATURE_COLLECTION
            style.addSource(GeoJsonSource(ROUTE_SOURCE_ID, routeInitial))
            style.addLayer(
                LineLayer(ROUTE_GLOW_LAYER_ID, ROUTE_SOURCE_ID).withProperties(
                    PropertyFactory.lineColor(settings.theme.routeGlowHex),
                    PropertyFactory.lineWidth(12f),
                    PropertyFactory.lineBlur(4.5f),
                    PropertyFactory.lineOpacity(0.58f),
                    PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                    PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND)
                )
            )
            style.addLayer(
                LineLayer(ROUTE_CORE_LAYER_ID, ROUTE_SOURCE_ID).withProperties(
                    PropertyFactory.lineColor(settings.theme.routeCoreHex),
                    PropertyFactory.lineWidth(4.8f),
                    PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                    PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND)
                )
            )

            val initialFeat = Feature.fromGeometry(Point.fromLngLat(telemetry.lon, telemetry.lat)).apply {
                addNumberProperty("bearing", telemetry.bearingDeg)
            }
            style.addSource(GeoJsonSource(CURSOR_SOURCE_ID, FeatureCollection.fromFeature(initialFeat)))
            style.addLayer(
                SymbolLayer(CURSOR_LAYER_ID, CURSOR_SOURCE_ID).withProperties(
                    PropertyFactory.iconImage(CURSOR_IMAGE_ID),
                    PropertyFactory.iconSize(0.56f),
                    PropertyFactory.iconAllowOverlap(true),
                    PropertyFactory.iconIgnorePlacement(true),
                    PropertyFactory.iconRotate(Expression.get("bearing")),
                    PropertyFactory.iconRotationAlignment(Property.ICON_ROTATION_ALIGNMENT_MAP),
                    PropertyFactory.iconPitchAlignment(Property.ICON_PITCH_ALIGNMENT_MAP)
                )
            )
            loadedStyle = style
        }
    }

    // Hot-swap cursor sprite cleanly without reloading vector tiles
    LaunchedEffect(loadedStyle, settings.cursor) {
        val style = loadedStyle ?: return@LaunchedEffect
        style.addImage(CURSOR_IMAGE_ID, CursorBitmapFactory.buildBitmap(settings.cursor))
    }

    // Sync active OSRM route geometry
    LaunchedEffect(loadedStyle, activeRoute) {
        val style = loadedStyle ?: return@LaunchedEffect
        style.getSourceAs<GeoJsonSource>(ROUTE_SOURCE_ID)
            ?.setGeoJson(activeRoute?.geoJsonFeatureCollection ?: EMPTY_FEATURE_COLLECTION)
    }

    // Update GPS/demo cursor and 3D chase camera
    LaunchedEffect(loadedStyle, mapLibreMap, telemetry, followGps, settings.autoFlex3D) {
        val style = loadedStyle ?: return@LaunchedEffect
        val map = mapLibreMap ?: return@LaunchedEffect

        val feat = Feature.fromGeometry(Point.fromLngLat(telemetry.lon, telemetry.lat)).apply {
            addNumberProperty("bearing", telemetry.bearingDeg)
        }
        style.getSourceAs<GeoJsonSource>(CURSOR_SOURCE_ID)
            ?.setGeoJson(FeatureCollection.fromFeature(feat))

        if (followGps) {
            val targetPitch = if (telemetry.isFlexMode && settings.autoFlex3D) 64.0 else 50.0
            val targetZoom = if (telemetry.isFlexMode && settings.autoFlex3D) 16.8 else 16.3
            val cam = CameraPosition.Builder()
                .target(LatLng(telemetry.lat, telemetry.lon))
                .zoom(targetZoom)
                .tilt(targetPitch)
                .bearing(telemetry.bearingDeg.toDouble())
                .build()
            if (!cameraInitialized) {
                cameraInitialized = true
                map.moveCamera(CameraUpdateFactory.newCameraPosition(cam))
            } else {
                map.animateCamera(CameraUpdateFactory.newCameraPosition(cam), 240)
            }
        }
    }

    val mapBlock: @Composable (Modifier) -> Unit = { mod ->
        Box(
            modifier = mod
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, BorderHairline, RoundedCornerShape(16.dp))
                .background(BgObsidian)
        ) {
            AndroidView(modifier = Modifier.fillMaxSize(), factory = { mapView })

            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .background(Color(0xCC0D1119), RoundedCornerShape(10.dp))
                    .border(1.dp, BorderHairline, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (telemetry.isFlexMode) AccentGold else AccentCyan)
                )
                Column {
                    Text(
                        text = settings.theme.title,
                        color = InkPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (telemetry.isFlexMode) {
                            "Flex 3D · ${telemetry.hingeAngleDeg.roundToInt()}°"
                        } else {
                            "3D Extrusion · ${telemetry.hingeAngleDeg.roundToInt()}°"
                        },
                        color = AccentGold,
                        fontSize = 11.sp
                    )
                }
            }

            if (!followGps) {
                Button(
                    onClick = { followGps = true },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = Color.Black),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                ) {
                    Text("Re-Center 3D", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    val cockpitBlock: @Composable (Modifier) -> Unit = { mod ->
        Column(
            modifier = mod
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Digital Speedometer + Compass + G-Force Ball
            TelemetryCockpitCard(
                speedMps = telemetry.speedMps,
                bearingDeg = telemetry.bearingDeg,
                lateralG = telemetry.lateralG,
                longitudinalG = telemetry.longitudinalG,
                useMph = settings.useMph,
                speedAlertMph = settings.speedAlertMph,
                isDemo = telemetry.isDemoMode,
                statusText = statusBanner
            )

            // 2. Destination Search, Presets & Google Maps Handoff
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BgControl, RoundedCornerShape(14.dp))
                    .border(1.dp, BorderHairline, RoundedCornerShape(14.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search address or place...", color = InkMuted, fontSize = 13.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            if (searchQuery.isNotBlank()) {
                                coroutineScope.launch {
                                    statusBanner = "Searching..."
                                    val found = RoutingClient.searchPlaces(searchQuery)
                                    searchResults = found
                                    if (found.size == 1) routeToPlace(found.first())
                                }
                            }
                        }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentGold,
                            unfocusedBorderColor = BorderHairline,
                            focusedTextColor = InkPrimary,
                            unfocusedTextColor = InkPrimary
                        )
                    )
                    Button(
                        onClick = {
                            if (searchQuery.isNotBlank()) {
                                coroutineScope.launch {
                                    statusBanner = "Searching..."
                                    val found = RoutingClient.searchPlaces(searchQuery)
                                    searchResults = found
                                    if (found.size == 1) routeToPlace(found.first())
                                }
                            }
                        },
                        enabled = !routeBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (routeBusy) "..." else "Go", fontWeight = FontWeight.SemiBold)
                    }
                }

                if (searchResults.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        searchResults.take(3).forEach { place ->
                            Text(
                                text = place.displayName,
                                color = AccentCyan,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(BgPanel)
                                    .clickable { routeToPlace(place) }
                                    .padding(8.dp)
                            )
                        }
                    }
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(DefaultDestinations.presets) { preset ->
                        Text(
                            text = preset.label,
                            color = InkPrimary,
                            fontSize = 11.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(BgPanel)
                                .border(1.dp, BorderHairline, RoundedCornerShape(8.dp))
                                .clickable {
                                    routeToPlace(PlaceResult(preset.label, preset.lat, preset.lon))
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val d = lastSelectedDest
                            voiceAnnouncer.speak(
                                "Launching Google Maps to ${d.displayName.substringBefore(',')}",
                                settings.voiceEnabled,
                                force = true
                            )
                            GoogleMapsLauncher.launchTurnByTurn(context, d.lat, d.lon, d.displayName, false)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentCyan, contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Google Maps Nav", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = {
                            val d = lastSelectedDest
                            GoogleMapsLauncher.launchTurnByTurn(context, d.lat, d.lon, d.displayName, true)
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Split-Screen Maps", color = InkPrimary, fontSize = 12.sp)
                    }
                }
            }

            // 3. Map Theme Carousel
            Text("Custom 3D Map Style", color = InkMuted, fontSize = 12.sp)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 6.dp)
            ) {
                items(MapTheme.entries) { theme ->
                    val selected = theme == settings.theme
                    Column(
                        modifier = Modifier
                            .width(156.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) Color(0x26D6B47B) else BgControl)
                            .border(1.dp, if (selected) AccentGold else BorderHairline, RoundedCornerShape(12.dp))
                            .clickable {
                                applySettings(settings.copy(theme = theme))
                                voiceAnnouncer.speak("${theme.title} map style enabled", settings.voiceEnabled)
                            }
                            .padding(10.dp)
                    ) {
                        Text(theme.title, color = InkPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(3.dp))
                        Text(theme.tagline, color = InkMuted, fontSize = 11.sp, maxLines = 2, lineHeight = 14.sp)
                    }
                }
            }

            // 4. Custom Vehicle / Character Cursor Carousel
            Text("Custom Nav Cursor", color = InkMuted, fontSize = 12.sp)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 6.dp)
            ) {
                items(NavCursor.entries) { cursor ->
                    val selected = cursor == settings.cursor
                    Column(
                        modifier = Modifier
                            .width(142.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) Color(0x2422E5FF) else BgControl)
                            .border(1.dp, if (selected) AccentCyan else BorderHairline, RoundedCornerShape(12.dp))
                            .clickable {
                                applySettings(settings.copy(cursor = cursor))
                                voiceAnnouncer.speak("${cursor.title} cursor selected", settings.voiceEnabled)
                            }
                            .padding(10.dp)
                    ) {
                        Text(cursor.title, color = InkPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(3.dp))
                        Text(cursor.subtitle, color = InkMuted, fontSize = 11.sp, maxLines = 2, lineHeight = 14.sp)
                    }
                }
            }

            // 5. Floating HUD, Demo Cruise & Fold Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BgControl, RoundedCornerShape(14.dp))
                    .border(1.dp, BorderHairline, RoundedCornerShape(14.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ToggleRow(
                    label = if (hasOverlayPermission) "Floating HUD Pill Over Maps" else "Floating HUD (Grant Overlay)",
                    checked = hudActive,
                    onCheckedChange = { wantOn ->
                        if (wantOn) {
                            if (!Settings.canDrawOverlays(context)) {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            } else {
                                context.startForegroundService(Intent(context, FoldHudOverlayService::class.java))
                            }
                        } else {
                            context.stopService(Intent(context, FoldHudOverlayService::class.java))
                        }
                    }
                )
                ToggleRow(
                    label = "Chicago 3D Demo Cruise",
                    checked = settings.demoCruise,
                    onCheckedChange = { applySettings(settings.copy(demoCruise = it)) }
                )
                ToggleRow(
                    label = "Auto Flex Mode 3D Tilt",
                    checked = settings.autoFlex3D,
                    onCheckedChange = { applySettings(settings.copy(autoFlex3D = it)) }
                )
                ToggleRow(
                    label = "Speed Units (MPH / KM/H)",
                    checked = settings.useMph,
                    onCheckedChange = { applySettings(settings.copy(useMph = it)) }
                )
                ToggleRow(
                    label = "Voice Co-Pilot Alerts",
                    checked = settings.voiceEnabled,
                    onCheckedChange = { applySettings(settings.copy(voiceEnabled = it)) }
                )

                if (!hasLocationPermission) {
                    Button(
                        onClick = {
                            locationLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BgPanel, contentColor = AccentGold),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Enable Live GPS Location", fontSize = 12.sp)
                    }
                }
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(BgObsidian)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        val useFlexSplit = telemetry.isFlexMode && settings.autoFlex3D
        val isWideInner = maxWidth >= 600.dp && !useFlexSplit

        if (isWideInner) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                mapBlock(
                    Modifier
                        .weight(0.58f)
                        .fillMaxHeight()
                )
                cockpitBlock(
                    Modifier
                        .weight(0.42f)
                        .fillMaxHeight()
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                mapBlock(
                    Modifier
                        .fillMaxWidth()
                        .weight(if (useFlexSplit) 0.50f else 0.52f)
                )
                cockpitBlock(
                    Modifier
                        .fillMaxWidth()
                        .weight(if (useFlexSplit) 0.50f else 0.48f)
                )
            }
        }
    }
}

@Composable
private fun TelemetryCockpitCard(
    speedMps: Float,
    bearingDeg: Float,
    lateralG: Float,
    longitudinalG: Float,
    useMph: Boolean,
    speedAlertMph: Int,
    isDemo: Boolean,
    statusText: String
) {
    val speedVal = if (useMph) (speedMps * 2.23694f).roundToInt() else (speedMps * 3.6f).roundToInt()
    val limit = if (useMph) speedAlertMph else (speedAlertMph * 1.60934f).roundToInt()
    val overLimit = speedVal >= limit

    val dirs = remember { arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW") }
    val normDeg = ((bearingDeg % 360f) + 360f) % 360f
    val cardinal = dirs[((normDeg / 45f).roundToInt()) % 8]

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgControl, RoundedCornerShape(14.dp))
            .border(1.dp, BorderHairline, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = speedVal.toString(),
                        color = if (overLimit) Color(0xFFFF4D6D) else AccentCyan,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (useMph) "MPH" else "KM/H",
                        color = InkMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
                Text(
                    text = if (isDemo) "SIMULATED CRUISE · CHICAGO 3D" else "LIVE GPS TELEMETRY",
                    color = AccentGold,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = String.format(Locale.US, "%s · %03d°", cardinal, normDeg.roundToInt()),
                    color = InkPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                // Compact G-Force Meter Circle
                Canvas(modifier = Modifier.size(44.dp)) {
                    val r = size.minDimension * 0.46f
                    val center = Offset(size.width / 2f, size.height / 2f)
                    drawCircle(color = BorderHairline, radius = r, center = center, style = Stroke(width = 2f))
                    drawLine(
                        color = BorderHairline,
                        start = Offset(center.x - r, center.y),
                        end = Offset(center.x + r, center.y),
                        strokeWidth = 1f
                    )
                    drawLine(
                        color = BorderHairline,
                        start = Offset(center.x, center.y - r),
                        end = Offset(center.x, center.y + r),
                        strokeWidth = 1f
                    )
                    val dx = (lateralG.coerceIn(-1f, 1f)) * r
                    val dy = (-longitudinalG.coerceIn(-1f, 1f)) * r
                    drawCircle(color = AccentCyan, radius = 5.5f, center = Offset(center.x + dx, center.y + dy))
                }
            }
        }

        Text(
            text = statusText,
            color = InkMuted,
            fontSize = 11.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = InkPrimary, fontSize = 13.sp)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = AccentGold)
        )
    }
}

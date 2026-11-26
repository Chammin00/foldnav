package com.foldnav.app.sensor

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

data class TelemetrySnapshot(
    val lat: Double = 41.8826,
    val lon: Double = -87.6242,
    val speedMps: Float = 0f,
    val bearingDeg: Float = 0f,
    val altitudeM: Double = 182.0,
    val lateralG: Float = 0f,
    val longitudinalG: Float = 0f,
    val hingeAngleDeg: Float = 180f,
    val isFlexMode: Boolean = false,
    val hasLiveGps: Boolean = false,
    val isDemoMode: Boolean = true
)

object TelemetryBus {
    private val _state = MutableStateFlow(TelemetrySnapshot())
    val state: StateFlow<TelemetrySnapshot> = _state.asStateFlow()

    fun publish(snapshot: TelemetrySnapshot) {
        _state.value = snapshot
    }
}

/**
 * Combines hardware hinge angle + Jetpack WindowInfoTracker Flex detection, linear G-forces,
 * Android LocationManager GPS fixes, and a dense downtown Chicago 3D Demo Cruise loop.
 */
class FoldTelemetryEngine(context: Context) : SensorEventListener, LocationListener {
    private val appContext = context.applicationContext
    private val sensorManager = appContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val locationManager = appContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    private val hingeSensor: Sensor? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        sensorManager?.getDefaultSensor(Sensor.TYPE_HINGE_ANGLE)
    } else {
        null
    }
    private val linearAccelSensor: Sensor? =
        sensorManager?.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)

    private var scope: CoroutineScope? = null
    private var windowJob: Job? = null
    private var tickerJob: Job? = null

    @Volatile
    var demoCruiseEnabled: Boolean = true

    private var hingeAngleDeg = 180f
    private var foldingFeatureFlex = false
    private var lateralG = 0f
    private var longitudinalG = 0f

    private var lastGpsLocation: Location? = null
    private var demoProgress = 0.0

    // Rectangular 3D skyscraper showcase loop in Chicago Loop (Michigan Ave -> Wacker -> State -> Adams)
    private val loopWaypoints = listOf(
        41.8794 to -87.6245,
        41.8878 to -87.6245,
        41.8878 to -87.6278,
        41.8794 to -87.6278
    )

    fun start(uiContext: Context) {
        if (scope != null) return
        val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        scope = engineScope

        hingeSensor?.let { sensor ->
            sensorManager?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
        }
        linearAccelSensor?.let { sensor ->
            sensorManager?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
        }

        windowJob = engineScope.launch {
            try {
                WindowInfoTracker.getOrCreate(uiContext).windowLayoutInfo(uiContext).collect { info ->
                    val folding = info.displayFeatures.filterIsInstance<FoldingFeature>().firstOrNull()
                    foldingFeatureFlex = folding?.state == FoldingFeature.State.HALF_OPENED
                }
            } catch (_: Throwable) {
                foldingFeatureFlex = false
            }
        }

        refreshLocationUpdates()

        tickerJob = engineScope.launch {
            while (isActive) {
                emitNextTick(0.10)
                delay(100L)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun refreshLocationUpdates() {
        val lm = locationManager ?: return
        val hasFine = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasFine && !hasCoarse) return

        try {
            lm.removeUpdates(this)
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 800L, 1.5f, this)
                lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.let { lastGpsLocation = it }
            }
            if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2500L, 10f, this)
                if (lastGpsLocation == null) {
                    lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)?.let { lastGpsLocation = it }
                }
            }
        } catch (_: Throwable) {
            // Fall back smoothly to demo cruise coordinates when location provider is restricted.
        }
    }

    fun stop() {
        sensorManager?.unregisterListener(this)
        try {
            locationManager?.removeUpdates(this)
        } catch (_: Throwable) {
        }
        windowJob?.cancel()
        tickerJob?.cancel()
        scope = null
        windowJob = null
        tickerJob = null
    }

    private fun emitNextTick(dtSec: Double) {
        val gps = lastGpsLocation
        val useLiveGps = gps != null && !demoCruiseEnabled

        val isFlex = foldingFeatureFlex || (hingeAngleDeg in 62f..128f)

        val snap = if (useLiveGps && gps != null) {
            TelemetrySnapshot(
                lat = gps.latitude,
                lon = gps.longitude,
                speedMps = if (gps.hasSpeed()) gps.speed else 0f,
                bearingDeg = if (gps.hasBearing()) gps.bearing else 0f,
                altitudeM = if (gps.hasAltitude()) gps.altitude else 182.0,
                lateralG = lateralG,
                longitudinalG = longitudinalG,
                hingeAngleDeg = hingeAngleDeg,
                isFlexMode = isFlex,
                hasLiveGps = true,
                isDemoMode = false
            )
        } else {
            demoProgress = (demoProgress + dtSec * 0.048) % loopWaypoints.size.toDouble()
            val segIdx = demoProgress.toInt()
            val segT = demoProgress - segIdx
            val nextIdx = (segIdx + 1) % loopWaypoints.size
            val p0 = loopWaypoints[segIdx]
            val p1 = loopWaypoints[nextIdx]

            val lat = p0.first + (p1.first - p0.first) * segT
            val lon = p0.second + (p1.second - p0.second) * segT
            val bearing = computeBearing(p0.first, p0.second, p1.first, p1.second).toFloat()

            // Subtle dynamic speed modulation along avenues + synthetic corner G if sensor is still
            val cruiseMps = (16.5 + 3.2 * sin(demoProgress * 3.14159)).toFloat()
            val synthLatG = if (kotlin.math.abs(lateralG) < 0.02f) {
                (sin(demoProgress * 6.28318) * 0.28).toFloat()
            } else {
                lateralG
            }

            TelemetrySnapshot(
                lat = lat,
                lon = lon,
                speedMps = cruiseMps,
                bearingDeg = bearing,
                altitudeM = 182.0,
                lateralG = synthLatG,
                longitudinalG = longitudinalG,
                hingeAngleDeg = hingeAngleDeg,
                isFlexMode = isFlex,
                hasLiveGps = gps != null,
                isDemoMode = true
            )
        }

        TelemetryBus.publish(snap)
    }

    private fun computeBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val dLon = Math.toRadians(lon2 - lon1)
        val y = sin(dLon) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(dLon)
        return (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && event.sensor.type == Sensor.TYPE_HINGE_ANGLE) {
            hingeAngleDeg = (event.values.firstOrNull() ?: hingeAngleDeg).coerceIn(0f, 180f)
        } else if (event.sensor.type == Sensor.TYPE_LINEAR_ACCELERATION && event.values.size >= 2) {
            val gx = (event.values[0] / 9.81f).coerceIn(-1.5f, 1.5f)
            val gy = (event.values[1] / 9.81f).coerceIn(-1.5f, 1.5f)
            lateralG += (gx - lateralG) * 0.22f
            longitudinalG += (gy - longitudinalG) * 0.22f
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onLocationChanged(location: Location) {
        lastGpsLocation = location
    }

    @Deprecated("Deprecated in Android API 29")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
}

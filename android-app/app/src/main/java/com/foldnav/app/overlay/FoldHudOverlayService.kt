package com.foldnav.app.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import com.foldnav.app.model.FoldNavPreferences
import com.foldnav.app.sensor.TelemetryBus
import com.foldnav.app.sensor.TelemetrySnapshot
import com.foldnav.app.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.hypot
import kotlin.math.roundToInt

object HudOverlayState {
    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running.asStateFlow()

    internal fun setRunning(value: Boolean) {
        _running.value = value
    }
}

/**
 * Lightweight floating Z Fold6 HUD pill (Speed, Heading, G-Force, Quick App Return)
 * designed to stay visible right over official Google Maps.
 */
class FoldHudOverlayService : Service() {
    private var windowManager: WindowManager? = null
    private var rootView: View? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var scope: CoroutineScope? = null
    private var telemetryJob: Job? = null

    private var speedTv: TextView? = null
    private var unitTv: TextView? = null
    private var headingTv: TextView? = null
    private var gForceTv: TextView? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundSafe()
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager = wm
        val card = buildOverlayView()
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(20)
            y = dp(96)
        }

        attachDragBehavior(card, params, wm)
        try {
            wm.addView(card, params)
            rootView = card
            layoutParams = params
            HudOverlayState.setRunning(true)
        } catch (_: Throwable) {
            stopSelf()
            return
        }

        val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        scope = serviceScope
        telemetryJob = serviceScope.launch {
            TelemetryBus.state.collect { snap ->
                renderSnapshot(snap)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        HudOverlayState.setRunning(false)
        telemetryJob?.cancel()
        scope?.cancel()
        rootView?.let { v ->
            try {
                windowManager?.removeView(v)
            } catch (_: Throwable) {
            }
        }
        rootView = null
        layoutParams = null
        windowManager = null
    }

    private fun renderSnapshot(snap: TelemetrySnapshot) {
        val prefs = FoldNavPreferences(this).load()
        val speedVal = if (prefs.useMph) {
            (snap.speedMps * 2.23694f).roundToInt()
        } else {
            (snap.speedMps * 3.6f).roundToInt()
        }
        val limit = if (prefs.useMph) prefs.speedAlertMph else (prefs.speedAlertMph * 1.60934f).roundToInt()
        val speedColor = if (speedVal >= limit) {
            Color.parseColor("#FF4D6D")
        } else {
            Color.parseColor("#22E5FF")
        }
        speedTv?.setTextColor(speedColor)
        speedTv?.text = speedVal.toString()
        unitTv?.text = if (prefs.useMph) "MPH" else "KM/H"

        val dirs = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        val deg = ((snap.bearingDeg % 360f) + 360f) % 360f
        val cardinal = dirs[((deg / 45f).roundToInt()) % 8]
        headingTv?.text = String.format(Locale.US, "%s %03d°", cardinal, deg.roundToInt())

        val totalG = hypot(snap.lateralG, snap.longitudinalG)
        gForceTv?.text = String.format(Locale.US, "%.2fG", totalG)
    }

    private fun buildOverlayView(): View {
        val bg = GradientDrawable().apply {
            setColor(Color.argb(232, 11, 14, 20))
            cornerRadius = dp(14).toFloat()
            setStroke(dp(1), Color.parseColor("#D6B47B"))
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = bg
            setPadding(dp(12), dp(8), dp(10), dp(8))
        }

        val speedCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }
        val sTv = TextView(this).apply {
            text = "0"
            setTextColor(Color.parseColor("#22E5FF"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            typeface = Typeface.DEFAULT_BOLD
            includeFontPadding = false
        }
        val uTv = TextView(this).apply {
            text = "MPH"
            setTextColor(Color.parseColor("#94A3B8"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f)
            typeface = Typeface.DEFAULT_BOLD
        }
        speedCol.addView(sTv)
        speedCol.addView(uTv)
        speedTv = sTv
        unitTv = uTv

        val metaCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), 0, dp(10), 0)
        }
        val hTv = TextView(this).apply {
            text = "N 000°"
            setTextColor(Color.parseColor("#F8FAFC"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = Typeface.DEFAULT_BOLD
        }
        val gTv = TextView(this).apply {
            text = "0.00G"
            setTextColor(Color.parseColor("#D6B47B"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        }
        metaCol.addView(hTv)
        metaCol.addView(gTv)
        headingTv = hTv
        gForceTv = gTv

        val openBtn = TextView(this).apply {
            text = "3D"
            setTextColor(Color.parseColor("#0B0E14"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(8), dp(5), dp(8), dp(5))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#D6B47B"))
                cornerRadius = dp(8).toFloat()
            }
            setOnClickListener {
                val launch = Intent(this@FoldHudOverlayService, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                startActivity(launch)
            }
        }

        val closeBtn = TextView(this).apply {
            text = "×"
            setTextColor(Color.parseColor("#CBD5E1"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setPadding(dp(10), dp(2), dp(4), dp(2))
            setOnClickListener { stopSelf() }
        }

        container.addView(speedCol)
        container.addView(metaCol)
        container.addView(openBtn)
        container.addView(closeBtn)
        return container
    }

    private fun attachDragBehavior(
        target: View,
        params: WindowManager.LayoutParams,
        wm: WindowManager
    ) {
        var initialX = 0
        var initialY = 0
        var touchX = 0f
        var touchY = 0f
        target.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    touchX = event.rawX
                    touchY = event.rawY
                    false
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - touchX).roundToInt()
                    params.y = initialY + (event.rawY - touchY).roundToInt()
                    try {
                        wm.updateViewLayout(v, params)
                    } catch (_: Throwable) {
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun startForegroundSafe() {
        try {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val ch = NotificationChannel(CHANNEL_ID, "FoldNav Floating HUD", NotificationManager.IMPORTANCE_LOW)
            nm.createNotificationChannel(ch)

            val pending = PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE
            )
            val notification = Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("FoldNav HUD Active")
                .setContentText("Speed, compass & G-force overlay ready over Google Maps")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setContentIntent(pending)
                .setOngoing(true)
                .build()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(NOTIF_ID, notification)
            }
        } catch (_: Throwable) {
        }
    }

    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics
        ).roundToInt()
    }

    companion object {
        private const val CHANNEL_ID = "foldnav_hud_channel"
        private const val NOTIF_ID = 4041
        const val ACTION_STOP = "com.foldnav.app.STOP_HUD"
    }
}

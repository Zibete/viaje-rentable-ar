package com.zibete.driverassistant.overlay

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import com.zibete.driverassistant.calculator.DriverDecision

class DriverDecisionOverlayService : Service() {
    private var currentState: OverlayCardState? = null
    private var overlayView: View? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private val windowManager: WindowManager by lazy {
        getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }

        val state = intent?.toOverlayCardState() ?: OverlayCardState.simulatedAccept()
        updateOverlayState(state)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        removeOverlay()
        currentState = null
        super.onDestroy()
    }

    fun updateOverlayState(state: OverlayCardState) {
        currentState = state
        showOverlay(state)
    }

    private fun showOverlay(state: OverlayCardState) {
        removeOverlay()

        val view = buildOverlayView(state)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 24
            y = 140
        }

        view.enableDragging(params)
        windowManager.addView(view, params)
        overlayView = view
        layoutParams = params
    }

    private fun removeOverlay() {
        overlayView?.let { view ->
            runCatching { windowManager.removeView(view) }
        }
        overlayView = null
        layoutParams = null
    }

    private fun buildOverlayView(state: OverlayCardState): View {
        val accentColor = state.decision.toAccentColor()
        val background = GradientDrawable().apply {
            cornerRadius = 28f
            setColor(Color.argb(238, 20, 24, 28))
            setStroke(4, accentColor)
        }

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 22, 28, 22)
            this.background = background
            elevation = 12f
            minimumWidth = 260

            addView(
                overlayText(
                    text = state.decision.toSpanishLabel(),
                    sizeSp = 20f,
                    color = accentColor,
                    style = Typeface.BOLD
                )
            )
            addView(overlayText(text = state.fareText, sizeSp = 24f, style = Typeface.BOLD))
            addView(
                overlayText(
                    text = "${state.arsPerHourText}/h - ${state.arsPerKmText}/km",
                    sizeSp = 14f
                )
            )
            addView(
                overlayText(
                    text = "${state.totalTimeText} - ${state.totalKmText}",
                    sizeSp = 14f
                )
            )

            state.shortReason?.takeIf { it.isNotBlank() }?.let { reason ->
                addView(
                    overlayText(
                        text = reason,
                        sizeSp = 13f,
                        color = Color.rgb(230, 234, 238)
                    )
                )
            }
        }
    }

    private fun overlayText(
        text: String,
        sizeSp: Float,
        color: Int = Color.WHITE,
        style: Int = Typeface.NORMAL
    ): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = sizeSp
            setTextColor(color)
            typeface = Typeface.create(Typeface.DEFAULT, style)
            includeFontPadding = false
            setPadding(0, 3, 0, 3)
        }
    }

    private fun View.enableDragging(params: WindowManager.LayoutParams) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX - (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(view, params)
                    true
                }
                else -> false
            }
        }
    }

    private fun Intent.toOverlayCardState(): OverlayCardState? {
        val decision = getStringExtra(EXTRA_DECISION)
            ?.let { runCatching { DriverDecision.valueOf(it) }.getOrNull() }
            ?: return null

        return OverlayCardState(
            decision = decision,
            fareText = getStringExtra(EXTRA_FARE_TEXT).orEmpty(),
            arsPerHourText = getStringExtra(EXTRA_ARS_PER_HOUR_TEXT).orEmpty(),
            arsPerKmText = getStringExtra(EXTRA_ARS_PER_KM_TEXT).orEmpty(),
            totalTimeText = getStringExtra(EXTRA_TOTAL_TIME_TEXT).orEmpty(),
            totalKmText = getStringExtra(EXTRA_TOTAL_KM_TEXT).orEmpty(),
            shortReason = getStringExtra(EXTRA_SHORT_REASON)
        )
    }

    private fun DriverDecision.toSpanishLabel(): String {
        return when (this) {
            DriverDecision.ACCEPT -> "ACEPTAR"
            DriverDecision.REJECT -> "RECHAZAR"
            DriverDecision.REVIEW -> "REVISAR"
        }
    }

    private fun DriverDecision.toAccentColor(): Int {
        return when (this) {
            DriverDecision.ACCEPT -> Color.rgb(40, 190, 120)
            DriverDecision.REJECT -> Color.rgb(235, 83, 83)
            DriverDecision.REVIEW -> Color.rgb(245, 178, 58)
        }
    }

    companion object {
        const val EXTRA_DECISION = "extra_decision"
        const val EXTRA_FARE_TEXT = "extra_fare_text"
        const val EXTRA_ARS_PER_HOUR_TEXT = "extra_ars_per_hour_text"
        const val EXTRA_ARS_PER_KM_TEXT = "extra_ars_per_km_text"
        const val EXTRA_TOTAL_TIME_TEXT = "extra_total_time_text"
        const val EXTRA_TOTAL_KM_TEXT = "extra_total_km_text"
        const val EXTRA_SHORT_REASON = "extra_short_reason"
    }
}

package com.greaterheight.justx

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.TextView
import kotlin.math.abs

class ExitAccessibilityService : AccessibilityService() {

    private lateinit var preferences: SharedPreferences
    private lateinit var windowManager: WindowManager
    private var floatingButton: View? = null
    private var floatingParams: WindowManager.LayoutParams? = null
    private var lastForegroundPackage: String? = null

    private val preferencesListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key?.startsWith(WATCH_PREFIX) == true) {
            lastForegroundPackage?.let { packageName ->
                if (shouldShowFor(packageName)) showFloatingButton() else hideFloatingButton()
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        preferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        preferences.registerOnSharedPreferenceChangeListener(preferencesListener)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val appPackage = event.packageName?.toString() ?: return
        lastForegroundPackage = appPackage

        if (shouldShowFor(appPackage)) {
            showFloatingButton()
        } else {
            hideFloatingButton()
        }
    }

    override fun onInterrupt() {
        hideFloatingButton()
    }

    override fun onDestroy() {
        if (::preferences.isInitialized) {
            preferences.unregisterOnSharedPreferenceChangeListener(preferencesListener)
        }
        hideFloatingButton()
        super.onDestroy()
    }

    private fun shouldShowFor(appPackage: String): Boolean {
        if (appPackage == packageName) return false
        val defaults = setOf(
            "com.google.android.youtube",
            "com.zhiliaoapp.musically",
            "com.ss.android.ugc.trill"
        )
        return preferences.getBoolean(WATCH_PREFIX + appPackage, appPackage in defaults)
    }

    private fun showFloatingButton() {
        if (floatingButton != null || !::windowManager.isInitialized) return

        val density = resources.displayMetrics.density
        fun dp(value: Int) = (value * density).toInt()

        val button = TextView(this).apply {
            text = "×"
            textSize = 32f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            contentDescription = "Exit this app and return to the home screen"
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.rgb(239, 67, 91))
                setStroke(dp(1), Color.argb(150, 255, 255, 255))
            }
            elevation = dp(8).toFloat()
            isClickable = true
        }

        val params = WindowManager.LayoutParams(
            dp(54),
            dp(54),
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            x = dp(5)
            y = 0
        }

        var downX = 0f
        var downY = 0f
        var startX = 0
        var startY = 0
        var moved = false
        val touchSlop = dp(8)

        button.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    startX = params.x
                    startY = params.y
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.rawX - downX
                    val deltaY = event.rawY - downY
                    if (abs(deltaX) > touchSlop.toFloat() || abs(deltaY) > touchSlop.toFloat()) moved = true
                    if (moved) {
                        val screenWidth = resources.displayMetrics.widthPixels
                        val screenHeight = resources.displayMetrics.heightPixels
                        params.x = (startX - deltaX.toInt()).coerceIn(0, screenWidth - dp(30))
                        val maxY = (screenHeight / 2 - dp(35)).coerceAtLeast(0)
                        params.y = (startY + deltaY.toInt()).coerceIn(-maxY, maxY)
                        try {
                            windowManager.updateViewLayout(button, params)
                        } catch (_: RuntimeException) {
                            hideFloatingButton()
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) performGlobalAction(GLOBAL_ACTION_HOME)
                    true
                }
                MotionEvent.ACTION_CANCEL -> true
                else -> false
            }
        }

        try {
            windowManager.addView(button, params)
            floatingButton = button
            floatingParams = params
        } catch (_: RuntimeException) {
            floatingButton = null
            floatingParams = null
        }
    }

    private fun hideFloatingButton() {
        val view = floatingButton ?: return
        try {
            if (::windowManager.isInitialized) windowManager.removeView(view)
        } catch (_: RuntimeException) {
            // The system may already have removed the accessibility overlay.
        } finally {
            floatingButton = null
            floatingParams = null
        }
    }
}

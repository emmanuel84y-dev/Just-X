package com.greaterheight.justx

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

private const val PREFS_NAME = "just_x_settings"
private const val WATCH_PREFIX = "watch_"
private val DEFAULT_TARGETS = setOf(
    "com.google.android.youtube",
    "com.zhiliaoapp.musically",
    "com.ss.android.ugc.trill"
)

class MainActivity : Activity() {

    private val preferences: SharedPreferences
        get() = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private lateinit var statusLabel: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(17, 18, 24)
        window.navigationBarColor = Color.rgb(17, 18, 24)
        setContentView(buildScreen())
    }

    override fun onResume() {
        super.onResume()
        if (::statusLabel.isInitialized) updateAccessibilityStatus()
    }

    private fun buildScreen(): View {
        val density = resources.displayMetrics.density
        fun dp(value: Int) = (value * density).toInt()

        val scroll = ScrollView(this).apply {
            setFillViewport(true)
            setBackgroundColor(Color.rgb(17, 18, 24))
        }
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(26), dp(22), dp(32))
        }
        scroll.addView(page)

        page.addView(TextView(this).apply {
            text = "Just X"
            textSize = 36f
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
        })

        page.addView(TextView(this).apply {
            text = "Make leaving the scroll effortless."
            textSize = 16f
            setTextColor(Color.rgb(185, 188, 202))
            setPadding(0, dp(4), 0, dp(20))
        })

        val statusCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            setBackgroundColor(Color.rgb(30, 31, 41))
        }
        statusLabel = TextView(this).apply {
            textSize = 15f
            setTextColor(Color.rgb(255, 190, 100))
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
        statusCard.addView(statusLabel)
        statusCard.addView(TextView(this).apply {
            text = "Just X needs Accessibility access to notice which app is open. It does not read page content."
            textSize = 13f
            setTextColor(Color.rgb(185, 188, 202))
            setPadding(0, dp(7), 0, 0)
        })
        page.addView(statusCard, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        val settingsButton = Button(this).apply {
            text = "Open Accessibility Settings"
            isAllCaps = false
            setTextColor(Color.WHITE)
            setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(235, 75, 98)))
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }
        val buttonParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(12); bottomMargin = dp(25) }
        page.addView(settingsButton, buttonParams)

        page.addView(TextView(this).apply {
            text = "Show the X in these apps"
            textSize = 21f
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
        })
        page.addView(TextView(this).apply {
            text = "Choose the apps where you tend to get stuck. YouTube and TikTok are selected by default when installed."
            textSize = 14f
            setTextColor(Color.rgb(185, 188, 202))
            setPadding(0, dp(6), 0, dp(12))
        })

        val appList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        val apps = getLaunchableApps().filter { it.packageName != packageName }
        if (apps.isEmpty()) {
            appList.addView(TextView(this).apply {
                text = "No launchable apps were found."
                setTextColor(Color.LTGRAY)
                textSize = 14f
            })
        } else {
            apps.forEach { app ->
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(4), dp(7), dp(4), dp(7))
                }
                row.addView(ImageView(this).apply {
                    setImageDrawable(app.icon)
                    scaleType = ImageView.ScaleType.FIT_CENTER
                }, LinearLayout.LayoutParams(dp(38), dp(38)).apply {
                    marginEnd = dp(10)
                })

                val checked = preferences.getBoolean(WATCH_PREFIX + app.packageName, app.packageName in DEFAULT_TARGETS)
                row.addView(CheckBox(this).apply {
                    text = app.label
                    textSize = 15f
                    setTextColor(Color.rgb(238, 239, 245))
                    isChecked = checked
                    setOnCheckedChangeListener { _, isChecked ->
                        preferences.edit().putBoolean(WATCH_PREFIX + app.packageName, isChecked).apply()
                    }
                }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                appList.addView(row)
                appList.addView(View(this).apply {
                    setBackgroundColor(Color.rgb(45, 46, 57))
                }, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(1)
                ))
            }
        }
        page.addView(appList)

        page.addView(TextView(this).apply {
            text = "How it works\nThe floating X sends you to your home screen. Android does not allow ordinary apps to force-stop another app, so Just X leaves it running in the background."
            textSize = 13f
            setTextColor(Color.rgb(165, 168, 184))
            setPadding(0, dp(22), 0, 0)
        })

        updateAccessibilityStatus()
        return scroll
    }

    private fun updateAccessibilityStatus() {
        val enabled = isJustXAccessibilityEnabled()
        statusLabel.text = if (enabled) {
            "●  Accessibility is enabled"
        } else {
            "○  Accessibility needs to be enabled"
        }
        statusLabel.setTextColor(
            if (enabled) Color.rgb(105, 225, 164) else Color.rgb(255, 190, 100)
        )
    }

    private fun isJustXAccessibilityEnabled(): Boolean {
        val accessibilityOn = Settings.Secure.getInt(
            contentResolver,
            Settings.Secure.ACCESSIBILITY_ENABLED,
            0
        ) == 1
        if (!accessibilityOn) return false

        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ).orEmpty()
        val expected = ComponentName(this, ExitAccessibilityService::class.java).flattenToString()
        return enabledServices.split(':').any { it.equals(expected, ignoreCase = true) }
    }

    private fun getLaunchableApps(): List<AppEntry> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        @Suppress("DEPRECATION")
        val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            packageManager.queryIntentActivities(intent, 0)
        }

        val unique = linkedMapOf<String, AppEntry>()
        resolved.forEach { info ->
            val appPackage = info.activityInfo?.packageName ?: return@forEach
            if (!unique.containsKey(appPackage)) {
                unique[appPackage] = AppEntry(
                    label = info.loadLabel(packageManager)?.toString() ?: appPackage,
                    packageName = appPackage,
                    icon = info.loadIcon(packageManager)
                )
            }
        }
        return unique.values.sortedBy { it.label.lowercase() }
    }

    private data class AppEntry(
        val label: String,
        val packageName: String,
        val icon: Drawable
    )
}

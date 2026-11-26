package com.v2ray.ang.ui.base

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import com.v2ray.ang.AppConfig
import com.v2ray.ang.handler.AppLocaleManager
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.ui.theme.ProxymaThemeWrapper

abstract class BaseComponentActivity : AppCompatActivity() {

    private var currentThemeMode: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLocaleManager.onActivityCreated(this)

        currentThemeMode = MmkvManager.decodeSettingsString(AppConfig.PREF_UI_MODE_NIGHT, "0")

        setContent {
            val isDark = when (currentThemeMode) {
                "1" -> false
                "2" -> true
                else -> isSystemInDarkTheme()
            }

            SideEffect {
                enableEdgeToEdge(
                    statusBarStyle = if (isDark) {
                        SystemBarStyle.dark(Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    },
                    navigationBarStyle = if (isDark) {
                        SystemBarStyle.dark(Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    }
                )
            }

            ProxymaThemeWrapper {
                ScreenContent()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val newThemeMode = MmkvManager.decodeSettingsString(AppConfig.PREF_UI_MODE_NIGHT, "0")
        if (currentThemeMode != newThemeMode) {
            currentThemeMode = newThemeMode
            recreate()
        }
    }

    @Composable
    protected abstract fun ScreenContent()
}

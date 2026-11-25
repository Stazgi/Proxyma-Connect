package com.v2ray.ang.ui.base

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import com.v2ray.ang.AppConfig
import com.v2ray.ang.handler.AppLocaleManager
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.ui.theme.ProxymaThemeWrapper

abstract class BaseComponentActivity : AppCompatActivity() {

    private var currentThemeMode: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLocaleManager.onActivityCreated(this)
        enableEdgeToEdge()
        
        currentThemeMode = MmkvManager.decodeSettingsString(AppConfig.PREF_UI_MODE_NIGHT, "0")

        setContent {
            ProxymaThemeWrapper {
                ScreenContent()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Проверяем, не изменилась ли тема, пока мы были на другом экране
        val newThemeMode = MmkvManager.decodeSettingsString(AppConfig.PREF_UI_MODE_NIGHT, "0")
        if (currentThemeMode != newThemeMode) {
            currentThemeMode = newThemeMode
            recreate() // Пересоздаем экран с новой темой
        }
    }

    @Composable
    protected abstract fun ScreenContent()
}

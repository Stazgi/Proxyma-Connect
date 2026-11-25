package com.v2ray.ang.ui.base

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import com.v2ray.ang.handler.AppLocaleManager
import com.v2ray.ang.ui.theme.ProxymaThemeWrapper // <--- НОВЫЙ ИМПОРТ

abstract class BaseComponentActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLocaleManager.onActivityCreated(this)
        enableEdgeToEdge()
        setContent {
            ProxymaThemeWrapper { // <--- ЗАМЕНИЛИ AppTheme НА ВАШУ ОБЕРТКУ
                ScreenContent()
            }
        }
    }

    @Composable
    protected abstract fun ScreenContent()
}

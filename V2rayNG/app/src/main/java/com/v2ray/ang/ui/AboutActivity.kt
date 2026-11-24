package com.v2ray.ang.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.webkit.WebView
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import com.v2ray.ang.BuildConfig
import com.v2ray.ang.R
import com.v2ray.ang.core.CoreNativeManager
import com.v2ray.ang.ui.base.BaseComponentActivity
import com.v2ray.ang.ui.compose.NavigationBarsSpacer
import com.v2ray.ang.util.Utils

private const val PROXYMA_REPO_URL = "https://github.com"
private const val PROXYMA_PRIVACY_URL = "https://github.com/blob/master/PRIVACY.md"

private val GrayBg = Color(0xFF121316)
private val GraySurface = Color(0xFF1C1D22)
private val GrayBorder = Color(0xFF2E3038)
private val GrayAccent = Color(0xFFA1A7B3)
private val GrayTextPrimary = Color(0xFFF3F4F6)
private val GrayTextSecondary = Color(0xFF9CA3AF)

class AboutActivity : BaseComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    @Composable
    override fun ScreenContent() {
        AboutScreen(
            onBackClick = { finish() },
            onTranslatorsClick = {
                startActivity(Intent(this, TranslatorsActivity::class.java))
            }
        )
    }
}

@Composable
fun AboutScreen(
    onBackClick: () -> Unit,
    onTranslatorsClick: () -> Unit
) {
    val context = LocalContext.current
    var showOssDialog by remember { mutableStateOf(false) }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            }
        }
    }

    val libVersion = CoreNativeManager.getLibVersion()
    val versionText = "v${BuildConfig.VERSION_NAME} ($libVersion)"
    val appIdText = BuildConfig.APPLICATION_ID

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GrayBg)
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Верхняя панель
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Text(
                        text = "←",
                        color = GrayTextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column {
                    Text(
                        text = stringResource(R.string.title_about),
                        color = GrayTextPrimary,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Лицензии и правовая информация",
                        color = GrayTextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AboutGrayCardItem(
                    iconRes = R.drawable.ic_source_code_24dp,
                    title = stringResource(R.string.title_source_code),
                    subtitle = "Открытый исходный код проекта (GPLv3)",
                    onClick = { Utils.openUri(context, PROXYMA_REPO_URL) }
                )

                AboutGrayCardItem(
                    iconRes = R.drawable.license_24px,
                    title = stringResource(R.string.title_oss_license),
                    subtitle = "Сторонние библиотеки и компоненты",
                    onClick = { showOssDialog = true }
                )

                AboutGrayCardItem(
                    iconRes = R.drawable.ic_translate_24dp,
                    title = stringResource(R.string.title_translators),
                    subtitle = "Участники локализации сообщества",
                    onClick = onTranslatorsClick
                )

                AboutGrayCardItem(
                    iconRes = R.drawable.ic_privacy_24dp,
                    title = stringResource(R.string.title_privacy_policy),
                    subtitle = "Политика конфиденциальности и обработки данных",
                    onClick = { Utils.openUri(context, PROXYMA_PRIVACY_URL) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = GraySurface.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, GrayBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Proxyma Connect",
                            color = GrayTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = versionText,
                            color = GrayTextSecondary,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = appIdText,
                            color = GrayTextSecondary.copy(alpha = 0.75f),
                            fontSize = 11.sp
                        )
                    }
                }

                NavigationBarsSpacer()
            }
        }
    }

    if (showOssDialog) {
        AlertDialog(
            onDismissRequest = { showOssDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.title_oss_license),
                    color = GrayTextPrimary
                )
            },
            text = {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            loadUrl("file:///android_asset/open_source_licenses.html")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 300.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = { showOssDialog = false }) {
                    Text(
                        text = stringResource(R.string.action_ok),
                        color = GrayAccent
                    )
                }
            },
            containerColor = GraySurface,
            modifier = Modifier.padding(bottom = 40.dp)
        )
    }
}

@Composable
private fun AboutGrayCardItem(
    @DrawableRes iconRes: Int,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = GraySurface,
        border = BorderStroke(1.dp, GrayBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(GrayBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = GrayAccent,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = GrayTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = GrayTextSecondary,
                   fontSize = 12.sp
                 )
               }
               Text(
               text = "›",
               color = GrayTextSecondary,
               fontSize = 20.sp
               )
            }
          }
        }

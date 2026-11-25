package com.v2ray.ang.ui.main

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v2ray.ang.R
import com.v2ray.ang.ui.compose.verticalScrollbar
import com.v2ray.ang.ui.theme.ProxymaTheme
import com.v2ray.ang.util.Utils

private const val TELEGRAM_BOT_URL = "https://t.me"
private const val TELEGRAM_CHANNEL_URL = "https://t.me"
private const val TELEGRAM_SUPPORT_URL = "https://t.me"

private val AccentIndigo = Color(0xFF6366F1)

enum class MainDestination(@DrawableRes val iconRes: Int, @StringRes val labelRes: Int) {
    Subscriptions(R.drawable.ic_subscriptions_24dp, R.string.title_sub_setting),
    PerAppProxy(R.drawable.ic_per_apps_24dp, R.string.per_app_proxy_settings),
    Routing(R.drawable.ic_routing_24dp, R.string.routing_settings_title),
    UserAssets(R.drawable.ic_file_24dp, R.string.title_user_asset_setting),
    Settings(R.drawable.ic_settings_24dp, R.string.title_settings),
    Promotion(R.drawable.ic_promotion_24dp, R.string.title_pref_promotion),
    Logcat(R.drawable.ic_logcat_24dp, R.string.title_logcat),
    CheckUpdate(R.drawable.ic_check_update_24dp, R.string.update_check_for_update),
    BackupRestore(R.drawable.ic_restore_24dp, R.string.title_configuration_backup_restore),
    About(R.drawable.ic_about_24dp, R.string.title_about)
}

private val proxymaDrawerItems = listOf(
    MainDestination.PerAppProxy,
    MainDestination.Routing,
    MainDestination.Settings,
    MainDestination.About
)

@Composable
fun MainDrawerContent(drawerState: DrawerState, onNavigate: (MainDestination) -> Unit) {
    val drawerScrollState = rememberScrollState()
    val context = LocalContext.current

    ModalDrawerSheet(
        drawerState = drawerState,
        modifier = Modifier.fillMaxWidth(0.78f),
        drawerContainerColor = ProxymaTheme.colors.bgTop
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(drawerScrollState)
                .verticalScrollbar(drawerScrollState)
                .padding(16.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = ProxymaTheme.colors.cardSurface,
                border = BorderStroke(1.dp, ProxymaTheme.colors.cardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(ProxymaTheme.colors.accentCyan, AccentIndigo)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "P",
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Proxyma Connect",
                        color = ProxymaTheme.colors.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Безопасный и быстрый VPN",
                        color = ProxymaTheme.colors.accentCyan,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            proxymaDrawerItems.forEach { item ->
                NavigationDrawerItem(
                    label = {
                        Text(
                            text = stringResource(item.labelRes),
                            color = ProxymaTheme.colors.textPrimary,
                            fontSize = 15.sp
                        )
                    },
                    selected = false,
                    onClick = { onNavigate(item) },
                    icon = {
                        Icon(
                            painter = painterResource(item.iconRes),
                            contentDescription = null,
                            tint = ProxymaTheme.colors.accentCyan
                        )
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color.Transparent
                    ),
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = ProxymaTheme.colors.cardBorder)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = { Utils.openUri(context, TELEGRAM_BOT_URL) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, ProxymaTheme.colors.accentCyan)
            ) {
                Text(
                    text = "✈ Наш Telegram-бот",
                    color = ProxymaTheme.colors.accentCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { Utils.openUri(context, TELEGRAM_CHANNEL_URL) },
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    border = BorderStroke(1.dp, ProxymaTheme.colors.cardBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = ProxymaTheme.colors.cardSurface
                    )
                ) {
                    Text(
                        text = "📢 Канал",
                        color = ProxymaTheme.colors.textPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                OutlinedButton(
                    onClick = { Utils.openUri(context, TELEGRAM_SUPPORT_URL) },
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    border = BorderStroke(1.dp, ProxymaTheme.colors.cardBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = ProxymaTheme.colors.cardSurface
                    )
                ) {
                    Text(
                        text = "💬 Поддержка",
                        color = ProxymaTheme.colors.textPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "Proxyma Connect • v${com.v2ray.ang.BuildConfig.VERSION_NAME}",
                color = ProxymaTheme.colors.textSecondary,
                fontSize = 11.sp,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 24.dp)
            )
        }
    }
}

package com.v2ray.ang.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LaptopMac
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.PhoneIphone
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.WifiTethering
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v2ray.ang.AppConfig
import com.v2ray.ang.core.LauncherManager
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.handler.MmkvManager.rememberMmkvBool
import com.v2ray.ang.handler.SettingsManager
import com.v2ray.ang.ui.base.BaseComponentActivity
import com.v2ray.ang.ui.compose.NavigationBarsSpacer
import com.v2ray.ang.ui.theme.ProxymaTheme
import java.net.Inet4Address
import java.net.NetworkInterface

private val AccentIndigo = Color(0xFF6366F1)
private val ConnectedGreen = Color(0xFF10B981)

class LanSharingActivity : BaseComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    @Composable
    override fun ScreenContent() {
        LanSharingScreen(
            onBackClick = { finish() }
        )
    }
}

private fun getLocalIpAddresses(): List<String> {
    val result = mutableListOf<String>()
    try {
        val interfaces = NetworkInterface.getNetworkInterfaces() ?: return emptyList()
        for (intf in interfaces) {
            if (!intf.isUp || intf.isLoopback) continue
            val addresses = intf.inetAddresses
            for (addr in addresses) {
                if (!addr.isLoopbackAddress && addr is Inet4Address) {
                    val host = addr.hostAddress ?: continue
                    if (host.startsWith("192.168.") || host.startsWith("10.") || host.startsWith("172.")) {
                        result.add(host)
                    }
                }
            }
        }
    } catch (_: Exception) {
    }
    return result.distinct()
}
@Composable
fun LanSharingScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var proxySharing by rememberMmkvBool(AppConfig.PREF_PROXY_SHARING, false)
    var localIps by remember { mutableStateOf(getLocalIpAddresses()) }
    val port = remember { SettingsManager.getHttpPort().toString() }
    val primaryIp = localIps.firstOrNull() ?: "Подключитесь к Wi-Fi"

    fun copyText(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(context, "$label скопировано: $text", Toast.LENGTH_SHORT).show()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(ProxymaTheme.colors.bgTop, ProxymaTheme.colors.bgBottom)
                )
            )
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBackClick) {
                        Text(
                            text = "←",
                            color = ProxymaTheme.colors.textPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column {
                        Text(
                            text = "Раздача интернета",
                            color = ProxymaTheme.colors.textPrimary,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Общий доступ к VPN по Wi-Fi и точке доступа",
                            color = ProxymaTheme.colors.accentCyan,
                            fontSize = 11.sp
                        )
                    }
                }

                IconButton(onClick = { localIps = getLocalIpAddresses() }) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Обновить IP",
                        tint = ProxymaTheme.colors.accentCyan
                    )
                }
            }
                        Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = ProxymaTheme.colors.cardSurface,
                    border = BorderStroke(
                        width = if (proxySharing) 1.5.dp else 1.dp,
                        color = if (proxySharing) ConnectedGreen else ProxymaTheme.colors.cardBorder
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val next = !proxySharing
                                proxySharing = next
                                if (next) {
                                    MmkvManager.encodeSettings(AppConfig.PREF_ENABLE_LOCAL_PROXY, true)
                                }
                                LauncherManager.restartService(context)
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (proxySharing) ConnectedGreen.copy(alpha = 0.16f)
                                    else ProxymaTheme.colors.bgTop
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.WifiTethering,
                                contentDescription = null,
                                tint = if (proxySharing) ConnectedGreen else ProxymaTheme.colors.accentCyan,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Раздача по локальной сети",
                                color = ProxymaTheme.colors.textPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (proxySharing) {
                                    "Активно • Другие устройства могут подключаться к этому телефону"
                                } else {
                                    "Позволяет подключить ПК, ноутбук или телевизор через ваш VPN"
                                },
                                color = if (proxySharing) ConnectedGreen else ProxymaTheme.colors.textSecondary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = proxySharing,
                            onCheckedChange = { next ->
                                proxySharing = next
                                if (next) {
                                    MmkvManager.encodeSettings(AppConfig.PREF_ENABLE_LOCAL_PROXY, true)
                                }
                                LauncherManager.restartService(context)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = ConnectedGreen,
                                uncheckedThumbColor = ProxymaTheme.colors.textSecondary,
                                uncheckedTrackColor = ProxymaTheme.colors.bgTop
                            )
                        )
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = ProxymaTheme.colors.cardSurface,
                    border = BorderStroke(1.dp, ProxymaTheme.colors.cardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Параметры подключения для других устройств",
                            color = ProxymaTheme.colors.textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        ParameterCopyRow(
                            label = "IP-адрес сервера (Прокси)",
                            value = primaryIp,
                            canCopy = localIps.isNotEmpty(),
                            onCopy = { copyText("IP-адрес", primaryIp) }
                        )

                        ParameterCopyRow(
                            label = "Порт (HTTP / SOCKS5)",
                            value = port,
                            canCopy = true,
                            onCopy = { copyText("Порт", port) }
                        )

                        if (localIps.size > 1) {
                            Text(
                                text = "Другие адреса устройства: ${localIps.drop(1).joinToString(", ")}",
                                color = ProxymaTheme.colors.textSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
                                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = AccentIndigo.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, AccentIndigo.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = null,
                            tint = ProxymaTheme.colors.accentCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Оба устройства должны быть подключены к одной сети Wi-Fi (или второе устройство подключено к точке доступа этого телефона), а VPN в Proxyma Connect должен быть включён.",
                            color = ProxymaTheme.colors.textPrimary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }

                Text(
                    text = "Инструкции по настройке устройств",
                    color = ProxymaTheme.colors.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )

                DeviceGuideCard(
                    icon = Icons.Rounded.Computer,
                    title = "Windows 10 / 11",
                    steps = listOf(
                        "1. Откройте «Параметры» → «Сеть и Интернет» → «Прокси-сервер».",
                        "2. В разделе «Настройка прокси вручную» нажмите «Настроить».",
                        "3. Включите переключатель «Использовать прокси-сервер».",
                        "4. В поле «Адрес» введите $primaryIp, в поле «Порт» — $port.",
                        "5. Нажмите «Сохранить» — весь трафик ПК пойдёт через VPN телефона."
                    )
                )

                DeviceGuideCard(
                    icon = Icons.Rounded.LaptopMac,
                    title = "macOS (MacBook / iMac)",
                    steps = listOf(
                        "1. Откройте «Системные настройки» → «Сеть» → выберите ваш Wi-Fi.",
                        "2. Нажмите «Подробнее…» и перейдите во вкладку «Прокси».",
                        "3. Включите «Веб-прокси (HTTP)» и «Защищённый веб-прокси (HTTPS)».",
                        "4. Укажите сервер $primaryIp и порт $port для обоих пунктов.",
                        "5. Нажмите «ОК» для применения настроек."
                    )
                )
                                DeviceGuideCard(
                    icon = Icons.Rounded.PhoneIphone,
                    title = "iPhone / iPad (iOS)",
                    steps = listOf(
                        "1. Откройте «Настройки» → «Wi-Fi» и нажмите значок (i) рядом с вашей сетью.",
                        "2. Пролистайте вниз до пункта «Настройка прокси» и выберите «Вручную».",
                        "3. В поле «Сервер» введите $primaryIp, в поле «Порт» — $port.",
                        "4. Оставьте «Аутентификацию» выключенной и нажмите «Сохранить»."
                    )
                )

                DeviceGuideCard(
                    icon = Icons.Rounded.PhoneAndroid,
                    title = "Android (второй телефон или планшет)",
                    steps = listOf(
                        "1. Откройте «Настройки» → «Wi-Fi» и нажмите на настройки текущей сети.",
                        "2. Нажмите «Изменить» (карандаш) → «Дополнительно» → «Прокси: Вручную».",
                        "3. В поле «Имя хоста прокси» укажите $primaryIp, в поле «Порт» — $port.",
                        "4. Нажмите «Сохранить»."
                    )
                )

                DeviceGuideCard(
                    icon = Icons.Rounded.Tv,
                    title = "Телевизоры (Android TV / Smart TV)",
                    steps = listOf(
                        "1. Откройте настройки сети Wi-Fi на телевизоре и выберите «Изменить сеть».",
                        "2. Переключите настройки IP или Прокси в режим «Вручную».",
                        "3. Введите адрес прокси $primaryIp и порт $port.",
                        "4. Сохраните — YouTube и стриминги на ТВ заработают через VPN."
                    )
                )

                NavigationBarsSpacer()
            }
        }
    }
}
@Composable
private fun ParameterCopyRow(
    label: String,
    value: String,
    canCopy: Boolean,
    onCopy: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = canCopy) { onCopy() },
        shape = RoundedCornerShape(12.dp),
        color = ProxymaTheme.colors.bgTop.copy(alpha = 0.75f),
        border = BorderStroke(1.dp, ProxymaTheme.colors.cardBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    color = ProxymaTheme.colors.textSecondary,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = value,
                    color = ProxymaTheme.colors.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            if (canCopy) {
                Icon(
                    imageVector = Icons.Rounded.ContentCopy,
                    contentDescription = "Скопировать",
                    tint = ProxymaTheme.colors.accentCyan,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun DeviceGuideCard(
    icon: ImageVector,
    title: String,
    steps: List<String>
) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "guide_arrow"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(16.dp),
        color = ProxymaTheme.colors.cardSurface,
        border = BorderStroke(1.dp, ProxymaTheme.colors.cardBorder)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = ProxymaTheme.colors.accentCyan,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = title,
                        color = ProxymaTheme.colors.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    tint = ProxymaTheme.colors.textSecondary,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(rotation)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HorizontalDivider(color = ProxymaTheme.colors.cardBorder)
                    Spacer(modifier = Modifier.height(4.dp))
                    steps.forEach { step ->
                        Text(
                            text = step,
                            color = ProxymaTheme.colors.textSecondary,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}

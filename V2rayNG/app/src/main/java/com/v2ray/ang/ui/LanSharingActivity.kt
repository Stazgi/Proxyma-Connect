package com.v2ray.ang.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LaptopMac
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.PhoneIphone
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v2ray.ang.AppConfig
import com.v2ray.ang.core.LauncherManager
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.handler.MmkvManager.rememberMmkvBool
import com.v2ray.ang.handler.MmkvManager.rememberMmkvString
import com.v2ray.ang.handler.SettingsManager
import com.v2ray.ang.ui.base.BaseComponentActivity
import com.v2ray.ang.ui.compose.NavigationBarsSpacer
import com.v2ray.ang.ui.theme.ProxymaTheme
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.UUID

private val AccentIndigo = Color(0xFF6366F1)
private val ConnectedGreen = Color(0xFF10B981)

private data class LanEndpoint(
    val label: String,
    val ip: String
)

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

private fun detectLanEndpoint(): LanEndpoint? {
    var wifiIp: String? = null
    try {
        val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
        for (intf in interfaces) {
            if (!intf.isUp || intf.isLoopback) continue
            val name = intf.name.lowercase()
            val isHotspot = name.startsWith("ap") ||
                name.startsWith("swlan") ||
                name.startsWith("rndis") ||
                name == "wlan1" ||
                name == "wlan2"
            val isWifi = name == "wlan0" || name.startsWith("eth")
            if (!isHotspot && !isWifi) continue

            for (addr in intf.inetAddresses) {
                if (!addr.isLoopbackAddress && addr is Inet4Address) {
                    val host = addr.hostAddress ?: continue
                    if (host.startsWith("192.168.") || host.startsWith("172.") || host.startsWith("10.")) {
                        if (isHotspot) {
                            return LanEndpoint("Точка доступа", host)
                        } else if (wifiIp == null) {
                            wifiIp = host
                        }
                    }
                }
            }
        }
    } catch (_: Exception) {
    }
    return wifiIp?.let { LanEndpoint("Wi-Fi", it) }
}

private fun generateRandomUser(): String = "proxyma_" + UUID.randomUUID().toString().take(8)
private fun generateRandomPass(): String = UUID.randomUUID().toString().replace("-", "").take(14)

@Composable
fun LanSharingScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
        val isVpnRunning = remember {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            @Suppress("DEPRECATION")
            cm.allNetworks.any { network ->
                cm.getNetworkCapabilities(network)?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_VPN) == true
            }
        } catch (_: Exception) {
            false
        }
     }

    var proxySharing by rememberMmkvBool(AppConfig.PREF_PROXY_SHARING, false)
    var socksPort by rememberMmkvString(AppConfig.PREF_SOCKS_PORT, AppConfig.PORT_SOCKS)
    var socksUsername by rememberMmkvString(AppConfig.PREF_SOCKS_USERNAME, "")
    var socksPassword by rememberMmkvString(AppConfig.PREF_SOCKS_PASSWORD, "")

    var authEnabled by remember { mutableStateOf(socksUsername.isNotBlank() && socksPassword.isNotBlank()) }
    var passwordVisible by remember { mutableStateOf(false) }
    var endpoint by remember { mutableStateOf(detectLanEndpoint()) }

    val effectiveSocksPort = socksPort.toIntOrNull()?.takeIf { it in 1024..65535 } ?: AppConfig.PORT_SOCKS.toInt()
    val effectiveHttpPort = remember(effectiveSocksPort) { SettingsManager.getHttpPort() }
    val ipDisplay = endpoint?.ip ?: "IP телефона"

    fun copyText(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(context, "$label скопировано", Toast.LENGTH_SHORT).show()
    }

    fun applyAndRestartIfNeeded() {
        endpoint = detectLanEndpoint()
        if (isVpnRunning) {
            LauncherManager.restartService(context)
        }
    }

    fun openHotspotSettings() {
        try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                setClassName("com.android.settings", "com.android.settings.TetherSettings")
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
            } catch (_: Exception) {
                context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
            }
        }
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Text(
                        text = "←",
                        color = ProxymaTheme.colors.textPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "Раздача интернета",
                    color = ProxymaTheme.colors.textPrimary,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
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
                                applyAndRestartIfNeeded()
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Раздавать VPN другим устройствам",
                                color = ProxymaTheme.colors.textPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Ноутбук, телевизор или другой телефон смогут выходить в интернет через VPN этого телефона.",
                                color = ProxymaTheme.colors.textSecondary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Switch(
                            checked = proxySharing,
                            onCheckedChange = { next ->
                                proxySharing = next
                                if (next) {
                                    MmkvManager.encodeSettings(AppConfig.PREF_ENABLE_LOCAL_PROXY, true)
                                }
                                applyAndRestartIfNeeded()
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

                val currentEndpoint = endpoint
                if (isVpnRunning && proxySharing && currentEndpoint != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = ProxymaTheme.colors.accentCyan.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, ProxymaTheme.colors.cardBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { copyText(currentEndpoint.label, currentEndpoint.ip) },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = currentEndpoint.label,
                                        color = ProxymaTheme.colors.textSecondary,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = currentEndpoint.ip,
                                        color = ProxymaTheme.colors.textPrimary,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Rounded.ContentCopy,
                                    contentDescription = "Скопировать IP",
                                    tint = ProxymaTheme.colors.accentCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { copyText("Порт HTTP-прокси", effectiveHttpPort.toString()) },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Порт HTTP-прокси",
                                        color = ProxymaTheme.colors.textSecondary,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = effectiveHttpPort.toString(),
                                        color = ProxymaTheme.colors.textPrimary,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Rounded.ContentCopy,
                                    contentDescription = "Скопировать HTTP порт",
                                    tint = ProxymaTheme.colors.accentCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { copyText("Порт SOCKS5", effectiveSocksPort.toString()) },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Порт SOCKS5",
                                        color = ProxymaTheme.colors.textSecondary,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = effectiveSocksPort.toString(),
                                        color = ProxymaTheme.colors.textPrimary,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Rounded.ContentCopy,
                                    contentDescription = "Скопировать SOCKS5 порт",
                                    tint = ProxymaTheme.colors.accentCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                } else {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = ProxymaTheme.colors.accentCyan.copy(alpha = 0.12f)
                        border = BorderStroke(1.dp, ProxymaTheme.colors.cardBorder)
                    ) {
                        val statusMessage = when {
                            !isVpnRunning -> "Подключите VPN — раздача начнётся автоматически."
                            !proxySharing -> "Включите тумблер выше, чтобы начать раздачу."
                            else -> "Подключитесь к Wi-Fi или включите точку доступа на телефоне."
                        }
                        Text(
                            text = statusMessage,
                            color = ProxymaTheme.colors.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
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
                            text = "Порты",
                            color = ProxymaTheme.colors.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = effectiveHttpPort.toString(),
                                onValueChange = { input ->
                                    val digits = input.filter { it.isDigit() }.take(5).toIntOrNull()
                                    if (digits != null && digits in 1025..65535) {
                                        socksPort = (digits - 1).toString()
                                    }
                                },
                                label = { Text("HTTP-прокси", fontSize = 11.sp) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = ProxymaTheme.colors.textPrimary,
                                    unfocusedTextColor = ProxymaTheme.colors.textPrimary,
                                    focusedBorderColor = ProxymaTheme.colors.accentCyan,
                                    unfocusedBorderColor = ProxymaTheme.colors.cardBorder
                                )
                            )

                            OutlinedTextField(
                                value = socksPort.ifBlank { AppConfig.PORT_SOCKS },
                                onValueChange = { input ->
                                    val digits = input.filter { it.isDigit() }.take(5)
                                    socksPort = digits
                                },
                                label = { Text("SOCKS5", fontSize = 11.sp) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = ProxymaTheme.colors.textPrimary,
                                    unfocusedTextColor = ProxymaTheme.colors.textPrimary,
                                    focusedBorderColor = ProxymaTheme.colors.accentCyan,
                                    unfocusedBorderColor = ProxymaTheme.colors.cardBorder
                                )
                            )
                        }

                        Text(
                            text = "От 1024 до 65535. Если порт занят, будет выбран следующий свободный.",
                            color = ProxymaTheme.colors.textSecondary,
                            fontSize = 11.sp
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = null,
                                tint = ProxymaTheme.colors.textSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Авторизация",
                                    color = ProxymaTheme.colors.textPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Для подключения понадобятся логин и пароль. Системные настройки прокси Android и многих телевизоров пароль не поддерживают.",
                                    color = ProxymaTheme.colors.textSecondary,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = authEnabled,
                                onCheckedChange = { enabled ->
                                    authEnabled = enabled
                                    if (enabled) {
                                        if (socksUsername.isBlank()) socksUsername = generateRandomUser()
                                        if (socksPassword.isBlank()) socksPassword = generateRandomPass()
                                    } else {
                                        socksUsername = ""
                                        socksPassword = ""
                                    }
                                    applyAndRestartIfNeeded()
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = AccentIndigo,
                                    uncheckedThumbColor = ProxymaTheme.colors.textSecondary,
                                    uncheckedTrackColor = ProxymaTheme.colors.bgTop
                                )
                            )
                        }

                        if (authEnabled) {
                            HorizontalDivider(color = ProxymaTheme.colors.cardBorder)

                            CredentialRow(
                                label = "Логин",
                                value = socksUsername,
                                isSecret = false,
                                revealed = true,
                                onToggleReveal = {},
                                onCopy = { copyText("Логин", socksUsername) }
                            )

                            CredentialRow(
                                label = "Пароль",
                                value = socksPassword,
                                isSecret = true,
                                revealed = passwordVisible,
                                onToggleReveal = { passwordVisible = !passwordVisible },
                                onCopy = { copyText("Пароль", socksPassword) }
                            )

                            TextButton(
                                onClick = {
                                    socksUsername = generateRandomUser()
                                    socksPassword = generateRandomPass()
                                    applyAndRestartIfNeeded()
                                    Toast.makeText(context, "Новый логин и пароль созданы", Toast.LENGTH_SHORT).show()
                                },
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = null,
                                    tint = ProxymaTheme.colors.accentCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Сбросить логин/пароль",
                                    color = ProxymaTheme.colors.accentCyan,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
                                Text(
                    text = "1. Подключите устройство к той же сети",
                    color = ProxymaTheme.colors.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Text(
                    text = "Подключите его к тому же Wi-Fi, что и телефон, или включите на телефоне точку доступа и подключитесь к ней. Устройства в точке доступа не получают VPN сами — на них нужно настроить прокси.",
                    color = ProxymaTheme.colors.textSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )

                OutlinedButton(
                    onClick = { openHotspotSettings() },
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, ProxymaTheme.colors.cardBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = ProxymaTheme.colors.cardSurface,
                        contentColor = ProxymaTheme.colors.textPrimary
                    )
                ) {
                    Text(
                        text = "Настройки точки доступа",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Text(
                    text = "2. Настройте прокси на устройстве",
                    color = ProxymaTheme.colors.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 6.dp)
                )

                Text(
                    text = buildString {
                        append("Укажите адрес и порт, которые показаны выше. Выберите устройство:")
                        if (authEnabled && socksUsername.isNotBlank()) {
                            append("\n\nВключена авторизация: когда спросят, укажите логин $socksUsername и пароль (его можно скопировать выше).")
                        }
                    },
                    color = ProxymaTheme.colors.textSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = ProxymaTheme.colors.cardSurface,
                    border = BorderStroke(1.dp, ProxymaTheme.colors.cardBorder)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        DeviceAccordionItem(
                            icon = Icons.Rounded.Computer,
                            title = "Windows",
                            body = "Параметры → Сеть и Интернет → Прокси-сервер → Настройка прокси вручную → Настроить. Включите «Использовать прокси-сервер», укажите адрес $ipDisplay и порт $effectiveHttpPort, нажмите «Сохранить»."
                        )
                        HorizontalDivider(color = ProxymaTheme.colors.cardBorder, modifier = Modifier.padding(horizontal = 16.dp))
                        DeviceAccordionItem(
                            icon = Icons.Rounded.LaptopMac,
                            title = "macOS",
                            body = "Системные настройки → Сеть → Wi-Fi → Подробнее → Прокси. Включите «Веб-прокси (HTTP)» и «Защищённый веб-прокси (HTTPS)», в обоих укажите $ipDisplay и порт $effectiveHttpPort, нажмите «ОК»."
                        )
                        HorizontalDivider(color = ProxymaTheme.colors.cardBorder, modifier = Modifier.padding(horizontal = 16.dp))
                        DeviceAccordionItem(
                            icon = Icons.Rounded.PhoneIphone,
                            title = "iPhone и iPad",
                            body = "Настройки → Wi-Fi → (i) рядом с сетью → Настройка прокси → Вручную. Сервер $ipDisplay, порт $effectiveHttpPort, нажмите «Сохранить»."
                        )
                                                HorizontalDivider(color = ProxymaTheme.colors.cardBorder, modifier = Modifier.padding(horizontal = 16.dp))
                        DeviceAccordionItem(
                            icon = Icons.Rounded.PhoneAndroid,
                            title = "Android",
                            body = "Настройки → Wi-Fi → нажмите на сеть (или ⚙️) → Изменить → Дополнительно → Прокси → Вручную. Имя хоста $ipDisplay, порт $effectiveHttpPort, нажмите «Сохранить»."
                        )
                        HorizontalDivider(color = ProxymaTheme.colors.cardBorder, modifier = Modifier.padding(horizontal = 16.dp))
                        DeviceAccordionItem(
                            icon = Icons.Rounded.Tv,
                            title = "Телевизоры и приставки",
                            body = "В настройках сети телевизора найдите «Прокси» (иногда он в дополнительных настройках Wi-Fi), выберите ручную настройку и укажите $ipDisplay и порт $effectiveHttpPort. Прокси поддерживают не все телевизоры."
                        )
                        HorizontalDivider(color = ProxymaTheme.colors.cardBorder, modifier = Modifier.padding(horizontal = 16.dp))
                        DeviceAccordionItem(
                            icon = Icons.AutoMirrored.Rounded.Send,
                            title = "Telegram и другие приложения",
                            body = "Telegram: Настройки → Данные и память → Прокси → Добавить прокси → SOCKS5. Сервер $ipDisplay, порт $effectiveSocksPort. Браузеры и другие программы с поддержкой SOCKS5 настраиваются так же."
                        )
                    }
                }

                Text(
                    text = "3. Проверьте",
                    color = ProxymaTheme.colors.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 6.dp)
                )

                Text(
                    text = "Откройте на устройстве сайт 2ip.ru — должен показаться адрес сервера VPN, а не ваш.",
                    color = ProxymaTheme.colors.textSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )

                Row(
                    modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null,
                        tint = ProxymaTheme.colors.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (authEnabled) {
                            "Прокси защищён логином и паролем. Выключайте раздачу, когда она не нужна."
                        } else {
                            "Прокси доступен только устройствам в вашей локальной сети, пароль не нужен. Выключайте раздачу, когда она не нужна."
                        },
                        color = ProxymaTheme.colors.textSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }

                NavigationBarsSpacer()
            }
        }
    }
}
@Composable
private fun CredentialRow(
    label: String,
    value: String,
    isSecret: Boolean,
    revealed: Boolean,
    onToggleReveal: () -> Unit,
    onCopy: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
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
                text = if (!isSecret || revealed) value else "•".repeat(value.length.coerceAtLeast(10)),
                color = ProxymaTheme.colors.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isSecret) {
                IconButton(onClick = onToggleReveal, modifier = Modifier.size(34.dp)) {
                    Icon(
                        imageVector = if (revealed) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        contentDescription = "Показать/скрыть",
                        tint = ProxymaTheme.colors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            IconButton(onClick = onCopy, modifier = Modifier.size(34.dp)) {
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
private fun DeviceAccordionItem(
    icon: ImageVector,
    title: String,
    body: String
) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "device_accordion_arrow"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
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
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    color = ProxymaTheme.colors.textPrimary,
                    fontSize = 14.sp,
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
            Text(
                text = body,
                color = ProxymaTheme.colors.textSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    }
}

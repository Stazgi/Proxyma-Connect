package com.v2ray.ang.ui.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.v2ray.ang.R
import com.v2ray.ang.dto.entities.SubscriptionItem
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.handler.SettingsManager
import com.v2ray.ang.handler.SubscriptionUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val PROXYMA_SUB_BASE_URL = "https://proxymavpn.duckdns.org:2096/a8f3b2c9d1/"

private val BgDarkTop = Color(0xFF0B0F19)
private val BgDarkBottom = Color(0xFF131B2E)
private val CardSurface = Color(0xFF1A233A)
private val CardBorder = Color(0xFF2B395C)
private val AccentCyan = Color(0xFF00E5FF)
private val AccentIndigo = Color(0xFF6366F1)
private val ConnectedGreen = Color(0xFF10B981)
private val TextPrimary = Color(0xFFF8FAFC)
private val TextSecondary = Color(0xFF94A3B8)

@Composable
fun MainScreen(
    mainViewModel: MainViewModel,
    onAction: (MainAction) -> Unit,
    onNavigate: (MainDestination) -> Unit,
) {
    val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()
    val isLoading by mainViewModel.isLoading.collectAsStateWithLifecycle()
    val groups = uiState.groups
    val activeGroup = groups.firstOrNull { it.id == uiState.selectedGroupId } ?: groups.firstOrNull()
    val serversState = if (activeGroup != null) {
        mainViewModel.serversForGroup(activeGroup.id).collectAsStateWithLifecycle()
    } else {
        remember { mutableStateOf(emptyList()) }
    }
    val servers = serversState.value

    val isRunning = uiState.isRunning
    val selectedGuid = uiState.selectedGuid
    val statusText = mainViewModel.formatStatus(uiState.status)

    LaunchedEffect(servers, selectedGuid) {
        if (servers.isNotEmpty() && (selectedGuid.isNullOrEmpty() || servers.none { it.guid == selectedGuid })) {
            onAction(MainAction.SelectServer(servers.first().guid))
        }
    }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showKeyDialog by remember { mutableStateOf(false) }

        fun activateKey(rawUuid: String) {
        val cleanKey = rawUuid.trim()
        if (cleanKey.isEmpty()) return

        scope.launch(Dispatchers.IO) {
            val trustAll = arrayOf<javax.net.ssl.TrustManager>(object : javax.net.ssl.X509TrustManager {
                override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
                override fun checkClientTrusted(certs: Array<java.security.cert.X509Certificate>, authType: String) {}
                override fun checkServerTrusted(certs: Array<java.security.cert.X509Certificate>, authType: String) {}
            })
            val sc = javax.net.ssl.SSLContext.getInstance("TLS")
            sc.init(null, trustAll, java.security.SecureRandom())

            fun fetchConfigs(url: String): String {
                val connection = java.net.URL(url).openConnection() as javax.net.ssl.HttpsURLConnection
                connection.sslSocketFactory = sc.socketFactory
                connection.hostnameVerifier = javax.net.ssl.HostnameVerifier { _, _ -> true }
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.setRequestProperty("User-Agent", "v2rayNG/1.9.0")
                val rawBody = connection.inputStream.bufferedReader().use { it.readText() }.trim()
                connection.disconnect()
                return if (rawBody.contains("://")) {
                    rawBody
                } else {
                    String(android.util.Base64.decode(rawBody, android.util.Base64.DEFAULT), Charsets.UTF_8)
                }
            }

            try {
                val decodedText = if (cleanKey.startsWith("http")) {
                    fetchConfigs(cleanKey)
                } else {
                    val subId = if (cleanKey.length > 16) cleanKey.take(16) else cleanKey
                    try {
                        fetchConfigs("$PROXYMA_SUB_BASE_URL$subId")
                    } catch (_: Exception) {
                        fetchConfigs("$PROXYMA_SUB_BASE_URL$cleanKey")
                    }
                }

                withContext(Dispatchers.Main) {
                    onAction(MainAction.RemoveAllServers)
                    onAction(MainAction.ImportBatchConfig(decodedText))
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    val fallbackUrl = if (cleanKey.startsWith("http")) cleanKey else "$PROXYMA_SUB_BASE_URL${cleanKey.take(16)}"
                    onAction(MainAction.ImportBatchConfig(fallbackUrl))
                }
            }
        }
    }
        
    
    if (showKeyDialog) {
        KeyInputDialog(
            isLoading = isLoading,
            onDismiss = { showKeyDialog = false },
            onActivate = { uuid ->
                activateKey(uuid)
                showKeyDialog = false
            }
        )
    }
        ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            MainDrawerContent(
                drawerState = drawerState,
                onNavigate = { route ->
                    scope.launch { drawerState.close() }
                    onNavigate(route)
                }
            )
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(BgDarkTop, BgDarkBottom)))
                .systemBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Brush.linearGradient(listOf(AccentCyan, AccentIndigo))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "P",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Proxyma Connect",
                                color = TextPrimary,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Защищённый туннель",
                                color = AccentCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Row {
                        IconButton(onClick = { showKeyDialog = true }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_add_24dp),
                                contentDescription = "Ключ доступа",
                                tint = TextPrimary
                            )
                        }
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_menu_24dp),
                                contentDescription = "Меню",
                                tint = TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (servers.isEmpty() && !isLoading) {
                    EmptyActivationCard(
                        onActivate = { uuid -> activateKey(uuid) }
                    )
                } else {
                        SubscriptionStatusCard(
                        groupRemarks = activeGroup?.remarks,
                        isLoading = isLoading,
                        onRefresh = { onAction(MainAction.UpdateSubscriptions) },
                        onDeleteSub = {
                            scope.launch(Dispatchers.IO) {
                                MmkvManager.decodeSubscriptions().forEach { cache ->
                                    SettingsManager.removeSubscriptionWithDefault(cache.guid)
                                }
                                withContext(Dispatchers.Main) {
                                    onAction(MainAction.RemoveAllServers)
                                    onAction(MainAction.RefreshGroups)
                                }
                            }
                        }
                    )
                                        

                    Spacer(modifier = Modifier.height(24.dp))

                    PowerConnectButton(
                        isRunning = isRunning,
                        isLoading = isLoading,
                        statusText = statusText,
                        onToggle = { onAction(MainAction.ToggleService) },
                        onTestPing = { onAction(MainAction.TestCurrentServer) }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Доступные локации",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${servers.size} шт.",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(servers, key = { it.guid }) { server ->
                            val isSelected = server.guid == selectedGuid
                            ServerLocationCard(
                                name = server.profile.remarks.ifBlank { "Сервер Proxyma" },
                                protocol = server.profile.configType.name,
                                delayMillis = server.testDelayMillis,
                                isSelected = isSelected,
                                onClick = { onAction(MainAction.SelectServer(server.guid)) }
                            )
                        }
                    }
                }
            }
        }
    }
}
@Composable
private fun SubscriptionStatusCard(
    groupRemarks: String?,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onDeleteSub: () -> Unit
) {
    val title = if (groupRemarks.isNullOrBlank() || groupRemarks.equals("Default", ignoreCase = true)) {
        "Подписка Proxyma VPN"
    } else {
        groupRemarks
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = CardSurface,
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Ключ активен • Нажмите ↻ для обновления",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = AccentCyan,
                        strokeWidth = 2.dp
                    )
                } else {
                    IconButton(onClick = onRefresh) {
                        Text(
                            text = "↻",
                            color = AccentCyan,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                IconButton(onClick = onDeleteSub) {
                    Text(
                        text = "✕",
                        color = Color(0xFFEF4444),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyActivationCard(onActivate: (String) -> Unit) {
    var inputKey by remember { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = CardSurface,
            border = BorderStroke(1.dp, CardBorder)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "🔑", fontSize = 36.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Активация доступа",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Скопируйте ключ (UUID) в Telegram-боте Proxyma VPN и вставьте его ниже",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = inputKey,
                    onValueChange = { inputKey = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Например: ce348496-200d-...", color = TextSecondary, fontSize = 13.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = CardBorder,
                        cursorColor = AccentCyan
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                TextButton(
                    onClick = { clipboard.getText()?.text?.let { inputKey = it.trim() } }
                ) {
                    Text("Вставить из буфера обмена", color = AccentCyan, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = { onActivate(inputKey) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo)
                ) {
                    Text(
                        text = "Активировать ключ",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
    
@Composable
private fun PowerConnectButton(
    isRunning: Boolean,
    isLoading: Boolean,
    statusText: String,
    onToggle: () -> Unit,
    onTestPing: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRunning) 1.08f else 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val ringColor by animateColorAsState(
        targetValue = if (isRunning) ConnectedGreen else AccentIndigo,
        animationSpec = tween(400),
        label = "ringColor"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(164.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(ringColor.copy(alpha = 0.16f))
            )
            Surface(
                modifier = Modifier
                    .size(134.dp)
                    .clip(CircleShape)
                    .clickable { onToggle() },
                shape = CircleShape,
                color = if (isRunning) ConnectedGreen else CardSurface,
                border = BorderStroke(3.dp, ringColor),
                shadowElevation = 12.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(42.dp))
                    } else {
                        Text(
                            text = if (isRunning) "ON" else "OFF",
                            color = Color.White,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 2.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = if (isRunning) "ЗАЩИТА ВКЛЮЧЕНА" else "НЕ ПОДКЛЮЧЕНО",
            color = if (isRunning) ConnectedGreen else TextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable { if (isRunning) onTestPing() }
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = if (isRunning) "⚡ $statusText" else statusText,
                color = TextSecondary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ServerLocationCard(
    name: String,
    protocol: String,
    delayMillis: Long,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) CardSurface.copy(alpha = 0.95f) else CardSurface.copy(alpha = 0.55f),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) AccentCyan else CardBorder
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (delayMillis > 0) "Пинг: ${delayMillis} мс" else "Протокол $protocol",
                    color = if (delayMillis > 0) ConnectedGreen else TextSecondary,
                    fontSize = 12.sp
                )
            }

            if (isSelected) {
                Text(
                    text = "✓",
                    color = AccentCyan,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun KeyInputDialog(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onActivate: (String) -> Unit
) {
    var keyText by remember { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardSurface,
        title = {
            Text("Сменить ключ доступа", color = TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text(
                    "Вставьте новый UUID из бота Proxyma VPN:",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = keyText,
                    onValueChange = { keyText = it },
                    singleLine = true,
                    placeholder = { Text("ce348496-200d-...", color = TextSecondary, fontSize = 12.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = CardBorder
                    )
                )
                TextButton(
                    onClick = { clipboard.getText()?.text?.let { keyText = it.trim() } }
                ) {
                    Text("Вставить из буфера", color = AccentCyan, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onActivate(keyText) },
                enabled = !isLoading && keyText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo)
            ) {
                Text("Применить", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = TextSecondary)
            }
        }
    )
}

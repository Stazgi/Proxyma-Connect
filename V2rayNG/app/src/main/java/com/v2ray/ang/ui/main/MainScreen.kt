package com.v2ray.ang.ui.main

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.v2ray.ang.handler.AngConfigManager
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.handler.SettingsManager
import com.v2ray.ang.ui.ScannerActivity
import com.v2ray.ang.util.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val PROXYMA_SUB_BASE_URL = com.v2ray.ang.BuildConfig.SUB_BASE_URL
private const val TELEGRAM_BOT_URL = "https://t.me/ProxymaVPN_robot?start=app_login"
private const val PRIVACY_POLICY_URL = "https://github.com/Stazgi/Proxyma-Connect/blob/master/PRIVACY.md"

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

    val view = androidx.compose.ui.platform.LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                androidx.core.view.WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            }
        }
    }

    LaunchedEffect(servers, selectedGuid) {
        if (servers.isNotEmpty() && (selectedGuid.isNullOrEmpty() || servers.none { it.guid == selectedGuid })) {
            onAction(MainAction.SelectServer(servers.first().guid))
        }
    }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val deviceHwid = remember {
        android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        ) ?: "android-unknown"
    }
    val deviceModel = remember { "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}".trim() }
    val osVersion = remember { android.os.Build.VERSION.RELEASE ?: "14" }

    var showKeyDialog by remember { mutableStateOf(false) }
    var showVpnDisclosure by remember { mutableStateOf(false) }
    val hasAcceptedDisclosure = remember {
        mutableStateOf(MmkvManager.decodeSettingsBool("vpn_disclosure_accepted", false))
    }

    fun activateKey(rawUuid: String) {
        val cleanKey = rawUuid.trim()
        if (cleanKey.isEmpty()) return
        MmkvManager.encodeSettings("proxyma_last_key", cleanKey)

        scope.launch(Dispatchers.IO) {
            fun fetchConfigs(url: String): String {
                val connection = java.net.URL(url).openConnection() as javax.net.ssl.HttpsURLConnection
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.setRequestProperty("User-Agent", "v2rayNG/1.9.0")
                connection.setRequestProperty("Accept-Encoding", "identity")
                connection.setRequestProperty("X-HWID", deviceHwid)
                connection.setRequestProperty("X-Device-Model", deviceModel)
                connection.setRequestProperty("X-Device-OS", "Android")
                connection.setRequestProperty("X-Ver-OS", osVersion)

                val stream = if ("gzip".equals(connection.contentEncoding, ignoreCase = true)) {
                    java.util.zip.GZIPInputStream(connection.inputStream)
                } else {
                    connection.inputStream
                }
                val rawBody = stream.bufferedReader().use { it.readText() }.trim()
                val userInfoHeader = connection.getHeaderField("Subscription-Userinfo").orEmpty()
                if (userInfoHeader.isNotEmpty()) {
                    val infoMap = userInfoHeader.split(";")
                        .mapNotNull { part ->
                            val kv = part.trim().split("=")
                            if (kv.size == 2) kv[0].lowercase() to (kv[1].toLongOrNull() ?: 0L) else null
                        }.toMap()
                    val usedBytes = (infoMap["upload"] ?: 0L) + (infoMap["download"] ?: 0L)
                    val totalBytes = infoMap["total"] ?: 0L
                    val expireSec = mapOf("expire" to (infoMap["expire"] ?: 0L))["expire"] ?: 0L

                    val gbDiv = 1024.0 * 1024.0 * 1024.0
                    val trafficText = if (totalBytes > 0L) {
                        val leftGb = ((totalBytes - usedBytes).coerceAtLeast(0L)) / gbDiv
                        val totalGb = totalBytes / gbDiv
                        String.format(java.util.Locale.US, "Остаток: %.2f из %.2f ГБ", leftGb, totalGb)
                    } else {
                        val usedGb = usedBytes / gbDiv
                        String.format(java.util.Locale.US, "Использовано: %.2f ГБ • Безлимит", usedGb)
                    }

                    val dateText = if (expireSec > 0L) {
                        val sdf = java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault())
                        " • До ${sdf.format(java.util.Date(expireSec * 1000L))}"
                    } else {
                        " • Сброс раз в 30 дн."
                    }
                    MmkvManager.encodeSettings("proxyma_sub_info", "$trafficText$dateText")
                } else {
                    MmkvManager.encodeSettings("proxyma_sub_info", "")
                }
                connection.disconnect()
                return if (rawBody.contains("://")) {
                    rawBody
                } else {
                    val cleanBase64 = rawBody.replace("\\s".toRegex(), "")
                    try {
                        String(
                            android.util.Base64.decode(
                                cleanBase64,
                                android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP
                            ),
                            Charsets.UTF_8
                        )
                    } catch (_: Exception) {
                        String(android.util.Base64.decode(cleanBase64, android.util.Base64.DEFAULT), Charsets.UTF_8)
                    }
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

                val cleanConfigs = decodedText.lines()
                    .map { it.trim() }
                    .filter {
                        it.startsWith("vless://") ||
                            it.startsWith("vmess://") ||
                            it.startsWith("trojan://") ||
                            it.startsWith("ss://")
                    }
                    .joinToString("\n")

                if (cleanConfigs.isNotEmpty()) {
                    val (count, _) = AngConfigManager.importBatchConfig(
                        server = cleanConfigs,
                        subid = uiState.selectedGroupId,
                        append = false
                    )
                    withContext(Dispatchers.Main) {
                        onAction(MainAction.RefreshGroups)
                        android.widget.Toast.makeText(
                            context,
                            "Обновлено локаций: $count",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                    } else {
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(
                            context,
                            "Ключ не найден или недействителен",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                val msg = if (e is java.io.FileNotFoundException) {
                    "Ключ не найден или срок его действия истек"
                } else {
                    "Ошибка сети: ${e.message}"
                }
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        context,
                        msg,
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    val activity = context as? MainActivity
    val incomingKey = activity?.incomingAuthKey?.value
    LaunchedEffect(incomingKey) {
        if (!incomingKey.isNullOrEmpty()) {
            activity.incomingAuthKey.value = null
            showKeyDialog = false
            activateKey(incomingKey)
        }
    }
    
  val qrScannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scannedText = result.data?.getStringExtra("SCAN_RESULT")?.trim().orEmpty()
            if (scannedText.isNotEmpty()) {
                showKeyDialog = false
                activateKey(scannedText)
            }
        }
    }

    fun launchQrScanner() {
        qrScannerLauncher.launch(Intent(context, ScannerActivity::class.java))
    }

    if (showKeyDialog) {
        KeyInputDialog(
            isLoading = isLoading,
            onDismiss = { showKeyDialog = false },
            onScanQr = { launchQrScanner() },
            onActivate = { uuid ->
                activateKey(uuid)
                showKeyDialog = false
            }
        )
    }

    if (showVpnDisclosure) {
        VpnDisclosureDialog(
            onAccept = {
                MmkvManager.encodeSettings("vpn_disclosure_accepted", true)
                hasAcceptedDisclosure.value = true
                showVpnDisclosure = false
                onAction(MainAction.ToggleService)
            },
            onDecline = {
                showVpnDisclosure = false
            },
            onOpenPrivacy = {
                Utils.openUri(context, PRIVACY_POLICY_URL)
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
                        onScanQr = { launchQrScanner() },
                        onActivate = { uuid -> activateKey(uuid) }
                    )
                } else {
                    SubscriptionStatusCard(
                        groupRemarks = activeGroup?.remarks,
                        isLoading = isLoading,
                        onRefresh = {
                            val savedKey = MmkvManager.decodeSettingsString("proxyma_last_key").orEmpty()
                            if (savedKey.isNotEmpty()) {
                                activateKey(savedKey)
                            } else {
                                showKeyDialog = true
                            }
                        },
                        onDeleteSub = {
                            MmkvManager.encodeSettings("proxyma_last_key", "")
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
                        onToggle = {
                            if (!isRunning && !hasAcceptedDisclosure.value) {
                                showVpnDisclosure = true
                            } else {
                                onAction(MainAction.ToggleService)
                            }
                        },
                        onTestPing = { onAction(MainAction.TestCurrentServer) }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Доступные локации (${servers.size})",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        TextButton(
                            onClick = { onAction(MainAction.TestRealAllServers) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Text(
                                text = "⚡ Проверить пинг",
                                color = AccentCyan,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
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
        "Конфигурация Proxyma"
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
                val subInfo = MmkvManager.decodeSettingsString("proxyma_sub_info")
                    .orEmpty()
                    .ifBlank { "Ключ активен • Нажмите ↻ для обновления" }
                Text(
                    text = subInfo,
                    color = AccentCyan,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
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
private fun EmptyActivationCard(
    onScanQr: () -> Unit,
    onActivate: (String) -> Unit
) {
    var inputKey by remember { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current
    val context = androidx.compose.ui.platform.LocalContext.current

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
                    text = "Получите ключ в Telegram-боте, вставьте из буфера или отсканируйте QR-код",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(18.dp))

                OutlinedButton(
                    onClick = { Utils.openUri(context, TELEGRAM_BOT_URL) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, AccentCyan)
                ) {
                    Text(
                        text = "✈ Войти через Telegram",
                        color = AccentCyan,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = inputKey,
                    onValueChange = { inputKey = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Введите ключ доступа (UUID)", color = TextSecondary, fontSize = 13.sp) },
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

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { clipboard.getText()?.text?.let { inputKey = it.trim() } }
                    ) {
                        Text("Из буфера", color = AccentCyan, fontSize = 13.sp)
                    }
                    TextButton(
                        onClick = onScanQr
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(R.drawable.ic_scan_24dp),
                                contentDescription = "Сканировать QR",
                                tint = AccentCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Сканировать QR", color = AccentCyan, fontSize = 13.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { onActivate(inputKey) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
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

                Spacer(modifier = Modifier.height(10.dp))

                TextButton(
                    onClick = { Utils.openUri(context, PRIVACY_POLICY_URL) }
                ) {
                    Text(
                        text = "Политика конфиденциальности",
                        color = TextSecondary,
                        fontSize = 12.sp
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
                    } else if (isRunning) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_proxyma_mono),
                            contentDescription = "Защита включена",
                            tint = Color.White,
                            modifier = Modifier.size(54.dp)
                        )
                    } else {
                        Text(
                            text = "Выкл",
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
    onScanQr: () -> Unit,
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
                    "Вставьте новый ключ (UUID) из Telegram-бота или отсканируйте QR-код:",
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
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { clipboard.getText()?.text?.let { keyText = it.trim() } },
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Text("Из буфера", color = AccentCyan, fontSize = 12.sp)
                    }
                    TextButton(
                        onClick = onScanQr,
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(R.drawable.ic_scan_24dp),
                                contentDescription = "Сканировать QR",
                                tint = AccentCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Сканировать QR", color = AccentCyan, fontSize = 12.sp)
                        }
                    }
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

@Composable
private fun VpnDisclosureDialog(
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onOpenPrivacy: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDecline,
        containerColor = CardSurface,
        title = {
            Text(
                text = "VPN-подключение и ваши данные",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column {
                Text(
                    text = "Proxyma Connect использует системный интерфейс Android VpnService для создания зашифрованного туннеля и защиты вашего интернет-трафика.",
                    color = TextPrimary,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "• Для авторизации ключа и привязки устройства на сервер передаются идентификатор устройства (Android ID), модель и версия ОС.\n• Мы не ведем журналы вашей сетевой активности и не передаем данные третьим лицам.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = onOpenPrivacy,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "Открыть Политику конфиденциальности",
                        color = AccentCyan,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onAccept,
                colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo)
            ) {
                Text("Согласен", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDecline) {
                Text("Отказаться", color = TextSecondary)
            }
        }
    )
}

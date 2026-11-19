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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val PROXYMA_SUB_BASE_URL = "https://duckdns.org"

// Фирменная палитра Proxyma Connect
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
        val fullUrl = if (cleanKey.startsWith("http")) cleanKey else "$PROXYMA_SUB_BASE_URL$cleanKey"
        scope.launch(Dispatchers.IO) {
            MmkvManager.decodeSubscriptions().forEach { (oldId, _) ->
                SettingsManager.removeSubscriptionWithDefault(oldId)
            }
            val subItem = SubscriptionItem().apply {
                remarks = "Proxyma VPN"
                url = fullUrl
                enabled = true
                autoUpdate = true
            }
            MmkvManager.encodeSubscription("", subItem)
            val newSubId = MmkvManager.decodeSubscriptions().firstOrNull()?.first.orEmpty()
            withContext(Dispatchers.Main) {
                onAction(MainAction.RefreshGroups)
                if (newSubId.isNotEmpty()) {
                    onAction(MainAction.SelectGroup(newSubId))
                    onAction(MainAction.UpdateGroupSubscription(newSubId))
                } else {
                    onAction(MainAction.UpdateSubscriptions)
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
                        onRefresh = { onAction(MainAction.UpdateSubscriptions) }
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
    override fun ScreenContent() {
        SubEditScreen(
            editSubId = editSubId,
            initial = subItem,
            profileSuggestions = suggestions,
            onBackClick = { finish() },
            onSave = { saveServer(it) },
            onDelete = { deleteServer() }
        )
    }

    private fun saveServer(subItem: SubscriptionItem): Boolean {
        if (TextUtils.isEmpty(subItem.remarks)) {
            return false
        }
        if (subItem.url.isNotEmpty()) {
            if (!Utils.isValidUrl(subItem.url)) {
                return false
            }
            if (!Utils.isValidSubUrl(subItem.url) && !subItem.allowInsecureUrl) {
                return false
            }
        }

        if (subItem.autoUpdate && subItem.updateInterval < AppConfig.SUBSCRIPTION_MIN_INTERVAL_MINUTES) {
            return false
        }

        MmkvManager.encodeSubscription(editSubId, subItem)
        SubscriptionUpdater.syncOne(subId = editSubId)
        SettingsChangeManager.makeSetupGroupTab()
        toastSuccess(R.string.toast_success)
        finish()
        return true
    }

    private fun deleteServer(): Boolean {
        if (editSubId.isNotEmpty()) {
            lifecycleScope.launch(Dispatchers.IO) {
                SettingsManager.removeSubscriptionWithDefault(editSubId)
                SettingsChangeManager.makeSetupGroupTab()
                launch(Dispatchers.Main) { finish() }
            }
        }
        return true
    }
}

@Composable
fun SubEditScreen(
    editSubId: String,
    initial: SubscriptionItem,
    profileSuggestions: List<String>,
    onBackClick: () -> Unit,
    onSave: (SubscriptionItem) -> Boolean,
    onDelete: () -> Unit
) {
    //val context = LocalContext.current
    var remarks by rememberSaveable { mutableStateOf(initial.remarks.orEmpty()) }
    var isRemarksError by rememberSaveable { mutableStateOf(false) }
    var url by rememberSaveable { mutableStateOf(initial.url.orEmpty()) }
    var isUrlError by rememberSaveable { mutableStateOf(false) }
    var userAgent by rememberSaveable { mutableStateOf(initial.userAgent.orEmpty()) }
    var requestHeaders by rememberSaveable { mutableStateOf(initial.requestHeaders.orEmpty()) }
    var filter by rememberSaveable { mutableStateOf(initial.filter ?: "") }
    var enabled by rememberSaveable { mutableStateOf(initial.enabled) }
    var autoUpdate by rememberSaveable { mutableStateOf(initial.autoUpdate) }
    var updateInterval by rememberSaveable { mutableStateOf(initial.updateInterval.toString()) }
    var isUpdateIntervalError by rememberSaveable { mutableStateOf(false) }
    var allowInsecureUrl by rememberSaveable { mutableStateOf(initial.allowInsecureUrl) }
    var prevProfile by rememberSaveable { mutableStateOf(initial.prevProfile ?: "") }
    var nextProfile by rememberSaveable { mutableStateOf(initial.nextProfile ?: "") }

    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }
    var showPrevProfileDialog by rememberSaveable { mutableStateOf(false) }
    var showNextProfileDialog by rememberSaveable { mutableStateOf(false) }
    val confirmRemove = MmkvManager.decodeSettingsBool(AppConfig.PREF_CONFIRM_REMOVE, false)
    val scrollState = rememberScrollState()

    fun buildSubItem(): SubscriptionItem {
        val subItem = MmkvManager.decodeSubscription(editSubId) ?: SubscriptionItem()
        subItem.remarks = remarks
        subItem.url = url
        subItem.userAgent = userAgent
        subItem.requestHeaders = requestHeaders
        subItem.filter = filter
        subItem.enabled = enabled
        subItem.autoUpdate = autoUpdate
        subItem.updateInterval = updateInterval.toLongEx()
        subItem.prevProfile = prevProfile
        subItem.nextProfile = nextProfile
        subItem.allowInsecureUrl = allowInsecureUrl
        return subItem
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            AppTopBar(
                title = stringResource(R.string.title_sub_setting),
                onBackClick = onBackClick,
                actions = {
                    if (editSubId.isNotEmpty()) {
                        IconButton(onClick = {
                            if (confirmRemove) showDeleteConfirm = true else onDelete()
                        }) {
                            Icon(painterResource(R.drawable.ic_delete_24dp), contentDescription = stringResource(R.string.acc_delete))
                        }
                    }
                    IconButton(onClick = {
                        val remarksErr = remarks.isBlank()
                        val urlErr = url.isNotEmpty() && (
                            !Utils.isValidUrl(url) || (!Utils.isValidSubUrl(url) && !allowInsecureUrl)
                        )
                        val intervalErr = autoUpdate && updateInterval.toLongEx() < AppConfig.SUBSCRIPTION_MIN_INTERVAL_MINUTES

                        isRemarksError = remarksErr
                        isUrlError = urlErr
                        isUpdateIntervalError = intervalErr

                        val hasError = remarksErr || urlErr || intervalErr
                        if (!hasError) {
                            onSave(buildSubItem())
                        }
                    }) {
                        Icon(painterResource(R.drawable.ic_fab_check), contentDescription = stringResource(R.string.acc_save))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
                .verticalScroll(scrollState)
                .verticalScrollbar(scrollState)
                .padding(vertical = 8.dp)
                .padding(bottom = 36.dp)
        ) {
            FormTextField(
                label = stringResource(R.string.sub_setting_remarks),
                value = remarks,
                onValueChange = { remarks = it },
                isError = isRemarksError
            )
            FormTextField(
                label = stringResource(R.string.sub_setting_url),
                value = url,
                onValueChange = { url = it },
                isError = isUrlError,
                supportingText = if (isUrlError) stringResource(R.string.toast_invalid_url) else null
            )
            FormTextField(stringResource(R.string.sub_setting_user_agent), userAgent, { userAgent = it })
            FormTextField(stringResource(R.string.sub_setting_request_headers), requestHeaders, { requestHeaders = it })
            FormTextField(stringResource(R.string.sub_setting_filter), filter, { filter = it })
            SettingsSwitchItem(
                title = stringResource(R.string.sub_setting_enable),
                checked = enabled,
                onCheckedChange = { enabled = it }
            )

            SettingsSwitchItem(
                title = stringResource(R.string.sub_auto_update),
                checked = autoUpdate,
                onCheckedChange = { autoUpdate = it }
            )

            FormTextField(
                label = stringResource(R.string.title_pref_auto_update_interval),
                value = updateInterval,
                onValueChange = { updateInterval = it },
                keyboardType = KeyboardType.Number,
                isError = isUpdateIntervalError,
                supportingText = if (isUpdateIntervalError) stringResource(R.string.toast_invalid_update_interval) else null
            )

            SettingsSwitchItem(
                title = stringResource(R.string.sub_allow_insecure_url),
                checked = allowInsecureUrl,
                onCheckedChange = { allowInsecureUrl = it }
            )
            ProfileSelectDialogField(
                label = stringResource(R.string.sub_setting_pre_profile),
                placeholder = stringResource(R.string.sub_setting_pre_profile_tip),
                value = prevProfile,
                onClick = { showPrevProfileDialog = true },
                onClear = { prevProfile = "" },
                supportingText = stringResource(R.string.sub_setting_entry_proxy_tip)
            )
            ProfileSelectDialogField(
                label = stringResource(R.string.sub_setting_next_profile),
                placeholder = stringResource(R.string.sub_setting_pre_profile_tip),
                value = nextProfile,
                onClick = { showNextProfileDialog = true },
                onClear = { nextProfile = "" },
                supportingText = stringResource(R.string.sub_setting_exit_proxy_tip)
            )
            NavigationBarsSpacer()
        }
    }

    if (showDeleteConfirm) {
        DeleteConfirmDialog(
            message = stringResource(R.string.confirm_delete_subscription_group),
            itemName = initial.remarks,
            onConfirm = onDelete,
            onDismiss = { showDeleteConfirm = false }
        )
    }
    if (showPrevProfileDialog) {
        ServerSelectListDialog(
            title = stringResource(R.string.sub_setting_pre_profile),
            options = profileSuggestions,
            selectedOption = prevProfile,
            onSelected = {
                prevProfile = it
                showPrevProfileDialog = false
            },
            onDismiss = { showPrevProfileDialog = false }
        )
    }
    if (showNextProfileDialog) {
        ServerSelectListDialog(
            title = stringResource(R.string.sub_setting_next_profile),
            options = profileSuggestions,
            selectedOption = nextProfile,
            onSelected = {
                nextProfile = it
                showNextProfileDialog = false
            },
            onDismiss = { showNextProfileDialog = false }
        )
    }
}

@Composable
private fun ProfileSelectDialogField(
    label: String,
    placeholder: String,
    value: String,
    onClick: () -> Unit,
    onClear: () -> Unit,
    supportingText: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(role = Role.Button, onClick = onClick),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(
                text = value.ifEmpty { placeholder },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onClear) {
            Icon(
                painterResource(R.drawable.ic_delete_24dp),
                contentDescription = "${stringResource(R.string.logcat_clear)}, $label"
            )
        }
    }
}

package com.v2ray.ang.ui.routing

import android.app.Activity
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.v2ray.ang.AppConfig.BUILTIN_OUTBOUND_TAGS
import com.v2ray.ang.AppConfig.TAG_PROXY
import com.v2ray.ang.R
import com.v2ray.ang.dto.entities.RulesetItem
import com.v2ray.ang.extension.nullIfBlank
import com.v2ray.ang.extension.toastSuccess
import com.v2ray.ang.handler.SettingsManager
import com.v2ray.ang.ui.apppicker.AppPickerActivity
import com.v2ray.ang.ui.base.BaseComponentActivity
import com.v2ray.ang.ui.compose.DeleteConfirmDialog
import com.v2ray.ang.ui.compose.NavigationBarsSpacer
import com.v2ray.ang.ui.compose.ServerSelectListDialog
import com.v2ray.ang.ui.compose.verticalScrollbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

private val BgDarkTop = Color(0xFF0B0F19)
private val BgDarkBottom = Color(0xFF131B2E)
private val CardSurface = Color(0xFF1A233A)
private val CardBorder = Color(0xFF2B395C)
private val AccentCyan = Color(0xFF00E5FF)
private val AccentIndigo = Color(0xFF6366F1)
private val ConnectedGreen = Color(0xFF10B981)
private val BlockRed = Color(0xFFEF4444)
private val TextPrimary = Color(0xFFF8FAFC)
private val TextSecondary = Color(0xFF94A3B8)

private val ROUTING_NETWORK_OPTIONS = listOf("tcp,udp", "tcp", "udp")

class RoutingEditActivity : BaseComponentActivity() {
    private val position by lazy { intent.getIntExtra("position", -1) }

    private var initial: RulesetItem? = null
    private lateinit var outboundSuggestions: List<String>
    private var canUseProcess: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initial = SettingsManager.getRoutingRuleset(position)
        outboundSuggestions = SettingsManager.getProfileRemarks()
        canUseProcess = SettingsManager.canUseProcessRouting()
    }

    @Composable
    override fun ScreenContent() {
        RoutingEditScreen(
            position = position,
            initial = initial,
            outboundSuggestions = outboundSuggestions,
            canUseProcess = canUseProcess,
            onBackClick = { finish() },
            onSave = { saveServer(it) },
            onDelete = { deleteServer() }
        )
    }

    private fun saveServer(rulesetItem: RulesetItem): Boolean {
        if (rulesetItem.remarks.isNullOrEmpty()) {
            return false
        }
        if (position < 0 && rulesetItem.id.isEmpty()) {
            rulesetItem.id = UUID.randomUUID().toString()
        }
        SettingsManager.saveRoutingRuleset(position, rulesetItem)
        toastSuccess(R.string.toast_success)
        finish()
        return true
    }

    private fun deleteServer(): Boolean {
        if (position >= 0) {
            lifecycleScope.launch(Dispatchers.IO) {
                SettingsManager.removeRoutingRuleset(position)
                withContext(Dispatchers.Main) { finish() }
            }
        }
        return true
    }
}
@Composable
fun RoutingEditScreen(
    position: Int,
    initial: RulesetItem?,
    outboundSuggestions: List<String>,
    canUseProcess: Boolean,
    onBackClick: () -> Unit,
    onSave: (RulesetItem) -> Boolean,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val processSelectTitle = stringResource(R.string.routing_settings_process_select)
    val scrollState = rememberScrollState()

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            }
        }
    }

    var remarks by rememberSaveable { mutableStateOf(initial?.remarks ?: "") }
    var isRemarksError by rememberSaveable { mutableStateOf(false) }
    var locked by rememberSaveable { mutableStateOf(initial?.locked == true) }
    var domain by rememberSaveable { mutableStateOf(initial?.domain?.joinToString(",") ?: "") }
    var ip by rememberSaveable { mutableStateOf(initial?.ip?.joinToString(",") ?: "") }
    var processText by rememberSaveable { mutableStateOf(initial?.process?.joinToString(",") ?: "") }
    var protocol by rememberSaveable { mutableStateOf(initial?.protocol?.joinToString(",") ?: "") }
    var network by rememberSaveable { mutableStateOf(initial?.network ?: "") }
    var port by rememberSaveable { mutableStateOf(initial?.port ?: "") }
    var outboundTag by rememberSaveable {
        mutableStateOf(initial?.outboundTag ?: BUILTIN_OUTBOUND_TAGS.first())
    }
    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }
    var showOutboundDialog by rememberSaveable { mutableStateOf(false) }
    val selectedNetwork = network.ifBlank { "tcp,udp" }

    val processPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val selectedPackages = AppPickerActivity.getSelectedPackages(result.data)
            processText = selectedPackages.joinToString(",")
        }
    }

    fun buildRuleset(): RulesetItem {
        val rulesetItem = SettingsManager.getRoutingRuleset(position) ?: RulesetItem()
        rulesetItem.apply {
            this.remarks = remarks
            this.locked = locked
            this.domain = domain.nullIfBlank()
                ?.split(",")
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?.takeIf { it.isNotEmpty() }
            this.ip = ip.nullIfBlank()
                ?.split(",")
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?.takeIf { it.isNotEmpty() }
            this.process = processText.nullIfBlank()
                ?.split(",")
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?.takeIf { it.isNotEmpty() }
            this.protocol = protocol.nullIfBlank()
                ?.split(",")
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?.takeIf { it.isNotEmpty() }
            this.port = port.nullIfBlank()
            this.network = network.nullIfBlank()
            this.outboundTag = outboundTag.trim().ifEmpty { TAG_PROXY }
        }
        return rulesetItem
    }

    fun triggerSave() {
        val remarksErr = remarks.isBlank()
        isRemarksError = remarksErr
        if (!remarksErr) {
            onSave(buildRuleset())
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BgDarkTop, BgDarkBottom)))
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Верхняя панель
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
                            color = TextPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column {
                        Text(
                            text = if (position >= 0) "Редактирование правила" else "Новое правило",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Настройка маршрутизации трафика",
                            color = AccentCyan,
                            fontSize = 11.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (position >= 0) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_delete_24dp),
                                contentDescription = "Удалить",
                                tint = BlockRed
                            )
                        }
                    }
                    IconButton(onClick = { triggerSave() }) {
                        Text(
                            text = "✓",
                            color = ConnectedGreen,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
                        Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .verticalScroll(scrollState)
                    .verticalScrollbar(scrollState)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Карточка основных параметров
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = CardSurface,
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ProxymaTextField(
                            label = "Название правила *",
                            placeholder = "Например: Российские банки",
                            value = remarks,
                            onValueChange = {
                                remarks = it
                                if (it.isNotBlank()) isRemarksError = false
                            },
                            isError = isRemarksError
                        )

                        // Выбор направления трафика (Через VPN / Без VPN / Блокировать)
                        Text(
                            text = "Куда направлять трафик:",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutboundChip(
                                title = "Через VPN",
                                selected = outboundTag.equals("proxy", ignoreCase = true),
                                color = AccentCyan,
                                modifier = Modifier.weight(1f),
                                onClick = { outboundTag = "proxy" }
                            )
                            OutboundChip(
                                title = "Без VPN",
                                selected = outboundTag.equals("direct", ignoreCase = true),
                                color = ConnectedGreen,
                                modifier = Modifier.weight(1f),
                                onClick = { outboundTag = "direct" }
                            )
                            OutboundChip(
                                title = "Блокировать",
                                selected = outboundTag.equals("block", ignoreCase = true),
                                color = BlockRed,
                                modifier = Modifier.weight(1f),
                                onClick = { outboundTag = "block" }
                            )
                        }

                        if (outboundSuggestions.isNotEmpty()) {
                            OutlinedButton(
                                onClick = { showOutboundDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, CardBorder)
                            ) {
                                Text(
                                    text = "Тег выхода: $outboundTag (выбрать сервер)",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { locked = !locked }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Закрепить правило",
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Сохранится при сбросе или импорте набора правил",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                            Switch(
                                checked = locked,
                                onCheckedChange = { locked = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = AccentIndigo,
                                    uncheckedThumbColor = TextSecondary,
                                    uncheckedTrackColor = BgDarkTop
                                )
                            )
                        }
                    }
                }
                                // Карточка условий срабатывания
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = CardSurface,
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Условия срабатывания (через запятую):",
                            color = AccentCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        ProxymaTextField(
                            label = "Домены (domain)",
                            placeholder = "geosite:ru, sberbank.ru",
                            value = domain,
                            onValueChange = { domain = it }
                        )

                        ProxymaTextField(
                            label = "IP-адреса (ip)",
                            placeholder = "geoip:ru, 192.168.0.0/16",
                            value = ip,
                            onValueChange = { ip = it }
                        )

                        ProxymaTextField(
                            label = "Порты (port)",
                            placeholder = "443 или 80,443",
                            value = port,
                            onValueChange = { port = it }
                        )

                        ProxymaTextField(
                            label = "Протокол (protocol)",
                            placeholder = "http, tls, bittorrent",
                            value = protocol,
                            onValueChange = { protocol = it }
                        )

                        Text(
                            text = "Тип сети:",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ROUTING_NETWORK_OPTIONS.forEach { netOption ->
                                OutboundChip(
                                    title = netOption.uppercase(),
                                    selected = selectedNetwork.equals(netOption, ignoreCase = true),
                                    color = AccentIndigo,
                                    modifier = Modifier.weight(1f),
                                    onClick = { network = netOption }
                                )
                            }
                        }

                        if (canUseProcess) {
                            ProxymaTextField(
                                label = "Приложения (process)",
                                placeholder = "ru.sberbankmobile",
                                value = processText,
                                onValueChange = { processText = it }
                            )
                            TextButton(
                                onClick = {
                                    val current = processText
                                        .split(",")
                                        .map { it.trim() }
                                        .filter { it.isNotEmpty() }
                                        .distinct()
                                    processPickerLauncher.launch(
                                        AppPickerActivity.createIntent(
                                            context = context,
                                            selectedPackages = current,
                                            title = processSelectTitle
                                        )
                                    )
                                }
                            ) {
                                Text("📱 Выбрать из установленных приложений", color = AccentCyan, fontSize = 13.sp)
                            }
                        }
                    }
                }

                Button(
                    onClick = { triggerSave() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo)
                ) {
                    Text(
                        text = "Сохранить правило",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                NavigationBarsSpacer()
            }
        }

        if (showDeleteConfirm) {
            DeleteConfirmDialog(
                message = stringResource(R.string.confirm_delete_routing_rule),
                itemName = initial?.remarks.orEmpty(),
                onConfirm = onDelete,
                onDismiss = { showDeleteConfirm = false }
            )
        }
        if (showOutboundDialog) {
            ServerSelectListDialog(
                title = stringResource(R.string.routing_settings_outbound_tag),
                options = outboundSuggestions,
                selectedOption = outboundTag,
                onSelected = {
                    outboundTag = it
                    showOutboundDialog = false
                },
                onDismiss = { showOutboundDialog = false }
            )
        }
    }
}

@Composable
private fun OutboundChip(
    title: String,
    selected: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (selected) color.copy(alpha = 0.2f) else BgDarkTop.copy(alpha = 0.6f),
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) color else CardBorder
        )
    ) {
        Box(
            modifier = Modifier.padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                color = if (selected) color else TextSecondary,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ProxymaTextField(
    label: String,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label, fontSize = 12.sp) },
        placeholder = { Text(placeholder, color = TextSecondary.copy(alpha = 0.6f), fontSize = 12.sp) },
        isError = isError,
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            focusedBorderColor = AccentCyan,
            unfocusedBorderColor = CardBorder,
            focusedLabelColor = AccentCyan,
            unfocusedLabelColor = TextSecondary,
            cursorColor = AccentCyan
        )
    )
}

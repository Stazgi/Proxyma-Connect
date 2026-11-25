package com.v2ray.ang.ui.routing

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.HomeWork
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.dto.entities.RulesetItem
import com.v2ray.ang.enums.RoutingType
import com.v2ray.ang.extension.toastError
import com.v2ray.ang.extension.toastSuccess
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.handler.SettingsManager
import com.v2ray.ang.ui.base.HelperBaseComponentActivity
import com.v2ray.ang.ui.compose.AppDropdownMenuItems
import com.v2ray.ang.ui.compose.NavigationBarsBottomPadding
import com.v2ray.ang.ui.compose.SelectListDialog
import com.v2ray.ang.ui.compose.verticalScrollbar
import com.v2ray.ang.ui.theme.ProxymaTheme
import com.v2ray.ang.util.JsonUtil
import com.v2ray.ang.util.LogUtil
import com.v2ray.ang.util.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.util.UUID

private val AccentIndigo = Color(0xFF6366F1)
private val ConnectedGreen = Color(0xFF10B981)
private val BlockRed = Color(0xFFEF4444)

private const val QUICK_TAG_LAN = "proxyma_quick_lan"
private const val QUICK_TAG_RU = "proxyma_quick_ru"
private const val QUICK_TAG_ADS = "proxyma_quick_ads"

private enum class RoutingMenuAction(@StringRes val labelRes: Int) {
    ImportPredefined(R.string.routing_settings_import_predefined_rulesets),
    ImportClipboard(R.string.routing_settings_import_rulesets_from_clipboard),
    ImportQRCode(R.string.routing_settings_import_rulesets_from_qrcode),
    ExportClipboard(R.string.routing_settings_export_rulesets_to_clipboard)
}

private enum class RoutingPreset(val type: RoutingType, @StringRes val labelRes: Int) {
    RussiaWhitelist(RoutingType.WHITE_RUSSIA, R.string.routing_preset_russia_whitelist),
    Global(RoutingType.GLOBAL, R.string.routing_preset_global),
    ChinaWhitelist(RoutingType.WHITE, R.string.routing_preset_china_whitelist),
    ChinaBlacklist(RoutingType.BLACK, R.string.routing_preset_china_blacklist),
    IranWhitelist(RoutingType.WHITE_IRAN, R.string.routing_preset_iran_whitelist)
}

class RoutingSettingActivity : HelperBaseComponentActivity() {
    private val viewModel: RoutingSettingsViewModel by viewModels()
    private val domainStrategyState = MutableStateFlow("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        domainStrategyState.value = getDomainStrategy()
    }

    @Composable
    override fun ScreenContent() {
        RoutingSettingScreen(
            viewModel = viewModel,
            domainStrategyState = domainStrategyState,
            onBackClick = { finish() },
            onAddRule = { startActivity(Intent(this, RoutingEditActivity::class.java)) },
            onEditRule = { position ->
                startActivity(Intent(this, RoutingEditActivity::class.java).putExtra("position", position))
            },
            onDomainStrategySelected = { value ->
                MmkvManager.encodeSettings(AppConfig.PREF_ROUTING_DOMAIN_STRATEGY, value)
                domainStrategyState.value = value
            },
            onImportPredefined = { type -> importPredefined(type) },
            onImportClipboard = { importFromClipboard() },
            onImportQRcode = { importQRcode() },
            onExportClipboard = { export2Clipboard() }
        )
    }

    override fun onResume() {
        super.onResume()
        viewModel.reload()
    }

    private fun getDomainStrategy(): String {
        val strategies = resources.getStringArray(R.array.routing_domain_strategy)
        return MmkvManager.decodeSettingsString(AppConfig.PREF_ROUTING_DOMAIN_STRATEGY) ?: strategies.first()
    }

    private fun importPredefined(type: RoutingType) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                SettingsManager.resetRoutingRulesetsFromPresets(this@RoutingSettingActivity, type)
                launch(Dispatchers.Main) {
                    viewModel.reload()
                    toastSuccess(R.string.toast_success)
                }
            } catch (e: Exception) {
                LogUtil.e(AppConfig.TAG, "Failed to import predefined ruleset", e)
            }
        }
    }

    private fun importFromClipboard() {
        val clipboard = try {
            Utils.getClipboard(this)
        } catch (e: Exception) {
            toastError(R.string.toast_failure)
            return
        }
        lifecycleScope.launch(Dispatchers.IO) {
            val result = SettingsManager.resetRoutingRulesets(clipboard)
            withContext(Dispatchers.Main) {
                if (result) {
                    viewModel.reload()
                    toastSuccess(R.string.toast_success)
                } else {
                    toastError(R.string.toast_failure)
                }
            }
        }
    }

    private fun importQRcode() {
        launchQRCodeScanner { scanResult ->
            if (scanResult != null) {
                lifecycleScope.launch(Dispatchers.IO) {
                    val result = SettingsManager.resetRoutingRulesets(scanResult)
                    withContext(Dispatchers.Main) {
                        if (result) {
                            viewModel.reload()
                            toastSuccess(R.string.toast_success)
                        } else {
                            toastError(R.string.toast_failure)
                        }
                    }
                }
            }
        }
    }

    private fun export2Clipboard() {
        val rulesetList = MmkvManager.decodeRoutingRulesets()
        if (rulesetList.isNullOrEmpty()) {
            toastError(R.string.toast_failure)
        } else {
            Utils.setClipboard(this, JsonUtil.toJson(rulesetList))
            toastSuccess(R.string.toast_success)
        }
    }
}
private fun formatStrategyTitle(strategy: String): String {
    return when (strategy) {
        "AsIs" -> "По адресу сайта (Быстро)"
        "IPIfNonMatch" -> "Умная проверка (Домен + IP)"
        "IPOnDemand" -> "Глубокая проверка (Всегда IP)"
        else -> strategy
    }
}

private fun formatStrategyDesc(strategy: String): String {
    return when (strategy) {
        "AsIs" -> "Проверять только доменное имя без DNS-запросов — максимальная скорость открытия страниц"
        "IPIfNonMatch" -> "Если домен не совпал с правилами — узнать его IP-адрес и проверить по базе стран"
        "IPOnDemand" -> "Сразу определять IP-адрес каждого сайта для максимально точной фильтрации"
        else -> "Режим проверки адресов при распределении трафика"
    }
}

private fun formatRuleTitle(raw: String?): String {
    val name = raw?.trim().orEmpty()
    if (name.isEmpty()) return "Правило маршрутизации"
    return when {
        name == QUICK_TAG_LAN || name.contains("绕过局域网") -> "Локальная сеть напрямую"
        name == QUICK_TAG_RU -> "Сайты .ru, .рф и .su напрямую"
        name == QUICK_TAG_ADS || name.contains("屏蔽广告") || name.contains("广告") -> "Блокировка рекламы и трекеров"
        name.contains("udp443", ignoreCase = true) -> "Ускорение YouTube (Блокировка QUIC)"
        name.contains("代理Google") -> "Сервисы Google через VPN"
        name.contains("绕过中国公共DNS") -> "Публичные DNS-серверы напрямую"
        name.contains("绕过中国IP") || name.contains("绕过中国域名") || name.contains("绕过大陆") -> "Региональный трафик (CN)"
        else -> name
    }
}

private fun formatRuleSummary(ruleset: RulesetItem): String {
    val name = ruleset.remarks?.trim().orEmpty()
    return when {
        name == QUICK_TAG_LAN -> "Роутер, принтеры, телевизоры и устройства в домашней сети"
        name == QUICK_TAG_RU -> "Российские сайты, банки и госсервисы работают без VPN"
        name == QUICK_TAG_ADS -> "Вырезает рекламные баннеры и счётчики слежки"
        name.contains("udp443", ignoreCase = true) -> "Заставляет видеохостинги работать по стабильному TCP"
        name.contains("绕过局域网") -> "Домашние и локальные IP-адреса"
        name.contains("绕过中国") || name.contains("绕过大陆") -> "Системное правило региональных адресов"
        else -> {
            val parts = buildList {
                ruleset.domain?.takeIf { it.isNotEmpty() }?.let { add(it.joinToString(", ")) }
                ruleset.ip?.takeIf { it.isNotEmpty() }?.let { add(it.joinToString(", ")) }
                ruleset.process?.takeIf { it.isNotEmpty() }?.let { add("Приложения: ${it.joinToString(", ")}") }
                ruleset.port?.takeIf { it.isNotBlank() }?.let { add("Порты: $it") }
            }
            parts.joinToString(" • ").ifEmpty { "Любой трафик" }
        }
    }
}

private fun isLanRule(item: RulesetItem): Boolean {
    return item.remarks == QUICK_TAG_LAN ||
        item.domain?.any { it.contains("geosite:private") } == true ||
        item.ip?.any { it.contains("geoip:private") } == true
}

private fun isRuRule(item: RulesetItem): Boolean {
    return item.remarks == QUICK_TAG_RU ||
        item.domain?.any { it.contains("category-ru") || it == "domain:.ru" } == true
}

private fun isAdsRule(item: RulesetItem): Boolean {
    return item.remarks == QUICK_TAG_ADS ||
        item.domain?.any { it.contains("category-ads") } == true
}

private fun isCustomUserRule(item: RulesetItem): Boolean {
    val r = item.remarks.orEmpty()
    if (r == QUICK_TAG_LAN || r == QUICK_TAG_RU || r == QUICK_TAG_ADS) return false
    if (r.contains("udp443", ignoreCase = true) || r.contains("绕过") || r.contains("屏蔽") || r.contains("代理Google")) return false
    return true
}
@Composable
fun RoutingSettingScreen(
    viewModel: RoutingSettingsViewModel,
    domainStrategyState: MutableStateFlow<String>,
    onBackClick: () -> Unit,
    onAddRule: () -> Unit,
    onEditRule: (Int) -> Unit,
    onDomainStrategySelected: (String) -> Unit,
    onImportPredefined: (RoutingType) -> Unit,
    onImportClipboard: () -> Unit,
    onImportQRcode: () -> Unit,
    onExportClipboard: () -> Unit
) {
    val rulesets by viewModel.rulesetsFlow.collectAsStateWithLifecycle()
    val domainStrategy by domainStrategyState.collectAsState()
    var showMenu by remember { mutableStateOf(false) }
    var showPresetDialog by remember { mutableStateOf(false) }
    var showStrategyDialog by remember { mutableStateOf(false) }
    var quickAddOutbound by remember { mutableStateOf<String?>(null) }

    val domainStrategies = stringArrayResource(R.array.routing_domain_strategy).toList()
    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        val fromIndex = rulesets.indexOfFirst { it.id == from.key }
        val toIndex = rulesets.indexOfFirst { it.id == to.key }
        viewModel.move(fromIndex, toIndex)
    }

    val lanEnabled = rulesets.any { isLanRule(it) && it.enabled == true }
    val ruEnabled = rulesets.any { isRuRule(it) && it.enabled == true }
    val adsEnabled = rulesets.any { isAdsRule(it) && it.enabled == true }

    fun toggleQuickRule(
        matcher: (RulesetItem) -> Boolean,
        enable: Boolean,
        builder: () -> RulesetItem
    ) {
        val current = MmkvManager.decodeRoutingRulesets()?.toMutableList() ?: mutableListOf()
        val matchedIndices = current.mapIndexedNotNull { idx, item -> if (matcher(item)) idx else null }
        if (matchedIndices.isNotEmpty()) {
            matchedIndices.forEach { idx ->
                current[idx] = current[idx].copy(enabled = enable)
            }
        } else if (enable) {
            current.add(0, builder())
        }
        MmkvManager.encodeRoutingRulesets(current)
        viewModel.reload()
    }

    fun addQuickDomainRule(rawInput: String, tag: String) {
        val items = rawInput.split(",", "\n", " ")
            .map { it.trim().removePrefix("https://").removePrefix("http://").trimEnd('/') }
            .filter { it.isNotEmpty() }
        if (items.isEmpty()) return

        val domains = mutableListOf<String>()
        val ips = mutableListOf<String>()
        val ipRegex = Regex("""^\d{1,3}(\.\d{1,3}){3}(/\d{1,2})?$""")
        items.forEach { entry ->
            if (ipRegex.matches(entry) || entry.contains(":")) {
                ips.add(entry)
            } else {
                domains.add(entry)
            }
        }

        val current = MmkvManager.decodeRoutingRulesets()?.toMutableList() ?: mutableListOf()
        val titlePrefix = when (tag) {
            "direct" -> "Напрямую"
            "block" -> "Блокировать"
            else -> "Через VPN"
        }
        val newRule = RulesetItem(
            id = UUID.randomUUID().toString(),
            remarks = "$titlePrefix: ${items.take(2).joinToString(", ")}",
            domain = domains.takeIf { it.isNotEmpty() },
            ip = ips.takeIf { it.isNotEmpty() },
            outboundTag = tag,
            enabled = true,
            locked = true
        )
        current.add(0, newRule)
        MmkvManager.encodeRoutingRulesets(current)
        viewModel.reload()
    }

    fun deleteRuleById(id: String) {
        val current = MmkvManager.decodeRoutingRulesets()?.toMutableList() ?: mutableListOf()
        val idx = current.indexOfFirst { it.id == id }
        if (idx >= 0) {
            current.removeAt(idx)
            MmkvManager.encodeRoutingRulesets(current)
            viewModel.reload()
        }
    }

    val directCustomRules = rulesets.filter { isCustomUserRule(it) && it.outboundTag.equals("direct", true) }
    val proxyCustomRules = rulesets.filter { isCustomUserRule(it) && it.outboundTag.equals("proxy", true) }
    val blockCustomRules = rulesets.filter { isCustomUserRule(it) && it.outboundTag.equals("block", true) }
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
                            text = "Маршрутизация",
                            color = ProxymaTheme.colors.textPrimary,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Какие сайты идут через VPN, а какие напрямую",
                            color = ProxymaTheme.colors.accentCyan,
                            fontSize = 11.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onAddRule) {
                        Icon(
                            painter = painterResource(R.drawable.ic_add_24dp),
                            contentDescription = "Создать правило вручную",
                            tint = ProxymaTheme.colors.accentCyan
                        )
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_more_vert_24dp),
                                contentDescription = stringResource(R.string.acc_more),
                                tint = ProxymaTheme.colors.textPrimary
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            containerColor = ProxymaTheme.colors.cardSurface
                        ) {
                            AppDropdownMenuItems(RoutingMenuAction.entries, { it.labelRes }) { action ->
                                showMenu = false
                                when (action) {
                                    RoutingMenuAction.ImportPredefined -> showPresetDialog = true
                                    RoutingMenuAction.ImportClipboard -> onImportClipboard()
                                    RoutingMenuAction.ImportQRCode -> onImportQRcode()
                                    RoutingMenuAction.ExportClipboard -> onExportClipboard()
                                }
                            }
                        }
                    }
                }
            }

            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .verticalScrollbar(lazyListState),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = NavigationBarsBottomPadding()
            ) {
                item(key = "quick_header") {
                    Text(
                        text = "Быстрые настройки",
                        color = ProxymaTheme.colors.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                    )
                }

                item(key = "quick_switches") {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = ProxymaTheme.colors.cardSurface,
                        border = BorderStroke(1.dp, ProxymaTheme.colors.cardBorder)
                    ) {
                        Column {
                            QuickToggleRow(
                                icon = Icons.Rounded.HomeWork,
                                title = "Локальная сеть напрямую",
                                subtitle = "Роутер, принтеры, телевизоры и другие устройства дома",
                                checked = lanEnabled,
                                onCheckedChange = { enabled ->
                                    toggleQuickRule(::isLanRule, enabled) {
                                        RulesetItem(
                                            id = UUID.randomUUID().toString(),
                                            remarks = QUICK_TAG_LAN,
                                            domain = listOf("geosite:private"),
                                            ip = listOf("geoip:private"),
                                            outboundTag = "direct",
                                            enabled = true,
                                            locked = true
                                        )
                                    }
                                }
                            )
                            HorizontalDivider(
                                color = ProxymaTheme.colors.cardBorder,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            QuickToggleRow(
                                icon = Icons.Rounded.Public,
                                title = "Сайты .ru, .рф и .su напрямую",
                                subtitle = "Эти сайты будут видеть ваш настоящий адрес (банки, Госуслуги)",
                                checked = ruEnabled,
                                onCheckedChange = { enabled ->
                                    toggleQuickRule(::isRuRule, enabled) {
                                        RulesetItem(
                                            id = UUID.randomUUID().toString(),
                                            remarks = QUICK_TAG_RU,
                                            domain = listOf(
                                                "geosite:category-ru",
                                                "domain:.ru",
                                                "domain:.рф",
                                                "domain:.su"
                                            ),
                                            ip = listOf("geoip:ru"),
                                            outboundTag = "direct",
                                            enabled = true,
                                            locked = true
                                        )
                                    }
                                }
                            )
                            HorizontalDivider(
                                color = ProxymaTheme.colors.cardBorder,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            QuickToggleRow(
                                icon = Icons.Rounded.Security,
                                title = "Блокировка рекламы",
                                subtitle = "Вырезает рекламные баннеры и трекеры в приложениях",
                                checked = adsEnabled,
                                onCheckedChange = { enabled ->
                                    toggleQuickRule(::isAdsRule, enabled) {
                                        RulesetItem(
                                            id = UUID.randomUUID().toString(),
                                            remarks = QUICK_TAG_ADS,
                                            domain = listOf("geosite:category-ads-all"),
                                            outboundTag = "block",
                                            enabled = true,
                                            locked = true
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
                                item(key = "custom_direct") {
                    CustomCategoryCard(
                        icon = Icons.Rounded.Language,
                        title = "Напрямую",
                        subtitle = "Без VPN",
                        accentColor = ConnectedGreen,
                        rules = directCustomRules,
                        onAddClick = { quickAddOutbound = "direct" },
                        onDeleteRule = { deleteRuleById(it) },
                        onEditRule = { rule ->
                            val idx = rulesets.indexOfFirst { it.id == rule.id }
                            if (idx >= 0) onEditRule(idx)
                        }
                    )
                }

                item(key = "custom_proxy") {
                    CustomCategoryCard(
                        icon = Icons.Rounded.Shield,
                        title = "Через VPN",
                        subtitle = "Даже если другие правила отправляют их напрямую",
                        accentColor = ProxymaTheme.colors.accentCyan,
                        rules = proxyCustomRules,
                        onAddClick = { quickAddOutbound = "proxy" },
                        onDeleteRule = { deleteRuleById(it) },
                        onEditRule = { rule ->
                            val idx = rulesets.indexOfFirst { it.id == rule.id }
                            if (idx >= 0) onEditRule(idx)
                        }
                    )
                }

                item(key = "custom_block") {
                    CustomCategoryCard(
                        icon = Icons.Rounded.Block,
                        title = "Блокировать",
                        subtitle = "Эти сайты и адреса не будут открываться",
                        accentColor = BlockRed,
                        rules = blockCustomRules,
                        onAddClick = { quickAddOutbound = "block" },
                        onDeleteRule = { deleteRuleById(it) },
                        onEditRule = { rule ->
                            val idx = rulesets.indexOfFirst { it.id == rule.id }
                            if (idx >= 0) onEditRule(idx)
                        }
                    )
                }
                                item(key = "strategy_header") {
                    Text(
                        text = "Дополнительные параметры маршрутизации",
                        color = ProxymaTheme.colors.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                    )
                }

                item(key = "domain_strategy") {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { showStrategyDialog = true },
                        shape = RoundedCornerShape(16.dp),
                        color = ProxymaTheme.colors.cardSurface,
                        border = BorderStroke(1.dp, ProxymaTheme.colors.cardBorder)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Режим проверки сайтов",
                                    color = ProxymaTheme.colors.textSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Изменить ›",
                                    color = ProxymaTheme.colors.accentCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = formatStrategyTitle(domainStrategy),
                                color = ProxymaTheme.colors.textPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = formatStrategyDesc(domainStrategy),
                                color = ProxymaTheme.colors.textSecondary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                item(key = "rules_header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp, bottom = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Все правила ядра (${rulesets.count { it.enabled == true }} из ${rulesets.size})",
                            color = ProxymaTheme.colors.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Порядок важен • ✎ правка",
                            color = ProxymaTheme.colors.textSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                itemsIndexed(
                    items = rulesets,
                    key = { _, ruleset -> ruleset.id }
                ) { index, ruleset ->
                    ReorderableItem(reorderableState, key = ruleset.id) { isDragging ->
                        RoutingRulesetCard(
                            ruleset = ruleset,
                            isDragging = isDragging,
                            onEdit = { onEditRule(index) },
                            onEnabledChange = { checked ->
                                val updated = ruleset.copy(enabled = checked)
                                viewModel.update(index, updated)
                            }
                        )
                    }
                }
            }
        }
    }

    if (quickAddOutbound != null) {
        QuickAddSiteDialog(
            outboundTag = quickAddOutbound!!,
            onDismiss = { quickAddOutbound = null },
            onConfirm = { input ->
                addQuickDomainRule(input, quickAddOutbound!!)
                quickAddOutbound = null
            }
        )
    }

    if (showStrategyDialog) {
        SelectListDialog(
            title = "Режим проверки сайтов",
            options = domainStrategies,
            optionText = { "${formatStrategyTitle(it)} — ${formatStrategyDesc(it)}" },
            onSelected = { selected ->
                showStrategyDialog = false
                onDomainStrategySelected(selected)
            },
            onDismiss = { showStrategyDialog = false }
        )
    }

    if (showPresetDialog) {
        SelectListDialog(
            title = "Загрузить готовый набор правил",
            options = RoutingPreset.entries,
            optionText = { stringResource(it.labelRes) },
            onSelected = { preset ->
                showPresetDialog = false
                onImportPredefined(preset.type)
            },
            onDismiss = { showPresetDialog = false }
        )
    }
}
@Composable
private fun QuickToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = ProxymaTheme.colors.accentCyan,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = ProxymaTheme.colors.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = ProxymaTheme.colors.textSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = AccentIndigo,
                uncheckedThumbColor = ProxymaTheme.colors.textSecondary,
                uncheckedTrackColor = ProxymaTheme.colors.bgTop
            )
        )
    }
}

@Composable
private fun CustomCategoryCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    rules: List<RulesetItem>,
    onAddClick: () -> Unit,
    onDeleteRule: (String) -> Unit,
    onEditRule: (RulesetItem) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = ProxymaTheme.colors.cardSurface,
        border = BorderStroke(1.dp, ProxymaTheme.colors.cardBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "$title · ${rules.size}",
                            color = ProxymaTheme.colors.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = subtitle,
                            color = ProxymaTheme.colors.textSecondary,
                            fontSize = 12.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
                TextButton(
                    onClick = onAddClick,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "+ Добавить",
                        color = ProxymaTheme.colors.accentCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (rules.isEmpty()) {
                Text(
                    text = "Пока пусто",
                    color = ProxymaTheme.colors.textSecondary.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 34.dp)
                )
            } else {
                Column(
                    modifier = Modifier.padding(start = 34.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    rules.forEach { rule ->
                        val itemsText = (rule.domain.orEmpty() + rule.ip.orEmpty())
                            .joinToString(", ")
                            .ifEmpty { rule.remarks.orEmpty() }
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onEditRule(rule) },
                            shape = RoundedCornerShape(10.dp),
                            color = ProxymaTheme.colors.bgTop.copy(alpha = 0.7f),
                            border = BorderStroke(1.dp, ProxymaTheme.colors.cardBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = itemsText,
                                    color = ProxymaTheme.colors.textPrimary,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { onDeleteRule(rule.id) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Text(
                                        text = "✕",
                                        color = ProxymaTheme.colors.textSecondary,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickAddSiteDialog(
    outboundTag: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    val title = when (outboundTag) {
        "direct" -> "Добавить сайт напрямую (Без VPN)"
        "block" -> "Заблокировать сайт или IP"
        else -> "Добавить сайт через VPN"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ProxymaTheme.colors.cardSurface,
        title = {
            Text(
                text = title,
                color = ProxymaTheme.colors.textPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Введите адрес сайта (например, ozon.ru) или IP-адрес. Можно указать несколько через запятую:",
                    color = ProxymaTheme.colors.textSecondary,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    singleLine = true,
                    placeholder = {
                        Text(
                            "example.com, 192.168.1.1",
                            color = ProxymaTheme.colors.textSecondary.copy(alpha = 0.6f),
                            fontSize = 13.sp
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ProxymaTheme.colors.textPrimary,
                        unfocusedTextColor = ProxymaTheme.colors.textPrimary,
                        focusedBorderColor = ProxymaTheme.colors.accentCyan,
                        unfocusedBorderColor = ProxymaTheme.colors.cardBorder,
                        cursorColor = ProxymaTheme.colors.accentCyan
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(inputText) },
                enabled = inputText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo)
            ) {
                Text("Добавить", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = ProxymaTheme.colors.textSecondary)
            }
        }
    )
}
@Composable
private fun RoutingRulesetCard(
    ruleset: RulesetItem,
    isDragging: Boolean,
    onEdit: () -> Unit,
    onEnabledChange: (Boolean) -> Unit
) {
    val isEnabled = ruleset.enabled ?: false
    val (tagLabel, tagColor) = when (ruleset.outboundTag?.lowercase()) {
        "proxy" -> "Через VPN" to ProxymaTheme.colors.accentCyan
        "direct" -> "Без VPN" to ConnectedGreen
        "block" -> "Блокировать" to BlockRed
        else -> (ruleset.outboundTag ?: "") to AccentIndigo
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (isDragging) {
            ProxymaTheme.colors.cardSurface
        } else if (isEnabled) {
            ProxymaTheme.colors.cardSurface.copy(alpha = 0.92f)
        } else {
            ProxymaTheme.colors.cardSurface.copy(alpha = 0.55f)
        },
        border = BorderStroke(
            width = if (isDragging) 1.5.dp else 1.dp,
            color = if (isDragging) {
                ProxymaTheme.colors.accentCyan
            } else if (isEnabled) {
                ProxymaTheme.colors.cardBorder
            } else {
                ProxymaTheme.colors.cardBorder.copy(alpha = 0.5f)
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatRuleTitle(ruleset.remarks),
                        color = if (isEnabled) ProxymaTheme.colors.textPrimary else ProxymaTheme.colors.textSecondary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (ruleset.locked == true) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            painter = painterResource(R.drawable.ic_lock_24dp),
                            contentDescription = stringResource(R.string.acc_locked),
                            modifier = Modifier.size(14.dp),
                            tint = ProxymaTheme.colors.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatRuleSummary(ruleset),
                    color = ProxymaTheme.colors.textSecondary,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (tagLabel.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = tagColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, tagColor.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = tagLabel,
                            color = tagColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                IconButton(onClick = onEdit) {
                    Icon(
                        painter = painterResource(R.drawable.ic_edit_24dp),
                        contentDescription = stringResource(R.string.acc_edit),
                        tint = ProxymaTheme.colors.textSecondary
                    )
                }
                Switch(
                    checked = isEnabled,
                    onCheckedChange = onEnabledChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = AccentIndigo,
                        uncheckedThumbColor = ProxymaTheme.colors.textSecondary,
                        uncheckedTrackColor = ProxymaTheme.colors.bgTop
                    )
                )
            }
        }
    }
}

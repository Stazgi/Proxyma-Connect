package com.v2ray.ang.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.v2ray.ang.AppConfig
import com.v2ray.ang.AppConfig.VPN
import com.v2ray.ang.R
import com.v2ray.ang.extension.toastError
import com.v2ray.ang.handler.AppLocaleManager
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.handler.MmkvManager.rememberMmkvBool
import com.v2ray.ang.handler.MmkvManager.rememberMmkvString
import com.v2ray.ang.handler.SettingsChangeManager
import com.v2ray.ang.root.RootManager
import com.v2ray.ang.ui.base.BaseComponentActivity
import com.v2ray.ang.ui.compose.AppTopBar
import com.v2ray.ang.ui.compose.NavigationBarsSpacer
import com.v2ray.ang.ui.compose.SettingsEditItem
import com.v2ray.ang.ui.compose.SettingsListItem
import com.v2ray.ang.ui.compose.SettingsSwitchItem
import com.v2ray.ang.ui.compose.ThemeManager
import com.v2ray.ang.ui.compose.verticalScrollbar
import com.v2ray.ang.util.LogUtil
import com.v2ray.ang.util.Utils
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
//  Улучшенная тёмно-серая палитра (более выраженная иерархия)
// ─────────────────────────────────────────────────────────────────────────────
private val DarkGreyBackground      = Color(0xFF0F1115)
private val DarkGreySurface         = Color(0xFF17191F)
private val DarkGreySurfaceElevated = Color(0xFF21242C)
private val DarkGreyBorder          = Color(0xFF2A2E38)
private val DarkGreyBorderSubtle    = Color(0xFF1F2229)

private val AccentBlue    = Color(0xFF7AB0FF)
private val AccentBlueDim = Color(0xFF3B6FC7)

private val TextPrimary   = Color(0xFFF3F5F8)
private val TextSecondary = Color(0xFFA3A9B8)
private val TextTertiary  = Color(0xFF6E7484)

private val DarkGreyColorScheme = darkColorScheme(
    primary               = AccentBlue,
    onPrimary             = Color(0xFF04101F),
    primaryContainer      = Color(0xFF1A2A45),
    onPrimaryContainer    = Color(0xFFDCE9FF),
    secondary             = Color(0xFF8E99B0),
    onSecondary           = Color(0xFF12151B),
    secondaryContainer    = DarkGreySurfaceElevated,
    onSecondaryContainer  = TextPrimary,
    background            = DarkGreyBackground,
    onBackground          = TextPrimary,
    surface               = DarkGreySurface,
    onSurface             = TextPrimary,
    surfaceVariant        = DarkGreySurfaceElevated,
    onSurfaceVariant      = TextSecondary,
    outline               = DarkGreyBorder,
    outlineVariant        = DarkGreyBorderSubtle,
    error                 = Color(0xFFFF8A8A),
)

class SettingsActivity : BaseComponentActivity() {

    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.refreshSystemVpnSettingsAvailability()
            }
        }
    }

    private fun openSystemVpnSettings() {
        try {
            startActivity(Intent(Settings.ACTION_VPN_SETTINGS))
        } catch (error: ActivityNotFoundException) {
            reportSystemVpnSettingsFailure(error)
        } catch (error: SecurityException) {
            reportSystemVpnSettingsFailure(error)
        }
    }

    private fun reportSystemVpnSettingsFailure(error: RuntimeException) {
        LogUtil.e(AppConfig.TAG, "Cannot open system VPN settings", error)
        toastError(R.string.toast_system_vpn_settings_unavailable)
    }

    @Composable
    override fun ScreenContent() {
        MaterialTheme(colorScheme = DarkGreyColorScheme) {
            SettingsScreen(
                viewModel = viewModel,
                onBackClick = { finish() },
                onModeHelpClicked = { Utils.openUri(this, AppConfig.APP_WIKI_MODE) },
                onSystemVpnSettingsClicked = ::openSystemVpnSettings
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit,
    onModeHelpClicked: () -> Unit,
    onSystemVpnSettingsClicked: () -> Unit
) {
    val scrollState = rememberScrollState()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val systemVpnSettingsAvailable by viewModel.systemVpnSettingsAvailable.collectAsStateWithLifecycle()

    // ── Состояние поиска ────────────────────────────────────────────────────
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val query = searchQuery.trim().lowercase()

    // ── Раскрытие секций ────────────────────────────────────────────────────
    var uiSettingsExpanded by rememberSaveable { mutableStateOf(true) }
    var vpnSettingsExpanded by rememberSaveable { mutableStateOf(true) }
    var coreSettingsExpanded by rememberSaveable { mutableStateOf(true) }
    var muxSettingsExpanded by rememberSaveable { mutableStateOf(false) }
    var fragmentSettingsExpanded by rememberSaveable { mutableStateOf(false) }
    var observatorySettingsExpanded by rememberSaveable { mutableStateOf(false) }
    var advancedSettingsExpanded by rememberSaveable { mutableStateOf(true) }
    var modeSettingsExpanded by rememberSaveable { mutableStateOf(true) }

    // ── MMKV-состояния ──────────────────────────────────────────────────────
    var localDns by rememberMmkvBool(AppConfig.PREF_LOCAL_DNS_ENABLED, false)
    var fakeDns by rememberMmkvBool(AppConfig.PREF_FAKE_DNS_ENABLED, false)
    var appendHttpProxy by rememberMmkvBool(AppConfig.PREF_APPEND_HTTP_PROXY, false)
    var vpnDns by rememberMmkvString(AppConfig.PREF_VPN_DNS, "")
    var vpnBypassLan by rememberMmkvString(AppConfig.PREF_VPN_BYPASS_LAN, AppConfig.DEFAULT_VPN_BYPASS_LAN)
    var vpnInterfaceAddress by rememberMmkvString(AppConfig.PREF_VPN_INTERFACE_ADDRESS_CONFIG_INDEX, "0")
    var vpnMtu by rememberMmkvString(AppConfig.PREF_VPN_MTU, "")

    var mux by rememberMmkvBool(AppConfig.PREF_MUX_ENABLED, false)
    var muxConcurrency by rememberMmkvString(AppConfig.PREF_MUX_CONCURRENCY, "8")
    var muxXudpConcurrency by rememberMmkvString(AppConfig.PREF_MUX_XUDP_CONCURRENCY, AppConfig.DEFAULT_MUX_XUDP_CONCURRENCY)
    var muxXudpQuic by rememberMmkvString(AppConfig.PREF_MUX_XUDP_QUIC, "reject")

    var fragment by rememberMmkvBool(AppConfig.PREF_FRAGMENT_ENABLED, false)
    var fragmentPackets by rememberMmkvString(AppConfig.PREF_FRAGMENT_PACKETS, "tlshello")
    var fragmentLength by rememberMmkvString(AppConfig.PREF_FRAGMENT_LENGTH, "50-100")
    var fragmentInterval by rememberMmkvString(AppConfig.PREF_FRAGMENT_INTERVAL, "10-20")
    var fragmentMaxSplit by rememberMmkvString(AppConfig.PREF_FRAGMENT_MAXSPLIT, "10")

    var observatoryLeastPingInterval by rememberMmkvString(AppConfig.PREF_OBSERVATORY_LEAST_PING_INTERVAL, AppConfig.OBSERVATORY_LEAST_PING_INTERVAL)
    var observatoryLeastLoadInterval by rememberMmkvString(AppConfig.PREF_OBSERVATORY_LEAST_LOAD_INTERVAL, AppConfig.OBSERVATORY_LEAST_LOAD_INTERVAL)
    var observatoryLeastLoadMethod by rememberMmkvString(AppConfig.PREF_OBSERVATORY_LEAST_LOAD_METHOD, AppConfig.OBSERVATORY_LEAST_LOAD_METHOD)
    var observatoryLeastLoadSampling by rememberMmkvString(AppConfig.PREF_OBSERVATORY_LEAST_LOAD_SAMPLING, AppConfig.OBSERVATORY_LEAST_LOAD_SAMPLING)
    var observatoryLeastLoadTimeout by rememberMmkvString(AppConfig.PREF_OBSERVATORY_LEAST_LOAD_TIMEOUT, AppConfig.OBSERVATORY_LEAST_LOAD_TIMEOUT)

    var mode by rememberMmkvString(AppConfig.PREF_MODE, VPN)
    var enableRootMode by rememberMmkvBool(AppConfig.PREF_ROOT_MODE_ENABLE, false)
    var lanSharing by rememberMmkvBool(AppConfig.PREF_ROOT_LAN_SHARING, false)

    var hevTunLogLevel by rememberMmkvString(AppConfig.PREF_HEV_TUNNEL_LOGLEVEL, AppConfig.DEFAULT_HEV_TUNNEL_LOGLEVEL)
    var hevTunRwTimeout by rememberMmkvString(AppConfig.PREF_HEV_TUNNEL_RW_TIMEOUT, "")
    var useHevTun by rememberMmkvBool(AppConfig.PREF_USE_HEV_TUNNEL, true)
    var enableLocalProxy by rememberMmkvBool(AppConfig.PREF_ENABLE_LOCAL_PROXY, true)
    var socksPort by rememberMmkvString(AppConfig.PREF_SOCKS_PORT, "")
    var dynamicSocksPort by rememberMmkvBool(AppConfig.PREF_DYNAMIC_SOCKS_PORT, false)
    var socksUsername by rememberMmkvString(AppConfig.PREF_SOCKS_USERNAME, "")
    var socksPassword by rememberMmkvString(AppConfig.PREF_SOCKS_PASSWORD, "")
    var socksEnableUdp by rememberMmkvBool(AppConfig.PREF_SOCKS_ENABLE_UDP, AppConfig.DEFAULT_SOCKS_ENABLE_UDP)
    var proxySharing by rememberMmkvBool(AppConfig.PREF_PROXY_SHARING, false)

    var speedEnabled by rememberMmkvBool(AppConfig.PREF_SPEED_ENABLED, false)
    var confirmRemove by rememberMmkvBool(AppConfig.PREF_CONFIRM_REMOVE, false)
    var doubleColumnDisplay by rememberMmkvBool(AppConfig.PREF_DOUBLE_COLUMN_DISPLAY, false)
    var groupAllDisplay by rememberMmkvBool(AppConfig.PREF_GROUP_ALL_DISPLAY, false)
    var language by remember {
        mutableStateOf(MmkvManager.decodeSettingsString(AppConfig.PREF_LANGUAGE, "auto") ?: "auto")
    }
    var uiModeNight by rememberMmkvString(AppConfig.PREF_UI_MODE_NIGHT, "0")
    var dynamicColor by rememberMmkvBool(AppConfig.PREF_DYNAMIC_COLOR, true)

    var ipv6Enabled by rememberMmkvBool(AppConfig.PREF_IPV6_ENABLED, false)
    var preferIpv6 by rememberMmkvBool(AppConfig.PREF_PREFER_IPV6, false)
    var sniffingEnabled by rememberMmkvBool(AppConfig.PREF_SNIFFING_ENABLED, true)
    var routeOnlyEnabled by rememberMmkvBool(AppConfig.PREF_ROUTE_ONLY_ENABLED, false)
    var remoteDns by rememberMmkvString(AppConfig.PREF_REMOTE_DNS, "")
    var domesticDns by rememberMmkvString(AppConfig.PREF_DOMESTIC_DNS, "")
    var dnsHosts by rememberMmkvString(AppConfig.PREF_DNS_HOSTS, "")
    var coreLogLevel by rememberMmkvString(AppConfig.PREF_LOGLEVEL, "warning")
    var outboundResolveMethod by rememberMmkvString(AppConfig.PREF_OUTBOUND_DOMAIN_RESOLVE_METHOD, AppConfig.DEFAULT_OUTBOUND_DOMAIN_RESOLVE_METHOD)

    var isBooted by rememberMmkvBool(AppConfig.PREF_IS_BOOTED, false)
    var delayTestUrl by rememberMmkvString(AppConfig.PREF_DELAY_TEST_URL, "")
    var realPingConcurrency by rememberMmkvString(AppConfig.PREF_REAL_PING_CONCURRENCY, "16")
    var ipApiUrl by rememberMmkvString(AppConfig.PREF_IP_API_URL, "")

    val isVpn = mode == VPN
    val hevTunEnabled = isVpn && useHevTun
    val localProxyForced = hevTunEnabled
    val effectiveLocalProxy = enableLocalProxy || localProxyForced
    val muxXudpConcurrencyInt = muxXudpConcurrency.toIntOrNull() ?: AppConfig.DEFAULT_MUX_XUDP_CONCURRENCY.toInt()

    val dynamicColorSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    LaunchedEffect(dynamicColorSupported) {
        if (!dynamicColorSupported && dynamicColor) {
            dynamicColor = false
            ThemeManager.setDynamicColorEnabled(false)
        }
    }

    val languageEntries = stringArrayResource(R.array.language_select).toList()
    val languageValues = stringArrayResource(R.array.language_select_value).toList()
    val uiModeNightEntries = stringArrayResource(R.array.ui_mode_night).toList()
    val uiModeNightValues = stringArrayResource(R.array.ui_mode_night_value).toList()
    val bypassLanEntries = stringArrayResource(R.array.vpn_bypass_lan).toList()
    val bypassLanValues = stringArrayResource(R.array.vpn_bypass_lan_value).toList()
    val interfaceAddrEntries = stringArrayResource(R.array.vpn_interface_address).toList()
    val interfaceAddrValues = stringArrayResource(R.array.vpn_interface_address_value).toList()
    val hevLogEntries = stringArrayResource(R.array.hev_tunnel_loglevel).toList()
    val hevLogValues = stringArrayResource(R.array.hev_tunnel_loglevel).toList()
    val coreLogLevelEntries = stringArrayResource(R.array.core_loglevel).toList()
    val coreLogLevelValues = stringArrayResource(R.array.core_loglevel).toList()
    val outboundResolveEntries = stringArrayResource(R.array.outbound_domain_resolve_method).toList()
    val outboundResolveValues = stringArrayResource(R.array.outbound_domain_resolve_method_value).toList()
    val xudpQuicEntries = stringArrayResource(R.array.mux_xudp_quic_entries).toList()
    val xudpQuicValues = stringArrayResource(R.array.mux_xudp_quic_value).toList()
    val fragmentPacketsEntries = stringArrayResource(R.array.fragment_packets).toList()
    val fragmentPacketsValues = stringArrayResource(R.array.fragment_packets).toList()
    val observatoryLeastLoadMethodEntries = stringArrayResource(R.array.observatory_least_load_method).toList()
    val observatoryLeastLoadMethodValues = stringArrayResource(R.array.observatory_least_load_method).toList()
    val modeEntries = stringArrayResource(R.array.mode_entries).toList()
    val modeValues = stringArrayResource(R.array.mode_value).toList()

    // ── Хелпер поиска ───────────────────────────────────────────────────────
    // Возвращает true, если запрос пустой ИЛИ хотя бы одна строка содержит подстроку.
    fun matches(vararg strings: String): Boolean {
        if (query.isEmpty()) return true
        return strings.any { it.lowercase().contains(query) }
    }

    // Заголовки секций (для поиска по названию секции тоже)
    val titleUi          = stringResource(R.string.title_ui_settings)
    val titleVpn         = stringResource(R.string.title_vpn_settings)
    val titleCore        = stringResource(R.string.title_core_settings)
    val titleMux         = stringResource(R.string.title_mux_settings)
    val titleFragment    = stringResource(R.string.title_fragment_settings)
    val titleObservatory = stringResource(R.string.title_observatory_settings)
    val titleAdvanced    = stringResource(R.string.title_advanced)
    val titleMode        = stringResource(R.string.title_mode_settings)

    // Предвычисление совпадений по секциям (нужно и для показа секций, и для «нет результатов»)
    val uiMatch = matches(
        titleUi,
        stringResource(R.string.title_pref_speed_enabled),
        stringResource(R.string.title_pref_confirm_remove),
        stringResource(R.string.title_pref_double_column_display),
        stringResource(R.string.title_pref_group_all_display),
        stringResource(R.string.title_pref_dynamic_color),
        stringResource(R.string.title_language),
        stringResource(R.string.title_pref_ui_mode_night)
    )
    val vpnMatch = matches(
        titleVpn,
        stringResource(R.string.title_pref_ipv6_enabled),
        stringResource(R.string.title_pref_prefer_ipv6),
        stringResource(R.string.title_pref_local_dns_enabled),
        stringResource(R.string.title_pref_fake_dns_enabled),
        stringResource(R.string.title_pref_vpn_dns),
        stringResource(R.string.title_pref_append_http_proxy),
        stringResource(R.string.title_pref_vpn_bypass_lan),
        stringResource(R.string.title_pref_vpn_interface_address),
        stringResource(R.string.title_pref_vpn_mtu),
        stringResource(R.string.title_pref_use_hev_tunnel),
        stringResource(R.string.title_pref_hev_tunnel_loglevel),
        stringResource(R.string.title_pref_hev_tunnel_rw_timeout)
    )
    val coreMatch = matches(
        titleCore,
        stringResource(R.string.title_pref_sniffing_enabled),
        stringResource(R.string.title_pref_route_only_enabled),
        stringResource(R.string.title_pref_enable_local_proxy),
        stringResource(R.string.title_pref_proxy_sharing_enabled),
        stringResource(R.string.title_pref_dynamic_socks_port),
        stringResource(R.string.title_pref_socks_port),
        stringResource(R.string.title_pref_socks_username),
        stringResource(R.string.title_pref_socks_password),
        stringResource(R.string.title_pref_socks_enable_udp),
        stringResource(R.string.title_pref_remote_dns),
        stringResource(R.string.title_pref_domestic_dns),
        stringResource(R.string.title_pref_dns_hosts),
        stringResource(R.string.title_core_loglevel),
        stringResource(R.string.title_outbound_domain_resolve_method)
    )
    val muxMatch = matches(
        titleMux,
        stringResource(R.string.title_pref_mux_enabled),
        stringResource(R.string.title_pref_mux_concurrency),
        stringResource(R.string.title_pref_mux_xudp_concurrency),
        stringResource(R.string.title_pref_mux_xudp_quic)
    )
    val fragmentMatch = matches(
        titleFragment,
        stringResource(R.string.title_pref_fragment_enabled),
        stringResource(R.string.title_pref_fragment_packets),
        stringResource(R.string.title_pref_fragment_length),
        stringResource(R.string.title_pref_fragment_interval),
        stringResource(R.string.title_pref_fragment_maxsplit)
    )
    val observatoryMatch = matches(
        titleObservatory,
        stringResource(R.string.title_pref_observatory_least_ping_interval),
        stringResource(R.string.title_pref_observatory_least_load_interval),
        stringResource(R.string.title_pref_observatory_least_load_method),
        stringResource(R.string.title_pref_observatory_least_load_sampling),
        stringResource(R.string.title_pref_observatory_least_load_timeout)
    )
    val advancedMatch = matches(
        titleAdvanced,
        stringResource(R.string.title_pref_is_booted),
        stringResource(R.string.title_system_vpn_settings),
        stringResource(R.string.title_pref_delay_test_url),
        stringResource(R.string.title_pref_real_ping_concurrency),
        stringResource(R.string.title_pref_ip_api_url)
    )
    val modeMatch = matches(
        titleMode,
        stringResource(R.string.title_root_mode_enabled),
        stringResource(R.string.title_root_lan_sharing)
    )
    val anyMatch = uiMatch || vpnMatch || coreMatch || muxMatch ||
            fragmentMatch || observatoryMatch || advancedMatch || modeMatch

    Scaffold(
        containerColor = DarkGreyBackground,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            AppTopBar(
                title = stringResource(R.string.title_settings),
                onBackClick = onBackClick,
                isLoading = isLoading
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkGreyBackground)
                .padding(innerPadding)
                .verticalScrollbar(scrollState)
                .verticalScroll(scrollState)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Поиск ───────────────────────────────────────────────────────
            SearchBar(query = searchQuery, onQueryChange = { searchQuery = it })

            // ── Быстрый выбор режима + действия ─────────────────────────────
            QuickModeAndActionsBar(
                modeEntries = modeEntries,
                modeValues = modeValues,
                selectedMode = mode,
                onModeSelected = { mode = it },
                systemVpnSettingsAvailable = systemVpnSettingsAvailable,
                onSystemVpnSettingsClicked = onSystemVpnSettingsClicked,
                onModeHelpClicked = onModeHelpClicked
            )

            // ── UI ──────────────────────────────────────────────────────────
            if (uiMatch) {
                SettingsSectionCard(
                    title = titleUi,
                    icon = Icons.Default.Palette,
                    expanded = uiSettingsExpanded || query.isNotEmpty(),
                    onExpandedChange = { uiSettingsExpanded = it }
                ) {
                    SettingsSwitchItem(stringResource(R.string.title_pref_speed_enabled), stringResource(R.string.summary_pref_speed_enabled), speedEnabled) { speedEnabled = it }
                    SettingsSwitchItem(stringResource(R.string.title_pref_confirm_remove), stringResource(R.string.summary_pref_confirm_remove), confirmRemove) { confirmRemove = it }
                    SettingsSwitchItem(stringResource(R.string.title_pref_double_column_display), stringResource(R.string.summary_pref_double_column_display), doubleColumnDisplay) { doubleColumnDisplay = it; SettingsChangeManager.makeSetupGroupTab() }
                    SettingsSwitchItem(stringResource(R.string.title_pref_group_all_display), stringResource(R.string.summary_pref_group_all_display), groupAllDisplay) { groupAllDisplay = it; SettingsChangeManager.makeSetupGroupTab() }
                    SettingsSwitchItem(stringResource(R.string.title_pref_dynamic_color), stringResource(R.string.summary_pref_dynamic_color), dynamicColor, enabled = dynamicColorSupported) { dynamicColor = it; ThemeManager.setDynamicColorEnabled(it) }
                    SettingsListItem(stringResource(R.string.title_language), languageEntries, languageValues, language) { language = it; AppLocaleManager.setApplicationLanguage(it) }
                    SettingsListItem(stringResource(R.string.title_pref_ui_mode_night), uiModeNightEntries, uiModeNightValues, uiModeNight) { uiModeNight = it; ThemeManager.setThemeMode(it) }
                }
            }

            // ── VPN ─────────────────────────────────────────────────────────
            if (vpnMatch) {
                SettingsSectionCard(
                    title = titleVpn,
                    icon = Icons.Default.Security,
                    expanded = vpnSettingsExpanded || query.isNotEmpty(),
                    onExpandedChange = { vpnSettingsExpanded = it }
                ) {
                    SettingsSwitchItem(stringResource(R.string.title_pref_ipv6_enabled), stringResource(R.string.summary_pref_ipv6_enabled), ipv6Enabled) { ipv6Enabled = it }
                    SettingsSwitchItem(stringResource(R.string.title_pref_prefer_ipv6), stringResource(R.string.summary_pref_prefer_ipv6), preferIpv6) { preferIpv6 = it }
                    SettingsSwitchItem(stringResource(R.string.title_pref_local_dns_enabled), stringResource(R.string.summary_pref_local_dns_enabled), localDns, enabled = isVpn) { localDns = it }
                    SettingsSwitchItem(stringResource(R.string.title_pref_fake_dns_enabled), stringResource(R.string.summary_pref_fake_dns_enabled), fakeDns, enabled = isVpn && localDns) { fakeDns = it }
                    SettingsEditItem(stringResource(R.string.title_pref_vpn_dns), vpnDns, enabled = isVpn && !localDns) { vpnDns = it }
                    SettingsSwitchItem(stringResource(R.string.title_pref_append_http_proxy), stringResource(R.string.summary_pref_append_http_proxy), appendHttpProxy, enabled = effectiveLocalProxy) { appendHttpProxy = it }
                    SettingsListItem(stringResource(R.string.title_pref_vpn_bypass_lan), bypassLanEntries, bypassLanValues, vpnBypassLan, enabled = isVpn) { vpnBypassLan = it }
                    SettingsListItem(stringResource(R.string.title_pref_vpn_interface_address), interfaceAddrEntries, interfaceAddrValues, vpnInterfaceAddress, enabled = isVpn) { vpnInterfaceAddress = it }
                    SettingsEditItem(stringResource(R.string.title_pref_vpn_mtu), vpnMtu, enabled = isVpn, keyboardNumber = true) { vpnMtu = it }
                    SettingsSwitchItem(stringResource(R.string.title_pref_use_hev_tunnel), stringResource(R.string.summary_pref_use_hev_tunnel), useHevTun, enabled = isVpn) { useHevTun = it; if (it && !enableLocalProxy) enableLocalProxy = true }
                    SettingsListItem(stringResource(R.string.title_pref_hev_tunnel_loglevel), hevLogEntries, hevLogValues, hevTunLogLevel, enabled = hevTunEnabled) { hevTunLogLevel = it }
                    SettingsEditItem(stringResource(R.string.title_pref_hev_tunnel_rw_timeout), hevTunRwTimeout, enabled = hevTunEnabled, keyboardNumber = true) { hevTunRwTimeout = it }
                }
            }

            // ── Ядро ────────────────────────────────────────────────────────
            if (coreMatch) {
                SettingsSectionCard(
                    title = titleCore,
                    icon = Icons.Default.Dns,
                    expanded = coreSettingsExpanded || query.isNotEmpty(),
                    onExpandedChange = { coreSettingsExpanded = it }
                ) {
                    SettingsSwitchItem(stringResource(R.string.title_pref_sniffing_enabled), stringResource(R.string.summary_pref_sniffing_enabled), sniffingEnabled) { sniffingEnabled = it }
                    SettingsSwitchItem(stringResource(R.string.title_pref_route_only_enabled), stringResource(R.string.summary_pref_route_only_enabled), routeOnlyEnabled) { routeOnlyEnabled = it }
                    SettingsSwitchItem(stringResource(R.string.title_pref_enable_local_proxy), stringResource(R.string.summary_pref_enable_local_proxy), enableLocalProxy, enabled = !localProxyForced) { if (!localProxyForced) { enableLocalProxy = it; if (!it && appendHttpProxy) appendHttpProxy = false } }
                    SettingsSwitchItem(stringResource(R.string.title_pref_proxy_sharing_enabled), stringResource(R.string.summary_pref_proxy_sharing_enabled), proxySharing, enabled = effectiveLocalProxy) { proxySharing = it }
                    SettingsSwitchItem(stringResource(R.string.title_pref_dynamic_socks_port), stringResource(R.string.summary_pref_dynamic_socks_port), dynamicSocksPort, enabled = effectiveLocalProxy) { dynamicSocksPort = it }
                    SettingsEditItem(stringResource(R.string.title_pref_socks_port), socksPort, enabled = effectiveLocalProxy && !dynamicSocksPort, keyboardNumber = true) { socksPort = it }
                    SettingsEditItem(stringResource(R.string.title_pref_socks_username), socksUsername, enabled = effectiveLocalProxy) { socksUsername = it }
                    SettingsEditItem(stringResource(R.string.title_pref_socks_password), socksPassword, enabled = effectiveLocalProxy, isPassword = true) { socksPassword = it }
                    SettingsSwitchItem(stringResource(R.string.title_pref_socks_enable_udp), stringResource(R.string.summary_pref_socks_enable_udp), socksEnableUdp, enabled = effectiveLocalProxy) { socksEnableUdp = it }
                    SettingsEditItem(stringResource(R.string.title_pref_remote_dns), remoteDns) { remoteDns = it }
                    SettingsEditItem(stringResource(R.string.title_pref_domestic_dns), domesticDns) { domesticDns = it }
                    SettingsEditItem(stringResource(R.string.title_pref_dns_hosts), dnsHosts) { dnsHosts = it }
                    SettingsListItem(stringResource(R.string.title_core_loglevel), coreLogLevelEntries, coreLogLevelValues, coreLogLevel) { coreLogLevel = it }
                    SettingsListItem(stringResource(R.string.title_outbound_domain_resolve_method), outboundResolveEntries, outboundResolveValues, outboundResolveMethod) { outboundResolveMethod = it }
                }
            }
            // ── Mux ─────────────────────────────────────────────────────────
            if (muxMatch) {
                SettingsSectionCard(
                    title = titleMux,
                    icon = Icons.Default.SwapHoriz,
                    expanded = muxSettingsExpanded || query.isNotEmpty(),
                    onExpandedChange = { muxSettingsExpanded = it }
                ) {
                    SettingsSwitchItem(stringResource(R.string.title_pref_mux_enabled), stringResource(R.string.summary_pref_mux_enabled), mux) { mux = it }
                    SettingsEditItem(stringResource(R.string.title_pref_mux_concurrency), muxConcurrency, enabled = mux, keyboardNumber = true) { muxConcurrency = it }
                    SettingsEditItem(stringResource(R.string.title_pref_mux_xudp_concurrency), muxXudpConcurrency, enabled = mux, keyboardNumber = true) { muxXudpConcurrency = it }
                    SettingsListItem(stringResource(R.string.title_pref_mux_xudp_quic), xudpQuicEntries, xudpQuicValues, muxXudpQuic, enabled = mux && muxXudpConcurrencyInt >= 0) { muxXudpQuic = it }
                }
            }

            // ── Fragment ────────────────────────────────────────────────────
            if (fragmentMatch) {
                SettingsSectionCard(
                    title = titleFragment,
                    icon = Icons.Default.Science,
                    expanded = fragmentSettingsExpanded || query.isNotEmpty(),
                    onExpandedChange = { fragmentSettingsExpanded = it }
                ) {
                    SettingsSwitchItem(stringResource(R.string.title_pref_fragment_enabled), checked = fragment) { fragment = it }
                    SettingsListItem(stringResource(R.string.title_pref_fragment_packets), fragmentPacketsEntries, fragmentPacketsValues, fragmentPackets, enabled = fragment) { fragmentPackets = it }
                    SettingsEditItem(stringResource(R.string.title_pref_fragment_length), fragmentLength, enabled = fragment) { fragmentLength = it }
                    SettingsEditItem(stringResource(R.string.title_pref_fragment_interval), fragmentInterval, enabled = fragment) { fragmentInterval = it }
                    SettingsEditItem(stringResource(R.string.title_pref_fragment_maxsplit), fragmentMaxSplit, enabled = fragment, keyboardNumber = true) { fragmentMaxSplit = it }
                }
            }

            // ── Observatory ─────────────────────────────────────────────────
            if (observatoryMatch) {
                SettingsSectionCard(
                    title = titleObservatory,
                    icon = Icons.Default.Speed,
                    expanded = observatorySettingsExpanded || query.isNotEmpty(),
                    onExpandedChange = { observatorySettingsExpanded = it }
                ) {
                    SettingsEditItem(stringResource(R.string.title_pref_observatory_least_ping_interval), observatoryLeastPingInterval) { viewModel.validateObservatoryDuration(it)?.let { v -> observatoryLeastPingInterval = v } }
                    SettingsEditItem(stringResource(R.string.title_pref_observatory_least_load_interval), observatoryLeastLoadInterval) { viewModel.validateObservatoryDuration(it)?.let { v -> observatoryLeastLoadInterval = v } }
                    SettingsListItem(stringResource(R.string.title_pref_observatory_least_load_method), observatoryLeastLoadMethodEntries, observatoryLeastLoadMethodValues, observatoryLeastLoadMethod) { observatoryLeastLoadMethod = it }
                    SettingsEditItem(stringResource(R.string.title_pref_observatory_least_load_sampling), observatoryLeastLoadSampling, keyboardNumber = true) { viewModel.validateObservatorySampling(it)?.let { v -> observatoryLeastLoadSampling = v } }
                    SettingsEditItem(stringResource(R.string.title_pref_observatory_least_load_timeout), observatoryLeastLoadTimeout) { viewModel.validateObservatoryDuration(it)?.let { v -> observatoryLeastLoadTimeout = v } }
                }
            }

            // ── Advanced ────────────────────────────────────────────────────
            if (advancedMatch) {
                SettingsSectionCard(
                    title = titleAdvanced,
                    icon = Icons.Default.Tune,
                    expanded = advancedSettingsExpanded || query.isNotEmpty(),
                    onExpandedChange = { advancedSettingsExpanded = it }
                ) {
                    SettingsSwitchItem(stringResource(R.string.title_pref_is_booted), stringResource(R.string.summary_pref_is_booted), isBooted) { isBooted = it }
                    if (systemVpnSettingsAvailable) {
                        ActionPillButton(
                            stringResource(R.string.title_system_vpn_settings),
                            onSystemVpnSettingsClicked,
                            stringResource(R.string.summary_system_vpn_settings)
                        )
                    }
                    SettingsEditItem(stringResource(R.string.title_pref_delay_test_url), delayTestUrl) { delayTestUrl = it }
                    SettingsEditItem(stringResource(R.string.title_pref_real_ping_concurrency), realPingConcurrency, keyboardNumber = true) { realPingConcurrency = it }
                    SettingsEditItem(stringResource(R.string.title_pref_ip_api_url), ipApiUrl) { ipApiUrl = it }
                }
            }
            // ── Mode ────────────────────────────────────────────────────────
            if (modeMatch) {
                SettingsSectionCard(
                    title = titleMode,
                    icon = Icons.Default.AdminPanelSettings,
                    expanded = modeSettingsExpanded || query.isNotEmpty(),
                    onExpandedChange = { modeSettingsExpanded = it }
                ) {
                    SettingsSwitchItem(stringResource(R.string.title_root_mode_enabled), stringResource(R.string.summary_root_mode_enabled), enableRootMode) { v ->
                        if (v && !RootManager.cachedRoot()) viewModel.checkAndRequestRoot { enableRootMode = true } else enableRootMode = v
                    }
                    SettingsSwitchItem(stringResource(R.string.title_root_lan_sharing), stringResource(R.string.summary_root_lan_sharing), lanSharing) { v ->
                        if (v && !RootManager.cachedRoot()) viewModel.checkAndRequestRoot { lanSharing = true } else lanSharing = v
                    }
                }
            }

            // ── Нет результатов ─────────────────────────────────────────────
            if (query.isNotEmpty() && !anyMatch) {
                EmptySearchState(query = searchQuery)
            }

            Spacer(modifier = Modifier.height(20.dp))
            NavigationBarsSpacer()
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Поиск
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Поиск настроек…", color = TextTertiary) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = if (query.isEmpty()) TextSecondary else AccentBlue
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Очистить",
                        tint = TextSecondary
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = DarkGreySurface,
            unfocusedContainerColor = DarkGreySurface,
            focusedBorderColor = AccentBlueDim,
            unfocusedBorderColor = DarkGreyBorder,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            focusedPlaceholderColor = TextTertiary,
            unfocusedPlaceholderColor = TextTertiary,
            cursorColor = AccentBlue,
            focusedLeadingIconColor = AccentBlue,
            unfocusedLeadingIconColor = TextSecondary,
            focusedTrailingIconColor = TextSecondary,
            unfocusedTrailingIconColor = TextSecondary
        )
    )
}

@Composable
private fun EmptySearchState(query: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = DarkGreySurface,
        border = BorderStroke(1.dp, DarkGreyBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                "Ничего не найдено",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(
                "По запросу «$query» настроек нет",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}
// ─────────────────────────────────────────────────────────────────────────────
//  Быстрый выбор режима + действия
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun QuickModeAndActionsBar(
    modeEntries: List<String>,
    modeValues: List<String>,
    selectedMode: String,
    onModeSelected: (String) -> Unit,
    systemVpnSettingsAvailable: Boolean,
    onSystemVpnSettingsClicked: () -> Unit,
    onModeHelpClicked: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = DarkGreySurface,
        border = BorderStroke(1.dp, DarkGreyBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = stringResource(R.string.title_mode),
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                modeValues.forEachIndexed { index, value ->
                    val label = modeEntries.getOrElse(index) { value }
                    val isSelected = value == selectedMode
                    FilledTonalButton(
                        onClick = { onModeSelected(value) },
                        modifier = Modifier.weight(1f).height(48.dp), // 48dp — touch target
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (isSelected) AccentBlue else DarkGreySurfaceElevated,
                            contentColor = if (isSelected) Color(0xFF0B1424) else TextPrimary
                        )
                    ) {
                        if (isSelected) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF0B1424)))
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (systemVpnSettingsAvailable) {
                    FilledTonalButton(
                        onClick = onSystemVpnSettingsClicked,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = DarkGreySurfaceElevated,
                            contentColor = TextPrimary
                        )
                    ) {
                        Text(stringResource(R.string.title_system_vpn_settings), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                FilledTonalButton(
                    onClick = onModeHelpClicked,
                    modifier = Modifier
                        .let { if (systemVpnSettingsAvailable) it.width(48.dp) else it.fillMaxWidth() }
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = DarkGreySurfaceElevated,
                        contentColor = TextPrimary
                    )
                ) {
                    Text(
                        if (systemVpnSettingsAvailable) "?" else stringResource(R.string.title_mode_help),
                        fontWeight = FontWeight.Bold,
                        color = AccentBlue,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
// ─────────────────────────────────────────────────────────────────────────────
//  Карточка секции с иконкой и аккуратным заголовком
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SettingsSectionCard(
    title: String,
    icon: ImageVector,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "section_arrow"
    )
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = DarkGreySurface,
        border = BorderStroke(1.dp, DarkGreyBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp) // 56dp — комфортный touch target
                    .clickable { onExpandedChange(!expanded) }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Иконка секции
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(AccentBlue.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = AccentBlue,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkGreySurfaceElevated),
                    contentAlignment = Alignment.Center
                ) {
                    ChevronDownGlyph(
                        tint = AccentBlue,
                        modifier = Modifier.size(16.dp).rotate(arrowRotation)
                    )
                }
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                    HorizontalDivider(
                        color = DarkGreyBorderSubtle,
                        thickness = 1.dp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                    content()
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Вспомогательные виджеты
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ActionPillButton(title: String, onClick: () -> Unit, subtitle: String? = null) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = DarkGreySurfaceElevated,
            contentColor = TextPrimary
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = AccentBlue
            )
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
    }
}

@Composable
private fun ChevronDownGlyph(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val path = Path().apply {
            moveTo(size.width * 0.2f, size.height * 0.35f)
            lineTo(size.width * 0.5f, size.height * 0.68f)
            lineTo(size.width * 0.8f, size.height * 0.35f)
        }
        drawPath(
            path = path,
            color = tint,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

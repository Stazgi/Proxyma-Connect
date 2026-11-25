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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.v2ray.ang.ui.theme.ProxymaTheme // <--- ДОБАВЛЕН ИМПОРТ ТЕМЫ
import com.v2ray.ang.util.LogUtil
import com.v2ray.ang.util.Utils
import kotlinx.coroutines.launch

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
        // Убрали обертку MaterialTheme
        SettingsScreen(
            viewModel = viewModel,
            onBackClick = { finish() },
            onModeHelpClicked = { Utils.openUri(this, AppConfig.APP_WIKI_MODE) },
            onSystemVpnSettingsClicked = ::openSystemVpnSettings,
            onThemeChanged = { recreate() } // <--- ДОБАВЛЕНА ЭТА СТРОКА
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit,
    onModeHelpClicked: () -> Unit,
    onSystemVpnSettingsClicked: () -> Unit,
    onThemeChanged: () -> Unit // <--- ДОБАВИТЬ СЮДА
) {
    val scrollState = rememberScrollState()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val systemVpnSettingsAvailable by viewModel.systemVpnSettingsAvailable.collectAsStateWithLifecycle()

    var uiSettingsExpanded by rememberSaveable { mutableStateOf(true) }
    var vpnSettingsExpanded by rememberSaveable { mutableStateOf(true) }
    var coreSettingsExpanded by rememberSaveable { mutableStateOf(true) }
    var muxSettingsExpanded by rememberSaveable { mutableStateOf(false) }
    var fragmentSettingsExpanded by rememberSaveable { mutableStateOf(false) }
    var observatorySettingsExpanded by rememberSaveable { mutableStateOf(false) }
    var advancedSettingsExpanded by rememberSaveable { mutableStateOf(true) }
    var modeSettingsExpanded by rememberSaveable { mutableStateOf(true) }
    
    // --- ДАЛЬШЕ ИДУТ ВАШИ НАСТРОЙКИ MMKV (var localDns ... и т.д.) ---
    var localDns by rememberMmkvBool(AppConfig.PREF_LOCAL_DNS_ENABLED, false)
    // ... (остальные переменные без изменений)
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
    mutableStateOf(
        MmkvManager.decodeSettingsString(AppConfig.PREF_LANGUAGE, "auto") ?: "auto"
    )
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
Scaffold(
    containerColor = ProxymaTheme.colors.bgTop, // <-- Заменили DarkGreyBackground
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
            .background(ProxymaTheme.colors.bgTop) // <-- Заменили DarkGreyBackground
            .padding(innerPadding)
            .verticalScrollbar(scrollState)
            .verticalScroll(scrollState)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        QuickModeAndActionsBar(
            modeEntries = modeEntries,
            modeValues = modeValues,
            selectedMode = mode,
            onModeSelected = { mode = it },
            systemVpnSettingsAvailable = systemVpnSettingsAvailable,
            onSystemVpnSettingsClicked = onSystemVpnSettingsClicked,
            onModeHelpClicked = onModeHelpClicked
        )

        SettingsSectionCard(
            title = stringResource(R.string.title_ui_settings),
            expanded = uiSettingsExpanded,
            onExpandedChange = { uiSettingsExpanded = it }
        ) {
            SettingsSwitchItem(
                title = stringResource(R.string.title_pref_speed_enabled),
                summary = stringResource(R.string.summary_pref_speed_enabled),
                checked = speedEnabled,
                onCheckedChange = { speedEnabled = it }
            )
            SettingsSwitchItem(
                title = stringResource(R.string.title_pref_confirm_remove),
                summary = stringResource(R.string.summary_pref_confirm_remove),
                checked = confirmRemove,
                onCheckedChange = { confirmRemove = it }
            )
            SettingsSwitchItem(
                title = stringResource(R.string.title_pref_double_column_display),
                summary = stringResource(R.string.summary_pref_double_column_display),
                checked = doubleColumnDisplay,
                onCheckedChange = {
                    doubleColumnDisplay = it
                    SettingsChangeManager.makeSetupGroupTab()
                }
            )
            SettingsSwitchItem(
                title = stringResource(R.string.title_pref_group_all_display),
                summary = stringResource(R.string.summary_pref_group_all_display),
                checked = groupAllDisplay,
                onCheckedChange = {
                    groupAllDisplay = it
                    SettingsChangeManager.makeSetupGroupTab()
                }
            )
            SettingsSwitchItem(
                title = stringResource(R.string.title_pref_dynamic_color),
                summary = stringResource(R.string.summary_pref_dynamic_color),
                checked = dynamicColor,
                enabled = dynamicColorSupported,
                onCheckedChange = {
                    dynamicColor = it
                    ThemeManager.setDynamicColorEnabled(it)
                }
            )
            SettingsListItem(
                title = stringResource(R.string.title_language),
                entries = languageEntries,
                values = languageValues,
                selectedValue = language,
                onSelected = {
                    language = it
                    AppLocaleManager.setApplicationLanguage(it)
                }
            )
            SettingsListItem(
                title = stringResource(R.string.title_pref_ui_mode_night),
                entries = uiModeNightEntries,
                values = uiModeNightValues,
                selectedValue = uiModeNight,
                onSelected = {
                    uiModeNight = it
                    ThemeManager.setThemeMode(it)
                }
            )
        }

        SettingsSectionCard(
            title = stringResource(R.string.title_vpn_settings),
            expanded = vpnSettingsExpanded,
            onExpandedChange = { vpnSettingsExpanded = it }
        ) {
            SettingsSwitchItem(
                title = stringResource(R.string.title_pref_ipv6_enabled),
                summary = stringResource(R.string.summary_pref_ipv6_enabled),
                checked = ipv6Enabled,
                onCheckedChange = { ipv6Enabled = it }
            )
            SettingsSwitchItem(
                title = stringResource(R.string.title_pref_prefer_ipv6),
                summary = stringResource(R.string.summary_pref_prefer_ipv6),
                checked = preferIpv6,
                onCheckedChange = { preferIpv6 = it }
            )
            SettingsSwitchItem(
                title = stringResource(R.string.title_pref_local_dns_enabled),
                summary = stringResource(R.string.summary_pref_local_dns_enabled),
                checked = localDns,
                enabled = isVpn,
                onCheckedChange = {
                    localDns = it
                    if (!it && fakeDns) fakeDns = false
                }
            )
            SettingsSwitchItem(
                title = stringResource(R.string.title_pref_fake_dns_enabled),
                summary = stringResource(R.string.summary_pref_fake_dns_enabled),
                checked = fakeDns,
                enabled = isVpn && localDns,
                onCheckedChange = { fakeDns = it }
            )
            SettingsEditItem(
                title = stringResource(R.string.title_pref_vpn_dns),
                value = vpnDns,
                enabled = isVpn && !localDns,
                onValueChanged = { vpnDns = it }
            )
            SettingsSwitchItem(
                title = stringResource(R.string.title_pref_append_http_proxy),
                summary = stringResource(R.string.summary_pref_append_http_proxy),
                checked = appendHttpProxy,
                enabled = effectiveLocalProxy,
                onCheckedChange = { appendHttpProxy = it }
            )
            SettingsListItem(
                title = stringResource(R.string.title_pref_vpn_bypass_lan),
                entries = bypassLanEntries,
                values = bypassLanValues,
                selectedValue = vpnBypassLan,
                enabled = isVpn,
                onSelected = { vpnBypassLan = it }
            )
            SettingsListItem(
                title = stringResource(R.string.title_pref_vpn_interface_address),
                entries = interfaceAddrEntries,
                values = interfaceAddrValues,
                selectedValue = vpnInterfaceAddress,
                enabled = isVpn,
                onSelected = { vpnInterfaceAddress = it }
            )
            SettingsEditItem(
                title = stringResource(R.string.title_pref_vpn_mtu),
                value = vpnMtu,
                enabled = isVpn,
                keyboardNumber = true,
                onValueChanged = { vpnMtu = it }
            )
            SettingsSwitchItem(
                title = stringResource(R.string.title_pref_use_hev_tunnel),
                summary = stringResource(R.string.summary_pref_use_hev_tunnel),
                checked = useHevTun,
                enabled = isVpn,
                onCheckedChange = {
                    useHevTun = it
                    if (it && !enableLocalProxy) {
                        enableLocalProxy = true
                    }
                }
            )
            SettingsListItem(
                title = stringResource(R.string.title_pref_hev_tunnel_loglevel),
                entries = hevLogEntries,
                values = hevLogValues,
                selectedValue = hevTunLogLevel,
                enabled = hevTunEnabled,
                onSelected = { hevTunLogLevel = it }
            )
            SettingsEditItem(
                title = stringResource(R.string.title_pref_hev_tunnel_rw_timeout),
                value = hevTunRwTimeout,
                enabled = hevTunEnabled,
                keyboardNumber = true,
                onValueChanged = { hevTunRwTimeout = it }
            )
        }
        SettingsSectionCard(
    title = stringResource(R.string.title_core_settings),
    expanded = coreSettingsExpanded,
    onExpandedChange = { coreSettingsExpanded = it }
) {
    SettingsSwitchItem(
        title = stringResource(R.string.title_pref_sniffing_enabled),
        summary = stringResource(R.string.summary_pref_sniffing_enabled),
        checked = sniffingEnabled,
        onCheckedChange = { sniffingEnabled = it }
    )
    SettingsSwitchItem(
        title = stringResource(R.string.title_pref_route_only_enabled),
        summary = stringResource(R.string.summary_pref_route_only_enabled),
        checked = routeOnlyEnabled,
        onCheckedChange = { routeOnlyEnabled = it }
    )
    SettingsSwitchItem(
        title = stringResource(R.string.title_pref_enable_local_proxy),
        summary = stringResource(R.string.summary_pref_enable_local_proxy),
        checked = enableLocalProxy,
        enabled = !localProxyForced,
        onCheckedChange = {
            if (!localProxyForced) {
                enableLocalProxy = it
                if (!it && appendHttpProxy) {
                    appendHttpProxy = false
                }
            }
        }
    )
    SettingsSwitchItem(
        title = stringResource(R.string.title_pref_proxy_sharing_enabled),
        summary = stringResource(R.string.summary_pref_proxy_sharing_enabled),
        checked = proxySharing,
        enabled = effectiveLocalProxy,
        onCheckedChange = { proxySharing = it }
    )
    SettingsSwitchItem(
        title = stringResource(R.string.title_pref_dynamic_socks_port),
        summary = stringResource(R.string.summary_pref_dynamic_socks_port),
        checked = dynamicSocksPort,
        enabled = effectiveLocalProxy,
        onCheckedChange = { dynamicSocksPort = it }
    )
    SettingsEditItem(
        title = stringResource(R.string.title_pref_socks_port),
        value = socksPort,
        enabled = effectiveLocalProxy && !dynamicSocksPort,
        keyboardNumber = true,
        onValueChanged = { socksPort = it }
    )
    SettingsEditItem(
        title = stringResource(R.string.title_pref_socks_username),
        value = socksUsername,
        enabled = effectiveLocalProxy,
        onValueChanged = { socksUsername = it }
    )
    SettingsEditItem(
        title = stringResource(R.string.title_pref_socks_password),
        value = socksPassword,
        enabled = effectiveLocalProxy,
        isPassword = true,
        onValueChanged = { socksPassword = it }
    )
    SettingsSwitchItem(
        title = stringResource(R.string.title_pref_socks_enable_udp),
        summary = stringResource(R.string.summary_pref_socks_enable_udp),
        checked = socksEnableUdp,
        enabled = effectiveLocalProxy,
        onCheckedChange = { socksEnableUdp = it }
    )
    SettingsEditItem(
        title = stringResource(R.string.title_pref_remote_dns),
        value = remoteDns,
        onValueChanged = { remoteDns = it }
    )
    SettingsEditItem(
        title = stringResource(R.string.title_pref_domestic_dns),
        value = domesticDns,
        onValueChanged = { domesticDns = it }
    )
    SettingsEditItem(
        title = stringResource(R.string.title_pref_dns_hosts),
        value = dnsHosts,
        onValueChanged = { dnsHosts = it }
    )
    SettingsListItem(
        title = stringResource(R.string.title_core_loglevel),
        entries = coreLogLevelEntries,
        values = coreLogLevelValues,
        selectedValue = coreLogLevel,
        onSelected = { coreLogLevel = it }
    )
    SettingsListItem(
        title = stringResource(R.string.title_outbound_domain_resolve_method),
        entries = outboundResolveEntries,
        values = outboundResolveValues,
        selectedValue = outboundResolveMethod,
        onSelected = { outboundResolveMethod = it }
    )
}

SettingsSectionCard(
    title = stringResource(R.string.title_mux_settings),
    expanded = muxSettingsExpanded,
    onExpandedChange = { muxSettingsExpanded = it }
) {
    SettingsSwitchItem(
        title = stringResource(R.string.title_pref_mux_enabled),
        summary = stringResource(R.string.summary_pref_mux_enabled),
        checked = mux,
        onCheckedChange = { mux = it }
    )
    SettingsEditItem(
        title = stringResource(R.string.title_pref_mux_concurrency),
        value = muxConcurrency,
        enabled = mux,
        keyboardNumber = true,
        onValueChanged = { muxConcurrency = it }
    )
    SettingsEditItem(
        title = stringResource(R.string.title_pref_mux_xudp_concurrency),
        value = muxXudpConcurrency,
        enabled = mux,
        keyboardNumber = true,
        onValueChanged = { muxXudpConcurrency = it }
    )
    SettingsListItem(
        title = stringResource(R.string.title_pref_mux_xudp_quic),
        entries = xudpQuicEntries,
        values = xudpQuicValues,
        selectedValue = muxXudpQuic,
        enabled = mux && muxXudpConcurrencyInt >= 0,
        onSelected = { muxXudpQuic = it }
    )
}
            SettingsSectionCard(
                title = stringResource(R.string.title_fragment_settings),
                expanded = fragmentSettingsExpanded,
                onExpandedChange = { fragmentSettingsExpanded = it }
            ) {
                SettingsSwitchItem(
                    title = stringResource(R.string.title_pref_fragment_enabled),
                    checked = fragment,
                    onCheckedChange = { fragment = it }
                )
                SettingsListItem(
                    title = stringResource(R.string.title_pref_fragment_packets),
                    entries = fragmentPacketsEntries,
                    values = fragmentPacketsValues,
                    selectedValue = fragmentPackets,
                    enabled = fragment,
                    onSelected = { fragmentPackets = it }
                )
                SettingsEditItem(
                    title = stringResource(R.string.title_pref_fragment_length),
                    value = fragmentLength,
                    enabled = fragment,
                    onValueChanged = { fragmentLength = it }
                )
                SettingsEditItem(
                    title = stringResource(R.string.title_pref_fragment_interval),
                    value = fragmentInterval,
                    enabled = fragment,
                    onValueChanged = { fragmentInterval = it }
                )
                SettingsEditItem(
                    title = stringResource(R.string.title_pref_fragment_maxsplit),
                    value = fragmentMaxSplit,
                    enabled = fragment,
                    keyboardNumber = true,
                    onValueChanged = { fragmentMaxSplit = it }
                )
            }

            SettingsSectionCard(
                title = stringResource(R.string.title_observatory_settings),
                expanded = observatorySettingsExpanded,
                onExpandedChange = { observatorySettingsExpanded = it }
            ) {
                SettingsEditItem(
                    title = stringResource(R.string.title_pref_observatory_least_ping_interval),
                    value = observatoryLeastPingInterval,
                    onValueChanged = {
                        viewModel.validateObservatoryDuration(it)?.let { value ->
                            observatoryLeastPingInterval = value
                        }
                    }
                )
                SettingsEditItem(
                    title = stringResource(R.string.title_pref_observatory_least_load_interval),
                    value = observatoryLeastLoadInterval,
                    onValueChanged = {
                        viewModel.validateObservatoryDuration(it)?.let { value ->
                            observatoryLeastLoadInterval = value
                        }
                    }
                )
                SettingsListItem(
                    title = stringResource(R.string.title_pref_observatory_least_load_method),
                    entries = observatoryLeastLoadMethodEntries,
                    values = observatoryLeastLoadMethodValues,
                    selectedValue = observatoryLeastLoadMethod,
                    onSelected = { observatoryLeastLoadMethod = it }
                )
                SettingsEditItem(
                    title = stringResource(R.string.title_pref_observatory_least_load_sampling),
                    value = observatoryLeastLoadSampling,
                    keyboardNumber = true,
                    onValueChanged = {
                        viewModel.validateObservatorySampling(it)?.let { value ->
                            observatoryLeastLoadSampling = value
                        }
                    }
                )
                SettingsEditItem(
                    title = stringResource(R.string.title_pref_observatory_least_load_timeout),
                    value = observatoryLeastLoadTimeout,
                    onValueChanged = {
                        viewModel.validateObservatoryDuration(it)?.let { value ->
                            observatoryLeastLoadTimeout = value
                        }
                    }
                )
            }

            SettingsSectionCard(
                title = stringResource(R.string.title_advanced),
                expanded = advancedSettingsExpanded,
                onExpandedChange = { advancedSettingsExpanded = it }
            ) {
                SettingsSwitchItem(
                    title = stringResource(R.string.title_pref_is_booted),
                    summary = stringResource(R.string.summary_pref_is_booted),
                    checked = isBooted,
                    onCheckedChange = { isBooted = it }
                )
                SettingsEditItem(
                    title = stringResource(R.string.title_pref_delay_test_url),
                    value = delayTestUrl,
                    onValueChanged = { delayTestUrl = it }
                )
                SettingsEditItem(
                    title = stringResource(R.string.title_pref_real_ping_concurrency),
                    value = realPingConcurrency,
                    keyboardNumber = true,
                    onValueChanged = { realPingConcurrency = it }
                )
                SettingsEditItem(
                    title = stringResource(R.string.title_pref_ip_api_url),
                    value = ipApiUrl,
                    onValueChanged = { ipApiUrl = it }
                )
    SettingsSwitchItem(
    title = stringResource(R.string.title_root_mode_enabled),
    summary = stringResource(R.string.summary_root_mode_enabled),
    checked = enableRootMode,
    enabled = RootManager.isRootAvailable(),
    onCheckedChange = { enableRootMode = it }
)
SettingsSwitchItem(
    title = stringResource(R.string.title_root_lan_sharing),
    summary = stringResource(R.string.summary_root_lan_sharing),
    checked = lanSharing,
    enabled = enableRootMode,
    onCheckedChange = { lanSharing = it }
)
            }
        }
    }
}

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
        color = ProxymaTheme.colors.cardSurface, // Заменено
        border = BorderStroke(1.dp, ProxymaTheme.colors.cardBorder), // Заменено
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.title_mode),
                style = MaterialTheme.typography.labelMedium,
                color = ProxymaTheme.colors.textSecondary // Заменено
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                modeValues.forEachIndexed { index, value ->
                    val label = modeEntries.getOrElse(index) { value }
                    val isSelected = value == selectedMode

                    FilledTonalButton(
                        onClick = { onModeSelected(value) },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (isSelected) ProxymaTheme.colors.accentCyan else ProxymaTheme.colors.bgBottom, // Заменено
                            contentColor = if (isSelected) ProxymaTheme.colors.bgTop else ProxymaTheme.colors.textPrimary // Заменено
                        )
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Rounded.Shield,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (systemVpnSettingsAvailable) {
                    FilledTonalButton(
                        onClick = onSystemVpnSettingsClicked,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = ProxymaTheme.colors.bgBottom, // Заменено
                            contentColor = ProxymaTheme.colors.textPrimary // Заменено
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.VpnKey,
                            contentDescription = null,
                            tint = ProxymaTheme.colors.accentCyan, // Заменено
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.title_system_vpn_settings),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                FilledTonalButton(
                    onClick = onModeHelpClicked,
                    modifier = Modifier
                        .let { if (systemVpnSettingsAvailable) it.width(48.dp) else it.fillMaxWidth() }
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = ProxymaTheme.colors.bgBottom, // Заменено
                        contentColor = ProxymaTheme.colors.textPrimary // Заменено
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.HelpOutline,
                        contentDescription = stringResource(R.string.title_mode_help),
                        tint = ProxymaTheme.colors.accentCyan, // Заменено
                        modifier = Modifier.size(20.dp)
                    )
                    if (!systemVpnSettingsAvailable) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.title_mode_help),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "section_arrow_rotation"
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = ProxymaTheme.colors.cardSurface, // Заменено
        border = BorderStroke(1.dp, ProxymaTheme.colors.cardBorder), // Заменено
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clickable { onExpandedChange(!expanded) }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = ProxymaTheme.colors.textPrimary, // Заменено
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ProxymaTheme.colors.bgBottom), // Заменено
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ExpandMore,
                        contentDescription = null,
                        tint = ProxymaTheme.colors.accentCyan, // Заменено
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(arrowRotation)
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    HorizontalDivider(
                        color = ProxymaTheme.colors.cardBorder, // Заменено
                        thickness = 1.dp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                    content()
                }
            }
        }
    }
}

@Composable
private fun ActionPillButton(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    subtitle: String? = null
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = ProxymaTheme.colors.bgBottom, // Заменено
            contentColor = ProxymaTheme.colors.textPrimary // Заменено
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ProxymaTheme.colors.accentCyan, // Заменено
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = ProxymaTheme.colors.textPrimary // Заменено
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = ProxymaTheme.colors.textSecondary // Заменено
                    )
                }
            }
        }
    }
}

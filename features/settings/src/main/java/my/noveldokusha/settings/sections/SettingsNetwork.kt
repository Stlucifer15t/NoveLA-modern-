package my.noveldokusha.settings.sections

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatterySaver
import androidx.compose.material.icons.outlined.Http
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import my.noveldokusha.coreui.components.NovelaSettingsGroupCard
import my.noveldokusha.coreui.components.NovelaSettingsTile
import my.noveldokusha.coreui.theme.NovelaRadius
import my.noveldokusha.coreui.theme.NovelaSize
import my.noveldokusha.coreui.theme.NovelaTheme
import my.noveldokusha.network.interceptors.UAPresets
import my.noveldokusha.settings.R

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SettingsNetwork(
    context: Context,
    scraperUserAgent: MutableState<String>,
    cloudflareBypassEnabled: MutableState<Boolean>,
    @Suppress("UNUSED_PARAMETER") cloudflareChallengeTimeoutSeconds: MutableState<Int>,
    massAddDelayMs: State<Long>,
    onMassAddDelayChange: (Long) -> Unit,
    downloadDelayMs: State<Long>,
    onDownloadDelayChange: (Long) -> Unit,
    @Suppress("UNUSED_PARAMETER") onCloudflareBypassChanged: (() -> Unit)? = null,
) {
    val colors = NovelaTheme.colors
    var showUserAgentDialog by remember { mutableStateOf(false) }

    val powerManager = remember { context.getSystemService(Context.POWER_SERVICE) as? PowerManager }
    val isIgnoringBatteryOptimizations = remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isIgnoringBatteryOptimizations.value =
                    powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    NovelaSettingsGroupCard(title = stringResource(id = R.string.network)) {
        // User-Agent setting
        NovelaSettingsTile(
            title = stringResource(R.string.scraper_user_agent),
            subtitle = if (scraperUserAgent.value.isBlank()) {
                stringResource(R.string.default_user_agent)
            } else {
                scraperUserAgent.value.take(52) + if (scraperUserAgent.value.length > 52) "..." else ""
            },
            icon = Icons.Outlined.Http,
            showChevron = true,
            showDivider = true,
            onClick = { showUserAgentDialog = true },
        )

        // Cloudflare bypass toggle
        NovelaSettingsTile(
            title = stringResource(R.string.cloudflare_bypass),
            subtitle = stringResource(R.string.cloudflare_bypass_description),
            icon = Icons.Outlined.Security,
            showDivider = true,
            onClick = { cloudflareBypassEnabled.value = !cloudflareBypassEnabled.value },
            trailing = {
                Switch(
                    checked = cloudflareBypassEnabled.value,
                    onCheckedChange = { cloudflareBypassEnabled.value = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = colors.onAccent,
                        checkedTrackColor = colors.accent,
                        uncheckedThumbColor = colors.textMuted,
                        uncheckedTrackColor = colors.surfaceMuted,
                    ),
                )
            },
        )

        // Download delay setting
        val formattedDownloadDelay = "${downloadDelayMs.value / 1000}.${(downloadDelayMs.value % 1000) / 100} s"
        NovelaSettingsTile(
            title = stringResource(R.string.download_delay),
            subtitle = stringResource(R.string.download_delay_description),
            icon = Icons.Outlined.Schedule,
            showDivider = true,
            onClick = {
                val newDelay = when (downloadDelayMs.value) {
                    500L -> 1000L
                    1000L -> 2000L
                    2000L -> 3000L
                    3000L -> 5000L
                    5000L -> 10000L
                    else -> 500L
                }
                onDownloadDelayChange(newDelay)
            },
            trailing = {
                Surface(
                    shape = RoundedCornerShape(NovelaRadius.pill),
                    color = colors.accentSoft,
                ) {
                    Text(
                        text = formattedDownloadDelay,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.accent,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                    )
                }
            },
        )

        // Mass-add delay setting
        val formattedAddDelay = "${massAddDelayMs.value / 1000}.${(massAddDelayMs.value % 1000) / 100} s"
        NovelaSettingsTile(
            title = stringResource(R.string.add_delay),
            subtitle = stringResource(R.string.add_delay_description, massAddDelayMs.value / 1000),
            icon = Icons.Outlined.Schedule,
            showDivider = true,
            onClick = {
                val newDelay = when (massAddDelayMs.value) {
                    500L -> 1000L
                    1000L -> 2000L
                    2000L -> 3000L
                    3000L -> 5000L
                    else -> 500L
                }
                onMassAddDelayChange(newDelay)
            },
            trailing = {
                Surface(
                    shape = RoundedCornerShape(NovelaRadius.pill),
                    color = colors.accentSoft,
                ) {
                    Text(
                        text = formattedAddDelay,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.accent,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                    )
                }
            },
        )

        // Battery optimization setting
        NovelaSettingsTile(
            title = stringResource(R.string.battery_optimization),
            subtitle = stringResource(
                if (isIgnoringBatteryOptimizations.value) R.string.battery_optimization_disabled
                else R.string.battery_optimization_enabled
            ) + " · " + stringResource(R.string.battery_optimization_description),
            icon = Icons.Outlined.BatterySaver,
            showChevron = true,
            onClick = {
                val intent = if (isIgnoringBatteryOptimizations.value) {
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                } else {
                    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                }
                context.startActivity(intent)
            },
        )
    }

    if (showUserAgentDialog) {
        var tempUserAgent by remember { mutableStateOf(scraperUserAgent.value) }
        val hasInvalidChars = tempUserAgent.isNotBlank() && tempUserAgent.any { it.code > 127 }
        Dialog(onDismissRequest = { showUserAgentDialog = false }) {
            Surface(
                shape = RoundedCornerShape(NovelaRadius.cardLarge),
                color = colors.surfaceElevated,
                border = BorderStroke(NovelaSize.hairline, colors.borderSubtle),
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = stringResource(R.string.scraper_user_agent),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                    )
                    Spacer(Modifier.height(12.dp))

                    val presetEntries = remember { UAPresets.allPresets().entries.toList() }
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        FilterChip(
                            selected = tempUserAgent.isBlank(),
                            onClick = { tempUserAgent = "" },
                            shape = RoundedCornerShape(NovelaRadius.pill),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.accentSoft,
                                selectedLabelColor = colors.accent,
                            ),
                            label = { Text(text = stringResource(R.string.default_user_agent)) },
                        )
                        presetEntries.forEach { (name, ua) ->
                            FilterChip(
                                selected = tempUserAgent == ua,
                                onClick = { tempUserAgent = ua },
                                shape = RoundedCornerShape(NovelaRadius.pill),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = colors.accentSoft,
                                    selectedLabelColor = colors.accent,
                                ),
                                label = { Text(text = name) },
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    OutlinedTextField(
                        value = tempUserAgent,
                        onValueChange = { tempUserAgent = it },
                        label = { Text(stringResource(R.string.user_agent)) },
                        modifier = Modifier.fillMaxWidth(),
                        isError = hasInvalidChars,
                        shape = RoundedCornerShape(NovelaRadius.md),
                        supportingText = {
                            if (hasInvalidChars) {
                                Text(
                                    text = stringResource(R.string.user_agent_invalid_chars),
                                    color = colors.danger,
                                )
                            } else {
                                Text(stringResource(R.string.leave_empty_for_default))
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (hasInvalidChars) colors.danger else colors.accent,
                            unfocusedBorderColor = if (hasInvalidChars) colors.danger else colors.borderSubtle,
                            cursorColor = colors.accent,
                        ),
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = { showUserAgentDialog = false }) {
                            Text(
                                text = stringResource(android.R.string.cancel),
                                color = colors.textSecondary,
                            )
                        }
                        Button(
                            onClick = {
                                scraperUserAgent.value = tempUserAgent.trim()
                                showUserAgentDialog = false
                            },
                            enabled = !hasInvalidChars,
                            shape = RoundedCornerShape(NovelaRadius.pill),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.accent,
                                contentColor = colors.onAccent,
                            ),
                        ) {
                            Text(
                                text = stringResource(android.R.string.ok),
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }
    }
}

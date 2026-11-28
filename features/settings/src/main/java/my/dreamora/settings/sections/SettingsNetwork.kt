package my.dreamora.settings.sections

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Http
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import my.dreamora.settings.ui.SettingsRow
import my.dreamora.coreui.theme.DreamoraSpacing
import my.dreamora.coreui.theme.colorAccent
import my.dreamora.coreui.theme.textPadding
import my.dreamora.network.interceptors.UAPresets
import my.dreamora.settings.R
import my.dreamora.settings.ui.ExpressiveSwitch
import my.dreamora.settings.ui.PremiumSectionHeader
import my.dreamora.settings.ui.PremiumSettingsRow
import androidx.compose.ui.text.font.FontWeight

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
    @Suppress("UNUSED_PARAMETER") onCloudflareBypassChanged: (() -> Unit)? = null
) {
    Column {
        PremiumSectionHeader(
            icon = Icons.Outlined.Public,
            title = stringResource(id = R.string.network),
            summary = stringResource(id = R.string.network_summary),
            accent = MaterialTheme.colorScheme.primary,
        )

        var showUserAgentDialog by remember { mutableStateOf(false) }
        PremiumSettingsRow(
            modifier = Modifier.textPadding().padding(top = DreamoraSpacing.sm),
            headlineContent = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.scraper_user_agent),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (scraperUserAgent.value.isBlank())
                            stringResource(R.string.default_user_agent)
                        else
                            scraperUserAgent.value.take(50) + if (scraperUserAgent.value.length > 50) "..." else "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            leadingContent = {
                Icon(Icons.Outlined.Http, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            onClick = { showUserAgentDialog = true }
        )

        if (showUserAgentDialog) {
            PremiumUserAgentDialog(
                onDismissRequest = { showUserAgentDialog = false },
                currentUserAgent = scraperUserAgent.value,
                onUserAgentChange = { scraperUserAgent.value = it.trim(); showUserAgentDialog = false },
                onCancel = { showUserAgentDialog = false },
            )
        }

        PremiumSettingsRow(
            modifier = Modifier.textPadding(),
            headlineContent = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.cloudflare_bypass),
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.cloudflare_bypass_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    ExpressiveSwitch(
                        checked = cloudflareBypassEnabled.value,
                        onCheckedChange = {
                            cloudflareBypassEnabled.value = it
                        }
                    )
                }
            },
            leadingContent = {
                Icon(Icons.Outlined.Security, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        )

        DelayRow(
            title = stringResource(R.string.download_delay),
            description = stringResource(R.string.download_delay_description),
            value = "${downloadDelayMs.value / 1000}.${(downloadDelayMs.value % 1000) / 100} s",
            icon = Icons.Outlined.Http,
            onClick = {
                val currentDelay = downloadDelayMs.value
                val newDelay = when (currentDelay) {
                    500L -> 1000L
                    1000L -> 2000L
                    2000L -> 3000L
                    3000L -> 5000L
                    5000L -> 10000L
                    else -> 500L
                }
                onDownloadDelayChange(newDelay)
            }
        )

        DelayRow(
            title = stringResource(R.string.add_delay),
            description = stringResource(R.string.add_delay_description, massAddDelayMs.value / 1000),
            value = "",
            icon = Icons.Outlined.Http,
            onClick = {
                val currentDelay = massAddDelayMs.value
                val newDelay = when (currentDelay) {
                    500L -> 1000L
                    1000L -> 2000L
                    2000L -> 3000L
                    3000L -> 5000L
                    else -> 500L
                }
                onMassAddDelayChange(newDelay)
            }
        )

        BatteryOptimizationRow(
            context = context,
            icon = Icons.Outlined.Security,
        )
    }
}

@Composable
private fun DelayRow(
    title: String,
    description: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    PremiumSettingsRow(
        modifier = Modifier.textPadding().clickable { onClick() },
        headlineContent = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (value.isNotBlank()) {
                        Text(
                            text = value,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        },
        leadingContent = {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        },
    )
}

@Composable
private fun BatteryOptimizationRow(
    context: Context,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    val powerManager = remember { context.getSystemService(Context.POWER_SERVICE) as? PowerManager }
    val isIgnoringBatteryOptimizations = remember { mutableStateOf(false) }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                isIgnoringBatteryOptimizations.value =
                    powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    PremiumSettingsRow(
        modifier = Modifier.textPadding(),
        headlineContent = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.battery_optimization),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = stringResource(
                            if (isIgnoringBatteryOptimizations.value) R.string.battery_optimization_disabled
                            else R.string.battery_optimization_enabled
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(R.string.battery_optimization_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        leadingContent = {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        onClick = {
            if (isIgnoringBatteryOptimizations.value) {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                context.startActivity(intent)
            } else {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                context.startActivity(intent)
            }
        }
    )
}

@Composable
private fun PremiumUserAgentDialog(
    onDismissRequest: () -> Unit,
    currentUserAgent: String,
    onUserAgentChange: (String) -> Unit,
    onCancel: () -> Unit,
) {
    var tempUserAgent by remember { mutableStateOf(currentUserAgent) }
    val hasInvalidChars = tempUserAgent.isNotBlank() && tempUserAgent.any { it.code > 127 }
    Dialog(onDismissRequest = onDismissRequest) {
        androidx.compose.material3.Card {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = stringResource(R.string.scraper_user_agent),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = DreamoraSpacing.md),
                    color = MaterialTheme.colorScheme.onSurface
                )

                val presetEntries = remember { UAPresets.allPresets().entries.toList() }
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(DreamoraSpacing.sm),
                ) {
                    FilterChip(
                        selected = tempUserAgent.isBlank(),
                        onClick = { tempUserAgent = "" },
                        label = { Text(text = stringResource(R.string.default_user_agent)) }
                    )
                    presetEntries.forEach { (name, ua) ->
                        FilterChip(
                            selected = tempUserAgent == ua,
                            onClick = { tempUserAgent = ua },
                            label = { Text(text = name) }
                        )
                    }
                }

                Spacer(Modifier.height(DreamoraSpacing.md))

                OutlinedTextField(
                    value = tempUserAgent,
                    onValueChange = { tempUserAgent = it },
                    label = { Text(stringResource(R.string.user_agent), color = MaterialTheme.colorScheme.onSurface) },
                    modifier = Modifier.fillMaxWidth(),
                    isError = hasInvalidChars,
                    supportingText = {
                        if (hasInvalidChars) {
                            Text(
                                text = stringResource(R.string.user_agent_invalid_chars),
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            Text(stringResource(R.string.leave_empty_for_default))
                        }
                    },
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (hasInvalidChars) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                        unfocusedBorderColor = if (hasInvalidChars) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                        cursorColor = MaterialTheme.colorScheme.onSurface
                    )
                )

                androidx.compose.foundation.layout.Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = DreamoraSpacing.md),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onCancel,
                        colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text(stringResource(android.R.string.cancel))
                    }
                    TextButton(
                        onClick = {
                            onUserAgentChange(tempUserAgent)
                        },
                        enabled = !hasInvalidChars,
                        colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text(stringResource(android.R.string.ok))
                    }
                }
            }
        }
    }
}

package my.dreamora.settings.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.BrightnessMedium
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Nightlight
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.material3.ripple
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import my.dreamora.coreui.theme.AppTheme
import my.dreamora.coreui.theme.DreamoraRadius
import my.dreamora.coreui.theme.DreamoraSpacing
import my.dreamora.coreui.theme.colorAccent
import my.dreamora.coreui.theme.DarkMode
import my.dreamora.coreui.theme.HighlightDark
import my.dreamora.coreui.theme.textPadding
import my.dreamora.settings.ui.PremiumSectionHeader
import my.dreamora.settings.ui.PremiumSectionTitle
import my.dreamora.settings.ui.PremiumSettingsRow
import my.dreamora.settings.ui.PremiumChip
import my.dreamora.settings.R
import androidx.compose.ui.text.font.FontWeight

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SettingsTheme(
    currentAppTheme: AppTheme,
    currentDarkMode: DarkMode,
    onAppThemeChange: (AppTheme) -> Unit,
    onDarkModeChange: (DarkMode) -> Unit,
) {
    Column {
        PremiumSectionHeader(
            icon = Icons.Outlined.Palette,
            title = stringResource(id = R.string.theme),
            summary = stringResource(id = R.string.theme_summary),
            accent = colorAccent(),
        )

        // ── Dark mode chips (System/Light/Dark/Black) ─────────────────────────
        PremiumSettingsRow(
            modifier = Modifier.textPadding().padding(top = DreamoraSpacing.sm),
            headlineContent = {
                Column {
                    Text(
                        text = stringResource(id = R.string.dark_mode),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = stringResource(id = R.string.dark_mode_summary),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            trailingContent = {
                // Use a compact FlowRow of toggle chips instead of a switch
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DarkMode.entries.forEach { mode ->
                        val isSelected = mode == currentDarkMode
                        PremiumChip(
                            selected = isSelected,
                            onClick = { onDarkModeChange(mode) },
                            label = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Icon(
                                        imageVector = when (mode) {
                                            DarkMode.SYSTEM -> Icons.Outlined.BrightnessMedium
                                            DarkMode.LIGHT -> Icons.Outlined.LightMode
                                            DarkMode.DARK -> Icons.Outlined.DarkMode
                                            DarkMode.BLACK -> Icons.Outlined.Nightlight
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        text = stringResource(id = mode.titleRes),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                                        ),
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            },
                        )
                    }
                }
            },
        )

        Spacer(Modifier.height(DreamoraSpacing.md))

        // ── Color schemes section ─────────────────────────────────────────────
        PremiumSectionTitle(text = stringResource(id = R.string.color_scheme))

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = DreamoraSpacing.screenH),
            horizontalArrangement = Arrangement.spacedBy(DreamoraSpacing.sm),
        ) {
            items(AppTheme.entries) { theme ->
                PremiumThemeChip(
                    theme = theme,
                    isSelected = theme == currentAppTheme,
                    onClick = { onAppThemeChange(theme) },
                )
            }
        }
        Spacer(Modifier.height(DreamoraSpacing.md))
    }
}

@Composable
private fun PremiumThemeChip(
    theme: AppTheme,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val accentColor = when (theme) {
        AppTheme.DEFAULT -> MaterialTheme.colorScheme.primary
        AppTheme.TACHIYOMI -> HighlightDark
        AppTheme.GREEN_APPLE -> Color(0xFF188140)
        AppTheme.LAVENDER -> Color(0xFFA177FF)
        AppTheme.MIDNIGHT_DUSK -> Color(0xFFF02475)
        AppTheme.STRAWBERRY_DAIQUIRI -> Color(0xFFED4A65)
        AppTheme.TAKO -> Color(0xFFF3B375)
        AppTheme.TEALTURQUOISE -> Color(0xFF40E0D0)
        AppTheme.TIDAL_WAVE -> Color(0xFF5ed4fc)
        AppTheme.YOTSUBA -> Color(0xFFAE3200)
        AppTheme.MONOCHROME -> Color(0xFF000000)
        AppTheme.CATPPUCCIN -> Color(0xFFCBA6F7)
        AppTheme.NORD -> Color(0xFF88C0D0)
        AppTheme.YINYANG -> Color(0xFF000000)
        AppTheme.CLOUDFLARE -> Color(0xFFF38020)
        AppTheme.COTTONCANDY -> Color(0xFFFFCBCB)
        AppTheme.DOOM -> Color(0xFFFF0000)
        AppTheme.MATRIX -> Color(0xFF00FF00)
        AppTheme.MOCHA -> Color(0xFFBF9270)
        AppTheme.SAPPHIRE -> Color(0xFF1E88E5)
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.isPressed.collectAsStateWithLifecycle()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(DreamoraRadius.lg))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true),
                onClick = onClick,
            )
            .width(64.dp)
            .padding(vertical = DreamoraSpacing.xs),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(accentColor)
                .border(
                    width = if (isSelected || isPressed) 2.dp else 0.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        Spacer(Modifier.height(DreamoraSpacing.xs))

        Text(
            text = stringResource(id = theme.titleRes),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

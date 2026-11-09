package my.noveldokusha.settings.sections

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.outlined.BrightnessMedium
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Nightlight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import my.noveldokusha.coreui.components.NovelaSettingsGroupCard
import my.noveldokusha.coreui.theme.AppTheme
import my.noveldokusha.coreui.theme.DarkMode
import my.noveldokusha.coreui.theme.HighlightDark
import my.noveldokusha.coreui.theme.NovelaRadius
import my.noveldokusha.coreui.theme.NovelaSize
import my.noveldokusha.coreui.theme.NovelaTheme
import my.noveldokusha.settings.R

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SettingsTheme(
    currentAppTheme: AppTheme,
    currentDarkMode: DarkMode,
    onAppThemeChange: (AppTheme) -> Unit,
    onDarkModeChange: (DarkMode) -> Unit,
) {
    val colors = NovelaTheme.colors

    NovelaSettingsGroupCard(title = stringResource(id = R.string.theme)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
        ) {
            // Mode pills (System / Light / Dark / Black AMOLED)
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DarkMode.entries.forEach { mode ->
                    val selected = mode == currentDarkMode
                    Surface(
                        onClick = { onDarkModeChange(mode) },
                        shape = RoundedCornerShape(NovelaRadius.pill),
                        color = if (selected) colors.accentSoft else colors.surfaceMuted,
                        border = BorderStroke(
                            NovelaSize.hairline,
                            if (selected) colors.accent.copy(alpha = 0.55f) else colors.borderSubtle,
                        ),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = when (mode) {
                                    DarkMode.SYSTEM -> Icons.Outlined.BrightnessMedium
                                    DarkMode.LIGHT -> Icons.Outlined.LightMode
                                    DarkMode.DARK -> Icons.Outlined.DarkMode
                                    DarkMode.BLACK -> Icons.Outlined.Nightlight
                                },
                                contentDescription = null,
                                tint = if (selected) colors.accent else colors.textSecondary,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = stringResource(id = mode.titleRes),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (selected) colors.accent else colors.textSecondary,
                            )
                        }
                    }
                }
            }

            HorizontalDivider(
                thickness = NovelaSize.hairline,
                color = colors.borderSubtle,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            )

            // Color schemes section header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(id = R.string.color_scheme),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(id = currentAppTheme.titleRes),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.accent,
                )
            }

            Spacer(Modifier.height(10.dp))

            // 20-swatch horizontal theme picker
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(AppTheme.entries) { theme ->
                    ThemePreviewChip(
                        theme = theme,
                        isSelected = theme == currentAppTheme,
                        onClick = { onAppThemeChange(theme) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemePreviewChip(
    theme: AppTheme,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val colors = NovelaTheme.colors
    val accentColor = when (theme) {
        AppTheme.DEFAULT -> Color(0xFFE8593C)
        AppTheme.TACHIYOMI -> HighlightDark
        AppTheme.GREEN_APPLE -> Color(0xFF188140)
        AppTheme.LAVENDER -> Color(0xFFA177FF)
        AppTheme.MIDNIGHT_DUSK -> Color(0xFFF02475)
        AppTheme.STRAWBERRY_DAIQUIRI -> Color(0xFFED4A65)
        AppTheme.TAKO -> Color(0xFFF3B375)
        AppTheme.TEALTURQUOISE -> Color(0xFF40E0D0)
        AppTheme.TIDAL_WAVE -> Color(0xFF5ED4FC)
        AppTheme.YOTSUBA -> Color(0xFFAE3200)
        AppTheme.MONOCHROME -> Color(0xFF6B7280)
        AppTheme.CATPPUCCIN -> Color(0xFFCBA6F7)
        AppTheme.NORD -> Color(0xFF88C0D0)
        AppTheme.YINYANG -> Color(0xFF374151)
        AppTheme.CLOUDFLARE -> Color(0xFFF38020)
        AppTheme.COTTONCANDY -> Color(0xFFFF8DA1)
        AppTheme.DOOM -> Color(0xFFEF4444)
        AppTheme.MATRIX -> Color(0xFF10B981)
        AppTheme.MOCHA -> Color(0xFFBF9270)
        AppTheme.SAPPHIRE -> Color(0xFF1E88E5)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(NovelaRadius.md))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                onClick = onClick,
            )
            .padding(vertical = 4.dp)
            .width(58.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(accentColor)
                .border(
                    width = if (isSelected) 2.5.dp else 1.dp,
                    color = if (isSelected) colors.textPrimary else colors.borderSubtle,
                    shape = CircleShape,
                ),
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        Spacer(Modifier.height(5.dp))

        Text(
            text = stringResource(id = theme.titleRes),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) colors.accent else colors.textMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

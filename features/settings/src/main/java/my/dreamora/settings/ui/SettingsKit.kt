package my.dreamora.settings.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import my.dreamora.coreui.theme.DreamoraMotion
import my.dreamora.coreui.theme.DreamoraRadius
import my.dreamora.coreui.theme.DreamoraSpacing
import my.dreamora.coreui.theme.DreamoraTheme
import my.dreamora.coreui.theme.DreamoraType

/**
 * Premium Settings design vocabulary.
 *
 * Design principles:
 *   • Every card carries a 1dp hairline border + subtle tonal elevation — never flat.
 *   • Section headers carry a colored left accent (the group's hue) + an underline.
 *   • Icons live in tinted surfaces; color appears exactly once per row.
 *   • Switches use gradient thumbs so state is readable without track color.
 *   • Chips carry borders; selected chips fill with accent + white text.
 *   • Typography uses the editorial scale: section titles are semibold, body is medium.
 *   • Whitespace is generous: sections breathe, rows have vertical rhythm.
 */

enum class SettingsGroup {
    Appearance,
    Reading,
    Layout,
    Stats,
    Library,
    Sources,
    Storage,
    Backup,
    Translation,
    Network,
    Support,
    ;

    companion object {
        fun fromKey(key: String?): SettingsGroup? = entries.firstOrNull {
            it.name.equals(key?.trim().orEmpty(), ignoreCase = true)
        }
    }
}

data class SettingsGroupSpec(
    val group: SettingsGroup,
    val title: String,
    val summary: String,
    val icon: ImageVector,
    val keywords: List<String>,
    val content: @Composable () -> Unit,
)

/** Premium card shape: softer than stock M3. */
private val PremiumCardShape = RoundedCornerShape(DreamoraRadius.xxl)

/** Hairline used for card borders and row separators. */
private val PremiumBorder = BorderStroke(1.dp, DreamoraTheme.colors.hairline)

/** The row's single note of colour — derived from the group name, rotated through the scheme. */
@Composable
fun SettingsGroup.accent(): Color {
    val scheme = MaterialTheme.colorScheme
    val palette = listOf(scheme.primary, scheme.tertiary, scheme.secondary, scheme.error)
    val index = (name.hashCode() and Int.MAX_VALUE) % palette.size
    return palette[index]
}

/** Gradient accent used behind hero titles and active chips. */
@Composable
private fun SettingsGroup.accentGradient(): Brush {
    val scheme = MaterialTheme.colorScheme
    val c = accent()
    return Brush.linearGradient(
        colors = listOf(
            c.copy(alpha = if (DreamoraTheme.colors.isLight) 0.12f else 0.20f),
            Color.Transparent,
        ),
    )
}

/** The tinted circle behind a group icon — premium version with subtle border. */
@Composable
private fun PremiumGroupIcon(icon: ImageVector, accent: Color) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(
                accent.copy(alpha = if (DreamoraTheme.colors.isLight) 0.10f else 0.16f),
            )
            .border(
                width = 1.dp,
                color = accent.copy(alpha = if (DreamoraTheme.colors.isLight) 0.25f else 0.35f),
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(22.dp),
        )
    }
}

/**
 * Premium group card for the hub (tappable row) and expanded group screens.
 * Carries a hairline border + tonal elevation. The icon circle has a subtle border.
 */
@Composable
fun PremiumSettingsCard(
    title: String,
    icon: ImageVector,
    accent: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier,
    summary: String? = null,
    visible: Boolean = true,
    expanded: Boolean = true,
    showHeader: Boolean = true,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed = interactionSource.isPressed.value

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(DreamoraMotion.STANDARD)) + expandVertically(
            animationSpec = tween(DreamoraMotion.EMPHASIZED, easing = DreamoraMotion.EnterEasing),
            expandFrom = Alignment.Top,
        ),
        exit = fadeOut(tween(DreamoraMotion.INSTANT)) + shrinkVertically(
            animationSpec = tween(DreamoraMotion.STANDARD, easing = DreamoraMotion.ExitEasing),
        ),
        modifier = modifier,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = DreamoraSpacing.screenH,
                    vertical = DreamoraSpacing.xs,
                )
                .then(
                    if (onClick != null)
                        Modifier.clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = onClick,
                        )
                    else Modifier,
                )
                .then(
                    Modifier
                        .background(
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            shape = PremiumCardShape,
                        )
                        .border(PremiumBorder, PremiumCardShape)
                ),
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = if (isPressed) 0.dp else 1.dp,
        ) {
            Column {
                if (showHeader) Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = DreamoraSpacing.lg,
                            end = DreamoraSpacing.lg,
                            top = DreamoraSpacing.md,
                            bottom = DreamoraSpacing.sm,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PremiumGroupIcon(icon = icon, accent = accent)
                    Spacer(Modifier.width(DreamoraSpacing.md))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (!summary.isNullOrBlank()) {
                            Text(
                                text = summary,
                                style = DreamoraType.bookMeta,
                                color = DreamoraTheme.colors.textMuted,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    if (onClick != null) {
                        PremiumCaret(isPressed = isPressed)
                    }
                }
                if (expanded) {
                    if (showHeader) Box(
                        Modifier
                            .padding(horizontal = DreamoraSpacing.lg)
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(DreamoraTheme.colors.hairline),
                    )
                    if (!showHeader) Spacer(Modifier.height(DreamoraSpacing.sm))
                    content()
                    Spacer(Modifier.height(DreamoraSpacing.sm))
                }
            }
        }
    }
}

/** Trailing chevron with pressed-state tint. */
@Composable
private fun PremiumCaret(isPressed: Boolean = false) {
    Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        tint = if (isPressed) MaterialTheme.colorScheme.primary else DreamoraTheme.colors.textMuted,
        modifier = Modifier.size(22.dp),
    )
}

/**
 * Premium section header: a colored left accent bar + title + optional summary.
 * Used at the top of every settings group screen and inside group cards.
 */
@Composable
fun PremiumSectionHeader(
    icon: ImageVector,
    title: String,
    summary: String?,
    accent: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = DreamoraSpacing.screenH)
            .padding(top = DreamoraSpacing.sm, bottom = DreamoraSpacing.md),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(accent, accent.copy(alpha = 0f)),
                        ),
                    ),
            )
            Spacer(Modifier.width(DreamoraSpacing.sm))
            PremiumGroupIcon(icon = icon, accent = accent)
            Spacer(Modifier.width(DreamoraSpacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!summary.isNullOrBlank()) {
                    Spacer(Modifier.height(DreamoraSpacing.xxs))
                    Text(
                        text = summary,
                        style = DreamoraType.bookMeta,
                        color = DreamoraTheme.colors.textMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(DreamoraTheme.colors.hairline)
                .padding(top = DreamoraSpacing.md),
        )
    }
}

/**
 * Premium search field: an outlined pill with a subtle border, always visible.
 */
@Composable
fun PremiumSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = DreamoraSpacing.screenH, vertical = DreamoraSpacing.xs),
        shape = RoundedCornerShape(DreamoraRadius.pill),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .padding(start = DreamoraSpacing.md, end = DreamoraSpacing.xs)
                .border(
                    BorderStroke(1.dp, DreamoraTheme.colors.hairline),
                    RoundedCornerShape(DreamoraRadius.pill),
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = DreamoraTheme.colors.textMuted,
                modifier = Modifier.size(20.dp),
            )
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = DreamoraSpacing.sm, vertical = 15.dp),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) {
                            Text(
                                text = placeholder ?: "Search settings",
                                style = MaterialTheme.typography.bodyMedium,
                                color = DreamoraTheme.colors.textMuted,
                            )
                        }
                        inner()
                    }
                },
            )
            if (query.isNotEmpty()) {
                IconButton(onClick = onClear, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Clear",
                        modifier = Modifier.size(18.dp),
                        tint = DreamoraTheme.colors.textMuted,
                    )
                }
            }
        }
    }
}

/**
 * Premium settings row inside a group card: tinted icon surface, headline, supporting line,
 * optional trailing content. Every section uses this for consistent rhythm.
 */
@Composable
fun PremiumSettingsRow(
    modifier: Modifier = Modifier,
    headlineContent: @Composable () -> Unit,
    supportingContent: (@Composable () -> Unit)? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    accent: Color = MaterialTheme.colorScheme.primary,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed = interactionSource.isPressed.value

    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp)
            .then(
                if (onClick != null)
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick,
                    )
                else Modifier,
            )
            .padding(horizontal = DreamoraSpacing.md, vertical = DreamoraSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingContent != null) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(DreamoraRadius.xl))
                    .background(
                        accent.copy(alpha = if (DreamoraTheme.colors.isLight) 0.08f else 0.14f),
                    )
                    .border(
                        BorderStroke(1.dp, accent.copy(alpha = if (DreamoraTheme.colors.isLight) 0.20f else 0.28f)),
                        RoundedCornerShape(DreamoraRadius.xl),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                leadingContent()
            }
            Spacer(Modifier.width(DreamoraSpacing.sm))
        }

        Column(Modifier.weight(1f)) {
            headlineContent()
            if (supportingContent != null) {
                Spacer(Modifier.height(2.dp))
                supportingContent()
            }
        }

        if (trailingContent != null) {
            Box(Modifier.padding(start = 12.dp)) {
                trailingContent()
            }
        }
    }
}

/** Premium section title inside a card — uppercase, tracked, muted. */
@Composable
fun PremiumSectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = DreamoraType.badge,
        color = DreamoraTheme.colors.textMuted,
        modifier = modifier.padding(
            start = DreamoraSpacing.md,
            top = DreamoraSpacing.sm,
            bottom = DreamoraSpacing.xxs,
        ),
    )
}

/** Premium switch: custom gradient thumb, check/cross for color-blind safety. */
@Composable
fun PremiumSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed = interactionSource.isPressed.value

    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        modifier = modifier
            .then(if (enabled) Modifier.clickable(interactionSource = interactionSource, indication = null) else Modifier),
        thumbContent = {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(
                        if (checked) {
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary,
                                ),
                            )
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (checked) Icons.Filled.Check else Icons.Filled.Close,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = Color.White,
                )
            }
        },
    )
}

/** Backwards-compatible alias for [PremiumSwitch]. */
@Composable
fun ExpressiveSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    PremiumSwitch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
    )
}

/** Premium outlined chip: border + fill on select. */
@Composable
fun PremiumChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    accent: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed = interactionSource.isPressed.value

    Box(
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .clip(RoundedCornerShape(DreamoraRadius.pill))
            .background(
                if (selected) accent else MaterialTheme.colorScheme.surfaceContainerHighest,
            )
            .border(
                if (selected) BorderStroke(1.dp, accent) else PremiumBorder,
                RoundedCornerShape(DreamoraRadius.pill),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            label()
        }
    }
}

/** Premium filter chip row: label + chips arranged horizontally. */
@Composable
fun PremiumFilterRow(
    title: String,
    selected: Any,
    choices: List<Pair<String, Any>>,
    onSelect: (Any) -> Unit,
    accent: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DreamoraSpacing.md),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            modifier = Modifier.weight(1f),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            choices.forEach { (label, value) ->
                PremiumChip(
                    selected = value == selected,
                    onClick = { onSelect(value) },
                    accent = accent,
                    label = { Text(text = label, style = MaterialTheme.typography.labelSmall) },
                )
            }
        }
    }
}

/** Padding used by every section composable that draws its own rows inside a card. */
val PremiumRowPadding = PaddingValues(
    start = DreamoraSpacing.md,
    end = DreamoraSpacing.lg,
    top = DreamoraSpacing.xs,
    bottom = DreamoraSpacing.xs,
)

/** The group's own title, for its screen header. */
@Composable
fun SettingsGroup.label(): String = stringResource(
    when (this) {
        SettingsGroup.Appearance -> my.dreamora.strings.R.string.settings_group_appearance
        SettingsGroup.Reading -> my.dreamora.strings.R.string.settings_group_reading
        SettingsGroup.Layout -> my.dreamora.strings.R.string.settings_group_layout
        SettingsGroup.Stats -> my.dreamora.strings.R.string.settings_group_stats
        SettingsGroup.Library -> my.dreamora.strings.R.string.settings_group_library
        SettingsGroup.Sources -> my.dreamora.strings.R.string.settings_group_sources
        SettingsGroup.Storage -> my.dreamora.strings.R.string.settings_group_data
        SettingsGroup.Backup -> my.dreamora.strings.R.string.settings_group_backup
        SettingsGroup.Translation -> my.dreamora.strings.R.string.settings_group_translation
        SettingsGroup.Network -> my.dreamora.strings.R.string.settings_group_network
        SettingsGroup.Support -> my.dreamora.strings.R.string.settings_group_about
    },
)

/** One line under the title: what to expect inside. */
@Composable
fun SettingsGroup.hint(): String = stringResource(
    when (this) {
        SettingsGroup.Appearance -> my.dreamora.strings.R.string.settings_group_appearance_hint
        SettingsGroup.Reading -> my.dreamora.strings.R.string.settings_group_reading_hint
        SettingsGroup.Layout -> my.dreamora.strings.R.string.settings_group_layout_hint
        SettingsGroup.Stats -> my.dreamora.strings.R.string.settings_group_stats_hint
        SettingsGroup.Library -> my.dreamora.strings.R.string.settings_group_library_hint
        SettingsGroup.Sources -> my.dreamora.strings.R.string.settings_group_sources_hint
        SettingsGroup.Storage -> my.dreamora.strings.R.string.settings_group_data_hint
        SettingsGroup.Backup -> my.dreamora.strings.R.string.settings_group_backup_hint
        SettingsGroup.Translation -> my.dreamora.strings.R.string.settings_group_translation_hint
        SettingsGroup.Network -> my.dreamora.strings.R.string.settings_group_network_hint
        SettingsGroup.Support -> my.dreamora.strings.R.string.settings_group_about_hint
    },
)

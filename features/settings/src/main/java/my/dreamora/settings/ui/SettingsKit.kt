package my.dreamora.settings.ui

import androidx.compose.ui.res.stringResource
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
 * The Settings "kit": the vocabulary the settings screens are built from.
 *
 * The look follows Android 16 / Pixel Settings: a large plain title instead of a
 * greeting, one search pill under it, and every group of options as a single flat,
 * generously rounded card — no borders, no gradients, no badges. Colour appears
 * exactly once per row, in the circle behind the icon; everything else is surface
 * and ink.
 *
 * Nothing here re-implements behaviour: the section composables keep their own state
 * and view-model wiring, they are just placed inside a card and filtered as a group.
 */

/**
 * A settings group.
 *
 * The Settings screen renders every spec as a *row* in one menu card (tap → open),
 * a group screen renders the same spec *expanded*, with its sections inside — so a
 * setting cannot exist on one level and be missing on the other.
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

/** The corner every settings card shares: Pixel's large, soft radius. */
private val SettingsCardShape = RoundedCornerShape(DreamoraRadius.xxl)

/**
 * The row's single note of colour, for the circle behind its icon. Derived from the
 * group's own name, so a group keeps its hue across lists and rebuilds, and the
 * active theme decides which four hues are in rotation.
 */
@Composable
fun SettingsGroup.accent(): Color {
    val scheme = MaterialTheme.colorScheme
    val palette = listOf(scheme.primary, scheme.tertiary, scheme.secondary, scheme.error)
    val index = (name.hashCode() and Int.MAX_VALUE) % palette.size
    return palette[index]
}

/**
 * One group of options as a single flat card. A group screen shows exactly one of
 * these, expanded, with its sections inside; [showHeader] drops the title row when
 * the screen's hero already says the name.
 */
@Composable
fun SettingsGroupCard(
    title: String,
    icon: ImageVector,
    accent: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier,
    summary: String? = null,
    visible: Boolean = true,
    expanded: Boolean = true,
    /** A group screen already shows its title in the hero: the card then drops its own. */
    showHeader: Boolean = true,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
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
                    // the whole box is the tap target once it opens a screen of its own
                    if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
                ),
            shape = SettingsCardShape,
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 0.dp,
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
                    GroupIcon(icon = icon, accent = accent)
                    Spacer(Modifier.width(DreamoraSpacing.md))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
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
                        SettingsCaret()
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

/**
 * The top of a settings group screen: the group's name, large and plain, with one
 * line under it saying what is inside. The app bar above carries only the back
 * arrow, so the name is said once — no wash, no icon tile, no decoration.
 */
@Composable
fun SettingsGroupHero(
    icon: ImageVector,
    title: String,
    summary: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = DreamoraSpacing.screenH)
            .padding(top = DreamoraSpacing.sm, bottom = DreamoraSpacing.md),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
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

/** The tinted circle behind a group icon — the row's single note of colour. */
@Composable
private fun GroupIcon(icon: ImageVector, accent: Color) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(accent.copy(alpha = if (DreamoraTheme.colors.isLight) 0.14f else 0.22f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(20.dp),
        )
    }
}

/**
 * Search field for the settings page: a quiet pill under the title, always visible,
 * like the one Pixel Settings opens with.
 */
@Composable
fun SettingsSearchField(
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
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier.padding(start = DreamoraSpacing.lg, end = DreamoraSpacing.xs),
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
                    .padding(horizontal = DreamoraSpacing.sm, vertical = 14.dp),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) {
                            Text(
                                text = placeholder ?: stringResource(my.dreamora.strings.R.string.action_search_settings),
                                style = MaterialTheme.typography.bodyMedium,
                                color = DreamoraTheme.colors.textMuted,
                            )
                        }
                        inner()
                    }
                },
            )
            if (query.isNotEmpty()) {
                IconButton(onClick = onClear, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(my.dreamora.strings.R.string.clear),
                        modifier = Modifier.size(16.dp),
                        tint = DreamoraTheme.colors.textMuted,
                    )
                }
            }
        }
    }
}

/**
 * The hub's menu: one flat card whose rows are the settings groups, separated by
 * hairlines that stop at the icon — the way Pixel Settings groups its entries.
 */
@Composable
fun SettingsMenuCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = DreamoraSpacing.screenH, vertical = DreamoraSpacing.xs),
        shape = SettingsCardShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 0.dp,
    ) {
        Column(content = content)
    }
}

/** One entry of the hub's menu: tinted circle icon, title, one-line summary, caret. */
@Composable
fun SettingsMenuRow(
    icon: ImageVector,
    title: String,
    summary: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(SettingsCardShape)
            .clickable(onClick = onClick)
            .padding(
                horizontal = DreamoraSpacing.lg,
                vertical = DreamoraSpacing.md,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DreamoraSpacing.md),
    ) {
        GroupIcon(icon = icon, accent = accent)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(1.dp))
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = DreamoraTheme.colors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        SettingsCaret()
    }
}

/** The hairline between two rows of a [SettingsMenuCard], inset past the icon. */
@Composable
fun SettingsMenuDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(start = DreamoraSpacing.lg + 38.dp + DreamoraSpacing.md)
            .fillMaxWidth()
            .height(1.dp)
            .background(DreamoraTheme.colors.hairline),
    )
}

/** Trailing chevron that marks a row as "opens another screen". */
@Composable
fun SettingsCaret(modifier: Modifier = Modifier) {
    Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        tint = DreamoraTheme.colors.textMuted,
        modifier = modifier.size(20.dp),
    )
}

/**
 * Expressive switch: the thumb carries a check or a cross, so state is readable
 * without looking at the track color (and color-blind safe).
 */
@Composable
fun ExpressiveSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        modifier = modifier,
        thumbContent = {
            Icon(
                imageVector = if (checked) Icons.Filled.Check else Icons.Filled.Close,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (checked) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.outline
                },
            )
        },
    )
}

/** Padding used by every section composable that draws its own rows inside a card. */
val SettingsRowPadding = PaddingValues(
    start = DreamoraSpacing.md,
    end = DreamoraSpacing.lg,
    top = DreamoraSpacing.xs,
    bottom = DreamoraSpacing.xs,
)

/** A section title inside a card — used where a group has two blocks of rows. */
@Composable
fun SettingsInnerLabel(text: String, modifier: Modifier = Modifier) {
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

/** The group's own title, for its screen header — the pre-existing group labels. */
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

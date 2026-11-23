package my.dreamora.settings.ui

import androidx.compose.ui.res.stringResource
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import my.dreamora.coreui.theme.DreamoraMotion
import my.dreamora.coreui.components.DreamoraIconTile
import my.dreamora.coreui.theme.DreamoraRadius
import my.dreamora.coreui.theme.DreamoraSpacing
import my.dreamora.coreui.theme.DreamoraTheme
import my.dreamora.coreui.theme.DreamoraType
import my.dreamora.settings.ui.ExpressiveSwitch

/**
 * The Settings "kit": the vocabulary the settings rebrand is built from.
 *
 * Layout language (Material 3 Expressive / Android 16 settings): every group of
 * related options is its own card, each card is labelled by a tinted icon chip and a
 * title, and a single search field narrows the page down to the groups that match —
 * with 20+ switches on one screen, "find it" beats "scroll to it".
 *
 * Nothing here re-implements behaviour: the section composables keep their own state
 * and view-model wiring, they are just placed inside a card and filtered as a group.
 */

/**
 * A settings group.
 *
 * [expandSections] is what makes the two-level navigation possible without duplicating
 * the section list: the Settings screen renders every spec as a *header-only* card
 * (tap → open), a group screen renders the same specs *expanded*, with their sections
 * inside. A spec that is not expanded draws no divider and no body, so it is exactly the
 * tappable "box" the design asks for.
 */
enum class SettingsGroup {
    Appearance,
    Reading,
    Layout,
    Stats,
    Library,
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

@Composable
fun SettingsGroupCard(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    summary: String? = null,
    visible: Boolean = true,
    expanded: Boolean = true,
    /** A group screen already shows its title in the app bar: the card then drops its own. */
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
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 0.dp,
        ) {
            Column {
                if (showHeader) Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = DreamoraSpacing.md,
                            end = DreamoraSpacing.lg,
                            top = DreamoraSpacing.md,
                            bottom = DreamoraSpacing.xs,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DreamoraIconTile(icon = icon)
                    Spacer(Modifier.width(DreamoraSpacing.md))
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .then(
                                if (onClick != null) {
                                    Modifier
                                        .clip(DreamoraRadius.cardShape)
                                        .clickable(onClick = onClick)
                                } else Modifier,
                            ),
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
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
                            .padding(horizontal = DreamoraSpacing.md)
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    )
                    if (!showHeader) Spacer(Modifier.height(DreamoraSpacing.xs))
                    content()
                    Spacer(Modifier.height(DreamoraSpacing.sm))
                }
            }
        }
    }
}

/**
 * The top of a settings group screen: the group's icon on a primary tile, its name and what is
 * inside. The app bar above it carries only the back arrow, so the name is said once.
 */
@Composable
fun SettingsGroupHero(
    icon: ImageVector,
    title: String,
    summary: String?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = DreamoraSpacing.screenH)
            .padding(top = DreamoraSpacing.xs, bottom = DreamoraSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DreamoraSpacing.md),
    ) {
        DreamoraIconTile(icon = icon, size = 52.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (!summary.isNullOrBlank()) {
                Spacer(Modifier.height(DreamoraSpacing.xxs))
                Text(
                    text = summary,
                    style = DreamoraType.bookMeta,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * Search field for the settings page. Suggestions are shown before the first
 * keystroke (recent/helpful queries) so the field is also a table of contents.
 */
@Composable
fun SettingsSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    suggestions: List<String> = emptyList(),
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = DreamoraSpacing.screenH, vertical = DreamoraSpacing.xs),
        shape = RoundedCornerShape(DreamoraRadius.pill),
        color = DreamoraTheme.colors.elevatedSurface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (query.isEmpty()) DreamoraTheme.colors.hairline else MaterialTheme.colorScheme.primary,
        ),
    ) {
        Column {
            Row(
                modifier = Modifier.padding(start = DreamoraSpacing.md, end = DreamoraSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = if (query.isEmpty()) Icons.Outlined.Search else Icons.Filled.Search,
                    contentDescription = null,
                    tint = DreamoraTheme.colors.textMuted,
                    modifier = Modifier.size(18.dp),
                )
                androidx.compose.foundation.text.BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = DreamoraSpacing.sm, vertical = 12.dp),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
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
            AnimatedVisibility(
                visible = query.isEmpty() && suggestions.isNotEmpty(),
                enter = fadeIn(tween(DreamoraMotion.STANDARD)) + expandVertically(tween(DreamoraMotion.STANDARD)),
                exit = fadeOut(tween(DreamoraMotion.INSTANT)) + shrinkVertically(tween(DreamoraMotion.STANDARD)),
            ) {
                Row(
                    modifier = Modifier.padding(
                        start = DreamoraSpacing.md,
                        end = DreamoraSpacing.md,
                        bottom = DreamoraSpacing.sm,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(DreamoraSpacing.xs),
                ) {
                    suggestions.take(4).forEach { label ->
                        SuggestionPill(label) { onQueryChange(label) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionPill(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        style = DreamoraType.badge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .clip(RoundedCornerShape(DreamoraRadius.pill))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
            .clickable(onClick = onClick)
            .padding(horizontal = DreamoraSpacing.sm, vertical = 5.dp),
    )
}

/** Trailing chevron that marks a row as "opens another screen". */
@Composable
fun SettingsCaret(modifier: Modifier = Modifier) {
    Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        tint = DreamoraTheme.colors.textMuted,
        modifier = modifier.size(18.dp),
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
        SettingsGroup.Storage -> my.dreamora.strings.R.string.settings_group_data_hint
        SettingsGroup.Backup -> my.dreamora.strings.R.string.settings_group_backup_hint
        SettingsGroup.Translation -> my.dreamora.strings.R.string.settings_group_translation_hint
        SettingsGroup.Network -> my.dreamora.strings.R.string.settings_group_network_hint
        SettingsGroup.Support -> my.dreamora.strings.R.string.settings_group_about_hint
    },
)

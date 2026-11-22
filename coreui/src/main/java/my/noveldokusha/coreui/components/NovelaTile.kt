package my.noveldokusha.coreui.components

import androidx.compose.material.icons.Icons
import my.noveldokusha.coreui.R
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import my.noveldokusha.coreui.theme.NovelaRadius
import my.noveldokusha.coreui.theme.NovelaSpacing
import my.noveldokusha.coreui.theme.NovelaTheme
import my.noveldokusha.coreui.theme.NovelaType

/**
 * The "tile" language: a gradient-wrapped box with a raised icon chip, a name, and a
 * footer of small facts plus a round action. Used by the Browse grid (one source per
 * tile) and by the extension cards, so both screens read as one design instead of a
 * stock list.
 *
 * Colour comes from the active scheme — never from a literal — so a theme change repaints
 * every tile, and a tile's hue is derived from the item's own id, which keeps a source
 * the same colour across lists and rebuilds.
 */

/** Four scheme colours, cycled by a stable hash of [seed]. */
@Composable
fun novelaTileAccent(seed: String): Color {
    val scheme = MaterialTheme.colorScheme
    val palette = listOf(scheme.primary, scheme.tertiary, scheme.secondary, scheme.error)
    val index = (seed.hashCode() and Int.MAX_VALUE) % palette.size
    return palette[index]
}

/** Diagonal wash of [accent] over the surface, stronger at the top-left. */
@Composable
fun Modifier.novelaTileBackground(accent: Color): Modifier {
    val light = NovelaTheme.colors.isLight
    return this.background(
        Brush.linearGradient(
            colors = listOf(
                accent.copy(alpha = if (light) 0.16f else 0.22f),
                accent.copy(alpha = if (light) 0.04f else 0.06f),
                Color.Transparent,
            ),
        ),
    )
}

/** The rounded square an icon or image sits in — the card's focal point. */
@Composable
fun NovelaIconChip(
    accent: Color,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 44.dp,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(NovelaTile.chipShape)
            .background(accent.copy(alpha = 0.18f))
            .border(1.dp, accent.copy(alpha = 0.35f), NovelaTile.chipShape)
            .padding(6.dp),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
fun NovelaTileCard(
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    overlay: @Composable (() -> Unit)? = null,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Surface(
        onClick = { onClick?.invoke() },
        modifier = modifier
            .fillMaxWidth()
            .clip(NovelaRadius.cardShape)
            .novelaTileBackground(accent),
        shape = NovelaRadius.cardShape,
        color = NovelaTheme.colors.elevatedSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, NovelaTheme.colors.hairline),
    ) {
        Box {
            Column(
                modifier = Modifier.padding(NovelaSpacing.md),
                verticalArrangement = Arrangement.spacedBy(NovelaSpacing.xs),
                content = content,
            )
            overlay?.let {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(NovelaSpacing.xxs),
                    horizontalArrangement = Arrangement.spacedBy(NovelaSpacing.xxs),
                    verticalAlignment = Alignment.CenterVertically,
                    content = { it() },
                )
            }
        }
    }
}

/** Title block used above a tile grid: label on the left, count/action on the right. */
@Composable
fun NovelaTileSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    count: Int? = null,
    action: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = NovelaSpacing.screenH)
            .padding(top = NovelaSpacing.md, bottom = NovelaSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (count != null) "$title · $count" else title,
            style = NovelaType.sectionTitle,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        action?.invoke()
    }
}

/** A small tinted fact label: "Novel", "en", "57 genres". */
@Composable
fun NovelaTileBadge(
    text: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier
            .clip(NovelaRadius.badgeShape)
            .background(accent.copy(alpha = 0.16f))
            .padding(horizontal = NovelaSpacing.xs, vertical = 1.dp),
        style = MaterialTheme.typography.labelSmall,
        color = accent,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/**
 * Screen-opening header: icon chip, big title, one-line subtitle, optional trailing
 * count pill. The "Browse / Discover novels from your sources / 10 sources" block.
 */
@Composable
fun NovelaHeroHeader(
    title: String,
    subtitle: String?,
    accent: Color,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
    pill: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = NovelaSpacing.screenH, vertical = NovelaSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(NovelaSpacing.md),
    ) {
        if (icon != null) {
            NovelaIconChip(accent = accent) { icon() }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = NovelaType.bookMeta,
                    color = NovelaTheme.colors.textMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        pill?.invoke()
    }
}

/** The count pill that sits at the end of a hero header. */
@Composable
fun NovelaHeroPill(
    value: String,
    label: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(NovelaRadius.chipShape)
            .background(accent.copy(alpha = 0.16f))
            .border(1.dp, accent.copy(alpha = 0.30f), NovelaRadius.chipShape)
            .padding(horizontal = NovelaSpacing.md, vertical = NovelaSpacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = accent,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = NovelaTheme.colors.textMuted,
            maxLines = 1,
        )
    }
}

/** Shared shapes for the tile language, kept here so tiles and chips agree. */
object NovelaTile {
    /** The icon chip: a squircle between a badge and a card corner. */
    val chipShape = RoundedCornerShape(NovelaRadius.md)
}

/**
 * The action row of a tile: a filled pill button whose tint comes from the tile's own
 * accent. Used where a card needs a verb ("Install", "Update") instead of a chevron.
 */
@Composable
fun NovelaTileActionButton(
    label: String,
    onClick: () -> Unit,
    accent: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable () -> Unit = {},
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = NovelaRadius.chipShape,
        color = accent.copy(alpha = if (enabled) 0.16f else 0.08f),
        contentColor = if (enabled) accent else NovelaTheme.colors.textMuted,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = NovelaSpacing.sm, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(NovelaSpacing.xxs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon()
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** The compact round sibling of [NovelaTileActionButton], for icon-only verbs. */
@Composable
fun NovelaTileIconButton(
    contentDescription: String?,
    onClick: () -> Unit,
    accent: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(32.dp),
        shape = CircleShape,
        color = accent.copy(alpha = 0.12f),
        contentColor = accent,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) { content() }
    }
}

/** A circle for an icon, as used by the row groups (the tile language uses [NovelaIconChip]). */
@Composable
fun NovelaCircleIcon(
    accent: Color,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 40.dp,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(accent.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** One number + its label, for the little summary row under a hero header. */
@Composable
fun NovelaStatCard(
    value: String,
    label: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = NovelaRadius.cardShape,
        color = NovelaTheme.colors.elevatedSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, NovelaTheme.colors.hairline),
    ) {
        Column(modifier = Modifier.padding(horizontal = NovelaSpacing.sm, vertical = NovelaSpacing.md)) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = accent,
                maxLines = 1,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = NovelaTheme.colors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * A labelled group of rows in one card — the "Menu"/"About" block shape: rows separated
 * by hairlines instead of floating as individual cards, so a group reads as one object.
 */
@Composable
fun NovelaCardGroup(
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = NovelaSpacing.screenH, vertical = NovelaSpacing.xxs),
        shape = NovelaRadius.cardShape,
        color = NovelaTheme.colors.elevatedSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, NovelaTheme.colors.hairline),
    ) {
        Column(content = content)
    }
}

/** The hairline between two rows of a [NovelaCardGroup]. */
@Composable
fun NovelaCardGroupDivider(modifier: Modifier = Modifier) {
    androidx.compose.material3.HorizontalDivider(
        modifier = modifier.padding(start = NovelaSpacing.lg),
        thickness = 0.5.dp,
        color = NovelaTheme.colors.hairline,
    )
}

/**
 * The greeting the "More" screen opens with. Deliberately computed from the wall clock
 * rather than stored: it is a one-line pleasantry, and a stale greeting on a screen that
 * is re-opened all day reads as a bug.
 */
@Composable
fun novelaGreeting(): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return stringResource(
        when (hour) {
            in 5..11 -> R.string.greeting_morning
            in 12..16 -> R.string.greeting_afternoon
            in 17..21 -> R.string.greeting_evening
            else -> R.string.greeting_night
        }
    )
}

/**
 * Header of the "More" screen: a soft wash of the accent behind a greeting and a one-line
 * status, with room for a trailing badge. No app bar — the greeting is the title.
 */
@Composable
fun NovelaGreetingHeader(
    greeting: String,
    subtitle: String?,
    accent: Color,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.14f),
                        accent.copy(alpha = 0.05f),
                        Color.Transparent,
                    ),
                )
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = NovelaSpacing.screenH)
                .padding(top = NovelaSpacing.lg, bottom = NovelaSpacing.xl),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NovelaSpacing.md),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = NovelaType.bookMeta,
                        color = accent,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            trailing?.invoke()
        }
    }
}

/** The streak badge: only ever shown for a number this app can actually compute. */
@Composable
fun NovelaStreakPill(
    days: Int,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = NovelaRadius.chipShape,
        color = accent.copy(alpha = 0.14f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = NovelaSpacing.md, vertical = NovelaSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NovelaSpacing.xxs),
        ) {
            Icon(
                imageVector = Icons.Rounded.LocalFireDepartment,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = accent,
            )
            Text(
                text = days.toString(),
                style = MaterialTheme.typography.titleSmall,
                color = accent,
            )
        }
    }
}

/** Label above a block of [NovelaMenuCard]s. */
@Composable
fun NovelaMenuSectionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = NovelaSpacing.screenH)
            .padding(top = NovelaSpacing.md, bottom = NovelaSpacing.xs),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
    )
}

/**
 * One entry in the "More" menu: circular tinted icon, title, one-line status, optional
 * badge, chevron. Each entry is its own card on purpose — a group of rows in one surface
 * reads as a list of settings, and this screen is navigation, not settings.
 */
@Composable
fun NovelaMenuCard(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: String? = null,
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = NovelaSpacing.screenH, vertical = NovelaSpacing.xxs),
        shape = NovelaRadius.cardShape,
        color = NovelaTheme.colors.elevatedSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, NovelaTheme.colors.hairline),
    ) {
        Row(
            modifier = Modifier.padding(NovelaSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NovelaSpacing.md),
        ) {
            NovelaCircleIcon(accent = accent) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = accent,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(NovelaSpacing.xs),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (!badge.isNullOrBlank()) {
                        Text(
                            text = badge,
                            modifier = Modifier
                                .clip(NovelaRadius.badgeShape)
                                .background(accent)
                                .padding(horizontal = 6.dp, vertical = 1.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = androidx.compose.ui.graphics.Color.White,
                        )
                    }
                }
                if (!subtitle.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = NovelaType.bookMeta,
                        color = NovelaTheme.colors.textMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = NovelaTheme.colors.textMuted.copy(alpha = 0.7f),
            )
        }
    }
}

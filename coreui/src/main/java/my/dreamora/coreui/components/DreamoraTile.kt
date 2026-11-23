package my.dreamora.coreui.components

import androidx.compose.material.icons.Icons
import my.dreamora.coreui.R
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import my.dreamora.coreui.theme.DreamoraRadius
import my.dreamora.coreui.theme.DreamoraSpacing
import my.dreamora.coreui.theme.DreamoraTheme
import my.dreamora.coreui.theme.DreamoraType

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
    val light = DreamoraTheme.colors.isLight
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
fun DreamoraIconChip(
    accent: Color,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 44.dp,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(DreamoraTile.chipShape)
            .background(accent.copy(alpha = 0.18f))
            .border(1.dp, accent.copy(alpha = 0.35f), DreamoraTile.chipShape)
            .padding(6.dp),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
fun DreamoraTileCard(
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
            .clip(DreamoraRadius.cardShape)
            .novelaTileBackground(accent),
        shape = DreamoraRadius.cardShape,
        color = DreamoraTheme.colors.elevatedSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, DreamoraTheme.colors.hairline),
    ) {
        Box {
            Column(
                modifier = Modifier.padding(DreamoraSpacing.md),
                verticalArrangement = Arrangement.spacedBy(DreamoraSpacing.xs),
                content = content,
            )
            overlay?.let {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(DreamoraSpacing.xxs),
                    horizontalArrangement = Arrangement.spacedBy(DreamoraSpacing.xxs),
                    verticalAlignment = Alignment.CenterVertically,
                    content = { it() },
                )
            }
        }
    }
}

/** Title block used above a tile grid: label on the left, count/action on the right. */
@Composable
fun DreamoraTileSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    count: Int? = null,
    action: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = DreamoraSpacing.screenH)
            .padding(top = DreamoraSpacing.md, bottom = DreamoraSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (count != null) "$title · $count" else title,
            style = DreamoraType.sectionTitle,
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
fun DreamoraTileBadge(
    text: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier
            .clip(DreamoraRadius.badgeShape)
            .background(accent.copy(alpha = 0.16f))
            .padding(horizontal = DreamoraSpacing.xs, vertical = 1.dp),
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
fun DreamoraHeroHeader(
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
            .padding(horizontal = DreamoraSpacing.screenH, vertical = DreamoraSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DreamoraSpacing.md),
    ) {
        if (icon != null) {
            DreamoraIconChip(accent = accent) { icon() }
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
                    style = DreamoraType.bookMeta,
                    color = DreamoraTheme.colors.textMuted,
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
fun DreamoraHeroPill(
    value: String,
    label: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(DreamoraRadius.chipShape)
            .background(accent.copy(alpha = 0.16f))
            .border(1.dp, accent.copy(alpha = 0.30f), DreamoraRadius.chipShape)
            .padding(horizontal = DreamoraSpacing.md, vertical = DreamoraSpacing.xs),
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
            color = DreamoraTheme.colors.textMuted,
            maxLines = 1,
        )
    }
}

/** Shared shapes for the tile language, kept here so tiles and chips agree. */
object DreamoraTile {
    /** The icon chip: a squircle between a badge and a card corner. */
    val chipShape = RoundedCornerShape(DreamoraRadius.md)
}

/**
 * The action row of a tile: a filled pill button whose tint comes from the tile's own
 * accent. Used where a card needs a verb ("Install", "Update") instead of a chevron.
 */
@Composable
fun DreamoraTileActionButton(
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
        shape = DreamoraRadius.chipShape,
        color = accent.copy(alpha = if (enabled) 0.16f else 0.08f),
        contentColor = if (enabled) accent else DreamoraTheme.colors.textMuted,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = DreamoraSpacing.sm, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(DreamoraSpacing.xxs),
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

/** The compact round sibling of [DreamoraTileActionButton], for icon-only verbs. */
@Composable
fun DreamoraTileIconButton(
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

/** A circle for an icon, as used by the row groups (the tile language uses [DreamoraIconChip]). */
@Composable
fun DreamoraCircleIcon(
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
fun DreamoraStatCard(
    value: String,
    label: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp)) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
fun DreamoraCardGroup(
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = DreamoraSpacing.screenH, vertical = DreamoraSpacing.xxs),
        shape = DreamoraRadius.cardShape,
        color = DreamoraTheme.colors.elevatedSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, DreamoraTheme.colors.hairline),
    ) {
        Column(content = content)
    }
}

/** The hairline between two rows of a [DreamoraCardGroup]. */
@Composable
fun DreamoraCardGroupDivider(modifier: Modifier = Modifier) {
    androidx.compose.material3.HorizontalDivider(
        modifier = modifier.padding(start = DreamoraSpacing.lg),
        thickness = 0.5.dp,
        color = DreamoraTheme.colors.hairline,
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
fun DreamoraGreetingHeader(
    greeting: String,
    subtitle: String?,
    accent: Color,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DreamoraSpacing.screenH)
                .padding(top = DreamoraSpacing.lg, bottom = DreamoraSpacing.xl),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DreamoraSpacing.md),
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
                        style = DreamoraType.bookMeta,
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
fun DreamoraStreakPill(
    days: Int,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = DreamoraRadius.chipShape,
        color = accent.copy(alpha = 0.14f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = DreamoraSpacing.md, vertical = DreamoraSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DreamoraSpacing.xxs),
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

/** Label above a block of [DreamoraMenuCard]s. */
@Composable
fun DreamoraMenuSectionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = DreamoraSpacing.screenH)
            .padding(top = DreamoraSpacing.md, bottom = DreamoraSpacing.xs),
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
fun DreamoraMenuCard(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: String? = null,
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = DreamoraSpacing.screenH, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            DreamoraIconTile(icon = icon)
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (!badge.isNullOrBlank()) {
                        Text(
                            text = badge,
                            modifier = Modifier
                                .clip(DreamoraRadius.badgeShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(horizontal = 6.dp, vertical = 1.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
                if (!subtitle.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * The rounded tile behind a settings or menu icon. One tone for every row: the icon carries
 * the meaning, the tile only frames it, so colour is not used as decoration.
 */
@Composable
fun DreamoraIconTile(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

package my.noveldokusha.coreui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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

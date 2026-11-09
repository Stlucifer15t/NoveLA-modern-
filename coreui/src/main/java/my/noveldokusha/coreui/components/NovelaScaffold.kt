package my.noveldokusha.coreui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import my.noveldokusha.coreui.theme.NovelaMotion
import my.noveldokusha.coreui.theme.NovelaRadius
import my.noveldokusha.coreui.theme.NovelaSpacing
import my.noveldokusha.coreui.theme.NovelaTheme
import my.noveldokusha.coreui.theme.NovelaType

/**
 * Large screen title with an optional subtitle and trailing action slot.
 *
 * Replaces the stock `TopAppBar` on the redesigned home screens: no elevation
 * line, no container fill, just type on the background so covers do the talking.
 */
@Composable
fun NovelaScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            // A bare Row has no window insets, unlike TopAppBar. Without this the
            // title slides under the status bar clock.
            .statusBarsPadding()
            .padding(
                start = NovelaSpacing.screenH,
                end = NovelaSpacing.sm,
                top = NovelaSpacing.md,
                bottom = NovelaSpacing.sm,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = NovelaType.bookMeta,
                    color = NovelaTheme.colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        actions?.invoke()
    }
}

/**
 * Section heading with an optional "See all"-style trailing label.
 */
@Composable
fun NovelaSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = NovelaSpacing.screenH, vertical = NovelaSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = NovelaType.sectionTitle,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (actionLabel != null && onActionClick != null) {
            Text(
                text = actionLabel,
                style = NovelaType.sectionAction,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(NovelaRadius.chipShape)
                    .clickable(onClick = onActionClick)
                    .padding(horizontal = NovelaSpacing.sm, vertical = NovelaSpacing.xs),
            )
        }
    }
}

/**
 * Pill filter chip.
 *
 * Selected state fills with the accent and swaps to `onPrimary`; unselected is a
 * transparent chip with a hairline. Width animates so the row feels deliberate
 * rather than snappy.
 */
@Composable
fun NovelaFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
) {
    val background by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent,
        animationSpec = tween(NovelaMotion.STANDARD, easing = NovelaMotion.StandardEasing),
        label = "chipBg",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else NovelaTheme.colors.textMuted,
        animationSpec = tween(NovelaMotion.STANDARD, easing = NovelaMotion.StandardEasing),
        label = "chipFg",
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else NovelaTheme.colors.hairline,
        animationSpec = tween(NovelaMotion.STANDARD, easing = NovelaMotion.StandardEasing),
        label = "chipBorder",
    )

    Row(
        modifier = modifier
            .clip(NovelaRadius.chipShape)
            .background(background)
            .border(1.dp, borderColor, NovelaRadius.chipShape)
            .clickable(onClick = onClick)
            .padding(horizontal = NovelaSpacing.md, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(5.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = contentColor,
            maxLines = 1,
        )
    }
}

/**
 * Horizontal row of [NovelaFilterChip]s with edge padding, used for
 * category/filter rails.
 */
@Composable
fun NovelaChipRow(
    items: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: Dp = NovelaSpacing.screenH,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = contentPadding),
        horizontalArrangement = Arrangement.spacedBy(NovelaSpacing.sm),
    ) {
        items(items) { label ->
            val index = items.indexOf(label)
            NovelaFilterChip(
                label = label,
                selected = index == selectedIndex,
                onClick = { onSelect(index) },
            )
        }
    }
}

/**
 * Thin indeterminate-looking progress line shown under the header while a
 * refresh runs. Uses an animated width so it reads as activity without a
 * spinning indicator competing with the content.
 */
@Composable
fun NovelaTopProgress(
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(NovelaMotion.INSTANT)) + expandVertically(tween(NovelaMotion.STANDARD)),
        exit = fadeOut(tween(NovelaMotion.INSTANT)) + shrinkVertically(tween(NovelaMotion.STANDARD)),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(NovelaTheme.colors.accentSoft)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(0.4f)
                    .height(2.dp)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

/**
 * Empty state: icon in a soft accent wash, headline, one line of guidance, and an
 * optional action. Used when a library category or search has no results.
 */
@Composable
fun NovelaEmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    androidx.compose.foundation.layout.Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(NovelaSpacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(NovelaRadius.xl))
                .background(NovelaTheme.colors.accentSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(Modifier.height(NovelaSpacing.lg))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (message.isNotBlank()) {
            Spacer(Modifier.height(NovelaSpacing.xs))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = NovelaTheme.colors.textMuted,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(NovelaSpacing.lg))
            NovelaPrimaryButton(text = actionLabel, onClick = onAction)
        }
    }
}

/**
 * Filled accent button with a subtle gradient and press feedback.
 */
@Composable
fun NovelaPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .clip(NovelaRadius.buttonShape)
            .background(
                if (enabled) {
                    androidx.compose.ui.graphics.Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.86f),
                        )
                    )
                } else {
                    androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.surfaceVariant)
                }
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = NovelaSpacing.xl, vertical = NovelaSpacing.md),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) MaterialTheme.colorScheme.onPrimary else NovelaTheme.colors.textMuted,
        )
    }
}

/**
 * Outline button for a secondary action next to [NovelaPrimaryButton].
 */
@Composable
fun NovelaSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .clip(NovelaRadius.buttonShape)
            .border(1.dp, NovelaTheme.colors.hairline, NovelaRadius.buttonShape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = NovelaSpacing.xl, vertical = NovelaSpacing.md),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

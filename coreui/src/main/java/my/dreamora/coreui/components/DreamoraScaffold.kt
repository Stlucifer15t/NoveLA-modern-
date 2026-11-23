package my.dreamora.coreui.components

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
import my.dreamora.coreui.theme.DreamoraMotion
import my.dreamora.coreui.theme.DreamoraRadius
import my.dreamora.coreui.theme.DreamoraSpacing
import my.dreamora.coreui.theme.DreamoraTheme
import my.dreamora.coreui.theme.DreamoraType

/**
 * Large screen title with an optional subtitle and trailing action slot.
 *
 * Replaces the stock `TopAppBar` on the redesigned home screens: no elevation
 * line, no container fill, just type on the background so covers do the talking.
 */
@Composable
fun DreamoraScreenHeader(
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
                start = DreamoraSpacing.screenH,
                end = DreamoraSpacing.sm,
                top = DreamoraSpacing.md,
                bottom = DreamoraSpacing.sm,
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
                    style = DreamoraType.bookMeta,
                    color = DreamoraTheme.colors.textMuted,
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
fun DreamoraSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = DreamoraSpacing.screenH, vertical = DreamoraSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = DreamoraType.sectionTitle,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (actionLabel != null && onActionClick != null) {
            Text(
                text = actionLabel,
                style = DreamoraType.sectionAction,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(DreamoraRadius.chipShape)
                    .clickable(onClick = onActionClick)
                    .padding(horizontal = DreamoraSpacing.sm, vertical = DreamoraSpacing.xs),
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
fun DreamoraFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
) {
    val background by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent,
        animationSpec = tween(DreamoraMotion.STANDARD, easing = DreamoraMotion.StandardEasing),
        label = "chipBg",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else DreamoraTheme.colors.textMuted,
        animationSpec = tween(DreamoraMotion.STANDARD, easing = DreamoraMotion.StandardEasing),
        label = "chipFg",
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else DreamoraTheme.colors.hairline,
        animationSpec = tween(DreamoraMotion.STANDARD, easing = DreamoraMotion.StandardEasing),
        label = "chipBorder",
    )

    Row(
        modifier = modifier
            .clip(DreamoraRadius.chipShape)
            .background(background)
            .border(1.dp, borderColor, DreamoraRadius.chipShape)
            .clickable(onClick = onClick)
            .padding(horizontal = DreamoraSpacing.md, vertical = 7.dp),
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
 * Horizontal row of [DreamoraFilterChip]s with edge padding, used for
 * category/filter rails.
 */
@Composable
fun DreamoraChipRow(
    items: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: Dp = DreamoraSpacing.screenH,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = contentPadding),
        horizontalArrangement = Arrangement.spacedBy(DreamoraSpacing.sm),
    ) {
        items(items) { label ->
            val index = items.indexOf(label)
            DreamoraFilterChip(
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
fun DreamoraTopProgress(
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(DreamoraMotion.INSTANT)) + expandVertically(tween(DreamoraMotion.STANDARD)),
        exit = fadeOut(tween(DreamoraMotion.INSTANT)) + shrinkVertically(tween(DreamoraMotion.STANDARD)),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(DreamoraTheme.colors.accentSoft)
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
fun DreamoraEmptyState(
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
            .padding(DreamoraSpacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(DreamoraRadius.xl))
                .background(DreamoraTheme.colors.accentSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(Modifier.height(DreamoraSpacing.lg))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (message.isNotBlank()) {
            Spacer(Modifier.height(DreamoraSpacing.xs))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = DreamoraTheme.colors.textMuted,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(DreamoraSpacing.lg))
            DreamoraPrimaryButton(text = actionLabel, onClick = onAction)
        }
    }
}

/**
 * Filled accent button with a subtle gradient and press feedback.
 */
@Composable
fun DreamoraPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .clip(DreamoraRadius.buttonShape)
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
            .padding(horizontal = DreamoraSpacing.xl, vertical = DreamoraSpacing.md),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) MaterialTheme.colorScheme.onPrimary else DreamoraTheme.colors.textMuted,
        )
    }
}

/**
 * Outline button for a secondary action next to [DreamoraPrimaryButton].
 */
@Composable
fun DreamoraSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .clip(DreamoraRadius.buttonShape)
            .border(1.dp, DreamoraTheme.colors.hairline, DreamoraRadius.buttonShape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = DreamoraSpacing.xl, vertical = DreamoraSpacing.md),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

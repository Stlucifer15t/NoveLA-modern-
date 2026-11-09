package my.noveldokusha.coreui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import my.noveldokusha.coreui.theme.NovelaMotion
import my.noveldokusha.coreui.theme.NovelaRadius
import my.noveldokusha.coreui.theme.NovelaSize
import my.noveldokusha.coreui.theme.NovelaSpacing
import my.noveldokusha.coreui.theme.NovelaTheme
import my.noveldokusha.coreui.theme.NovelaType

/**
 * Editorial WebNovel screen header with large title, optional subtitle pill/count,
 * navigation icon, trailing action slot, and optional bottom slot.
 */
@Composable
fun NovelaScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    applyStatusBarPadding: Boolean = true,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable (() -> Unit)? = null,
    bottomContent: (@Composable () -> Unit)? = null,
) {
    val colors = NovelaTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .then(if (applyStatusBarPadding) Modifier.statusBarsPadding() else Modifier)
            .padding(top = NovelaSpacing.sm, bottom = NovelaSpacing.xs),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = NovelaSpacing.lg,
                    end = NovelaSpacing.sm,
                    top = NovelaSpacing.xs,
                    bottom = NovelaSpacing.xs,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (navigationIcon != null) {
                navigationIcon()
                Spacer(Modifier.width(NovelaSpacing.sm))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = if (navigationIcon == null) {
                        MaterialTheme.typography.headlineMedium
                    } else {
                        MaterialTheme.typography.titleLarge
                    },
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(Modifier.height(1.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (actions != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    actions()
                }
            }
        }
        if (bottomContent != null) {
            Spacer(Modifier.height(NovelaSpacing.xs))
            bottomContent()
        }
    }
}

/**
 * Section header with accent bar indicator, bold title, optional count badge, and action label.
 */
@Composable
fun NovelaSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    badgeText: String? = null,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    val colors = NovelaTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = NovelaSpacing.lg, vertical = NovelaSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(15.dp)
                .clip(RoundedCornerShape(NovelaRadius.pill))
                .background(colors.accent)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
        )
        if (!badgeText.isNullOrBlank()) {
            Spacer(Modifier.width(6.dp))
            Surface(
                shape = RoundedCornerShape(NovelaRadius.pill),
                color = colors.accentSoft,
            ) {
                Text(
                    text = badgeText,
                    style = NovelaType.badge,
                    color = colors.accent,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                )
            }
        }
        Spacer(Modifier.weight(1f))
        if (actionLabel != null && onActionClick != null) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelLarge,
                color = colors.accent,
                modifier = Modifier
                    .clip(RoundedCornerShape(NovelaRadius.pill))
                    .clickable(onClick = onActionClick)
                    .padding(horizontal = NovelaSpacing.sm, vertical = NovelaSpacing.xs),
            )
        }
    }
}

/**
 * Animated pill filter chip with WebNovel active accent fill or subtle resting surface.
 */
@Composable
fun NovelaFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    count: Int? = null,
) {
    val colors = NovelaTheme.colors
    val containerColor by animateColorAsState(
        targetValue = if (selected) colors.accent else colors.surfaceSubtle,
        animationSpec = tween(NovelaMotion.fastMs),
        label = "chipContainer",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) colors.onAccent else colors.textSecondary,
        animationSpec = tween(NovelaMotion.fastMs),
        label = "chipContent",
    )
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(NovelaRadius.pill),
        color = containerColor,
        border = if (selected) null else BorderStroke(NovelaSize.hairline, colors.borderSubtle),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(15.dp),
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
                maxLines = 1,
            )
            if (count != null) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.82f),
                    maxLines = 1,
                )
            }
        }
    }
}

/**
 * Horizontally scrollable chip strip with standard screen padding.
 */
@Composable
fun NovelaChipRow(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = NovelaSpacing.lg, vertical = NovelaSpacing.xs),
        horizontalArrangement = Arrangement.spacedBy(NovelaSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

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
        contentPadding = PaddingValues(horizontal = contentPadding),
        horizontalArrangement = Arrangement.spacedBy(NovelaSpacing.sm),
    ) {
        itemsIndexed(items) { index, label ->
            NovelaFilterChip(
                label = label,
                selected = index == selectedIndex,
                onClick = { onSelect(index) },
            )
        }
    }
}

/**
 * WebNovel Segmented Tab Switcher (pill container with elevated active segment).
 */
@Composable
fun NovelaSegmentedRow(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = NovelaTheme.colors
    Surface(
        shape = RoundedCornerShape(NovelaRadius.pill),
        color = colors.surfaceSubtle,
        border = BorderStroke(NovelaSize.hairline, colors.borderSubtle),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            options.forEachIndexed { index, title ->
                val selected = index == selectedIndex
                val bg by animateColorAsState(
                    targetValue = if (selected) colors.accent else Color.Transparent,
                    animationSpec = tween(NovelaMotion.fastMs),
                    label = "segmentBg",
                )
                val fg by animateColorAsState(
                    targetValue = if (selected) colors.onAccent else colors.textSecondary,
                    animationSpec = tween(NovelaMotion.fastMs),
                    label = "segmentFg",
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(NovelaRadius.pill))
                        .background(bg)
                        .clickable { onSelect(index) }
                        .padding(vertical = 8.dp, horizontal = 8.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = fg,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/**
 * Tappable WebNovel Discovery Search Pill (e.g. "Search novels across all sources...").
 */
@Composable
fun NovelaSearchBarLauncher(
    placeholder: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    val colors = NovelaTheme.colors
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(NovelaRadius.pill),
        color = colors.surfaceSubtle,
        border = BorderStroke(NovelaSize.hairline, colors.borderSubtle),
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (trailingContent != null) {
                Spacer(Modifier.width(8.dp))
                trailingContent()
            }
        }
    }
}

/**
 * Inline editable WebNovel Pill Search Field (used in Extensions, Dialogs, etc.).
 */
@Composable
fun NovelaSearchInputPill(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    onSearch: (() -> Unit)? = null,
) {
    val colors = NovelaTheme.colors
    Surface(
        shape = RoundedCornerShape(NovelaRadius.pill),
        color = colors.surfaceSubtle,
        border = BorderStroke(NovelaSize.hairline, colors.borderSubtle),
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = colors.textMuted,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = colors.textPrimary,
                    ),
                    cursorBrush = SolidColor(colors.accent),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch?.invoke() }),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (value.isNotEmpty()) {
                Surface(
                    shape = CircleShape,
                    color = colors.borderSubtle,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { onValueChange("") },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(12.dp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * WebNovel 3-column novel metric card (e.g. Chapters | Progress | Status).
 */
@Composable
fun NovelaStatStrip(
    stats: List<Pair<String, String>>,
    modifier: Modifier = Modifier,
    bottomContent: (@Composable ColumnScope.() -> Unit)? = null,
) {
    if (stats.isEmpty()) return
    val colors = NovelaTheme.colors
    Surface(
        shape = RoundedCornerShape(NovelaRadius.card),
        color = colors.surfaceCard,
        border = BorderStroke(NovelaSize.hairline, colors.borderSubtle),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(vertical = 12.dp, horizontal = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                stats.forEachIndexed { index, (value, label) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = value,
                            style = NovelaType.statValue,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = label,
                            style = NovelaType.statLabel,
                            color = colors.textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (index < stats.lastIndex) {
                        Box(
                            modifier = Modifier
                                .width(NovelaSize.hairline)
                                .height(26.dp)
                                .background(colors.borderSubtle)
                        )
                    }
                }
            }
            if (bottomContent != null) {
                Spacer(Modifier.height(10.dp))
                bottomContent()
            }
        }
    }
}

/**
 * Calm, spacious empty state with soft icon badge, title, subtitle/message, and optional CTA.
 */
@Composable
fun NovelaEmptyState(
    title: String,
    modifier: Modifier = Modifier,
    message: String = "",
    subtitle: String? = null,
    icon: ImageVector? = null,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    onAction: (() -> Unit)? = null,
) {
    val colors = NovelaTheme.colors
    val body = subtitle?.takeIf { it.isNotBlank() } ?: message
    val actionHandler = onActionClick ?: onAction

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = NovelaSpacing.xxxl, vertical = NovelaSpacing.xxxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Surface(
                shape = CircleShape,
                color = colors.accentSoft,
                border = BorderStroke(NovelaSize.hairline, colors.accent.copy(alpha = 0.25f)),
                modifier = Modifier.size(68.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(30.dp),
                    )
                }
            }
            Spacer(Modifier.height(NovelaSpacing.lg))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        if (body.isNotBlank()) {
            Spacer(Modifier.height(NovelaSpacing.xs))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textMuted,
                textAlign = TextAlign.Center,
            )
        }
        if (actionLabel != null && actionHandler != null) {
            Spacer(Modifier.height(NovelaSpacing.xl))
            NovelaPrimaryButton(
                text = actionLabel,
                onClick = actionHandler,
            )
        }
    }
}

@Composable
fun NovelaPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val colors = NovelaTheme.colors
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(NovelaRadius.pill),
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.accent,
            contentColor = colors.onAccent,
            disabledContainerColor = colors.surfaceSubtle,
            disabledContentColor = colors.textMuted,
        ),
        contentPadding = PaddingValues(horizontal = NovelaSpacing.xl, vertical = 11.dp),
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(NovelaSpacing.sm))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun NovelaSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val colors = NovelaTheme.colors
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(NovelaRadius.pill),
        color = colors.surfaceCard,
        border = BorderStroke(NovelaSize.hairline, colors.borderStrong),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = NovelaSpacing.lg, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = colors.textPrimary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(NovelaSpacing.sm))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun NovelaTopProgress(
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = NovelaTheme.colors
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(NovelaMotion.INSTANT)) + expandVertically(tween(NovelaMotion.STANDARD)),
        exit = fadeOut(tween(NovelaMotion.INSTANT)) + shrinkVertically(tween(NovelaMotion.STANDARD)),
        modifier = modifier,
    ) {
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.5.dp),
            color = colors.accent,
            trackColor = colors.accentSoft,
        )
    }
}

@Composable
fun NovelaTopProgress(
    progress: Float?,
    modifier: Modifier = Modifier,
) {
    val colors = NovelaTheme.colors
    if (progress == null) {
        LinearProgressIndicator(
            modifier = modifier
                .fillMaxWidth()
                .height(2.5.dp),
            color = colors.accent,
            trackColor = colors.accentSoft,
        )
    } else {
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = modifier
                .fillMaxWidth()
                .height(2.5.dp),
            color = colors.accent,
            trackColor = colors.accentSoft,
        )
    }
}

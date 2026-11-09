package my.noveldokusha.features.reader.ui.settingDialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FormatPaint
import androidx.compose.material.icons.outlined.Highlight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import my.noveldokusha.coreui.components.NovelaSettingsTile
import my.noveldokusha.coreui.theme.NovelaRadius
import my.noveldokusha.coreui.theme.NovelaSize
import my.noveldokusha.coreui.theme.NovelaTheme
import my.noveldokusha.reader.R

private val ttsHighlightColors = listOf(
    "FFFF6D00", // Deep Orange
    "FFFF1744", // Red Accent
    "FFFF4081", // Pink Accent
    "FFE040FB", // Purple Accent
    "FF7C4DFF", // Deep Purple Accent
    "FF536DFE", // Indigo Accent
    "FF448AFF", // Blue Accent
    "FF40C4FF", // Light Blue Accent
    "FF18FFFF", // Cyan Accent
    "FF64FFDA", // Teal Accent
    "FF69F0AE", // Green Accent
    "FFFFD740", // Amber Accent
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MoreSettingDialog(
    ttsHighlightEnabled: Boolean,
    onTtsHighlightEnabledChange: (Boolean) -> Unit,
    ttsHighlightColor: String,
    onTtsHighlightColorChange: (String) -> Unit,
    manualHighlightEnabled: Boolean = false,
    onManualHighlightEnabledChange: (Boolean) -> Unit = {},
) {
    val colors = NovelaTheme.colors
    val scrollState = rememberScrollState()
    val isScrolling by remember { derivedStateOf { scrollState.isScrollInProgress } }

    val switchColors = SwitchDefaults.colors(
        checkedThumbColor = colors.onAccent,
        checkedTrackColor = colors.accent,
        uncheckedThumbColor = colors.textMuted,
        uncheckedTrackColor = colors.surfaceMuted,
    )

    Surface(
        shape = RoundedCornerShape(NovelaRadius.cardLarge),
        color = colors.surfaceElevated.copy(alpha = 0.97f),
        border = BorderStroke(NovelaSize.hairline, colors.borderSubtle),
        shadowElevation = 12.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(vertical = 6.dp),
        ) {
            NovelaSettingsTile(
                title = stringResource(id = R.string.manual_highlight),
                icon = Icons.Outlined.Highlight,
                showDivider = true,
                onClick = if (!isScrolling) {
                    { onManualHighlightEnabledChange(!manualHighlightEnabled) }
                } else {
                    null
                },
                trailing = {
                    Switch(
                        checked = manualHighlightEnabled,
                        onCheckedChange = onManualHighlightEnabledChange,
                        enabled = !isScrolling,
                        colors = switchColors,
                    )
                },
            )

            NovelaSettingsTile(
                title = stringResource(id = R.string.tts_highlight),
                icon = Icons.Outlined.FormatPaint,
                onClick = if (!isScrolling) {
                    { onTtsHighlightEnabledChange(!ttsHighlightEnabled) }
                } else {
                    null
                },
                trailing = {
                    Switch(
                        checked = ttsHighlightEnabled,
                        onCheckedChange = onTtsHighlightEnabledChange,
                        enabled = !isScrolling,
                        colors = switchColors,
                    )
                },
            )

            if (ttsHighlightEnabled) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ttsHighlightColors.forEach { hexColor ->
                        val isSelected = hexColor == ttsHighlightColor
                        val swatchColor = try {
                            Color(android.graphics.Color.parseColor("#$hexColor"))
                        } catch (_: Exception) {
                            Color(android.graphics.Color.parseColor("#FFFF6D00"))
                        }
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(swatchColor)
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isSelected) colors.textPrimary else colors.borderSubtle,
                                    shape = CircleShape,
                                )
                                .clickable(enabled = !isScrolling) {
                                    onTtsHighlightColorChange(hexColor)
                                },
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

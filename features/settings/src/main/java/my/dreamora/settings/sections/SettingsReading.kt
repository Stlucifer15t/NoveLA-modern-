package my.dreamora.settings.sections

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.BlurOn
import androidx.compose.material.icons.outlined.BrightnessHigh
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FindReplace
import androidx.compose.material.icons.outlined.FormatAlignJustify
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatIndentDecrease
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import my.dreamora.settings.ui.SettingsRow
import my.dreamora.coreui.components.PillSlider
import my.dreamora.coreui.theme.DreamoraSpacing
import my.dreamora.settings.ReadingPreferences
import androidx.compose.ui.text.font.FontWeight
import my.dreamora.settings.ReadingPreferences.Companion.MAX_FONT_SIZE
import my.dreamora.settings.ReadingPreferences.Companion.MAX_LINE_HEIGHT
import my.dreamora.settings.ReadingPreferences.Companion.MIN_FONT_SIZE
import my.dreamora.settings.ReadingPreferences.Companion.MIN_LINE_HEIGHT
import my.dreamora.settings.R
import my.dreamora.settings.ui.ExpressiveSwitch
import my.dreamora.settings.ui.PremiumSectionHeader
import my.dreamora.settings.ui.PremiumSectionTitle
import my.dreamora.settings.ui.PremiumSettingsRow
import my.dreamora.coreui.theme.textPadding
import androidx.compose.ui.text.font.FontWeight

/**
 * The reader's settings, reachable without opening a book.
 */
@Composable
internal fun SettingsReading(reading: ReadingPreferences) {
    Column {
        PremiumSectionHeader(
            icon = Icons.Outlined.MenuBook,
            title = stringResource(R.string.settings_group_reading),
            summary = stringResource(R.string.settings_group_reading_hint),
            accent = MaterialTheme.colorScheme.primary,
        )

        PillSlider(
            label = stringResource(R.string.text_size),
            value = reading.fontSize.value,
            valueRange = MIN_FONT_SIZE..MAX_FONT_SIZE,
            onValueChange = { reading.setFontSize(it) },
            valueText = "%.2f".format(reading.fontSize.value),
            modifier = Modifier.textPadding().padding(top = DreamoraSpacing.sm),
        )

        PillSlider(
            label = stringResource(R.string.line_height),
            value = reading.lineHeight.value,
            valueRange = MIN_LINE_HEIGHT..MAX_LINE_HEIGHT,
            onValueChange = { reading.lineHeight.value = it },
            valueText = "%.2f".format(reading.lineHeight.value),
            modifier = Modifier.textPadding(),
        )

        PillSlider(
            label = stringResource(R.string.paragraph_spacing),
            value = reading.paragraphSpacing.value,
            valueRange = 0f..40f,
            onValueChange = { reading.paragraphSpacing.value = it },
            valueText = "%.0f dp".format(reading.paragraphSpacing.value),
            modifier = Modifier.textPadding(),
        )

        PillSlider(
            label = stringResource(R.string.reader_margins),
            value = reading.sideMargins,
            valueRange = 0f..ReadingPreferences.MAX_MARGIN,
            onValueChange = { reading.setSideMargins(it) },
            valueText = "%.0f dp".format(reading.sideMargins),
            modifier = Modifier.textPadding(),
        )

        Spacer(Modifier.height(DreamoraSpacing.md))

        PremiumSectionTitle(text = stringResource(R.string.reading_text_options))

        ToggleRow(
            icon = Icons.Outlined.FormatAlignJustify,
            title = stringResource(R.string.reader_text_justify),
            summary = stringResource(R.string.reading_text_justify_description),
            checked = reading.justify.value,
            onCheckedChange = { reading.justify.value = it },
        )
        ToggleRow(
            icon = Icons.Outlined.FindReplace,
            title = stringResource(R.string.reader_text_hyphenation),
            summary = stringResource(R.string.reading_text_hyphenation_description),
            checked = reading.hyphenation.value,
            onCheckedChange = { reading.hyphenation.value = it },
        )
        ToggleRow(
            icon = Icons.Outlined.FormatIndentDecrease,
            title = stringResource(R.string.reader_paragraph_indent),
            summary = null,
            checked = reading.paragraphIndent.value,
            onCheckedChange = { reading.paragraphIndent.value = it },
        )
        ToggleRow(
            icon = Icons.Outlined.FormatBold,
            title = stringResource(R.string.reader_paragraph_first_letter_bold),
            summary = null,
            checked = reading.firstLetterBold.value,
            onCheckedChange = { reading.firstLetterBold.value = it },
        )
        ToggleRow(
            icon = Icons.Outlined.BlurOn,
            title = stringResource(R.string.reader_text_smooth),
            summary = null,
            checked = reading.textSmooth.value,
            onCheckedChange = { reading.textSmooth.value = it },
        )

        Spacer(Modifier.height(DreamoraSpacing.md))

        PremiumSectionTitle(text = stringResource(R.string.reading_behaviour))

        ToggleRow(
            icon = Icons.Outlined.BrightnessHigh,
            title = stringResource(R.string.reading_keep_screen_on),
            summary = stringResource(R.string.reading_keep_screen_on_description),
            checked = reading.keepScreenOn.value,
            onCheckedChange = { reading.keepScreenOn.value = it },
        )
        ToggleRow(
            icon = Icons.Outlined.Fullscreen,
            title = stringResource(R.string.reading_full_screen),
            summary = null,
            checked = reading.fullScreen.value,
            onCheckedChange = { reading.fullScreen.value = it },
        )
        ToggleRow(
            icon = Icons.Outlined.TouchApp,
            title = stringResource(R.string.reading_tap_for_settings),
            summary = stringResource(R.string.reading_tap_for_settings_description),
            checked = reading.singleTapOpensControls.value,
            onCheckedChange = { reading.singleTapOpensControls.value = it },
        )
        ToggleRow(
            icon = Icons.Outlined.SelectAll,
            title = stringResource(R.string.reading_selectable_text),
            summary = stringResource(R.string.reading_selectable_text_description),
            checked = reading.selectableText.value,
            onCheckedChange = { reading.selectableText.value = it },
        )

        Spacer(Modifier.height(DreamoraSpacing.md))

        PremiumSectionTitle(text = stringResource(R.string.reading_preload))

        SettingsRow(
            headlineContent = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.reading_preload_pages),
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.reading_preload_pages_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = reading.prefetchChapters.value.toString(),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            },
            leadingContent = {
                Icon(
                    Icons.Outlined.Download,
                    null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            onClick = {
                val steps = prefetchStepChoices
                val index = steps.indexOf(reading.prefetchChapters.value)
                reading.prefetchChapters.value =
                    if (index < 0) steps.first() else steps[(index + 1) % steps.size]
            },
        )
    }
}

private val prefetchStepChoices = listOf(0, 1, 2, 3, 5, 8)

@Composable
private fun ToggleRow(
    icon: ImageVector,
    title: String,
    summary: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    SettingsRow(
        modifier = Modifier.textPadding(),
        headlineContent = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    )
                    if (summary != null) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        leadingContent = {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        trailingContent = {
            ExpressiveSwitch(checked = checked, onCheckedChange = onCheckedChange)
        },
        onClick = { onCheckedChange(!checked) },
    )
}

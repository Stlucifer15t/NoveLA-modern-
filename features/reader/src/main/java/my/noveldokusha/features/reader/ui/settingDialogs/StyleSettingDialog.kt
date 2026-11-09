package my.noveldokusha.features.reader.ui.settingDialogs

import android.content.Intent
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.outlined.BrightnessMedium
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.FormatAlignJustify
import androidx.compose.material.icons.outlined.FormatColorFill
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Nightlight
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.SpaceBar
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import my.noveldokusha.coreui.components.PillSlider
import my.noveldokusha.coreui.composableActions.onDoAskForImage
import my.noveldokusha.coreui.theme.AppTheme
import my.noveldokusha.coreui.theme.DarkMode
import my.noveldokusha.coreui.theme.NovelaRadius
import my.noveldokusha.coreui.theme.NovelaSize
import my.noveldokusha.coreui.theme.NovelaTheme
import my.noveldokusha.coreui.theme.NovelaType
import my.noveldokusha.features.reader.tools.BackgroundImageLoader
import my.noveldokusha.features.reader.tools.FontsLoader
import my.noveldokusha.features.reader.ui.ReaderBackgroundPresets
import my.noveldokusha.features.reader.ui.ReaderScreenState
import my.noveldokusha.reader.R

@Composable
internal fun StyleSettingDialog(
    state: ReaderScreenState.Settings.StyleSettingsData,
    onTextSizeChange: (Float) -> Unit,
    onLineHeightChange: (Float) -> Unit,
    onParagraphSpacingChange: (Float) -> Unit,
    onLetterSpacingChange: (Float) -> Unit,
    onTextFontChange: (String) -> Unit,
    onTextColorChanged: (String) -> Unit,
    onBackgroundChanged: (String) -> Unit,
    onDarkModeChange: (DarkMode) -> Unit,
    onAppThemeChange: (AppTheme) -> Unit,
    onTextJustifyChange: (Boolean) -> Unit,
    onTextHyphenationChange: (Boolean) -> Unit,
    onParagraphIndentChange: (Boolean) -> Unit,
    onParagraphFirstLetterBoldChange: (Boolean) -> Unit,
    onSentenceSplittingChange: (Boolean) -> Unit,
    onTextBoldChange: (Boolean) -> Unit,
    onTextItalicChange: (Boolean) -> Unit,
    onTextUnderlineChange: (Boolean) -> Unit,
    onTextShadowChange: (Boolean) -> Unit,
    onTextSmoothChange: (Boolean) -> Unit,
    onMarginLeftChange: (Float) -> Unit,
    onMarginRightChange: (Float) -> Unit,
    onMarginTopChange: (Float) -> Unit,
    onMarginBottomChange: (Float) -> Unit,
    onTextDefaultsReset: () -> Unit = {},
) {
    val colors = NovelaTheme.colors
    val context = LocalContext.current
    val fontLoader = remember(context) { FontsLoader(context) }
    val backgroundLoader = remember(context) { BackgroundImageLoader(context) }
    val systemFontsSet = remember { FontsLoader.systemFonts.toSet() }
    val scope = rememberCoroutineScope()

    val chipColors = FilterChipDefaults.filterChipColors(
        containerColor = colors.surfaceMuted,
        labelColor = colors.textSecondary,
        selectedContainerColor = colors.accentSoft,
        selectedLabelColor = colors.accent,
    )

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val intent = result.data ?: return@rememberLauncherForActivityResult
        val uris = buildList {
            intent.clipData?.let { clip ->
                repeat(clip.itemCount) { i -> add(clip.getItemAt(i).uri) }
            }
            intent.data?.let { add(it) }
        }
        uris.forEach { uri ->
            scope.launch {
                fontLoader.importFont(uri).onFailure { e ->
                    Toast.makeText(
                        context,
                        e.message ?: "Font import failed",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
        }
    }

    Surface(
        shape = RoundedCornerShape(NovelaRadius.cardLarge),
        color = colors.surfaceElevated.copy(alpha = 0.97f),
        border = BorderStroke(NovelaSize.hairline, colors.borderSubtle),
        shadowElevation = 12.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        val scrollState = rememberScrollState()
        val isScrolling by remember { derivedStateOf { scrollState.isScrollInProgress } }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Header + Reset defaults
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.style).uppercase(),
                    style = NovelaType.sectionEyebrow,
                    color = colors.accent,
                )
                TextButton(onClick = onTextDefaultsReset) {
                    Text(
                        text = stringResource(R.string.typography_reset),
                        color = colors.accent,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            // Typography Sliders
            var currentTextSize by remember(state.textSize.value) { mutableFloatStateOf(state.textSize.value) }
            PillSlider(
                label = stringResource(R.string.text_size),
                value = currentTextSize,
                valueRange = 8f..32f,
                onValueChange = {
                    currentTextSize = it
                    onTextSizeChange(currentTextSize)
                },
                valueText = "%.2f".format(currentTextSize),
                enabled = !isScrolling,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp),
            )

            var currentLineHeight by remember(state.lineHeight.value) { mutableFloatStateOf(state.lineHeight.value) }
            PillSlider(
                label = stringResource(R.string.line_height),
                value = currentLineHeight,
                valueRange = 1.0f..2.5f,
                onValueChange = {
                    currentLineHeight = it
                    onLineHeightChange(currentLineHeight)
                },
                valueText = "%.2f".format(currentLineHeight),
                enabled = !isScrolling,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp),
            )

            var currentParagraphSpacing by remember(state.paragraphSpacing.value) { mutableFloatStateOf(state.paragraphSpacing.value) }
            PillSlider(
                label = stringResource(R.string.paragraph_spacing),
                value = currentParagraphSpacing,
                valueRange = 0f..40f,
                onValueChange = {
                    currentParagraphSpacing = it
                    onParagraphSpacingChange(currentParagraphSpacing)
                },
                valueText = "%.0f dp".format(currentParagraphSpacing),
                enabled = !isScrolling,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp),
            )

            var currentLetterSpacing by remember(state.letterSpacing.value) { mutableFloatStateOf(state.letterSpacing.value) }
            PillSlider(
                label = stringResource(R.string.letter_spacing),
                value = currentLetterSpacing,
                valueRange = 0f..0.3f,
                onValueChange = {
                    currentLetterSpacing = it
                    onLetterSpacingChange(currentLetterSpacing)
                },
                valueText = "%.2f em".format(currentLetterSpacing),
                enabled = !isScrolling,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp),
            )

            // Font Picker Popup
            Box {
                var showFontsDropdown by rememberSaveable { mutableStateOf(false) }

                Surface(
                    modifier = Modifier
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(NovelaRadius.md))
                        .clickable { showFontsDropdown = !showFontsDropdown },
                    shape = RoundedCornerShape(NovelaRadius.md),
                    color = colors.surfaceMuted,
                    border = BorderStroke(NovelaSize.hairline, colors.borderSubtle),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    ) {
                        Icon(
                            Icons.Filled.TextFields,
                            null,
                            tint = colors.accent,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = FontsLoader.displayName(state.textFont.value, systemFontsSet),
                            fontFamily = fontLoader.getFontFamily(state.textFont.value),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            Icons.Filled.ArrowDropDown,
                            null,
                            tint = colors.textMuted,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                if (showFontsDropdown) {
                    Popup(
                        onDismissRequest = { showFontsDropdown = false },
                        properties = PopupProperties(focusable = true),
                        popupPositionProvider = object : PopupPositionProvider {
                            override fun calculatePosition(
                                anchorBounds: IntRect,
                                windowSize: IntSize,
                                layoutDirection: LayoutDirection,
                                popupContentSize: IntSize,
                            ): IntOffset {
                                val desiredX = anchorBounds.left + anchorBounds.width / 2 - popupContentSize.width / 2
                                val x = desiredX.coerceIn(
                                    0,
                                    (windowSize.width - popupContentSize.width).coerceAtLeast(0),
                                )
                                val y = (anchorBounds.top - popupContentSize.height).coerceAtLeast(0)
                                return IntOffset(x, y)
                            }
                        },
                    ) {
                        Surface(
                            shape = RoundedCornerShape(NovelaRadius.card),
                            color = colors.surfaceElevated,
                            border = BorderStroke(NovelaSize.hairline, colors.borderSubtle),
                            shadowElevation = 10.dp,
                        ) {
                            Column(
                                Modifier
                                    .widthIn(max = 300.dp)
                                    .heightIn(max = 320.dp)
                                    .verticalScroll(rememberScrollState()),
                            ) {
                                val trailingIconSlot: @Composable (() -> Unit) = {
                                    Spacer(Modifier.size(48.dp))
                                }

                                val allFonts = FontsLoader.availableFonts.value
                                val importedFonts = allFonts.filter { FontsLoader.isImported(it) }
                                allFonts.filterNot { FontsLoader.isImported(it) }.forEach { item ->
                                    val active = state.textFont.value == item
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 46.dp)
                                            .background(if (active) colors.accentSoft else Color.Transparent)
                                            .clickable(enabled = !isScrolling) {
                                                onTextFontChange(item)
                                                showFontsDropdown = false
                                            }
                                            .padding(horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Box(
                                            modifier = Modifier.weight(1f),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                text = FontsLoader.displayName(item, systemFontsSet),
                                                fontFamily = fontLoader.getFontFamily(item),
                                                color = if (active) colors.accent else colors.textPrimary,
                                                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                        trailingIconSlot()
                                    }
                                }
                                if (importedFonts.isNotEmpty()) {
                                    Text(
                                        text = stringResource(R.string.reader_fonts_imported).uppercase(),
                                        style = NovelaType.sectionEyebrow,
                                        color = colors.accent,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                    )
                                    importedFonts.forEach { item ->
                                        val active = state.textFont.value == item
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(min = 46.dp)
                                                .background(if (active) colors.accentSoft else Color.Transparent)
                                                .clickable(enabled = !isScrolling) {
                                                    onTextFontChange(item)
                                                    showFontsDropdown = false
                                                }
                                                .padding(horizontal = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Box(
                                                modifier = Modifier.weight(1f),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Text(
                                                    text = FontsLoader.displayName(item, systemFontsSet),
                                                    fontFamily = fontLoader.getFontFamily(item),
                                                    color = if (active) colors.accent else colors.textPrimary,
                                                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                            }
                                            IconButton(onClick = {
                                                val deleted = fontLoader.deleteFont(item)
                                                if (deleted && state.textFont.value == item) {
                                                    onTextFontChange("serif")
                                                }
                                            }) {
                                                Icon(
                                                    Icons.Outlined.Delete,
                                                    contentDescription = stringResource(R.string.reader_delete_font),
                                                    tint = colors.danger,
                                                    modifier = Modifier.size(18.dp),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Import Font Action
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 14.dp),
            ) {
                TextButton(
                    onClick = {
                        val intent = ActivityResultContracts.OpenDocument()
                            .createIntent(context, FONT_MIME_TYPES)
                            .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                        importLauncher.launch(intent)
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        Icons.Outlined.FileUpload,
                        null,
                        tint = colors.accent,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.reader_import_font),
                        color = colors.accent,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            HorizontalDivider(
                thickness = NovelaSize.hairline,
                color = colors.borderSubtle,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            )

            // Alignment, Paragraph & Font Style Chips
            StyleSectionLabel(
                icon = Icons.Outlined.FormatAlignJustify,
                title = stringResource(R.string.reader_text_alignment),
            )
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                item {
                    FilterChip(
                        selected = state.textJustify.value,
                        onClick = { onTextJustifyChange(!state.textJustify.value) },
                        shape = RoundedCornerShape(NovelaRadius.pill),
                        colors = chipColors,
                        label = { Text(stringResource(R.string.reader_text_justify), maxLines = 1) },
                        enabled = !isScrolling,
                    )
                }
                item {
                    FilterChip(
                        selected = state.textHyphenation.value,
                        onClick = { onTextHyphenationChange(!state.textHyphenation.value) },
                        shape = RoundedCornerShape(NovelaRadius.pill),
                        colors = chipColors,
                        label = { Text(stringResource(R.string.reader_text_hyphenation), maxLines = 1) },
                        enabled = !isScrolling,
                    )
                }
            }

            StyleSectionLabel(
                icon = null,
                title = stringResource(R.string.reader_paragraph),
            )
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                item {
                    FilterChip(
                        selected = state.paragraphFirstLetterBold.value,
                        onClick = { onParagraphFirstLetterBoldChange(!state.paragraphFirstLetterBold.value) },
                        shape = RoundedCornerShape(NovelaRadius.pill),
                        colors = chipColors,
                        label = { Text(stringResource(R.string.reader_paragraph_first_letter_bold), maxLines = 1) },
                        enabled = !isScrolling,
                    )
                }
                item {
                    FilterChip(
                        selected = state.paragraphIndent.value,
                        onClick = { onParagraphIndentChange(!state.paragraphIndent.value) },
                        shape = RoundedCornerShape(NovelaRadius.pill),
                        colors = chipColors,
                        label = { Text(stringResource(R.string.reader_paragraph_indent), maxLines = 1) },
                        enabled = !isScrolling,
                    )
                }
                item {
                    FilterChip(
                        selected = state.sentenceSplitting.value,
                        onClick = { onSentenceSplittingChange(!state.sentenceSplitting.value) },
                        shape = RoundedCornerShape(NovelaRadius.pill),
                        colors = chipColors,
                        label = { Text(stringResource(R.string.reader_sentence_splitting), maxLines = 1) },
                        enabled = !isScrolling,
                    )
                }
            }

            StyleSectionLabel(
                icon = Icons.Filled.TextFields,
                title = stringResource(R.string.reader_font_style),
            )
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                item {
                    FilterChip(
                        selected = state.textBold.value,
                        onClick = { onTextBoldChange(!state.textBold.value) },
                        shape = RoundedCornerShape(NovelaRadius.pill),
                        colors = chipColors,
                        label = { Text(stringResource(R.string.reader_text_bold), maxLines = 1) },
                        enabled = !isScrolling,
                    )
                }
                item {
                    FilterChip(
                        selected = state.textItalic.value,
                        onClick = { onTextItalicChange(!state.textItalic.value) },
                        shape = RoundedCornerShape(NovelaRadius.pill),
                        colors = chipColors,
                        label = { Text(stringResource(R.string.reader_text_italic), maxLines = 1) },
                        enabled = !isScrolling,
                    )
                }
                item {
                    FilterChip(
                        selected = state.textUnderline.value,
                        onClick = { onTextUnderlineChange(!state.textUnderline.value) },
                        shape = RoundedCornerShape(NovelaRadius.pill),
                        colors = chipColors,
                        label = { Text(stringResource(R.string.reader_text_underline), maxLines = 1) },
                        enabled = !isScrolling,
                    )
                }
                item {
                    FilterChip(
                        selected = state.textShadow.value,
                        onClick = { onTextShadowChange(!state.textShadow.value) },
                        shape = RoundedCornerShape(NovelaRadius.pill),
                        colors = chipColors,
                        label = { Text(stringResource(R.string.reader_text_shadow), maxLines = 1) },
                        enabled = !isScrolling,
                    )
                }
                item {
                    FilterChip(
                        selected = state.textSmooth.value,
                        onClick = { onTextSmoothChange(!state.textSmooth.value) },
                        shape = RoundedCornerShape(NovelaRadius.pill),
                        colors = chipColors,
                        label = { Text(stringResource(R.string.reader_text_smooth), maxLines = 1) },
                        enabled = !isScrolling,
                    )
                }
            }

            HorizontalDivider(
                thickness = NovelaSize.hairline,
                color = colors.borderSubtle,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            )

            // Margins
            StyleSectionLabel(
                icon = Icons.Outlined.SpaceBar,
                title = stringResource(R.string.reader_margins),
            )
            var currentMarginLeft by remember(state.marginLeft.value) { mutableFloatStateOf(state.marginLeft.value) }
            PillSlider(
                label = stringResource(R.string.reader_margin_left),
                value = currentMarginLeft,
                valueRange = 0f..100f,
                onValueChange = {
                    currentMarginLeft = it
                    onMarginLeftChange(currentMarginLeft)
                },
                valueText = "%.0f dp".format(currentMarginLeft),
                enabled = !isScrolling,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp),
            )
            var currentMarginRight by remember(state.marginRight.value) { mutableFloatStateOf(state.marginRight.value) }
            PillSlider(
                label = stringResource(R.string.reader_margin_right),
                value = currentMarginRight,
                valueRange = 0f..100f,
                onValueChange = {
                    currentMarginRight = it
                    onMarginRightChange(currentMarginRight)
                },
                valueText = "%.0f dp".format(currentMarginRight),
                enabled = !isScrolling,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp),
            )
            var currentMarginTop by remember(state.marginTop.value) { mutableFloatStateOf(state.marginTop.value) }
            PillSlider(
                label = stringResource(R.string.reader_margin_top),
                value = currentMarginTop,
                valueRange = 0f..100f,
                onValueChange = {
                    currentMarginTop = it
                    onMarginTopChange(currentMarginTop)
                },
                valueText = "%.0f dp".format(currentMarginTop),
                enabled = !isScrolling,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp),
            )
            var currentMarginBottom by remember(state.marginBottom.value) { mutableFloatStateOf(state.marginBottom.value) }
            PillSlider(
                label = stringResource(R.string.reader_margin_bottom),
                value = currentMarginBottom,
                valueRange = 0f..100f,
                onValueChange = {
                    currentMarginBottom = it
                    onMarginBottomChange(currentMarginBottom)
                },
                valueText = "%.0f dp".format(currentMarginBottom),
                enabled = !isScrolling,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp),
            )

            HorizontalDivider(
                thickness = NovelaSize.hairline,
                color = colors.borderSubtle,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            )

            // Text Color & RGB Sliders
            StyleSectionLabel(
                icon = Icons.Outlined.FormatColorFill,
                title = stringResource(R.string.reader_text_color),
            )
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(listOf("") + TextColorPalette) { hex ->
                    if (hex.isEmpty()) {
                        val autoSelected = state.textColor.value.isEmpty()
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(colors.surfaceMuted)
                                .border(
                                    width = if (autoSelected) 2.5.dp else 1.dp,
                                    color = if (autoSelected) colors.accent else colors.borderSubtle,
                                    shape = CircleShape,
                                )
                                .clickable { onTextColorChanged("") },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "A",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (autoSelected) colors.accent else colors.textSecondary,
                            )
                        }
                    } else {
                        val selected = state.textColor.value.uppercase() == hex
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(safeParseColor(hex)))
                                .border(
                                    width = if (selected) 2.5.dp else 1.dp,
                                    color = if (selected) colors.accent else colors.borderSubtle,
                                    shape = CircleShape,
                                )
                                .clickable { onTextColorChanged(hex) },
                        )
                    }
                }
            }

            val parsedColor = safeParseColor(state.textColor.value)
            val red = ((parsedColor shr 16) and 0xFF).toFloat()
            val green = ((parsedColor shr 8) and 0xFF).toFloat()
            val blue = (parsedColor and 0xFF).toFloat()
            PillSlider(
                label = stringResource(R.string.manga_color_filter_red),
                value = red,
                valueRange = 0f..255f,
                onValueChange = { r ->
                    val argb = 0xFF000000.toInt() or (r.toInt() shl 16) or (green.toInt() shl 8) or blue.toInt()
                    onTextColorChanged("%08X".format(argb))
                },
                valueText = "%.0f".format(red),
                enabled = !isScrolling,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp),
            )
            PillSlider(
                label = stringResource(R.string.manga_color_filter_green),
                value = green,
                valueRange = 0f..255f,
                onValueChange = { g ->
                    val argb = 0xFF000000.toInt() or (red.toInt() shl 16) or (g.toInt() shl 8) or blue.toInt()
                    onTextColorChanged("%08X".format(argb))
                },
                valueText = "%.0f".format(green),
                enabled = !isScrolling,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp),
            )
            PillSlider(
                label = stringResource(R.string.manga_color_filter_blue),
                value = blue,
                valueRange = 0f..255f,
                onValueChange = { b ->
                    val argb = 0xFF000000.toInt() or (red.toInt() shl 16) or (green.toInt() shl 8) or b.toInt()
                    onTextColorChanged("%08X".format(argb))
                },
                valueText = "%.0f".format(blue),
                enabled = !isScrolling,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp),
            )

            HorizontalDivider(
                thickness = NovelaSize.hairline,
                color = colors.borderSubtle,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            )

            // Reader Background Presets & Custom Image
            val currentBackground = state.readerBackground.value
            StyleSectionLabel(
                icon = Icons.Outlined.FormatColorFill,
                title = stringResource(R.string.reader_background),
            )
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(
                    listOf("") +
                        ReaderBackgroundPresets.map { it.id } +
                        BackgroundImageLoader.availableBackgrounds.value.map { "background_file:" + it.name }
                ) { value ->
                    val selected = currentBackground == value
                    val borderModifier = Modifier.border(
                        width = if (selected) 2.5.dp else 1.dp,
                        color = if (selected) colors.accent else colors.borderSubtle,
                        shape = CircleShape,
                    )
                    when {
                        value.isEmpty() -> {
                            val autoDesc = stringResource(R.string.reader_background_auto)
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(colors.surfaceMuted)
                                    .then(borderModifier)
                                    .semantics { contentDescription = autoDesc }
                                    .clickable {
                                        onBackgroundChanged("")
                                        onTextColorChanged("")
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "A",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) colors.accent else colors.textSecondary,
                                )
                            }
                        }
                        else -> {
                            val preset = ReaderBackgroundPresets.firstOrNull { it.id == value }
                            if (preset != null) {
                                val presetName = stringResource(preset.nameRes)
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Brush.verticalGradient(preset.colors))
                                        .then(borderModifier)
                                        .semantics { contentDescription = presetName }
                                        .clickable {
                                            onBackgroundChanged(value)
                                            onTextColorChanged(preset.textColor)
                                        },
                                )
                            } else {
                                AsyncImage(
                                    model = BackgroundImageLoader.resolveFile(value),
                                    contentDescription = stringResource(R.string.reader_background_custom),
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .then(borderModifier)
                                        .clickable { onBackgroundChanged(value) },
                                )
                            }
                        }
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 14.dp),
            ) {
                TextButton(
                    onClick = onDoAskForImage { uri ->
                        scope.launch {
                            backgroundLoader.importBackgroundImage(uri).onFailure { e ->
                                Toast.makeText(
                                    context,
                                    e.message ?: "Background import failed",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }.onSuccess {
                                val rawName = context.contentResolver
                                    .query(uri, null, null, null, null)?.use { cursor ->
                                        if (cursor.moveToFirst()) {
                                            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                                            if (idx >= 0) cursor.getString(idx) else null
                                        } else null
                                    } ?: uri.lastPathSegment ?: "image"
                                var fileName = BackgroundImageLoader.sanitizeFileName(rawName)
                                if (!BackgroundImageLoader.isValidImageExtension(fileName)) {
                                    fileName += ".png"
                                }
                                onBackgroundChanged("background_file:$fileName")
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        Icons.Outlined.FileUpload,
                        null,
                        tint = colors.accent,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.reader_background_custom),
                        color = colors.accent,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                if (BackgroundImageLoader.isImported(currentBackground)) {
                    TextButton(
                        onClick = {
                            backgroundLoader.deleteBackground(currentBackground)
                            onBackgroundChanged("")
                        }
                    ) {
                        Icon(
                            Icons.Outlined.Delete,
                            null,
                            tint = colors.danger,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.reader_background_delete),
                            color = colors.danger,
                        )
                    }
                }
            }

            HorizontalDivider(
                thickness = NovelaSize.hairline,
                color = colors.borderSubtle,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            )

            // Dark Mode & Theme Swatches
            StyleSectionLabel(
                icon = Icons.Outlined.ColorLens,
                title = stringResource(R.string.theme),
            )
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(DarkMode.entries) { mode ->
                    FilterChip(
                        selected = mode == state.currentDarkMode.value,
                        onClick = { onDarkModeChange(mode) },
                        shape = RoundedCornerShape(NovelaRadius.pill),
                        colors = chipColors,
                        label = { Text(text = stringResource(id = mode.titleRes)) },
                        enabled = !isScrolling,
                        leadingIcon = {
                            Icon(
                                imageVector = when (mode) {
                                    DarkMode.SYSTEM -> Icons.Outlined.BrightnessMedium
                                    DarkMode.LIGHT -> Icons.Outlined.LightMode
                                    DarkMode.DARK -> Icons.Outlined.DarkMode
                                    DarkMode.BLACK -> Icons.Outlined.Nightlight
                                },
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                            )
                        },
                        modifier = Modifier.heightIn(min = 32.dp),
                    )
                }
            }

            StyleSectionLabel(
                icon = Icons.Outlined.Palette,
                title = stringResource(R.string.color_scheme),
            )
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(AppTheme.entries) { theme ->
                    CompactThemePreviewChip(
                        theme = theme,
                        isSelected = theme == state.currentAppTheme.value,
                        onClick = { onAppThemeChange(theme) },
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun StyleSectionLabel(
    icon: ImageVector?,
    title: String,
) {
    val colors = NovelaTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = colors.textPrimary,
        )
    }
}

@Composable
private fun CompactThemePreviewChip(
    theme: AppTheme,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val colors = NovelaTheme.colors
    val accentColor = if (theme.isMonet) Color(0xFFE8593C)
    else Color(getThemeAccentColor(theme))

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(NovelaRadius.sm))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                onClick = onClick,
            )
            .width(48.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(accentColor)
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) colors.textPrimary else colors.borderSubtle,
                    shape = CircleShape,
                ),
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = stringResource(id = theme.titleRes),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) colors.accent else colors.textMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun getThemeAccentColor(theme: AppTheme): Long = when (theme) {
    AppTheme.DEFAULT -> 0xFFE8593C
    AppTheme.TACHIYOMI -> 0xFF0088FF
    AppTheme.GREEN_APPLE -> 0xFF188140
    AppTheme.LAVENDER -> 0xFFA177FF
    AppTheme.MIDNIGHT_DUSK -> 0xFFF02475
    AppTheme.STRAWBERRY_DAIQUIRI -> 0xFFED4A65
    AppTheme.TAKO -> 0xFFF3B375
    AppTheme.TEALTURQUOISE -> 0xFF40E0D0
    AppTheme.TIDAL_WAVE -> 0xFF5ED4FC
    AppTheme.YOTSUBA -> 0xFFAE3200
    AppTheme.MONOCHROME -> 0xFF6B7280
    AppTheme.CATPPUCCIN -> 0xFFCBA6F7
    AppTheme.NORD -> 0xFF88C0D0
    AppTheme.YINYANG -> 0xFF374151
    AppTheme.CLOUDFLARE -> 0xFFF38020
    AppTheme.COTTONCANDY -> 0xFFFF8DA1
    AppTheme.DOOM -> 0xFFEF4444
    AppTheme.MATRIX -> 0xFF10B981
    AppTheme.MOCHA -> 0xFFBF9270
    AppTheme.SAPPHIRE -> 0xFF1E88E5
}

private val TextColorPalette = listOf(
    "FF000000", "FF212121", "FF616161", "FFFFFFFF",
    "FF3B2F1E", "FF1A237E", "FF0D47A1", "FF1B5E20",
    "FFB71C1C", "FF4A148C", "FF004D40", "FFBF360C",
)

private val FONT_MIME_TYPES = arrayOf(
    "font/ttf",
    "font/otf",
    "application/x-font-ttf",
    "application/x-font-otf",
    "application/vnd.ms-opentype",
    "application/font-sfnt",
)

private fun safeParseColor(hex: String): Int =
    runCatching { android.graphics.Color.parseColor("#$hex") }.getOrElse { 0xFF333333.toInt() }

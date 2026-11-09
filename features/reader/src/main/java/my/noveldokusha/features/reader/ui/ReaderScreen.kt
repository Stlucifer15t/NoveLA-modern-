package my.noveldokusha.features.reader.ui

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Rule
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import my.noveldokusha.core.appPreferences.AppPreferences
import my.noveldokusha.coreui.theme.AppTheme
import my.noveldokusha.coreui.theme.DarkMode
import my.noveldokusha.coreui.theme.InternalTheme
import my.noveldokusha.coreui.theme.NovelaRadius
import my.noveldokusha.coreui.theme.NovelaSize
import my.noveldokusha.coreui.theme.NovelaSpacing
import my.noveldokusha.coreui.theme.NovelaTheme
import my.noveldokusha.coreui.theme.rememberMutableStateOf
import my.noveldokusha.features.reader.domain.ReaderItem
import my.noveldokusha.features.reader.features.LiveTranslationSettingData
import my.noveldokusha.features.reader.features.ManualHighlightSettingData
import my.noveldokusha.features.reader.features.TextSynthesis
import my.noveldokusha.features.reader.features.TextToSpeechSettingData
import my.noveldokusha.features.reader.services.FloatingTtsService
import my.noveldokusha.features.reader.ui.ReaderScreenState.Settings.Type
import my.noveldokusha.reader.R
import my.noveldokusha.settings.RegexCleanupSettingsViewModel
import my.noveldokusha.text_to_speech.Utterance
import my.noveldokusha.text_to_speech.VoiceData
import my.noveldokusha.text_translator.domain.TranslationModelState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReaderScreen(
    state: ReaderScreenState,
    appPreferences: AppPreferences,
    onSelectableTextChange: (Boolean) -> Unit,
    onKeepScreenOn: (Boolean) -> Unit,
    onDarkModeSelected: (DarkMode) -> Unit,
    onAppThemeChanged: (AppTheme) -> Unit,
    onFullScreen: (Boolean) -> Unit,
    onSingleTapToOpenSettingsChange: (Boolean) -> Unit,
    onTextFontChanged: (String) -> Unit,
    onTextColorChanged: (String) -> Unit,
    onBackgroundChanged: (String) -> Unit,
    onTextSizeChanged: (Float) -> Unit,
    onLineHeightChanged: (Float) -> Unit,
    onParagraphSpacingChanged: (Float) -> Unit,
    onLetterSpacingChanged: (Float) -> Unit,
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
    onPressBack: () -> Unit,
    onOpenChapterInWeb: () -> Unit,
    regexCleanupViewModel: RegexCleanupSettingsViewModel? = null,
    onTtsHighlightEnabledChange: (Boolean) -> Unit,
    onTtsHighlightColorChange: (String) -> Unit,
    onManualHighlightEnabledChange: (Boolean) -> Unit = {},
    onTextDefaultsReset: () -> Unit = {},
    manualHighlight: ManualHighlightSettingData? = null,
    onManualHighlightStart: () -> Unit = {},
    manualHighlightInitialPosition: Pair<Float, Float>? = null,
    onManualHighlightPositionChange: (Float, Float) -> Unit = { _, _ -> },
    readerContent: @Composable (paddingValues: PaddingValues) -> Unit,
) {
    val colors = NovelaTheme.colors
    val showReaderInfo by state.showReaderInfo
    val selectedSetting by state.settings.selectedSetting
    val fullScreen by state.settings.fullScreen
    val manualHighlightEnabled by state.settings.manualHighlightEnabled
    var readerAreaSize by remember { mutableStateOf(IntSize.Zero) }

    val context = LocalContext.current
    val windowToken = LocalView.current.windowToken

    // Capture back action when viewing info
    BackHandler(enabled = showReaderInfo) {
        state.showReaderInfo.value = false
    }

    LaunchedEffect(showReaderInfo) {
        if (!showReaderInfo) {
            state.settings.selectedSetting.value = Type.None
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { readerAreaSize = it }
    ) {
        // Reader background layer (theme, preset gradient, or imported image)
        when (val layer = backgroundLayer(state.settings.style.readerBackground.value)) {
            BackgroundLayer.None -> Spacer(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            )
            is BackgroundLayer.Preset -> Spacer(
                Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(layer.preset.colors))
            )
            is BackgroundLayer.Image -> AsyncImage(
                model = layer.file,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                AnimatedVisibility(
                    visible = showReaderInfo,
                    enter = expandVertically(initialHeight = { 0 }, expandFrom = Alignment.Top) + fadeIn(),
                    exit = shrinkVertically(targetHeight = { 0 }, shrinkTowards = Alignment.Top) + fadeOut(),
                ) {
                    val chapterTitle by state.readerInfo.chapterTitle
                    val autoscrollOn by appPreferences.READER_AUTOSCROLL_ENABLED.flow()
                        .collectAsState(initial = appPreferences.READER_AUTOSCROLL_ENABLED.value)
                    val speed by appPreferences.READER_AUTOSCROLL_SPEED.flow()
                        .collectAsState(initial = appPreferences.READER_AUTOSCROLL_SPEED.value)

                    val toggleOrSet = { type: Type ->
                        state.settings.selectedSetting.value =
                            if (selectedSetting == type) Type.None else type
                    }

                    Column(
                        modifier = Modifier
                            .statusBarsPadding()
                            .then(if (fullScreen) Modifier.displayCutoutPadding() else Modifier)
                            .padding(horizontal = NovelaSpacing.sm, vertical = NovelaSpacing.xs),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        // Frosted Floating Reader Top Bar
                        Surface(
                            color = colors.surfaceElevated.copy(alpha = 0.95f),
                            shape = RoundedCornerShape(NovelaRadius.cardLarge),
                            shadowElevation = 10.dp,
                            border = BorderStroke(NovelaSize.hairline, colors.borderSubtle),
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateContentSize(),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                // Row 1: Back + Chapter Title + Open in Web
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    IconButton(
                                        onClick = onPressBack,
                                        modifier = Modifier.size(36.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                            contentDescription = null,
                                            tint = colors.textPrimary,
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }
                                    Text(
                                        text = chapterTitle,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 4.dp),
                                    )
                                    IconButton(
                                        onClick = onOpenChapterInWeb,
                                        modifier = Modifier.size(36.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Public,
                                            contentDescription = stringResource(R.string.open_in_browser),
                                            tint = colors.textSecondary,
                                            modifier = Modifier.size(19.dp),
                                        )
                                    }
                                }

                                // Row 2: Reader Tool Pills (Translate, TTS, Style, Regex, More, AutoScroll)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    if (state.settings.liveTranslation.isAvailable) {
                                        ReaderTopToolPill(
                                            icon = Icons.Outlined.Translate,
                                            contentDescription = stringResource(R.string.translator),
                                            active = selectedSetting == Type.LiveTranslation,
                                            badgeDot = state.settings.liveTranslation.enable.value,
                                            onClick = { toggleOrSet(Type.LiveTranslation) },
                                        )
                                    }
                                    ReaderTopToolPill(
                                        icon = Icons.Rounded.RecordVoiceOver,
                                        contentDescription = stringResource(R.string.voice_reader),
                                        active = selectedSetting == Type.TextToSpeech,
                                        badgeDot = state.settings.textToSpeech.isPlaying.value,
                                        onClick = { toggleOrSet(Type.TextToSpeech) },
                                    )
                                    ReaderTopToolPill(
                                        icon = Icons.Outlined.ColorLens,
                                        contentDescription = stringResource(R.string.style),
                                        active = selectedSetting == Type.Style,
                                        onClick = { toggleOrSet(Type.Style) },
                                    )
                                    ReaderTopToolPill(
                                        icon = Icons.Outlined.Rule,
                                        contentDescription = stringResource(R.string.regex_cleanup_novel_rules),
                                        active = selectedSetting == Type.RegexRules,
                                        onClick = { toggleOrSet(Type.RegexRules) },
                                    )
                                    ReaderTopToolPill(
                                        icon = Icons.Rounded.Tune,
                                        contentDescription = stringResource(R.string.more),
                                        active = selectedSetting == Type.More,
                                        onClick = { toggleOrSet(Type.More) },
                                    )
                                    ReaderTopToolPill(
                                        icon = if (autoscrollOn) Icons.Rounded.Pause else Icons.Outlined.PlayArrow,
                                        contentDescription = stringResource(R.string.manga_reader_auto_scroll),
                                        active = autoscrollOn,
                                        onClick = {
                                            appPreferences.READER_AUTOSCROLL_ENABLED.value = !autoscrollOn
                                        },
                                    )
                                }
                            }
                        }

                        // Autoscroll speed pill bar
                        if (autoscrollOn) {
                            Surface(
                                color = colors.surfaceElevated.copy(alpha = 0.94f),
                                shape = RoundedCornerShape(NovelaRadius.pill),
                                border = BorderStroke(NovelaSize.hairline, colors.borderSubtle),
                                shadowElevation = 6.dp,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    IconButton(
                                        onClick = {
                                            appPreferences.READER_AUTOSCROLL_SPEED.value =
                                                (speed - 10).coerceAtLeast(10)
                                        },
                                        modifier = Modifier.size(32.dp),
                                    ) {
                                        Icon(
                                            Icons.Rounded.Remove,
                                            contentDescription = null,
                                            tint = colors.textPrimary,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                    Text(
                                        text = stringResource(R.string.manga_autoscroll_speed_value, speed),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.accent,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.weight(1f),
                                    )
                                    IconButton(
                                        onClick = {
                                            appPreferences.READER_AUTOSCROLL_SPEED.value =
                                                (speed + 10).coerceAtMost(200)
                                        },
                                        modifier = Modifier.size(32.dp),
                                    ) {
                                        Icon(
                                            Icons.Rounded.Add,
                                            contentDescription = null,
                                            tint = colors.textPrimary,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            content = readerContent,
            bottomBar = {
                AnimatedVisibility(
                    visible = showReaderInfo,
                    enter = expandVertically(initialHeight = { 0 }) + fadeIn(),
                    exit = shrinkVertically(targetHeight = { 0 }) + fadeOut(),
                ) {
                    Column(modifier = Modifier.navigationBarsPadding()) {
                        ReaderScreenBottomBarDialogs(
                            settings = state.settings,
                            regexCleanupViewModel = regexCleanupViewModel,
                            onTextFontChanged = onTextFontChanged,
                            onTextColorChanged = onTextColorChanged,
                            onBackgroundChanged = onBackgroundChanged,
                            onTextSizeChanged = onTextSizeChanged,
                            onLineHeightChanged = onLineHeightChanged,
                            onParagraphSpacingChanged = onParagraphSpacingChanged,
                            onLetterSpacingChanged = onLetterSpacingChanged,
                            onSelectableTextChange = onSelectableTextChange,
                            onDarkModeSelected = onDarkModeSelected,
                            onAppThemeSelected = onAppThemeChanged,
                            onKeepScreenOn = onKeepScreenOn,
                            onFullScreen = onFullScreen,
                            onSingleTapToOpenSettingsChange = onSingleTapToOpenSettingsChange,
                            onTextJustifyChange = onTextJustifyChange,
                            onTextHyphenationChange = onTextHyphenationChange,
                            onParagraphIndentChange = onParagraphIndentChange,
                            onParagraphFirstLetterBoldChange = onParagraphFirstLetterBoldChange,
                            onSentenceSplittingChange = onSentenceSplittingChange,
                            onTextBoldChange = onTextBoldChange,
                            onTextItalicChange = onTextItalicChange,
                            onTextUnderlineChange = onTextUnderlineChange,
                            onTextShadowChange = onTextShadowChange,
                            onTextSmoothChange = onTextSmoothChange,
                            onMarginLeftChange = onMarginLeftChange,
                            onMarginRightChange = onMarginRightChange,
                            onMarginTopChange = onMarginTopChange,
                            onMarginBottomChange = onMarginBottomChange,
                            onTtsHighlightEnabledChange = onTtsHighlightEnabledChange,
                            onTtsHighlightColorChange = onTtsHighlightColorChange,
                            onManualHighlightEnabledChange = onManualHighlightEnabledChange,
                            onTextDefaultsReset = onTextDefaultsReset,
                            modifier = Modifier.padding(bottom = 6.dp),
                        )

                        Box(modifier = Modifier.fillMaxWidth()) {
                            var quickToggleNotice by remember { mutableStateOf<String?>(null) }
                            LaunchedEffect(quickToggleNotice) {
                                if (quickToggleNotice != null) {
                                    delay(1200)
                                    quickToggleNotice = null
                                }
                            }

                            val chapterCurrentNumber by state.readerInfo.chapterCurrentNumber
                            val chaptersCount by state.readerInfo.chaptersCount
                            val chapterPercentageProgress by state.readerInfo.chapterPercentageProgress
                            val keepScreenOn by state.settings.keepScreenOn
                            val isTextSelectable by state.settings.isTextSelectable
                            val isSingleTapToOpenSettings by state.settings.isSingleTapToOpenSettings

                            val keepScreenOnLabel = stringResource(R.string.keep_screen_on)
                            val fullScreenLabel = stringResource(R.string.features_reader_full_screen)
                            val selectableTextLabel = stringResource(R.string.allow_text_selection)
                            val singleTapLabel = stringResource(R.string.single_tap_to_open_settings)

                            val enabledLabel = stringResource(R.string.rule_enabled)
                            val disabledLabel = stringResource(R.string.rule_disabled)

                            // Frosted Floating Bottom Dock
                            Surface(
                                color = colors.surfaceElevated.copy(alpha = 0.95f),
                                shape = RoundedCornerShape(NovelaRadius.cardLarge),
                                shadowElevation = 10.dp,
                                border = BorderStroke(NovelaSize.hairline, colors.borderSubtle),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = NovelaSpacing.sm, vertical = NovelaSpacing.xs)
                                    .animateContentSize(),
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    // Chapter index + Percentage Progress Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = stringResource(
                                                id = R.string.chapter_x_over_n,
                                                chapterCurrentNumber,
                                                chaptersCount,
                                            ),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colors.textPrimary,
                                        )
                                        Text(
                                            text = stringResource(
                                                id = R.string.progress_x_percentage,
                                                chapterPercentageProgress,
                                            ),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.accent,
                                        )
                                    }

                                    // Smooth Linear Chapter Progress Indicator
                                    LinearProgressIndicator(
                                        progress = { (chapterPercentageProgress / 100f).coerceIn(0f, 1f) },
                                        color = colors.accent,
                                        trackColor = colors.surfaceMuted,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(NovelaRadius.pill)),
                                    )

                                    // Quick Toggle Buttons Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        ReaderQuickTogglePill(
                                            icon = Icons.Rounded.WbSunny,
                                            contentDescription = keepScreenOnLabel,
                                            active = keepScreenOn,
                                            onClick = {
                                                onKeepScreenOn(!keepScreenOn)
                                                quickToggleNotice =
                                                    "$keepScreenOnLabel: ${if (!keepScreenOn) enabledLabel else disabledLabel}"
                                            },
                                        )
                                        ReaderQuickTogglePill(
                                            icon = Icons.Rounded.Fullscreen,
                                            contentDescription = fullScreenLabel,
                                            active = fullScreen,
                                            onClick = {
                                                onFullScreen(!fullScreen)
                                                quickToggleNotice =
                                                    "$fullScreenLabel: ${if (!fullScreen) enabledLabel else disabledLabel}"
                                            },
                                        )
                                        ReaderQuickTogglePill(
                                            icon = Icons.Rounded.TextFields,
                                            contentDescription = selectableTextLabel,
                                            active = isTextSelectable,
                                            onClick = {
                                                onSelectableTextChange(!isTextSelectable)
                                                quickToggleNotice =
                                                    "$selectableTextLabel: ${if (!isTextSelectable) enabledLabel else disabledLabel}"
                                            },
                                        )
                                        ReaderQuickTogglePill(
                                            icon = Icons.Rounded.TouchApp,
                                            contentDescription = singleTapLabel,
                                            active = isSingleTapToOpenSettings,
                                            onClick = {
                                                onSingleTapToOpenSettingsChange(!isSingleTapToOpenSettings)
                                                quickToggleNotice =
                                                    "$singleTapLabel: ${if (!isSingleTapToOpenSettings) enabledLabel else disabledLabel}"
                                            },
                                        )
                                    }
                                }
                            }

                            // Floating quick-toggle notice pill above bottom bar
                            androidx.compose.animation.AnimatedVisibility(
                                visible = quickToggleNotice != null,
                                enter = fadeIn(),
                                exit = fadeOut(),
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .offset(y = (-42).dp),
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(NovelaRadius.pill),
                                    color = colors.textPrimary,
                                    shadowElevation = 6.dp,
                                    modifier = Modifier.padding(vertical = 4.dp),
                                ) {
                                    Text(
                                        text = quickToggleNotice ?: "",
                                        color = colors.background,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            },
        )

        var showOverlayPermissionDialog by remember { mutableStateOf(false) }

        val overlayPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) {
            if (Settings.canDrawOverlays(context)) {
                FloatingTtsService.activityWindowToken = windowToken
                FloatingTtsService.ttsState.value = state.settings.textToSpeech
                FloatingTtsService.showOutsideApp.value =
                    state.settings.floatingTts.showOutsideApp.value
                FloatingTtsService.opacity.value = state.settings.floatingTts.opacity.value
                FloatingTtsService.start(context)
            }
        }

        if (showOverlayPermissionDialog) {
            AlertDialog(
                onDismissRequest = { showOverlayPermissionDialog = false },
                containerColor = colors.surfaceElevated,
                shape = RoundedCornerShape(NovelaRadius.cardLarge),
                title = {
                    Text(
                        text = stringResource(R.string.tts_floating_overlay_permission_title),
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                    )
                },
                text = {
                    Text(
                        text = stringResource(R.string.tts_floating_overlay_permission_message),
                        color = colors.textSecondary,
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showOverlayPermissionDialog = false
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}"),
                            )
                            overlayPermissionLauncher.launch(intent)
                        },
                        shape = RoundedCornerShape(NovelaRadius.pill),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.accent,
                            contentColor = colors.onAccent,
                        ),
                    ) {
                        Text(
                            text = stringResource(R.string.tts_floating_open_settings),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showOverlayPermissionDialog = false }) {
                        Text(
                            text = stringResource(android.R.string.cancel),
                            color = colors.textSecondary,
                        )
                    }
                },
            )
        }

        LaunchedEffect(state.settings.floatingTts.isEnabled.value) {
            val floatingEnabled = state.settings.floatingTts.isEnabled.value
            if (floatingEnabled) {
                if (Settings.canDrawOverlays(context)) {
                    FloatingTtsService.activityWindowToken = windowToken
                    FloatingTtsService.ttsState.value = state.settings.textToSpeech
                    FloatingTtsService.showOutsideApp.value =
                        state.settings.floatingTts.showOutsideApp.value
                    FloatingTtsService.opacity.value = state.settings.floatingTts.opacity.value
                    FloatingTtsService.ttsHighlightEnabled.value =
                        state.settings.ttsHighlight.isEnabled.value
                    FloatingTtsService.ttsHighlightColor.value =
                        state.settings.ttsHighlight.highlightColor.value
                    FloatingTtsService.start(context)
                } else {
                    showOverlayPermissionDialog = true
                    state.settings.floatingTts.isEnabled.value = false
                }
            } else {
                FloatingTtsService.stop(context)
            }
        }

        LaunchedEffect(
            state.settings.floatingTts.showOutsideApp.value,
            state.settings.floatingTts.opacity.value,
            state.settings.ttsHighlight.isEnabled.value,
            state.settings.ttsHighlight.highlightColor.value,
        ) {
            FloatingTtsService.showOutsideApp.value =
                state.settings.floatingTts.showOutsideApp.value
            FloatingTtsService.opacity.value = state.settings.floatingTts.opacity.value
            FloatingTtsService.ttsHighlightEnabled.value =
                state.settings.ttsHighlight.isEnabled.value
            FloatingTtsService.ttsHighlightColor.value =
                state.settings.ttsHighlight.highlightColor.value
        }

        LaunchedEffect(state.settings.floatingTts.showOutsideApp.value) {
            if (FloatingTtsService.isRunning(context)) {
                FloatingTtsService.activityWindowToken = windowToken
                FloatingTtsService.recreateOverlay()
            }
        }

        val lifecycle = LocalLifecycleOwner.current.lifecycle

        DisposableEffect(
            lifecycle,
            state.settings.selectedSetting.value,
            state.settings.floatingTts.showOutsideApp.value,
        ) {
            val currentSelected = state.settings.selectedSetting.value
            val showOutsideApp = state.settings.floatingTts.showOutsideApp.value

            val observer = LifecycleEventObserver { _, event ->
                if (!FloatingTtsService.isRunning(context)) return@LifecycleEventObserver
                when (event) {
                    Lifecycle.Event.ON_RESUME -> {
                        FloatingTtsService.setOverlayHidden(currentSelected != Type.None)
                    }
                    Lifecycle.Event.ON_PAUSE -> {
                        FloatingTtsService.setOverlayHidden(!showOutsideApp)
                    }
                    else -> {}
                }
            }
            lifecycle.addObserver(observer)

            if (FloatingTtsService.isRunning(context)) {
                FloatingTtsService.setOverlayHidden(currentSelected != Type.None)
            }

            onDispose { lifecycle.removeObserver(observer) }
        }

        val manual = manualHighlight
        val manualHighlightVisible = manual != null &&
            manualHighlightEnabled &&
            selectedSetting == Type.None &&
            !state.settings.textToSpeech.isPlaying.value

        if (manualHighlightVisible) {
            ManualHighlightPill(
                areaSize = readerAreaSize,
                highlightedItem = manual.highlightedItem.value,
                onStart = onManualHighlightStart,
                onNext = manual.next,
                onPrevious = manual.previous,
                onClear = manual.clear,
                initialPosition = manualHighlightInitialPosition,
                onPositionChange = onManualHighlightPositionChange,
            )
        }
    }
}

@Composable
private fun ReaderTopToolPill(
    icon: ImageVector,
    contentDescription: String,
    active: Boolean,
    badgeDot: Boolean = false,
    onClick: () -> Unit,
) {
    val colors = NovelaTheme.colors
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(NovelaRadius.pill),
        color = if (active) colors.accentSoft else Color.Transparent,
        border = if (active) BorderStroke(NovelaSize.hairline, colors.accent.copy(alpha = 0.55f)) else null,
        modifier = Modifier.size(width = 44.dp, height = 34.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (active) colors.accent else colors.textSecondary,
                modifier = Modifier.size(19.dp),
            )
            if (badgeDot && !active) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 6.dp, end = 9.dp)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(colors.accent),
                )
            }
        }
    }
}

@Composable
private fun ReaderQuickTogglePill(
    icon: ImageVector,
    contentDescription: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    val colors = NovelaTheme.colors
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(NovelaRadius.pill),
        color = if (active) colors.accentSoft else colors.surfaceMuted,
        border = BorderStroke(
            NovelaSize.hairline,
            if (active) colors.accent.copy(alpha = 0.55f) else colors.borderSubtle,
        ),
        modifier = Modifier.size(width = 52.dp, height = 34.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (active) colors.accent else colors.textSecondary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ViewsPreview(
    @PreviewParameter(PreviewDataProvider::class) data: PreviewDataProvider.Data,
) {
    val liveTranslationSettingData = LiveTranslationSettingData(
        isAvailable = true,
        enable = remember { mutableStateOf(true) },
        listOfAvailableModels = remember { mutableStateListOf() },
        source = remember {
            mutableStateOf(
                TranslationModelState(
                    language = "fr",
                    available = true,
                    downloading = false,
                    downloadingFailed = false,
                )
            )
        },
        target = remember {
            mutableStateOf(
                TranslationModelState(
                    language = "en",
                    available = true,
                    downloading = false,
                    downloadingFailed = false,
                )
            )
        },
        onTargetChange = {},
        onEnable = {},
        onSourceChange = {},
        onRedoTranslation = {},
        novelPrompt = remember { mutableStateOf("") },
        onNovelPromptChange = {},
        novelPromptAppendMode = remember { mutableStateOf(false) },
        onNovelPromptAppendModeChange = {},
        currentProvider = remember { mutableStateOf("GOOGLE_PA") },
        onProviderChange = {},
        parallelEnabled = remember { mutableStateOf(false) },
        onParallelEnabledChange = {},
        parallelOrder = remember { mutableStateOf("ORIGINAL_FIRST") },
        onParallelOrderChange = {},
        translationGlobalMode = remember { mutableStateOf(false) },
        onTranslationGlobalModeChange = {},
    )

    val textToSpeechSettingData = TextToSpeechSettingData(
        isPlaying = rememberMutableStateOf(false),
        isLoadingChapter = rememberMutableStateOf(false),
        voicePitch = rememberMutableStateOf(1f),
        voiceSpeed = rememberMutableStateOf(1f),
        availableVoices = remember { mutableStateListOf() },
        activeVoice = remember {
            mutableStateOf(
                VoiceData(
                    id = "",
                    language = "",
                    quality = 100,
                    needsInternet = true,
                    enginePackage = "",
                )
            )
        },
        currentActiveItemState = remember {
            mutableStateOf(
                TextSynthesis(
                    playState = Utterance.PlayState.PLAYING,
                    itemPos = ReaderItem.Title(
                        chapterUrl = "",
                        chapterIndex = 0,
                        chapterItemPosition = 1,
                        text = "",
                    ),
                )
            )
        },
        isThereActiveItem = rememberMutableStateOf(true),
        setPlaying = {},
        playPreviousItem = {},
        playPreviousChapter = {},
        playNextItem = {},
        playNextChapter = {},
        setVoiceId = {},
        playFirstVisibleItem = {},
        scrollToActiveItem = {},
        setVoiceSpeed = {},
        setVoicePitch = {},
        setCustomSavedVoices = {},
        customSavedVoices = rememberMutableStateOf(value = listOf()),
        chapterWordCount = remember { mutableStateOf(0) },
        remainingWordCount = remember { mutableStateOf(0) },
        estimatedWpm = remember { mutableStateOf(0) },
        estimatedTotalSeconds = remember { mutableStateOf(0) },
        estimatedRemainingSeconds = remember { mutableStateOf(0) },
        currentParagraphText = remember { mutableStateOf("") },
        alternateParagraphText = remember { mutableStateOf("") },
        parallelEnabled = remember { mutableStateOf(false) },
        originalVoiceId = remember { mutableStateOf("") },
        setOriginalVoiceId = {},
        spokenWordRange = remember { mutableStateOf(null) },
    )

    val style = ReaderScreenState.Settings.StyleSettingsData(
        currentDarkMode = remember { mutableStateOf(DarkMode.DARK) },
        currentAppTheme = remember { mutableStateOf(AppTheme.DEFAULT) },
        textFont = remember { mutableStateOf("Arial") },
        textColor = remember { mutableStateOf("") },
        readerBackground = remember { mutableStateOf("") },
        textSize = remember { mutableFloatStateOf(20f) },
        lineHeight = remember { mutableFloatStateOf(1.35f) },
        paragraphSpacing = remember { mutableFloatStateOf(8f) },
        letterSpacing = remember { mutableFloatStateOf(0f) },
        textJustify = remember { mutableStateOf(false) },
        textHyphenation = remember { mutableStateOf(false) },
        textBold = remember { mutableStateOf(false) },
        textItalic = remember { mutableStateOf(false) },
        textUnderline = remember { mutableStateOf(false) },
        textShadow = remember { mutableStateOf(false) },
        textSmooth = remember { mutableStateOf(true) },
        marginLeft = remember { mutableFloatStateOf(16f) },
        marginRight = remember { mutableFloatStateOf(16f) },
        marginTop = remember { mutableFloatStateOf(0f) },
        marginBottom = remember { mutableFloatStateOf(0f) },
    )

    InternalTheme {
        Surface(color = Color.Black) {
            ReaderScreen(
                appPreferences = AppPreferences(LocalContext.current),
                state = ReaderScreenState(
                    showReaderInfo = remember { mutableStateOf(true) },
                    readerInfo = ReaderScreenState.CurrentInfo(
                        chapterTitle = remember { mutableStateOf("Chapter title") },
                        chapterCurrentNumber = remember { mutableIntStateOf(2) },
                        chapterPercentageProgress = remember { mutableFloatStateOf(50f) },
                        chaptersCount = remember { mutableIntStateOf(255) },
                        chapterUrl = remember { mutableStateOf("Chapter url") },
                    ),
                    settings = ReaderScreenState.Settings(
                        isTextSelectable = remember { mutableStateOf(false) },
                        keepScreenOn = remember { mutableStateOf(false) },
                        textToSpeech = textToSpeechSettingData,
                        liveTranslation = liveTranslationSettingData,
                        style = style,
                        selectedSetting = remember { mutableStateOf(data.selectedSetting) },
                        fullScreen = remember { mutableStateOf(false) },
                        isSingleTapToOpenSettings = remember { mutableStateOf(false) },
                        floatingTts = ReaderScreenState.Settings.FloatingTtsSettingsData(
                            isEnabled = remember { mutableStateOf(false) },
                            showOutsideApp = remember { mutableStateOf(true) },
                            opacity = remember { mutableFloatStateOf(0.6f) },
                        ),
                        ttsHighlight = ReaderScreenState.Settings.TtsHighlightSettingsData(
                            isEnabled = remember { mutableStateOf(false) },
                            highlightColor = remember { mutableStateOf("FFFF6D00") },
                        ),
                        manualHighlight = ManualHighlightSettingData(
                            highlightedItem = remember { mutableStateOf(null) },
                            next = {},
                            previous = {},
                            clear = {},
                        ),
                        manualHighlightEnabled = remember { mutableStateOf(false) },
                    ),
                    showInvalidChapterDialog = remember { mutableStateOf(false) },
                ),
                onTextSizeChanged = {},
                onLineHeightChanged = {},
                onParagraphSpacingChanged = {},
                onLetterSpacingChanged = {},
                onTextFontChanged = {},
                onTextColorChanged = {},
                onBackgroundChanged = {},
                onSelectableTextChange = {},
                onDarkModeSelected = {},
                onAppThemeChanged = {},
                onPressBack = {},
                onOpenChapterInWeb = {},
                readerContent = {},
                onKeepScreenOn = {},
                onFullScreen = {},
                onSingleTapToOpenSettingsChange = {},
                onTextJustifyChange = {},
                onTextHyphenationChange = {},
                onParagraphIndentChange = {},
                onParagraphFirstLetterBoldChange = {},
                onSentenceSplittingChange = {},
                onTextBoldChange = {},
                onTextItalicChange = {},
                onTextUnderlineChange = {},
                onTextShadowChange = {},
                onTextSmoothChange = {},
                onMarginLeftChange = {},
                onMarginRightChange = {},
                onMarginTopChange = {},
                onMarginBottomChange = {},
                onTtsHighlightEnabledChange = {},
                onTtsHighlightColorChange = {},
                onTextDefaultsReset = {},
            )
        }
    }
}

private class PreviewDataProvider : PreviewParameterProvider<PreviewDataProvider.Data> {
    data class Data(
        val selectedSetting: Type,
    )

    override val values = sequenceOf(
        Data(selectedSetting = Type.None),
        Data(selectedSetting = Type.LiveTranslation),
        Data(selectedSetting = Type.TextToSpeech),
        Data(selectedSetting = Type.Style),
        Data(selectedSetting = Type.More),
    )
}

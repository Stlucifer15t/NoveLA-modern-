package my.noveldokusha.features.reader.ui.settingDialogs

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.NavigateBefore
import androidx.compose.material.icons.automirrored.rounded.NavigateNext
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.CenterFocusWeak
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.StarRate
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.withContext
import my.noveldokusha.core.appPreferences.VoicePredefineState
import my.noveldokusha.coreui.components.MyOutlinedTextField
import my.noveldokusha.coreui.components.PillSlider
import my.noveldokusha.coreui.components.SlimListItem
import my.noveldokusha.coreui.composableActions.debouncedAction
import my.noveldokusha.coreui.theme.InternalTheme
import my.noveldokusha.coreui.theme.NovelaRadius
import my.noveldokusha.coreui.theme.NovelaSize
import my.noveldokusha.coreui.theme.NovelaTheme
import my.noveldokusha.coreui.theme.NovelaType
import my.noveldokusha.coreui.theme.rememberMutableStateOf
import my.noveldokusha.features.reader.features.TextToSpeechSettingData
import my.noveldokusha.reader.R
import my.noveldokusha.text_to_speech.VoiceData

@Composable
internal fun VoiceReaderSettingDialog(
    state: TextToSpeechSettingData,
    floatingTtsState: my.noveldokusha.features.reader.ui.ReaderScreenState.Settings.FloatingTtsSettingsData? = null,
    parallelEnabled: MutableState<Boolean>? = null,
) {
    val colors = NovelaTheme.colors
    var openVoicesDialog by rememberSaveable { mutableStateOf(false) }
    var openOriginalVoiceDialog by rememberSaveable { mutableStateOf(false) }
    val dropdownCustomSavedVoicesExpanded = rememberSaveable { mutableStateOf(false) }

    Column {
        AnimatedVisibility(visible = state.isLoadingChapter.value) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    strokeWidth = 3.dp,
                    color = colors.accent,
                    modifier = Modifier.size(28.dp),
                )
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
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(scrollState),
            ) {
                Text(
                    text = stringResource(R.string.voice_reader).uppercase(),
                    style = NovelaType.sectionEyebrow,
                    color = colors.accent,
                )

                var localPitch by remember { mutableFloatStateOf(state.voicePitch.value) }
                var localSpeed by remember { mutableFloatStateOf(state.voiceSpeed.value) }
                LaunchedEffect(state.voicePitch.value) { localPitch = state.voicePitch.value }
                LaunchedEffect(state.voiceSpeed.value) { localSpeed = state.voiceSpeed.value }

                PillSlider(
                    label = stringResource(R.string.voice_pitch),
                    value = localPitch,
                    valueRange = 0.1f..5f,
                    onValueChange = { localPitch = it },
                    valueText = "%.2f".format(localPitch),
                    enabled = !isScrolling,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    onValueChangeFinished = { state.setVoicePitch(localPitch) },
                )
                PillSlider(
                    label = stringResource(R.string.voice_speed),
                    value = localSpeed,
                    valueRange = 0.1f..5f,
                    onValueChange = { localSpeed = it },
                    valueText = "%.2f".format(localSpeed),
                    enabled = !isScrolling,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    onValueChangeFinished = { state.setVoiceSpeed(localSpeed) },
                )

                val chipColors = AssistChipDefaults.assistChipColors(
                    containerColor = colors.surfaceMuted,
                    labelColor = colors.textPrimary,
                    leadingIconContentColor = colors.accent,
                    disabledLeadingIconContentColor = colors.textMuted,
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                ) {
                    AssistChip(
                        label = { Text(text = stringResource(id = R.string.start_here)) },
                        onClick = debouncedAction { state.playFirstVisibleItem() },
                        shape = RoundedCornerShape(NovelaRadius.pill),
                        leadingIcon = { Icon(Icons.Filled.CenterFocusWeak, null, Modifier.size(14.dp)) },
                        modifier = Modifier.heightIn(min = 32.dp),
                        colors = chipColors,
                    )
                    AssistChip(
                        label = { Text(text = stringResource(id = R.string.focus)) },
                        onClick = debouncedAction { state.scrollToActiveItem() },
                        shape = RoundedCornerShape(NovelaRadius.pill),
                        leadingIcon = { Icon(Icons.Filled.CenterFocusStrong, null, Modifier.size(14.dp)) },
                        modifier = Modifier.heightIn(min = 32.dp),
                        colors = chipColors,
                    )
                    AssistChip(
                        label = { Text(text = stringResource(id = R.string.voices)) },
                        onClick = { openVoicesDialog = !openVoicesDialog },
                        shape = RoundedCornerShape(NovelaRadius.pill),
                        leadingIcon = { Icon(Icons.Filled.RecordVoiceOver, null, Modifier.size(14.dp)) },
                        modifier = Modifier.heightIn(min = 32.dp),
                        enabled = !isScrolling,
                        colors = chipColors,
                    )
                    if (parallelEnabled?.value == true) {
                        AssistChip(
                            label = { Text(text = stringResource(R.string.original_voice)) },
                            onClick = { openOriginalVoiceDialog = !openOriginalVoiceDialog },
                            shape = RoundedCornerShape(NovelaRadius.pill),
                            leadingIcon = { Icon(Icons.Filled.RecordVoiceOver, null, Modifier.size(14.dp)) },
                            modifier = Modifier.heightIn(min = 32.dp),
                            enabled = !isScrolling,
                            colors = chipColors,
                        )
                    }
                    AssistChip(
                        label = { Text(text = stringResource(R.string.saved_voices)) },
                        onClick = {
                            dropdownCustomSavedVoicesExpanded.value =
                                !dropdownCustomSavedVoicesExpanded.value
                        },
                        shape = RoundedCornerShape(NovelaRadius.pill),
                        leadingIcon = { Icon(Icons.Filled.Bookmarks, null, Modifier.size(14.dp)) },
                        modifier = Modifier.heightIn(min = 32.dp),
                        enabled = !isScrolling,
                        colors = chipColors,
                    )
                    Box {
                        DropdownCustomSavedVoices(
                            expanded = dropdownCustomSavedVoicesExpanded,
                            list = state.customSavedVoices.value,
                            currentVoice = state.activeVoice.value,
                            currentVoiceSpeed = state.voiceSpeed.value,
                            currentVoicePitch = state.voicePitch.value,
                            onPredefinedSelected = {
                                state.setVoiceSpeed(it.speed)
                                state.setVoicePitch(it.pitch)
                                state.setVoiceId(it.voiceId)
                            },
                            setCustomSavedVoices = state.setCustomSavedVoices,
                        )
                        VoiceSelectorDialog(
                            availableVoices = state.availableVoices,
                            currentVoice = state.activeVoice.value,
                            inputTextFilter = rememberSaveable { mutableStateOf("") },
                            setVoice = state.setVoiceId,
                            isDialogOpen = openVoicesDialog,
                            setDialogOpen = { openVoicesDialog = it },
                        )
                        VoiceSelectorDialog(
                            availableVoices = state.availableVoices,
                            currentVoice = state.availableVoices.find { it.id == state.originalVoiceId.value },
                            inputTextFilter = rememberSaveable { mutableStateOf("") },
                            setVoice = { state.setOriginalVoiceId(it) },
                            isDialogOpen = openOriginalVoiceDialog,
                            setDialogOpen = { openOriginalVoiceDialog = it },
                        )
                    }
                }

                if (floatingTtsState != null) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                        ) {
                            FloatingTtsToggleCard(
                                text = stringResource(R.string.tts_floating),
                                checked = floatingTtsState.isEnabled.value,
                                enabled = !isScrolling,
                                onToggle = { floatingTtsState.isEnabled.value = !floatingTtsState.isEnabled.value },
                            )
                            FloatingTtsToggleCard(
                                text = stringResource(R.string.tts_floating_show_outside_app),
                                checked = floatingTtsState.showOutsideApp.value,
                                enabled = floatingTtsState.isEnabled.value && !isScrolling,
                                onToggle = { floatingTtsState.showOutsideApp.value = !floatingTtsState.showOutsideApp.value },
                            )
                        }
                    }
                }

                // Media Transport Bar
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    val alpha by animateFloatAsState(
                        targetValue = if (state.isThereActiveItem.value) 1f else 0.45f,
                        label = "tts_transport_alpha",
                    )

                    TtsTransportCircleButton(
                        icon = Icons.Rounded.FastRewind,
                        size = 38.dp,
                        iconSize = 18.dp,
                        enabled = state.isThereActiveItem.value,
                        modifier = Modifier.alpha(alpha),
                        onClick = debouncedAction(waitMillis = 1000) { state.playPreviousChapter() },
                    )
                    TtsTransportCircleButton(
                        icon = Icons.AutoMirrored.Rounded.NavigateBefore,
                        size = 42.dp,
                        iconSize = 22.dp,
                        enabled = state.isThereActiveItem.value,
                        modifier = Modifier.alpha(alpha),
                        onClick = debouncedAction(waitMillis = 450) { state.playPreviousItem() },
                    )

                    Surface(
                        onClick = { state.setPlaying(!state.isPlaying.value) },
                        shape = CircleShape,
                        color = colors.accent,
                        shadowElevation = 4.dp,
                        modifier = Modifier.size(54.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            AnimatedContent(
                                targetState = state.isPlaying.value,
                                label = "tts_play_pause",
                            ) { playing ->
                                Icon(
                                    imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    tint = colors.onAccent,
                                    modifier = Modifier.size(28.dp),
                                )
                            }
                        }
                    }

                    TtsTransportCircleButton(
                        icon = Icons.AutoMirrored.Rounded.NavigateNext,
                        size = 42.dp,
                        iconSize = 22.dp,
                        enabled = state.isThereActiveItem.value,
                        modifier = Modifier.alpha(alpha),
                        onClick = debouncedAction(waitMillis = 450) { state.playNextItem() },
                    )
                    TtsTransportCircleButton(
                        icon = Icons.Rounded.FastForward,
                        size = 38.dp,
                        iconSize = 18.dp,
                        enabled = state.isThereActiveItem.value,
                        modifier = Modifier.alpha(alpha),
                        onClick = debouncedAction(waitMillis = 1000) { state.playNextChapter() },
                    )
                }
            }
        }
    }
}

@Composable
private fun TtsTransportCircleButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    size: androidx.compose.ui.unit.Dp,
    iconSize: androidx.compose.ui.unit.Dp,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = NovelaTheme.colors
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = colors.surfaceMuted,
        border = BorderStroke(NovelaSize.hairline, colors.borderSubtle),
        modifier = modifier.size(size),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.textPrimary,
                modifier = Modifier.size(iconSize),
            )
        }
    }
}

@Composable
private fun FloatingTtsToggleCard(
    text: String,
    checked: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
) {
    val colors = NovelaTheme.colors
    Surface(
        onClick = onToggle,
        enabled = enabled,
        shape = RoundedCornerShape(NovelaRadius.pill),
        color = if (checked) colors.accentSoft else colors.surfaceMuted,
        border = BorderStroke(
            NovelaSize.hairline,
            if (checked) colors.accent.copy(alpha = 0.55f) else colors.borderSubtle,
        ),
        modifier = Modifier.heightIn(min = 32.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (checked) colors.accent else colors.textSecondary,
            )
            if (checked) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, FlowPreview::class)
@Composable
private fun VoiceSelectorDialog(
    availableVoices: List<VoiceData>,
    currentVoice: VoiceData?,
    inputTextFilter: MutableState<String>,
    setVoice: (voiceId: String) -> Unit,
    isDialogOpen: Boolean,
    setDialogOpen: (Boolean) -> Unit,
) {
    val colors = NovelaTheme.colors
    val voicesSorted = remember { mutableStateListOf<VoiceData>() }
    val voicesFiltered = remember { mutableStateListOf<VoiceData>() }

    LaunchedEffect(availableVoices) {
        val sorted = withContext(Dispatchers.Default) {
            availableVoices.sortedWith(
                compareBy<VoiceData> { it.language }
                    .thenByDescending { it.quality }
                    .thenBy { it.needsInternet }
            )
        }
        voicesSorted.clear()
        voicesSorted.addAll(sorted)
    }

    LaunchedEffect(Unit) {
        snapshotFlow { inputTextFilter.value to voicesSorted.toList() }
            .debounce(200)
            .collectLatest { (filter, sorted) ->
                val items = withContext(Dispatchers.Default) {
                    if (filter.isEmpty()) sorted
                    else sorted.filter { it.language.contains(filter, ignoreCase = true) }
                }
                voicesFiltered.clear()
                voicesFiltered.addAll(items)
            }
    }

    val listState = rememberLazyListState()
    val inputFocusRequester = remember { FocusRequester() }

    LaunchedEffect(isDialogOpen) {
        if (!isDialogOpen || currentVoice == null) return@LaunchedEffect
        snapshotFlow { voicesFiltered.toList() }
            .debounce(100)
            .collectLatest { filtered ->
                val index = filtered.indexOfFirst { it.id == currentVoice.id }
                if (index < 0) return@collectLatest
                val targetIndex = index + 1
                listState.scrollToItem(index = targetIndex)
                val headerHeight = listState.layoutInfo.visibleItemsInfo
                    .firstOrNull { it.index == 0 }
                    ?.size ?: 0
                if (headerHeight > 0) {
                    listState.scrollToItem(
                        index = targetIndex,
                        scrollOffset = -headerHeight,
                    )
                }
            }
    }

    if (isDialogOpen) Dialog(
        onDismissRequest = { setDialogOpen(false) }
    ) {
        Surface(
            shape = RoundedCornerShape(NovelaRadius.cardLarge),
            color = colors.surfaceElevated,
            border = BorderStroke(NovelaSize.hairline, colors.borderSubtle),
            shadowElevation = 12.dp,
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .clip(RoundedCornerShape(NovelaRadius.cardLarge)),
            ) {
                stickyHeader {
                    Surface(color = colors.surfaceElevated) {
                        Column {
                            MyOutlinedTextField(
                                value = inputTextFilter.value,
                                onValueChange = { inputTextFilter.value = it },
                                placeHolderText = stringResource(R.string.search_voice_by_language),
                                modifier = Modifier
                                    .padding(12.dp)
                                    .focusRequester(inputFocusRequester),
                            )
                            HorizontalDivider(thickness = NovelaSize.hairline, color = colors.borderSubtle)
                        }
                    }
                    LaunchedEffect(Unit) {
                        delay(300)
                        inputFocusRequester.requestFocus()
                    }
                }

                if (voicesFiltered.isEmpty()) item {
                    Text(
                        text = stringResource(R.string.no_matches),
                        textAlign = TextAlign.Center,
                        color = colors.textMuted,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    )
                }

                items(voicesFiltered) { voice ->
                    val selected by remember(voice.id, currentVoice?.id) {
                        derivedStateOf { voice.id == currentVoice?.id }
                    }
                    Row(
                        modifier = Modifier
                            .heightIn(min = 52.dp)
                            .background(if (selected) colors.accentSoft else Color.Transparent)
                            .clickable(enabled = !selected) { setVoice(voice.id) }
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = voice.language,
                            fontWeight = FontWeight.Bold,
                            color = if (selected) colors.accent else colors.textPrimary,
                            modifier = Modifier.widthIn(min = 84.dp),
                        )
                        Row {
                            for (star in 0..4) {
                                val yay = voice.quality > star * 100
                                Icon(
                                    imageVector = if (yay) Icons.Filled.StarRate else Icons.Outlined.StarBorder,
                                    contentDescription = null,
                                    tint = if (yay) colors.starGold else colors.textMuted.copy(alpha = 0.4f),
                                    modifier = Modifier.size(11.dp),
                                )
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        Column(
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.wrapContentHeight(),
                        ) {
                            Surface(
                                shape = RoundedCornerShape(NovelaRadius.pill),
                                color = colors.accentSoft,
                            ) {
                                Text(
                                    text = voice.id,
                                    color = colors.accent,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                )
                            }
                            if (voice.needsInternet) {
                                Surface(
                                    shape = RoundedCornerShape(NovelaRadius.pill),
                                    color = colors.surfaceMuted,
                                ) {
                                    Text(
                                        text = stringResource(R.string.needs_internet),
                                        color = colors.textSecondary,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                        fontSize = 10.sp,
                                        style = MaterialTheme.typography.labelSmall,
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DropdownCustomSavedVoices(
    expanded: MutableState<Boolean>,
    list: List<VoicePredefineState>,
    currentVoice: VoiceData?,
    currentVoiceSpeed: Float,
    currentVoicePitch: Float,
    onPredefinedSelected: (VoicePredefineState) -> Unit,
    setCustomSavedVoices: (List<VoicePredefineState>) -> Unit,
) {
    val colors = NovelaTheme.colors
    var expandedAddNextEntry by rememberMutableStateOf(false)
    DropdownMenu(
        expanded = expanded.value,
        onDismissRequest = { expanded.value = !expanded.value },
    ) {
        SlimListItem(
            headlineContent = {
                Text(text = stringResource(R.string.save_current_voice))
            },
            leadingContent = {
                Icon(
                    Icons.Outlined.Save,
                    null,
                    tint = colors.accent,
                )
            },
            modifier = Modifier.clickable { expandedAddNextEntry = true },
        )
        HorizontalDivider()
        if (list.isEmpty()) {
            Text(text = stringResource(R.string.no_voices_saved), Modifier.padding(16.dp))
        }
        list.forEachIndexed { index, predefinedVoice ->
            var deleteEntryExpand by rememberMutableStateOf(false)
            SlimListItem(
                headlineContent = {
                    Text(text = predefinedVoice.savedName)
                },
                modifier = Modifier.combinedClickable(
                    enabled = true,
                    onClick = { onPredefinedSelected(predefinedVoice) },
                    onLongClick = { deleteEntryExpand = true },
                ),
            )
            if (deleteEntryExpand) AlertDialog(
                onDismissRequest = { deleteEntryExpand = false },
                containerColor = colors.surfaceElevated,
                shape = RoundedCornerShape(NovelaRadius.cardLarge),
                title = { Text(text = stringResource(R.string.delete_voice), fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        text = predefinedVoice.savedName,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            deleteEntryExpand = false
                            setCustomSavedVoices(
                                list.toMutableList().also { it.removeAt(index) }
                            )
                        },
                        shape = RoundedCornerShape(NovelaRadius.pill),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.danger,
                            contentColor = MaterialTheme.colorScheme.onError,
                        ),
                    ) {
                        Text(text = stringResource(id = R.string.delete))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { deleteEntryExpand = false }) {
                        Text(text = stringResource(R.string.cancel))
                    }
                },
            )
        }
    }

    if (expandedAddNextEntry) {
        var name by rememberMutableStateOf(value = "")
        val focusRequester = remember { FocusRequester() }
        LaunchedEffect(Unit) {
            delay(300)
            focusRequester.requestFocus()
        }
        AlertDialog(
            onDismissRequest = { expandedAddNextEntry = false },
            containerColor = colors.surfaceElevated,
            shape = RoundedCornerShape(NovelaRadius.cardLarge),
            title = {
                Text(
                    text = stringResource(R.string.save_current_voice_parameters),
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                MyOutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeHolderText = stringResource(R.string.name),
                    modifier = Modifier.focusRequester(focusRequester),
                )
            },
            confirmButton = {
                Button(
                    onClick = onClick@{
                        val voice = currentVoice ?: return@onClick
                        val preset = VoicePredefineState(
                            savedName = name,
                            voiceId = voice.id,
                            speed = currentVoiceSpeed,
                            pitch = currentVoicePitch,
                        )
                        setCustomSavedVoices(list + preset)
                        expandedAddNextEntry = false
                    },
                    shape = RoundedCornerShape(NovelaRadius.pill),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accent,
                        contentColor = colors.onAccent,
                    ),
                ) {
                    Text(text = stringResource(R.string.save_voice))
                }
            },
            dismissButton = {
                TextButton(onClick = { expandedAddNextEntry = false }) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Preview(group = "dialog")
@Composable
private fun VoiceSelectorDialogContentPreview() {
    InternalTheme {
        VoiceSelectorDialog(
            availableVoices = (0..7).map {
                VoiceData(
                    id = "$it",
                    language = "lang${it / 2}",
                    needsInternet = (it % 2) == 0,
                    quality = (it * 100) % 501,
                    enginePackage = "",
                )
            },
            setVoice = {},
            inputTextFilter = remember { mutableStateOf("hello") },
            currentVoice = VoiceData(
                id = "2",
                language = "",
                needsInternet = false,
                quality = 100,
                enginePackage = "",
            ),
            setDialogOpen = {},
            isDialogOpen = true,
        )
    }
}

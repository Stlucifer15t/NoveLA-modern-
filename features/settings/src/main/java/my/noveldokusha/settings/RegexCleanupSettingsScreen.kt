package my.noveldokusha.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Rule
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import my.noveldokusha.core.models.RegexRule
import my.noveldokusha.coreui.components.NovelaEmptyState
import my.noveldokusha.coreui.components.NovelaSheetHeader
import my.noveldokusha.coreui.theme.NovelaRadius
import my.noveldokusha.coreui.theme.NovelaSize
import my.noveldokusha.coreui.theme.NovelaSpacing
import my.noveldokusha.coreui.theme.NovelaTheme
import my.noveldokusha.coreui.theme.NovelaType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegexCleanupSettingsScreen(
    viewModel: RegexCleanupSettingsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    applyStatusBarPadding: Boolean = true,
    compactHeader: Boolean = false,
) {
    val colors = NovelaTheme.colors
    val state by viewModel.uiState
    val filteredRules = viewModel.filteredRules
    val focusManager = LocalFocusManager.current

    Surface(
        shape = if (compactHeader) RoundedCornerShape(NovelaRadius.cardLarge) else RoundedCornerShape(0.dp),
        color = if (compactHeader) colors.surfaceElevated else colors.background,
        border = if (compactHeader) BorderStroke(NovelaSize.hairline, colors.borderSubtle) else null,
        modifier = modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (applyStatusBarPadding) Modifier.statusBarsPadding() else Modifier),
        ) {
            // ── TopBar ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.textPrimary,
                        )
                    }
                    if (compactHeader) {
                        Surface(
                            shape = CircleShape,
                            color = colors.accentSoft,
                            modifier = Modifier.size(30.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.Rule,
                                    contentDescription = null,
                                    tint = colors.accent,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                    Text(
                        text = stringResource(
                            id = if (viewModel.isGlobal) R.string.regex_cleanup_title
                            else R.string.regex_cleanup_novel_rules
                        ),
                        style = if (compactHeader) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Surface(
                    onClick = { viewModel.onAddRule() },
                    shape = RoundedCornerShape(NovelaRadius.pill),
                    color = colors.accent,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            tint = colors.onAccent,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = stringResource(id = R.string.add_new_rule),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.onAccent,
                        )
                    }
                }
            }

            // ── Novel Header & Actions (per-novel mode) ──
            if (!viewModel.isGlobal) {
                Text(
                    text = stringResource(
                        id = R.string.regex_cleanup_novel_title_format,
                        viewModel.novelTitle.value.ifEmpty { viewModel.bookUrl.orEmpty() },
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 6.dp),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = { viewModel.onMoveRulesToGlobal() },
                        shape = RoundedCornerShape(NovelaRadius.pill),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.accent),
                        border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.5f)),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = stringResource(id = R.string.regex_cleanup_move_to_global),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    OutlinedButton(
                        onClick = { viewModel.onRemoveNovelRules() },
                        shape = RoundedCornerShape(NovelaRadius.pill),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.danger),
                        border = BorderStroke(1.dp, colors.danger.copy(alpha = 0.5f)),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = stringResource(id = R.string.regex_cleanup_remove_novel_rules),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            // ── Search Bar ──
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                placeholder = {
                    Text(
                        text = stringResource(id = R.string.search_patterns_hint),
                        style = MaterialTheme.typography.bodySmall,
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(18.dp),
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(NovelaRadius.pill),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = colors.surfaceMuted,
                    focusedContainerColor = colors.surfaceMuted,
                    unfocusedBorderColor = colors.borderSubtle,
                    focusedBorderColor = colors.accent,
                    cursorColor = colors.accent,
                ),
                textStyle = MaterialTheme.typography.bodySmall,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .padding(bottom = 8.dp),
            )

            // ── Rules List ──
            if (filteredRules.isEmpty()) {
                NovelaEmptyState(
                    icon = Icons.Outlined.Rule,
                    title = stringResource(id = R.string.no_regex_rules_yet),
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .then(if (applyStatusBarPadding) Modifier.navigationBarsPadding() else Modifier),
                ) {
                    itemsIndexed(
                        items = filteredRules,
                        key = { index, _ -> "rule_$index" },
                    ) { _, rule ->
                        RegexRuleRow(
                            rule = rule,
                            onEdit = {
                                val realIndex = state.rules.indexOf(rule)
                                if (realIndex >= 0) viewModel.onEditRule(realIndex)
                            },
                            onDelete = { viewModel.onDeleteRule(rule.pattern) },
                            onToggle = {
                                val realIndex = state.rules.indexOf(rule)
                                if (realIndex >= 0) viewModel.onToggleRule(realIndex)
                            },
                            onMoveUp = {
                                val realIndex = state.rules.indexOf(rule)
                                if (realIndex > 0) viewModel.onMoveRule(realIndex, realIndex - 1)
                            },
                            onMoveDown = {
                                val realIndex = state.rules.indexOf(rule)
                                if (realIndex < state.rules.size - 1) viewModel.onMoveRule(realIndex, realIndex + 1)
                            },
                        )
                    }
                }
            }
        }

        // ── Modal Bottom Sheet ──
        if (state.isBottomSheetOpen && state.editingRule != null) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.onDismissBottomSheet() },
                containerColor = colors.surfaceElevated,
                dragHandle = null,
                shape = RoundedCornerShape(topStart = NovelaRadius.sheet, topEnd = NovelaRadius.sheet),
                modifier = Modifier.navigationBarsPadding(),
            ) {
                RegexRuleBottomSheetContent(
                    rule = state.editingRule!!,
                    validationError = state.validationError?.let { pattern ->
                        stringResource(id = R.string.invalid_regex_pattern_with, pattern)
                    },
                    duplicatePatternError = state.duplicatePatternError,
                    previewText = state.previewText.ifEmpty {
                        stringResource(id = R.string.preview_sample_text)
                    },
                    onSave = { pattern, replacement, enabled, wholeWords, desc ->
                        viewModel.onSaveRule(pattern, replacement, enabled, wholeWords, desc)
                    },
                    onPreviewChange = { viewModel.updatePreview(it) },
                    onDismiss = { viewModel.onDismissBottomSheet() },
                )
            }
        }

        // ── Delete Confirmation Dialog ──
        if (state.deleteConfirmationPattern != null) {
            AlertDialog(
                onDismissRequest = { viewModel.onDismissDelete() },
                containerColor = colors.surfaceElevated,
                shape = RoundedCornerShape(NovelaRadius.cardLarge),
                title = {
                    Text(
                        text = stringResource(id = R.string.delete_rule),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                    )
                },
                text = {
                    Text(
                        text = stringResource(
                            id = R.string.delete_rule_confirmation,
                            state.deleteConfirmationPattern!!,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.onConfirmDelete() },
                        shape = RoundedCornerShape(NovelaRadius.pill),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.danger,
                            contentColor = MaterialTheme.colorScheme.onError,
                        ),
                    ) {
                        Text(
                            text = stringResource(id = R.string.delete),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.onDismissDelete() }) {
                        Text(
                            text = stringResource(id = R.string.cancel),
                            color = colors.textSecondary,
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun RegexRuleRow(
    rule: RegexRule,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggle: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    val colors = NovelaTheme.colors
    val alphaModifier = if (rule.isEnabled) Modifier else Modifier.alpha(0.55f)

    Surface(
        shape = RoundedCornerShape(NovelaRadius.card),
        color = colors.surfaceCard,
        border = BorderStroke(NovelaSize.hairline, colors.borderSubtle),
        modifier = alphaModifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(NovelaRadius.card))
            .clickable(onClick = onToggle),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = rule.pattern,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (rule.replacement.isNotEmpty() || rule.description.isNotBlank()) {
                        Text(
                            text = buildString {
                                if (rule.replacement.isNotEmpty()) {
                                    append("→ ")
                                    append(rule.replacement)
                                }
                                if (rule.description.isNotBlank()) {
                                    if (isNotEmpty()) append(" · ")
                                    append(rule.description)
                                }
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(NovelaRadius.pill),
                    color = if (rule.isEnabled) colors.accentSoft else colors.surfaceMuted,
                    modifier = Modifier.padding(horizontal = 6.dp),
                ) {
                    Text(
                        text = if (rule.isEnabled) stringResource(id = R.string.rule_enabled)
                        else stringResource(id = R.string.rule_disabled),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (rule.isEnabled) colors.accent else colors.textMuted,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = null,
                            tint = colors.danger,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    IconButton(onClick = onMoveUp, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowUpward,
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    IconButton(onClick = onMoveDown, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowDownward,
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RegexRuleBottomSheetContent(
    rule: RegexRule,
    validationError: String?,
    duplicatePatternError: Boolean,
    previewText: String,
    onSave: (pattern: String, replacement: String, enabled: Boolean, wholeWordsOnly: Boolean, description: String) -> Unit,
    onPreviewChange: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = NovelaTheme.colors
    var pattern by remember { mutableStateOf(rule.pattern) }
    var replacement by remember { mutableStateOf(rule.replacement) }
    var description by remember { mutableStateOf(rule.description) }
    var isEnabled by remember { mutableStateOf(rule.isEnabled) }
    var wholeWordsOnly by remember { mutableStateOf(rule.wholeWordsOnly) }
    var localPreviewText by remember { mutableStateOf(previewText) }
    var isPatternValid by remember { mutableStateOf(true) }
    var showError by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val fieldColors = OutlinedTextFieldDefaults.colors(
        unfocusedContainerColor = colors.surfaceMuted,
        focusedContainerColor = colors.surfaceMuted,
        unfocusedBorderColor = colors.borderSubtle,
        focusedBorderColor = colors.accent,
        cursorColor = colors.accent,
    )

    val switchColors = SwitchDefaults.colors(
        checkedThumbColor = colors.onAccent,
        checkedTrackColor = colors.accent,
        uncheckedThumbColor = colors.textMuted,
        uncheckedTrackColor = colors.surfaceMuted,
    )

    LaunchedEffect(pattern) {
        isPatternValid = try {
            if (pattern.isNotEmpty()) Regex(pattern)
            true
        } catch (_: Exception) {
            false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        NovelaSheetHeader(
            title = if (rule.pattern.isEmpty()) stringResource(id = R.string.new_rule)
            else stringResource(id = R.string.edit_rule),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = NovelaSpacing.lg),
        ) {
            // Pattern field
            Text(
                text = stringResource(id = R.string.pattern).uppercase(),
                style = NovelaType.sectionEyebrow,
                color = colors.accent,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            OutlinedTextField(
                value = pattern,
                onValueChange = {
                    pattern = it
                    showError = false
                },
                placeholder = {
                    Text(
                        text = stringResource(id = R.string.enter_regex_pattern),
                        style = MaterialTheme.typography.bodySmall,
                    )
                },
                isError = showError && pattern.isNotEmpty() && !isPatternValid,
                singleLine = true,
                shape = RoundedCornerShape(NovelaRadius.md),
                colors = fieldColors,
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                modifier = Modifier.fillMaxWidth(),
            )
            if (showError && pattern.isNotEmpty() && !isPatternValid) {
                Text(
                    text = stringResource(id = R.string.invalid_regex_pattern),
                    color = colors.danger,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(start = 8.dp, top = 2.dp),
                )
            }
            if (duplicatePatternError) {
                Text(
                    text = stringResource(id = R.string.duplicate_regex_pattern),
                    color = colors.danger,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(start = 8.dp, top = 2.dp),
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Replacement field
            Text(
                text = stringResource(id = R.string.replacement).uppercase(),
                style = NovelaType.sectionEyebrow,
                color = colors.accent,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            OutlinedTextField(
                value = replacement,
                onValueChange = { replacement = it },
                placeholder = {
                    Text(
                        text = stringResource(id = R.string.enter_replacement_text),
                        style = MaterialTheme.typography.bodySmall,
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(NovelaRadius.md),
                colors = fieldColors,
                textStyle = MaterialTheme.typography.bodySmall,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Description field
            Text(
                text = stringResource(id = R.string.description).uppercase(),
                style = NovelaType.sectionEyebrow,
                color = colors.accent,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                placeholder = {
                    Text(
                        text = stringResource(id = R.string.description_hint),
                        style = MaterialTheme.typography.bodySmall,
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(NovelaRadius.md),
                colors = fieldColors,
                textStyle = MaterialTheme.typography.bodySmall,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Enable toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(id = R.string.enabled),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textPrimary,
                )
                Switch(
                    checked = isEnabled,
                    onCheckedChange = { isEnabled = it },
                    colors = switchColors,
                )
            }

            // Whole words only toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(id = R.string.whole_words_only),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textPrimary,
                )
                Switch(
                    checked = wholeWordsOnly,
                    onCheckedChange = { wholeWordsOnly = it },
                    colors = switchColors,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Preview
            Text(
                text = stringResource(id = R.string.preview).uppercase(),
                style = NovelaType.sectionEyebrow,
                color = colors.accent,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            OutlinedTextField(
                value = localPreviewText,
                onValueChange = {
                    localPreviewText = it
                    onPreviewChange(it)
                },
                singleLine = true,
                shape = RoundedCornerShape(NovelaRadius.md),
                colors = fieldColors,
                textStyle = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(NovelaRadius.md),
                color = colors.surfaceMuted,
                border = BorderStroke(NovelaSize.hairline, colors.borderSubtle),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = stringResource(id = R.string.preview_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textMuted,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    PreviewText(
                        text = localPreviewText,
                        pattern = pattern,
                        replacement = replacement,
                        isEnabled = isEnabled,
                        wholeWordsOnly = wholeWordsOnly,
                    )
                }
            }

            if (validationError != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = validationError,
                    color = colors.danger,
                    style = MaterialTheme.typography.labelSmall,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(NovelaRadius.pill),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textSecondary),
                    border = BorderStroke(1.dp, colors.borderSubtle),
                ) {
                    Text(text = stringResource(id = R.string.cancel))
                }

                Button(
                    onClick = {
                        if (pattern.isNotEmpty() && isPatternValid) {
                            onSave(pattern, replacement, isEnabled, wholeWordsOnly, description)
                        } else {
                            showError = true
                        }
                    },
                    modifier = Modifier.weight(1.8f),
                    shape = RoundedCornerShape(NovelaRadius.pill),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accent,
                        contentColor = colors.onAccent,
                    ),
                ) {
                    Text(
                        text = stringResource(id = R.string.save),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewText(
    text: String,
    pattern: String,
    replacement: String,
    isEnabled: Boolean,
    wholeWordsOnly: Boolean,
) {
    val colors = NovelaTheme.colors
    if (!isEnabled || pattern.isEmpty()) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
        )
        return
    }

    val effectivePattern = remember(pattern, wholeWordsOnly) {
        RegexRule(pattern = pattern, wholeWordsOnly = wholeWordsOnly).effectivePattern
    }

    val resultText = remember(pattern, wholeWordsOnly, replacement, text) {
        try {
            text.replace(Regex(effectivePattern), replacement)
        } catch (_: Exception) {
            text
        }
    }

    if (resultText == text) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
        )
        return
    }

    val matches = remember(pattern, wholeWordsOnly, text) {
        try {
            Regex(effectivePattern).findAll(text).toList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    val annotated = buildAnnotatedString {
        var lastEnd = 0
        for (match in matches) {
            if (match.range.first > lastEnd) {
                append(text.substring(lastEnd, match.range.first))
            }
            pushStyle(
                SpanStyle(
                    textDecoration = TextDecoration.LineThrough,
                    color = colors.danger,
                )
            )
            append(match.value)
            pop()
            if (replacement.isNotEmpty()) {
                pushStyle(SpanStyle(color = colors.accent, fontWeight = FontWeight.SemiBold))
                append(replacement)
                pop()
            }
            lastEnd = match.range.last + 1
        }
        if (lastEnd < text.length) {
            append(text.substring(lastEnd))
        }
    }

    Text(
        text = annotated,
        style = MaterialTheme.typography.bodySmall,
        color = colors.textSecondary,
    )
}

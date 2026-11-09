package my.noveldokusha.sourceexplorer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import my.noveldokusha.coreui.components.NovelaPrimaryButton
import my.noveldokusha.coreui.theme.NovelaRadius
import my.noveldokusha.coreui.theme.NovelaSize
import my.noveldokusha.coreui.theme.NovelaSpacing
import my.noveldokusha.coreui.theme.NovelaTheme
import my.noveldokusha.coreui.theme.NovelaType
import my.noveldokusha.scraper.ActiveFilters
import my.noveldokusha.scraper.FilterHistoryEntry
import my.noveldokusha.scraper.FilterPreset
import my.noveldokusha.scraper.LuaFilter

enum class TriStateValue { NEUTRAL, INCLUDED, EXCLUDED }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun FilterBottomSheet(
    filterList: List<LuaFilter>,
    activeFilters: ActiveFilters,
    onApply: (ActiveFilters) -> Unit,
    onDismiss: () -> Unit,
    presets: List<FilterPreset>,
    textHistory: List<FilterHistoryEntry>,
    onPresetSave: (String) -> Unit,
    onPresetLoad: (FilterPreset) -> Unit,
    onPresetDelete: (FilterPreset) -> Unit,
    onTextHistoryAdd: (filterKey: String, value: String) -> Unit,
    onTextHistoryRemove: (filterKey: String, value: String) -> Unit = { _, _ -> },
) {
    val colors = NovelaTheme.colors
    val sheetState = rememberModalBottomSheetState()

    val sortValues = remember { mutableStateMapOf<String, String>() }
    val sortAscending = remember { mutableStateMapOf<String, Boolean>() }
    val selectValues = remember { mutableStateMapOf<String, String>() }
    val checkboxState = remember { mutableStateMapOf<String, Set<String>>() }
    val tristateState = remember { mutableStateMapOf<String, Map<String, TriStateValue>>() }
    val switchValues = remember { mutableStateMapOf<String, Boolean>() }
    val textValues = remember { mutableStateMapOf<String, String>() }
    val tagInputValues = remember { mutableStateMapOf<String, MutableList<String>>() }

    LaunchedEffect(filterList) {
        filterList.forEach { filter ->
            when (filter) {
                is LuaFilter.Sort -> {
                    sortValues[filter.key] = activeFilters.sortValues[filter.key] ?: filter.defaultValue
                    sortAscending[filter.key] = activeFilters.sortAscending[filter.key] ?: filter.defaultAscending
                }
                is LuaFilter.Select ->
                    selectValues[filter.key] = activeFilters.selectValues[filter.key] ?: filter.defaultValue
                is LuaFilter.CheckboxGroup ->
                    checkboxState[filter.key] = activeFilters.checkboxIncluded[filter.key]?.toSet() ?: emptySet()
                is LuaFilter.TriState -> {
                    val inc = activeFilters.triIncluded[filter.key] ?: emptyList()
                    val exc = activeFilters.triExcluded[filter.key] ?: emptyList()
                    tristateState[filter.key] = filter.options.associate { opt ->
                        opt.value to when {
                            opt.value in inc -> TriStateValue.INCLUDED
                            opt.value in exc -> TriStateValue.EXCLUDED
                            else -> TriStateValue.NEUTRAL
                        }
                    }
                }
                is LuaFilter.Switch ->
                    switchValues[filter.key] = activeFilters.switchValues[filter.key] ?: filter.defaultValue
                is LuaFilter.TextInput ->
                    textValues[filter.key] = activeFilters.textValues[filter.key] ?: filter.defaultValue
                is LuaFilter.TagInput ->
                    tagInputValues[filter.key] = (activeFilters.tagInputValues[filter.key] ?: emptyList()).toMutableList()
            }
        }
    }

    fun resetToDefaults() {
        filterList.forEach { filter ->
            when (filter) {
                is LuaFilter.Sort -> {
                    sortValues[filter.key] = filter.defaultValue
                    sortAscending[filter.key] = filter.defaultAscending
                }
                is LuaFilter.Select -> selectValues[filter.key] = filter.defaultValue
                is LuaFilter.CheckboxGroup -> checkboxState[filter.key] = emptySet()
                is LuaFilter.TriState -> tristateState[filter.key] = filter.options.associate { it.value to TriStateValue.NEUTRAL }
                is LuaFilter.Switch -> switchValues[filter.key] = filter.defaultValue
                is LuaFilter.TextInput -> textValues[filter.key] = filter.defaultValue
                is LuaFilter.TagInput -> tagInputValues[filter.key] = mutableListOf()
            }
        }
    }

    fun buildActiveFilters(): ActiveFilters {
        val triInc = mutableMapOf<String, List<String>>()
        val triExc = mutableMapOf<String, List<String>>()
        tristateState.forEach { (key, map) ->
            triInc[key] = map.filter { it.value == TriStateValue.INCLUDED }.keys.toList()
            triExc[key] = map.filter { it.value == TriStateValue.EXCLUDED }.keys.toList()
        }
        return ActiveFilters(
            sortValues = sortValues.toMap(),
            sortAscending = sortAscending.toMap(),
            selectValues = selectValues.toMap(),
            checkboxIncluded = checkboxState.mapValues { it.value.toList() },
            triIncluded = triInc,
            triExcluded = triExc,
            switchValues = switchValues.toMap(),
            textValues = textValues.toMap(),
            tagInputValues = tagInputValues.mapValues { it.value.toList() },
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = NovelaRadius.sheet, topEnd = NovelaRadius.sheet),
        containerColor = colors.surfaceFloating,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = NovelaSpacing.lg, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.FilterList,
                        contentDescription = null,
                        tint = colors.accent,
                    )
                    Text(
                        text = stringResource(R.string.filters),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                    )
                }
                TextButton(onClick = { resetToDefaults() }) {
                    Text(
                        text = stringResource(R.string.filters_reset),
                        color = colors.accent,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            PresetSection(
                presets = presets,
                onLoad = onPresetLoad,
                onDelete = onPresetDelete,
                onSave = onPresetSave,
            )

            HorizontalDivider(thickness = NovelaSize.hairline, color = colors.borderSubtle)

            // Filter sections
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = NovelaSpacing.lg, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (filterList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = colors.accent)
                    }
                }
                filterList.forEach { filter ->
                    when (filter) {
                        is LuaFilter.Sort -> SortSection(
                            filter = filter,
                            value = sortValues[filter.key] ?: filter.defaultValue,
                            ascending = sortAscending[filter.key] ?: filter.defaultAscending,
                            onValueChange = { sortValues[filter.key] = it },
                            onAscendingChange = { sortAscending[filter.key] = it },
                        )
                        is LuaFilter.Select -> SelectSection(
                            filter = filter,
                            value = selectValues[filter.key] ?: filter.defaultValue,
                        ) { selectValues[filter.key] = it }
                        is LuaFilter.CheckboxGroup -> CheckboxSection(
                            filter = filter,
                            included = checkboxState[filter.key] ?: emptySet(),
                        ) { value ->
                            val current = checkboxState[filter.key] ?: emptySet()
                            checkboxState[filter.key] = if (!filter.multiselect) {
                                if (value in current) emptySet() else setOf(value)
                            } else {
                                if (value in current) current - value else current + value
                            }
                        }
                        is LuaFilter.TriState -> TriStateSection(
                            filter = filter,
                            stateMap = tristateState[filter.key] ?: emptyMap(),
                        ) { value ->
                            val current = tristateState[filter.key] ?: emptyMap()
                            tristateState[filter.key] = current + (value to when (current[value] ?: TriStateValue.NEUTRAL) {
                                TriStateValue.NEUTRAL -> TriStateValue.INCLUDED
                                TriStateValue.INCLUDED -> TriStateValue.EXCLUDED
                                TriStateValue.EXCLUDED -> TriStateValue.NEUTRAL
                            })
                        }
                        is LuaFilter.Switch -> SwitchSection(
                            filter = filter,
                            value = switchValues[filter.key] ?: filter.defaultValue,
                        ) { switchValues[filter.key] = it }
                        is LuaFilter.TextInput -> TextSection(
                            filter = filter,
                            value = textValues[filter.key] ?: filter.defaultValue,
                            onChange = { textValues[filter.key] = it },
                            history = textHistory,
                            onHistoryAdd = { onTextHistoryAdd(filter.key, it) },
                            onHistoryRemove = { onTextHistoryRemove(filter.key, it) },
                        )
                        is LuaFilter.TagInput -> TagInputSection(
                            filter = filter,
                            selectedValues = tagInputValues[filter.key] ?: emptyList(),
                            onToggle = { value ->
                                val current = tagInputValues[filter.key]?.toMutableList() ?: mutableListOf()
                                if (value in current) current.remove(value) else current.add(value)
                                tagInputValues[filter.key] = current
                            },
                            onAddCustom = { value ->
                                val current = tagInputValues[filter.key]?.toMutableList() ?: mutableListOf()
                                if (value !in current) {
                                    current.add(value)
                                    tagInputValues[filter.key] = current
                                }
                            },
                        )
                    }
                    HorizontalDivider(
                        thickness = NovelaSize.hairline,
                        color = colors.borderSubtle,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            }

            // Apply button
            HorizontalDivider(thickness = NovelaSize.hairline, color = colors.borderSubtle)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = NovelaSpacing.lg, vertical = 12.dp),
            ) {
                NovelaPrimaryButton(
                    text = stringResource(R.string.filters_apply),
                    onClick = {
                        onApply(buildActiveFilters())
                        textValues.forEach { (key, v) -> if (v.isNotBlank()) onTextHistoryAdd(key, v) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun FilterSectionHeader(
    title: String,
    expanded: Boolean = true,
    collapsible: Boolean = false,
    onToggle: (() -> Unit)? = null,
) {
    val colors = NovelaTheme.colors
    if (collapsible) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle?.invoke() }
                .padding(top = 10.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = title.uppercase(),
                style = NovelaType.sectionEyebrow,
                color = colors.accent,
            )
            Icon(
                imageVector = if (expanded) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = colors.accent,
            )
        }
    } else {
        Text(
            text = title.uppercase(),
            style = NovelaType.sectionEyebrow,
            color = colors.accent,
            modifier = Modifier.padding(top = 10.dp, bottom = 8.dp),
        )
    }
}

@Composable
private fun SortSection(
    filter: LuaFilter.Sort,
    value: String,
    ascending: Boolean,
    onValueChange: (String) -> Unit,
    onAscendingChange: (Boolean) -> Unit,
) {
    val colors = NovelaTheme.colors
    Column {
        FilterSectionHeader(filter.label)
        filter.options.sortedBy { it.label.lowercase() }.forEach { opt ->
            val selected = value == opt.value
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                RadioButton(
                    selected = selected,
                    onClick = {
                        if (selected) onAscendingChange(!ascending) else onValueChange(opt.value)
                    },
                    colors = RadioButtonDefaults.colors(selectedColor = colors.accent),
                )
                Text(
                    text = opt.label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp),
                    color = if (selected) colors.accent else colors.textPrimary,
                )
                if (selected) {
                    IconButton(onClick = { onAscendingChange(!ascending) }) {
                        Icon(
                            imageVector = if (ascending) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                            contentDescription = null,
                            tint = colors.accent,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectSection(
    filter: LuaFilter.Select,
    value: String,
    onChange: (String) -> Unit,
) {
    val colors = NovelaTheme.colors
    Column {
        FilterSectionHeader(filter.label)
        if (filter.options.size <= 3) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
            ) {
                filter.options.sortedBy { it.label.lowercase() }.forEach { opt ->
                    SelectOptionButton(
                        label = opt.label,
                        selected = value == opt.value,
                        onClick = { onChange(opt.value) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
            }
        } else {
            var expanded by remember { mutableStateOf(false) }
            val currentLabel = filter.options.find { it.value == value }?.label ?: value
            Column {
                OutlinedButton(
                    onClick = { expanded = !expanded },
                    shape = RoundedCornerShape(NovelaRadius.md),
                    border = BorderStroke(NovelaSize.hairline, colors.accentLine),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = currentLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = colors.textPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = colors.accent,
                    )
                }
                if (expanded) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(bottomStart = NovelaRadius.md, bottomEnd = NovelaRadius.md),
                        color = colors.surfaceCard,
                        border = BorderStroke(NovelaSize.hairline, colors.borderStrong),
                    ) {
                        Column(
                            modifier = Modifier
                                .heightIn(max = 200.dp)
                                .verticalScroll(rememberScrollState()),
                        ) {
                            filter.options.sortedBy { it.label.lowercase() }.forEach { opt ->
                                val selected = value == opt.value
                                Surface(
                                    onClick = {
                                        onChange(opt.value)
                                        expanded = false
                                    },
                                    color = if (selected) colors.accentSoft else colors.surfaceCard,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text(
                                            text = opt.label,
                                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                            color = if (selected) colors.accent else colors.textPrimary,
                                        )
                                        if (selected) {
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = null,
                                                tint = colors.accent,
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
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CheckboxSection(
    filter: LuaFilter.CheckboxGroup,
    included: Set<String>,
    onToggle: (String) -> Unit,
) {
    val colors = NovelaTheme.colors
    val isCollapsible = filter.options.size > 4
    var expanded by rememberSaveable { mutableStateOf(included.isNotEmpty()) }
    Column {
        FilterSectionHeader(
            title = filter.label,
            expanded = expanded,
            collapsible = isCollapsible,
            onToggle = if (isCollapsible) ({ expanded = !expanded }) else null,
        )
        AnimatedVisibility(visible = !isCollapsible || expanded) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                filter.options.sortedBy { it.label.lowercase() }.forEach { opt ->
                    val isIncluded = opt.value in included
                    FilterChip(
                        selected = isIncluded,
                        onClick = { onToggle(opt.value) },
                        shape = RoundedCornerShape(NovelaRadius.pill),
                        label = { Text(opt.label) },
                        leadingIcon = if (isIncluded) {
                            {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                                )
                            }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = colors.surfaceSubtle,
                            labelColor = colors.textSecondary,
                            selectedContainerColor = colors.accentSoft,
                            selectedLabelColor = colors.accent,
                            selectedLeadingIconColor = colors.accent,
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isIncluded,
                            borderColor = colors.borderSubtle,
                            selectedBorderColor = colors.accentLine,
                            borderWidth = NovelaSize.hairline,
                            selectedBorderWidth = NovelaSize.hairline,
                        ),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TriStateSection(
    filter: LuaFilter.TriState,
    stateMap: Map<String, TriStateValue>,
    onToggle: (String) -> Unit,
) {
    val isCollapsible = filter.options.size > 4
    val hasActive = stateMap.any { it.value != TriStateValue.NEUTRAL }
    var expanded by rememberSaveable { mutableStateOf(hasActive) }
    Column {
        FilterSectionHeader(
            title = filter.label,
            expanded = expanded,
            collapsible = isCollapsible,
            onToggle = if (isCollapsible) ({ expanded = !expanded }) else null,
        )
        AnimatedVisibility(visible = !isCollapsible || expanded) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                filter.options.sortedBy { it.label.lowercase() }.forEach { opt ->
                    TriStateChip(opt.label, stateMap[opt.value] ?: TriStateValue.NEUTRAL) {
                        onToggle(opt.value)
                    }
                }
            }
        }
    }
}

@Composable
private fun SwitchSection(
    filter: LuaFilter.Switch,
    value: Boolean,
    onChange: (Boolean) -> Unit,
) {
    val colors = NovelaTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
    ) {
        Text(
            text = filter.label,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = value,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.onAccent,
                checkedTrackColor = colors.accent,
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TextSection(
    filter: LuaFilter.TextInput,
    value: String,
    onChange: (String) -> Unit,
    history: List<FilterHistoryEntry> = emptyList(),
    onHistoryAdd: (String) -> Unit = {},
    onHistoryRemove: (String) -> Unit = {},
) {
    val colors = NovelaTheme.colors
    val filteredHistory by remember(value, history, filter.key) {
        derivedStateOf {
            history
                .mapNotNull { entry -> entry.filters.textValues[filter.key]?.let { v -> v to entry.timestamp } }
                .sortedByDescending { it.second }
                .distinctBy { it.first }
                .filter { (histValue, _) -> histValue != value }
                .take(10)
        }
    }

    var showSuggestions by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier.onFocusChanged { showSuggestions = it.hasFocus },
    ) {
        FilterSectionHeader(filter.label)
        OutlinedTextField(
            value = value,
            onValueChange = { onChange(it) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(NovelaRadius.md),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.accent,
                unfocusedBorderColor = colors.borderSubtle,
                focusedContainerColor = colors.surfaceSubtle,
                unfocusedContainerColor = colors.surfaceSubtle,
                cursorColor = colors.accent,
            ),
            trailingIcon = {
                if (value.isNotEmpty()) {
                    IconButton(onClick = { onChange("") }) {
                        Icon(Icons.Filled.Close, "Clear", tint = colors.textMuted)
                    }
                }
            },
        )
        Spacer(modifier = Modifier.height(4.dp))
        AnimatedVisibility(visible = showSuggestions && filteredHistory.isNotEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(NovelaRadius.md),
                color = colors.surfaceCard,
                border = BorderStroke(NovelaSize.hairline, colors.borderSubtle),
            ) {
                Column(modifier = Modifier.heightIn(max = 200.dp)) {
                    filteredHistory.forEach { (histValue, _) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onChange(histValue)
                                    onHistoryAdd(histValue)
                                    showSuggestions = false
                                },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = histValue,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textPrimary,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                            )
                            IconButton(
                                onClick = { onHistoryRemove(histValue) },
                                modifier = Modifier.size(32.dp),
                            ) {
                                Icon(
                                    Icons.Filled.Close,
                                    "Remove",
                                    modifier = Modifier.size(14.dp),
                                    tint = colors.textMuted,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun TagInputSection(
    filter: LuaFilter.TagInput,
    selectedValues: List<String>,
    onToggle: (String) -> Unit,
    onAddCustom: (String) -> Unit,
) {
    val colors = NovelaTheme.colors
    var text by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    val filteredOptions = filter.options.filter {
        it.label.contains(text, ignoreCase = true) && it.value !in selectedValues
    }
    val showAddCustom = filter.allowCustom && text.isNotBlank() &&
        filter.options.none { it.label.equals(text, ignoreCase = true) } &&
        text !in selectedValues

    Column {
        FilterSectionHeader(filter.label)
        if (selectedValues.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(bottom = 8.dp),
            ) {
                selectedValues.forEach { value ->
                    val label = filter.options.find { it.value == value }?.label ?: value
                    FilterChip(
                        selected = true,
                        onClick = { onToggle(value) },
                        shape = RoundedCornerShape(NovelaRadius.pill),
                        label = { Text(label) },
                        trailingIcon = {
                            Icon(
                                Icons.Filled.Close,
                                "Remove",
                                modifier = Modifier.size(FilterChipDefaults.IconSize),
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.accentSoft,
                            selectedLabelColor = colors.accent,
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = true,
                            selectedBorderColor = colors.accentLine,
                            selectedBorderWidth = NovelaSize.hairline,
                        ),
                    )
                }
            }
        }
        val showSuggestions = expanded && (filteredOptions.isNotEmpty() || showAddCustom)
        Column(
            modifier = Modifier.onFocusChanged { if (!it.hasFocus) expanded = false },
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = {
                    text = it
                    expanded = true
                },
                placeholder = { Text("Type to search...", color = colors.textMuted) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(NovelaRadius.md),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.accent,
                    unfocusedBorderColor = colors.borderSubtle,
                    focusedContainerColor = colors.surfaceSubtle,
                    unfocusedContainerColor = colors.surfaceSubtle,
                    cursorColor = colors.accent,
                ),
                trailingIcon = {
                    if (text.isNotEmpty()) {
                        IconButton(onClick = {
                            text = ""
                            expanded = false
                        }) {
                            Icon(Icons.Filled.Close, "Clear", tint = colors.textMuted)
                        }
                    }
                },
            )
            if (showSuggestions) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(bottomStart = NovelaRadius.md, bottomEnd = NovelaRadius.md),
                    color = colors.surfaceCard,
                    border = BorderStroke(NovelaSize.hairline, colors.borderStrong),
                ) {
                    Column(
                        modifier = Modifier
                            .heightIn(max = 200.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        filteredOptions.take(10).forEach { opt ->
                            Surface(
                                onClick = {
                                    onToggle(opt.value)
                                    text = ""
                                    expanded = false
                                },
                                color = colors.surfaceCard,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(opt.label, color = colors.textPrimary)
                                }
                            }
                        }
                        if (showAddCustom) {
                            Surface(
                                onClick = {
                                    onAddCustom(text)
                                    text = ""
                                    expanded = false
                                },
                                color = colors.accentSoft,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text("Add: $text", color = colors.accent, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PresetSection(
    presets: List<FilterPreset>,
    onLoad: (FilterPreset) -> Unit,
    onDelete: (FilterPreset) -> Unit,
    onSave: (String) -> Unit,
) {
    val colors = NovelaTheme.colors
    var expanded by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var presetName by remember { mutableStateOf("") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = NovelaSpacing.lg, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (presets.isNotEmpty()) {
            Column {
                OutlinedButton(
                    onClick = { expanded = !expanded },
                    shape = RoundedCornerShape(NovelaRadius.pill),
                    border = BorderStroke(NovelaSize.hairline, colors.accentLine),
                ) {
                    Text("Presets", style = MaterialTheme.typography.labelLarge, color = colors.accent)
                }
                if (expanded) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(NovelaRadius.md),
                        color = colors.surfaceCard,
                        border = BorderStroke(NovelaSize.hairline, colors.borderStrong),
                    ) {
                        Column(
                            modifier = Modifier
                                .heightIn(max = 200.dp)
                                .verticalScroll(rememberScrollState()),
                        ) {
                            presets.forEach { preset ->
                                Surface(
                                    onClick = {
                                        onLoad(preset)
                                        expanded = false
                                    },
                                    color = colors.surfaceCard,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            Icon(
                                                Icons.Filled.Check,
                                                null,
                                                tint = colors.accent,
                                                modifier = Modifier.size(18.dp),
                                            )
                                            Text(preset.name, color = colors.textPrimary)
                                        }
                                        IconButton(
                                            onClick = { onDelete(preset) },
                                            modifier = Modifier.size(24.dp),
                                        ) {
                                            Icon(
                                                Icons.Filled.Close,
                                                "Delete",
                                                tint = colors.danger,
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
        }
        OutlinedButton(
            onClick = { showSaveDialog = true },
            shape = RoundedCornerShape(NovelaRadius.pill),
            border = BorderStroke(NovelaSize.hairline, colors.borderStrong),
        ) {
            Text("Save filters", style = MaterialTheme.typography.labelLarge, color = colors.textPrimary)
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            shape = RoundedCornerShape(NovelaRadius.sheet),
            containerColor = colors.surfaceFloating,
            title = { Text("Save filter preset", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = presetName,
                    onValueChange = { if (it.length <= 50) presetName = it },
                    label = { Text("Preset name") },
                    singleLine = true,
                    shape = RoundedCornerShape(NovelaRadius.md),
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (presetName.isNotBlank()) {
                            onSave(presetName.trim())
                            presetName = ""
                            showSaveDialog = false
                        }
                    },
                ) {
                    Text("Save", color = colors.accent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        presetName = ""
                        showSaveDialog = false
                    },
                ) {
                    Text("Cancel", color = colors.textSecondary)
                }
            },
        )
    }
}

@Composable
private fun TriStateChip(label: String, state: TriStateValue, onClick: () -> Unit) {
    val colors = NovelaTheme.colors
    val containerColor = when (state) {
        TriStateValue.NEUTRAL -> colors.surfaceSubtle
        TriStateValue.INCLUDED -> colors.accentSoft
        TriStateValue.EXCLUDED -> colors.danger.copy(alpha = 0.14f)
    }
    val labelColor = when (state) {
        TriStateValue.NEUTRAL -> colors.textSecondary
        TriStateValue.INCLUDED -> colors.accent
        TriStateValue.EXCLUDED -> colors.danger
    }
    val borderColor = when (state) {
        TriStateValue.NEUTRAL -> colors.borderSubtle
        TriStateValue.INCLUDED -> colors.accentLine
        TriStateValue.EXCLUDED -> colors.danger.copy(alpha = 0.45f)
    }
    val icon: @Composable (() -> Unit)? = when (state) {
        TriStateValue.NEUTRAL -> null
        TriStateValue.INCLUDED -> {
            {
                Icon(
                    Icons.Filled.Check,
                    null,
                    tint = colors.accent,
                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                )
            }
        }
        TriStateValue.EXCLUDED -> {
            {
                Icon(
                    Icons.Filled.Close,
                    null,
                    tint = colors.danger,
                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                )
            }
        }
    }

    FilterChip(
        selected = state != TriStateValue.NEUTRAL,
        onClick = onClick,
        shape = RoundedCornerShape(NovelaRadius.pill),
        label = { Text(label, color = labelColor) },
        leadingIcon = icon,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = containerColor,
            selectedContainerColor = containerColor,
            labelColor = labelColor,
            selectedLabelColor = labelColor,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = state != TriStateValue.NEUTRAL,
            borderColor = borderColor,
            selectedBorderColor = borderColor,
            borderWidth = NovelaSize.hairline,
            selectedBorderWidth = NovelaSize.hairline,
        ),
    )
}

@Composable
private fun SelectOptionButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = NovelaTheme.colors
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(NovelaRadius.md),
        color = if (selected) colors.accentSoft else colors.surfaceSubtle,
        border = BorderStroke(
            width = if (selected) 1.25.dp else NovelaSize.hairline,
            color = if (selected) colors.accent else colors.borderSubtle,
        ),
        modifier = modifier,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 11.dp, horizontal = 8.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) colors.accent else colors.textSecondary,
            )
        }
    }
}

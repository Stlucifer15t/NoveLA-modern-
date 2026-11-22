package my.noveldokusha.settings

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import my.noveldokusha.core.appPreferences.AppPreferences
import my.noveldokusha.core.models.RegexRule
import my.noveldokusha.core.utils.StateExtra_StringNullable
import my.noveldokusha.data.AppRepository
import javax.inject.Inject

data class RegexCleanupUiState(
    val searchQuery: String = "",
    val rules: List<RegexRule> = emptyList(),
    val isBottomSheetOpen: Boolean = false,
    val editingRule: RegexRule? = null,
    val editingIndex: Int? = null,
    val validationError: String? = null,
    val duplicatePatternError: Boolean = false,
    val previewText: String = "",
    val deleteConfirmationPattern: String? = null
)

@HiltViewModel
class RegexCleanupSettingsViewModel @Inject constructor(
    private val appPreferences: AppPreferences,
    appRepository: AppRepository,
    stateHandler: SavedStateHandle,
) : ViewModel() {

    // null — global rules; otherwise the per-novel rules of this bookUrl
    var bookUrl: String? by StateExtra_StringNullable(stateHandler)

    val isGlobal: Boolean get() = bookUrl == null

    var novelTitle = mutableStateOf("")
        private set

    var uiState = mutableStateOf(RegexCleanupUiState())
        private set

    private val _searchQuery = mutableStateOf("")

    val filteredRules: List<RegexRule>
        get() {
            val query = _searchQuery.value.trim().lowercase()
            if (query.isEmpty()) return uiState.value.rules
            return uiState.value.rules.filter {
                it.pattern.lowercase().contains(query) ||
                it.description.lowercase().contains(query)
            }
        }

    init {
        loadRules()
        val url = bookUrl
        if (url != null) {
            viewModelScope.launch(Dispatchers.IO) {
                val title = appRepository.libraryBooks.get(url)?.title.orEmpty()
                withContext(Dispatchers.Main) { novelTitle.value = title }
            }
        }
    }

    // ── Target store: the global list or the per-novel set ─────────────────────

    private var targetRules: List<RegexRule>
        get() = if (isGlobal) appPreferences.USER_REGEX_CLEANUP_RULES.value
        else appPreferences.USER_REGEX_CLEANUP_RULES_PER_NOVEL.value[bookUrl] ?: emptyList()
        set(value) {
            if (isGlobal) {
                appPreferences.USER_REGEX_CLEANUP_RULES.value = value
            } else {
                val url = checkNotNull(bookUrl) { "bookUrl is required in per-novel mode" }
                val map = appPreferences.USER_REGEX_CLEANUP_RULES_PER_NOVEL.value.toMutableMap()
                map[url] = value
                appPreferences.USER_REGEX_CLEANUP_RULES_PER_NOVEL.value = map
            }
        }

    private fun loadRules() {
        viewModelScope.launch {
            uiState.value = uiState.value.copy(rules = targetRules)
        }
    }

    // ── Callbacks ──────────────────────────────────────────────────────────

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onAddRule() {
        uiState.value = uiState.value.copy(
            isBottomSheetOpen = true,
            editingRule = RegexRule("", "", isEnabled = true),
            editingIndex = null,
            validationError = null
        )
    }

    fun onEditRule(index: Int) {
        val rule = uiState.value.rules.getOrNull(index) ?: return
        uiState.value = uiState.value.copy(
            isBottomSheetOpen = true,
            editingRule = rule,
            editingIndex = index,
            validationError = null
        )
    }

    fun onDismissBottomSheet() {
        uiState.value = uiState.value.copy(
            isBottomSheetOpen = false,
            editingRule = null,
            editingIndex = null,
            validationError = null,
            duplicatePatternError = false
        )
    }

    fun onSaveRule(
        pattern: String,
        replacement: String,
        enabled: Boolean,
        wholeWordsOnly: Boolean,
        description: String
    ) {
        if (!validateRegex(pattern)) {
            uiState.value = uiState.value.copy(
                validationError = pattern,
                duplicatePatternError = false
            )
            return
        }

        // Reject a pattern that is already in the list
        val trimmedPattern = pattern.trim()
        val currentRules = targetRules
        val isDuplicate = currentRules.any { it.pattern == trimmedPattern &&
            uiState.value.editingIndex?.let { idx -> currentRules.indexOf(it) != idx } ?: true
        }
        if (isDuplicate) {
            uiState.value = uiState.value.copy(
                validationError = null,
                duplicatePatternError = true
            )
            return
        }

        uiState.value = uiState.value.copy(validationError = null, duplicatePatternError = false)

        viewModelScope.launch {
            val currentRules = targetRules.toMutableList()
            val newRule = RegexRule(
                pattern = pattern.trim(),
                replacement = replacement.trim(),
                isEnabled = enabled,
                description = description.trim(),
                wholeWordsOnly = wholeWordsOnly
            )

            val editingIndex = uiState.value.editingIndex
            if (editingIndex != null && editingIndex in currentRules.indices) {
                currentRules[editingIndex] = newRule
            } else {
                currentRules.add(newRule)
            }

            targetRules = currentRules
            loadRules()
            onDismissBottomSheet()
        }
    }

    fun onDeleteRule(pattern: String) {
        uiState.value = uiState.value.copy(deleteConfirmationPattern = pattern)
    }

    fun onConfirmDelete() {
        val pattern = uiState.value.deleteConfirmationPattern ?: return
        viewModelScope.launch {
            val currentRules = targetRules.toMutableList()
            currentRules.removeAll { it.pattern == pattern }
            targetRules = currentRules
            loadRules()
            uiState.value = uiState.value.copy(deleteConfirmationPattern = null)
        }
    }

    fun onDismissDelete() {
        uiState.value = uiState.value.copy(deleteConfirmationPattern = null)
    }

    fun onToggleRule(index: Int) {
        viewModelScope.launch {
            val currentRules = targetRules.toMutableList()
            val rule = currentRules.getOrNull(index) ?: return@launch
            currentRules[index] = rule.copy(isEnabled = !rule.isEnabled)
            targetRules = currentRules
            loadRules()
        }
    }

    fun onMoveRule(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            val currentRules = targetRules.toMutableList()
            if (fromIndex in currentRules.indices && toIndex in currentRules.indices) {
                val item = currentRules.removeAt(fromIndex)
                currentRules.add(toIndex, item)
                targetRules = currentRules
            }
            loadRules()
        }
    }

    // ── Per-novel rules: promote to global / drop the whole set ─────────────────

    fun onMoveRulesToGlobal() {
        val url = bookUrl ?: return
        viewModelScope.launch {
            val novelRules = appPreferences.USER_REGEX_CLEANUP_RULES_PER_NOVEL.value[url]
                ?: return@launch
            // A per-novel rule wins over a global rule with the same pattern, so no
            // duplicate is created and the global order is preserved.
            val merged = appPreferences.USER_REGEX_CLEANUP_RULES.value.toMutableList()
            novelRules.forEach { rule ->
                val idx = merged.indexOfFirst { it.pattern == rule.pattern }
                if (idx >= 0) merged[idx] = rule else merged.add(rule)
            }
            appPreferences.USER_REGEX_CLEANUP_RULES.value = merged
            val map = appPreferences.USER_REGEX_CLEANUP_RULES_PER_NOVEL.value.toMutableMap()
            map.remove(url)
            appPreferences.USER_REGEX_CLEANUP_RULES_PER_NOVEL.value = map
            loadRules()
        }
    }

    fun onRemoveNovelRules() {
        val url = bookUrl ?: return
        viewModelScope.launch {
            val map = appPreferences.USER_REGEX_CLEANUP_RULES_PER_NOVEL.value.toMutableMap()
            map.remove(url)
            appPreferences.USER_REGEX_CLEANUP_RULES_PER_NOVEL.value = map
            loadRules()
        }
    }

    fun updatePreview(text: String) {
        uiState.value = uiState.value.copy(previewText = text)
    }

    // ── Validation ─────────────────────────────────────────────────────────

    private fun validateRegex(pattern: String): Boolean {
        return try {
            Regex(pattern)
            true
        } catch (e: Exception) {
            false
        }
    }
}
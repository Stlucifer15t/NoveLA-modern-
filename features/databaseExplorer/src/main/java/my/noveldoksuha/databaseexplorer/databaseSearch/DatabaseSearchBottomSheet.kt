package my.noveldokusha.databaseexplorer.databaseSearch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import my.noveldokusha.coreui.components.CollapsibleDivider
import my.noveldokusha.coreui.components.NovelaPrimaryButton
import my.noveldokusha.coreui.components.NovelaSecondaryButton
import my.noveldokusha.coreui.components.next
import my.noveldokusha.coreui.theme.InternalTheme
import my.noveldokusha.coreui.theme.NovelaRadius
import my.noveldokusha.coreui.theme.NovelaSize
import my.noveldokusha.coreui.theme.NovelaSpacing
import my.noveldokusha.coreui.theme.NovelaTheme
import my.noveldokusha.coreui.theme.PreviewThemes
import my.noveldokusha.databaseexplorer.R

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DatabaseSearchBottomSheet(
    genresList: SnapshotStateList<GenreItem>,
    onGenresFiltersSubmit: () -> Unit,
) {
    val colors = NovelaTheme.colors
    val scrollState = rememberScrollState()

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .padding(horizontal = NovelaSpacing.lg, vertical = 10.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NovelaSecondaryButton(
                text = stringResource(R.string.clear_filters),
                onClick = {
                    val newList = genresList.map { it.copy(state = ToggleableState.Off) }
                    genresList.clear()
                    genresList.addAll(newList)
                },
                modifier = Modifier.weight(1f),
            )

            NovelaPrimaryButton(
                text = stringResource(R.string.search_with_filters),
                onClick = onGenresFiltersSubmit,
                modifier = Modifier.weight(1.3f),
            )
        }

        CollapsibleDivider(scrollState = scrollState)

        FlowRow(
            modifier = Modifier
                .padding(horizontal = NovelaSpacing.lg, vertical = 8.dp)
                .fillMaxWidth()
                .verticalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            genresList.forEachIndexed { index, genreItem ->
                FilterChip(
                    selected = genreItem.state != ToggleableState.Off,
                    onClick = {
                        val item = genresList[index]
                        genresList[index] = item.copy(state = item.state.next())
                    },
                    shape = RoundedCornerShape(NovelaRadius.pill),
                    label = {
                        Text(
                            text = genreItem.genre,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (genreItem.state != ToggleableState.Off) {
                                FontWeight.SemiBold
                            } else {
                                FontWeight.Normal
                            },
                        )
                    },
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = genreItem.state != ToggleableState.Off,
                        borderColor = colors.borderSubtle,
                        selectedBorderColor = when (genreItem.state) {
                            ToggleableState.On -> colors.accentLine
                            ToggleableState.Indeterminate -> colors.danger.copy(alpha = 0.45f)
                            else -> colors.borderSubtle
                        },
                        borderWidth = NovelaSize.hairline,
                        selectedBorderWidth = NovelaSize.hairline,
                    ),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = colors.surfaceSubtle,
                        labelColor = colors.textSecondary,
                        selectedContainerColor = when (genreItem.state) {
                            ToggleableState.On -> colors.accentSoft
                            ToggleableState.Indeterminate -> colors.danger.copy(alpha = 0.14f)
                            else -> colors.surfaceSubtle
                        },
                        selectedLabelColor = when (genreItem.state) {
                            ToggleableState.On -> colors.accent
                            ToggleableState.Indeterminate -> colors.danger
                            else -> colors.textSecondary
                        },
                        selectedLeadingIconColor = when (genreItem.state) {
                            ToggleableState.On -> colors.accent
                            ToggleableState.Indeterminate -> colors.danger
                            else -> colors.textMuted
                        },
                    ),
                    leadingIcon = {
                        when (genreItem.state) {
                            ToggleableState.On -> Icon(
                                Icons.Filled.CheckCircle,
                                null,
                                modifier = Modifier.size(16.dp),
                            )
                            ToggleableState.Indeterminate -> Icon(
                                Icons.Filled.RemoveCircle,
                                null,
                                modifier = Modifier.size(16.dp),
                            )
                            ToggleableState.Off -> Icon(
                                Icons.Filled.Add,
                                null,
                                tint = colors.textMuted,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    },
                )
            }
            Spacer(modifier = Modifier.height(260.dp))
        }
    }
}

@PreviewThemes
@Composable
private fun PreviewView() {
    val list = remember {
        mutableStateListOf(
            GenreItem("Fantasy", "fn", ToggleableState.On),
            GenreItem("Horror", "fn", ToggleableState.Off),
            GenreItem("Drama", "fn", ToggleableState.Indeterminate),
            GenreItem("Historical", "fn", ToggleableState.Off),
            GenreItem("Crime", "fn", ToggleableState.Off),
            GenreItem("Seinen", "fn", ToggleableState.Off),
            GenreItem("Action", "fn", ToggleableState.Off),
        )
    }
    InternalTheme {
        DatabaseSearchBottomSheet(
            genresList = list,
            onGenresFiltersSubmit = { },
        )
    }
}

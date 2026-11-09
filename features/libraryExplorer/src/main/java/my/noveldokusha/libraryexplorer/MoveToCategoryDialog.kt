package my.noveldokusha.libraryexplorer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import my.noveldokusha.coreui.R
import my.noveldokusha.coreui.theme.NovelaRadius
import my.noveldokusha.coreui.theme.NovelaTheme

@Composable
internal fun MoveToCategoryDialog(
    categories: List<String>,
    onDismiss: () -> Unit,
    onCategorySelected: (String) -> Unit,
    onCreateCategory: () -> Unit = {},
) {
    val colors = NovelaTheme.colors
    var selectedCategory by remember { mutableStateOf(categories.firstOrNull() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(NovelaRadius.sheet),
        containerColor = colors.surfaceFloating,
        title = {
            Text(
                text = stringResource(R.string.move_selected_to_category),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp),
            ) {
                items(categories) { category ->
                    val label = when (category) {
                        "" -> stringResource(R.string.reading)
                        "Completed" -> stringResource(R.string.completed)
                        else -> category
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(NovelaRadius.sm))
                            .clickable { selectedCategory = category }
                            .padding(vertical = 4.dp, horizontal = 4.dp),
                    ) {
                        RadioButton(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            colors = RadioButtonDefaults.colors(selectedColor = colors.accent),
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (selectedCategory == category) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selectedCategory == category) colors.accent else colors.textPrimary,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
                item {
                    HorizontalDivider(
                        color = colors.borderSubtle,
                        modifier = Modifier.padding(vertical = 6.dp),
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(NovelaRadius.sm))
                            .clickable(onClick = onCreateCategory)
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            tint = colors.accent,
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.create_category),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.accent,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onCategorySelected(selectedCategory) }) {
                Text(
                    text = stringResource(R.string.move_to),
                    color = colors.accent,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

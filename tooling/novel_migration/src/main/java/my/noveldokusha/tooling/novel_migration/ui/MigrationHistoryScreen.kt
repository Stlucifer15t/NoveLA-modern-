package my.noveldokusha.tooling.novel_migration.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import my.noveldokusha.coreui.components.NovelaEmptyState
import my.noveldokusha.coreui.theme.NovelaRadius
import my.noveldokusha.coreui.theme.NovelaSize
import my.noveldokusha.coreui.theme.NovelaSpacing
import my.noveldokusha.coreui.theme.NovelaTheme
import my.noveldokusha.strings.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MigrationHistoryScreen(
    groups: List<SourcePairGroup>,
    onPressBack: () -> Unit,
    onDeleteGroup: (oldSourceId: String, newSourceId: String) -> Unit,
) {
    val colors = NovelaTheme.colors

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        scrolledContainerColor = MaterialTheme.colorScheme.background,
                    ),
                    title = {
                        Text(
                            text = stringResource(R.string.migration_history),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.textPrimary,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onPressBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = null,
                                tint = colors.textPrimary,
                            )
                        }
                    },
                )
                HorizontalDivider(thickness = NovelaSize.hairline, color = colors.borderSubtle)
            }
        },
    ) { padding ->
        if (groups.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                NovelaEmptyState(
                    icon = Icons.Outlined.History,
                    title = stringResource(R.string.migration_no_history),
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(NovelaSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(groups, key = { "${it.oldSourceId}_${it.newSourceId}" }) { group ->
                    SourcePairGroupItem(
                        group = group,
                        onDelete = { onDeleteGroup(group.oldSourceId, group.newSourceId) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SourcePairGroupItem(
    group: SourcePairGroup,
    onDelete: () -> Unit,
) {
    val colors = NovelaTheme.colors
    Surface(
        shape = RoundedCornerShape(NovelaRadius.card),
        color = colors.surfaceCard,
        border = BorderStroke(NovelaSize.hairline, colors.borderSubtle),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${group.oldSourceName} → ${group.newSourceName}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.migration_books_count, group.totalCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.accent,
                    fontWeight = FontWeight.Medium,
                )
                if (group.completedCount > 0 || group.partialCount > 0) {
                    Text(
                        text = buildString {
                            if (group.completedCount > 0) {
                                append(stringResource(R.string.migration_completed_count, group.completedCount))
                            }
                            if (group.partialCount > 0) {
                                if (isNotEmpty()) append(", ")
                                append(stringResource(R.string.migration_partial_count, group.partialCount))
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted,
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = colors.danger,
                )
            }
        }
    }
}

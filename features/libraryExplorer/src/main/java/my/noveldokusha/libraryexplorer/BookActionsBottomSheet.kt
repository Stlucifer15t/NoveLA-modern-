package my.noveldokusha.libraryexplorer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.AltRoute
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.RemoveDone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import my.noveldokusha.core.rememberResolvedBookImagePath
import my.noveldokusha.coreui.R
import my.noveldokusha.coreui.components.BookRatingChip
import my.noveldokusha.coreui.components.ImageView
import my.noveldokusha.coreui.components.NovelaFilterChip
import my.noveldokusha.coreui.components.NovelaSettingsTile
import my.noveldokusha.coreui.theme.NovelaRadius
import my.noveldokusha.coreui.theme.NovelaSize
import my.noveldokusha.coreui.theme.NovelaSpacing
import my.noveldokusha.coreui.theme.NovelaTheme
import my.noveldokusha.coreui.theme.NovelaType
import my.noveldokusha.feature.local_database.tables.Book
import my.noveldokusha.strings.R as StringsR

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun BookActionsBottomSheet(
    book: Book,
    categories: List<String>,
    onDismiss: () -> Unit,
    onCategorySelected: (String) -> Unit,
    onMarkAllChaptersRead: () -> Unit,
    onMarkAllChaptersUnread: () -> Unit,
    onDeleteNovel: () -> Unit,
    onMigrate: () -> Unit = {},
) {
    val colors = NovelaTheme.colors
    var selectedCategory by remember { mutableStateOf(book.category) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = NovelaRadius.sheet, topEnd = NovelaRadius.sheet),
        containerColor = colors.surfaceFloating,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = NovelaSpacing.lg)
                .padding(bottom = 28.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            // ── Book Hero Row ────────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                val coverShape = RoundedCornerShape(NovelaRadius.cover)
                ImageView(
                    imageModel = rememberResolvedBookImagePath(
                        bookUrl = book.url,
                        imagePath = book.coverImageUrl,
                    ),
                    error = R.drawable.default_book_cover,
                    modifier = Modifier
                        .width(66.dp)
                        .aspectRatio(NovelaSize.coverAspectRatio)
                        .clip(coverShape)
                        .border(NovelaSize.hairline, colors.borderSubtle, coverShape),
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = book.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (book.rating.isNotBlank()) {
                        BookRatingChip(rating = book.rating)
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ── Category Pills ───────────────────────────────────────────────
            Text(
                text = stringResource(R.string.category_name).uppercase(),
                color = colors.accent,
                style = NovelaType.sectionEyebrow,
                modifier = Modifier.padding(start = 2.dp, bottom = 8.dp),
            )

            val categoryLabels = categories.map { category ->
                val label = when (category) {
                    "" -> stringResource(R.string.reading)
                    "Completed" -> stringResource(R.string.completed)
                    else -> category
                }
                category to label
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                categoryLabels.forEach { (category, label) ->
                    NovelaFilterChip(
                        label = label,
                        selected = selectedCategory == category,
                        onClick = {
                            selectedCategory = category
                            onCategorySelected(category)
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ── Grouped Quick Actions Card ───────────────────────────────────
            Surface(
                shape = RoundedCornerShape(NovelaRadius.card),
                color = colors.surfaceCard,
                border = BorderStroke(NovelaSize.hairline, colors.borderSubtle),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    NovelaSettingsTile(
                        title = stringResource(R.string.mark_all_chapters_read),
                        icon = Icons.Rounded.DoneAll,
                        showChevron = true,
                        showDivider = true,
                        onClick = {
                            onMarkAllChaptersRead()
                            onDismiss()
                        },
                    )
                    NovelaSettingsTile(
                        title = stringResource(R.string.mark_all_chapters_unread),
                        icon = Icons.Rounded.RemoveDone,
                        showChevron = true,
                        showDivider = true,
                        onClick = {
                            onMarkAllChaptersUnread()
                            onDismiss()
                        },
                    )
                    NovelaSettingsTile(
                        title = stringResource(StringsR.string.migration_migrate_src),
                        icon = Icons.AutoMirrored.Rounded.AltRoute,
                        showChevron = true,
                        showDivider = true,
                        onClick = {
                            onMigrate()
                            onDismiss()
                        },
                    )
                    NovelaSettingsTile(
                        title = stringResource(R.string.delete_novel),
                        icon = Icons.Outlined.DeleteOutline,
                        iconTint = colors.danger,
                        iconContainerColor = colors.danger.copy(alpha = 0.14f),
                        onClick = {
                            onDeleteNovel()
                            onDismiss()
                        },
                    )
                }
            }
        }
    }
}

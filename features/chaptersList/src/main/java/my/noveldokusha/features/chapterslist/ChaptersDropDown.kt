package my.noveldokusha.features.chapterslist

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import my.noveldokusha.chapterslist.R
import my.noveldokusha.coreui.theme.NovelaSize
import my.noveldokusha.coreui.theme.NovelaTheme
import my.noveldokusha.strings.R as StringsR

@Composable
internal fun ChaptersDropDown(
    isLocalSource: Boolean,
    bookUrl: String,
    bookTitle: String,
    openInBrowser: () -> Unit,
    onSearchBookInDatabase: () -> Unit,
    onResumeReading: () -> Unit,
    onChangeCover: () -> Unit,
    onDownloadNext100Chapters: () -> Unit,
    onDownloadAllChapters: () -> Unit,
    onExport: (bookUrl: String, bookTitle: String) -> Unit,
    onMigrateBook: () -> Unit = {},
    onDeleteTranslations: () -> Unit = {},
    onFixBook: () -> Unit = {},
) {
    val colors = NovelaTheme.colors

    DropdownMenuItem(
        onClick = onResumeReading,
        text = {
            Text(
                text = stringResource(id = R.string.resume_reading),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textPrimary,
            )
        },
        leadingIcon = {
            Icon(
                Icons.Filled.PlayArrow,
                stringResource(R.string.resume_reading),
                tint = colors.accent,
            )
        },
    )

    if (!isLocalSource) {
        DropdownMenuItem(
            onClick = openInBrowser,
            text = {
                Text(
                    text = stringResource(id = R.string.open_in_browser),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textPrimary,
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Outlined.Public,
                    stringResource(R.string.open_in_browser),
                    tint = colors.textSecondary,
                )
            },
        )
    }

    DropdownMenuItem(
        onClick = onSearchBookInDatabase,
        text = {
            Text(
                text = stringResource(id = R.string.find_in_database),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textPrimary,
            )
        },
        leadingIcon = {
            Icon(
                Icons.Outlined.Search,
                stringResource(R.string.find_in_database),
                tint = colors.textSecondary,
            )
        },
    )

    DropdownMenuItem(
        onClick = onChangeCover,
        text = {
            Text(
                text = stringResource(id = R.string.change_cover),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textPrimary,
            )
        },
        leadingIcon = {
            Icon(
                Icons.Filled.Image,
                stringResource(R.string.change_cover),
                tint = colors.textSecondary,
            )
        },
    )

    HorizontalDivider(
        thickness = NovelaSize.hairline,
        color = colors.borderSubtle,
        modifier = Modifier.padding(vertical = 4.dp),
    )

    if (!isLocalSource) {
        DropdownMenuItem(
            onClick = onDownloadNext100Chapters,
            text = {
                Text(
                    text = stringResource(StringsR.string.download_next_100_chapters),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textPrimary,
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Filled.CloudDownload,
                    stringResource(StringsR.string.download_next_100_chapters),
                    tint = colors.accent,
                )
            },
        )
        DropdownMenuItem(
            onClick = onDownloadAllChapters,
            text = {
                Text(
                    text = stringResource(R.string.download_all_chapters),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textPrimary,
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Filled.CloudDownload,
                    stringResource(R.string.download_all_chapters),
                    tint = colors.accent,
                )
            },
        )
    }

    DropdownMenuItem(
        onClick = { onExport(bookUrl, bookTitle) },
        text = {
            Text(
                text = stringResource(StringsR.string.export),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textPrimary,
            )
        },
        leadingIcon = {
            Icon(
                Icons.Filled.IosShare,
                stringResource(StringsR.string.export),
                tint = colors.textSecondary,
            )
        },
    )

    DropdownMenuItem(
        onClick = onMigrateBook,
        text = {
            Text(
                text = stringResource(StringsR.string.migration_tab),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textPrimary,
            )
        },
        leadingIcon = {
            Icon(
                Icons.AutoMirrored.Filled.AltRoute,
                contentDescription = stringResource(StringsR.string.migration_tab),
                tint = colors.textSecondary,
            )
        },
    )

    if (!isLocalSource) {
        DropdownMenuItem(
            onClick = onFixBook,
            text = {
                Text(
                    text = stringResource(R.string.book_fix),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textPrimary,
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Filled.Build,
                    contentDescription = stringResource(R.string.book_fix),
                    tint = colors.textSecondary,
                )
            },
        )
    }

    HorizontalDivider(
        thickness = NovelaSize.hairline,
        color = colors.borderSubtle,
        modifier = Modifier.padding(vertical = 4.dp),
    )

    DropdownMenuItem(
        onClick = onDeleteTranslations,
        text = {
            Text(
                text = stringResource(R.string.delete_book_translations),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.danger,
            )
        },
        leadingIcon = {
            Icon(
                Icons.Outlined.DeleteSweep,
                contentDescription = stringResource(R.string.delete_book_translations),
                tint = colors.danger,
            )
        },
    )
}

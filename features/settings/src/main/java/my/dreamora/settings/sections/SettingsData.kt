package my.dreamora.settings.sections

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DataArray
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.VideoFile
import androidx.compose.material.icons.automirrored.outlined.TextSnippet
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import my.dreamora.settings.ui.SettingsRow
import my.dreamora.coreui.theme.DreamoraSpacing
import my.dreamora.coreui.theme.textPadding
import my.dreamora.settings.R
import androidx.compose.ui.text.font.FontWeight

@Composable
internal fun SettingsData(
    databaseSize: String,
    imagesFolderSize: String,
    novelCacheSize: String,
    mangaCacheSize: String,
    offlineVideoSize: String,
    isCleaningDatabase: Boolean,
    isCleaningImages: Boolean,
    isCleaningNovelCache: Boolean,
    isCleaningMangaCache: Boolean,
    isCleaningOfflineVideo: Boolean,
    onRequestCleanDatabase: () -> Unit,
    onRequestCleanImageFolder: () -> Unit,
    onRequestCleanNovelCache: () -> Unit,
    onRequestCleanMangaCache: () -> Unit,
    onRequestCleanOfflineVideo: () -> Unit,
) {
    Column {
        Text(
            text = stringResource(id = R.string.data),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.textPadding().padding(bottom = DreamoraSpacing.sm),
            color = MaterialTheme.colorScheme.onSurface,
        )
        CleanRow(
            title = stringResource(R.string.clean_database),
            size = databaseSize,
            isCleaning = isCleaningDatabase,
            icon = Icons.Outlined.DataArray,
            onClick = onRequestCleanDatabase,
            enabled = !isCleaningDatabase,
        )
        CleanRow(
            title = stringResource(R.string.clean_images_folder),
            size = imagesFolderSize,
            isCleaning = isCleaningImages,
            icon = Icons.Outlined.Image,
            onClick = onRequestCleanImageFolder,
            enabled = !isCleaningImages,
        )
        CleanRow(
            title = stringResource(R.string.clean_novel_cache),
            size = novelCacheSize,
            isCleaning = isCleaningNovelCache,
            icon = Icons.AutoMirrored.Outlined.TextSnippet,
            onClick = onRequestCleanNovelCache,
            enabled = !isCleaningNovelCache,
        )
        CleanRow(
            title = stringResource(R.string.clean_manga_cache),
            size = mangaCacheSize,
            isCleaning = isCleaningMangaCache,
            icon = Icons.Outlined.Image,
            onClick = onRequestCleanMangaCache,
            enabled = !isCleaningMangaCache,
        )
        CleanRow(
            title = stringResource(R.string.clean_offline_video),
            size = offlineVideoSize,
            isCleaning = isCleaningOfflineVideo,
            icon = Icons.Outlined.VideoFile,
            onClick = onRequestCleanOfflineVideo,
            enabled = !isCleaningOfflineVideo,
        )
    }
}

@Composable
private fun CleanRow(
    title: String,
    size: String,
    isCleaning: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    enabled: Boolean,
) {
    SettingsRow(
        modifier = Modifier
            .textPadding()
            .clickable(enabled = enabled) { onClick() },
        headlineContent = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = size,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        leadingContent = {
            if (isCleaning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
    )
}

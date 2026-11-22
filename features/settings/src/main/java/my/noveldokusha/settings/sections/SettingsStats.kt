package my.noveldokusha.settings.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import my.noveldokusha.coreui.theme.NovelaType
import my.noveldokusha.coreui.theme.colorAccent
import my.noveldokusha.coreui.theme.textPadding
import my.noveldokusha.settings.ReadingStats
import my.noveldokusha.settings.R
import my.noveldokusha.settings.displayCount

/**
 * What this device has read and watched, separated by kind.
 *
 * Two lists, not one: a chapter and an episode are different units, and mixing them into a
 * single "chapters" number is how reading stats start lying. The values are counts over the
 * library, history and chapter tables — the app keeps no reading timer and no attention model,
 * so there is no "minutes read" here, and the watch time it does show is the player's own
 * position per episode rather than an estimate.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SettingsStats(stats: ReadingStats) {
    var showMovies by rememberSaveable { mutableStateOf(false) }

    Column {
        Text(
            text = stringResource(R.string.settings_group_stats),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.textPadding(),
            color = colorAccent(),
        )
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .textPadding(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            FilterChip(
                selected = !showMovies,
                onClick = { showMovies = false },
                label = { Text(stringResource(R.string.stats_books)) },
            )
            FilterChip(
                selected = showMovies,
                onClick = { showMovies = true },
                label = { Text(stringResource(R.string.stats_movies)) },
            )
        }

        if (!showMovies) {
            StatRow(
                icon = Icons.Outlined.MenuBook,
                title = stringResource(R.string.stats_library_books),
                value = stats.libraryBooks.displayCount(),
            )
            StatRow(
                icon = Icons.Outlined.MenuBook,
                title = stringResource(R.string.stats_chapters_read),
                value = stats.chaptersRead.displayCount(),
            )
            StatRow(
                icon = Icons.Outlined.MenuBook,
                title = stringResource(R.string.stats_books_finished),
                value = stats.booksFinished.displayCount(),
            )
            StatRow(
                icon = Icons.Outlined.MenuBook,
                title = stringResource(R.string.stats_streak),
                value = stats.streakDays.displayCount(),
            )
            StatRow(
                icon = Icons.Outlined.MenuBook,
                title = stringResource(R.string.stats_days_active),
                value = stats.daysActive.displayCount(),
            )
        } else {
            StatRow(
                icon = Icons.Outlined.PlayCircle,
                title = stringResource(R.string.stats_movies_in_library),
                value = stats.moviesInLibrary.displayCount(),
            )
            StatRow(
                icon = Icons.Outlined.PlayCircle,
                title = stringResource(R.string.stats_episodes_watched),
                value = stats.episodesWatched.displayCount(),
            )
            StatRow(
                icon = Icons.Outlined.PlayCircle,
                title = stringResource(R.string.stats_watch_time),
                value = watchTimeLabel(stats.watchMinutes()),
            )
            if (stats.moviesInLibrary <= 0 && stats.episodesWatched <= 0) {
                Text(
                    text = stringResource(R.string.stats_no_movies),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .textPadding()
                        .padding(top = 4.dp),
                )
            }
        }

        Text(
            text = stringResource(
                if (showMovies) R.string.stats_note_movies else R.string.stats_note_books
            ),
            style = NovelaType.bookMeta,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .textPadding()
                .padding(top = 6.dp),
        )
    }
}

/** "1 h 12 min" above an hour, "47 min" below it — and a dash while the count is not in yet. */
@Composable
internal fun watchTimeLabel(minutes: Long): String = when {
    minutes < 0L -> "…"
    minutes < 60L -> stringResource(R.string.stats_minute, minutes.toInt())
    else -> stringResource(R.string.stats_hour, (minutes / 60).toInt(), (minutes % 60).toInt())
}

@Composable
private fun StatRow(
    icon: ImageVector,
    title: String,
    value: String,
) {
    val palette = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .textPadding()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Surface(
            modifier = Modifier.size(30.dp),
            shape = RoundedCornerShape(9.dp),
            color = palette.primary.copy(alpha = 0.12f),
        ) {
            Icon(
                icon,
                null,
                modifier = Modifier
                    .padding(6.dp)
                    .size(18.dp),
                tint = palette.primary,
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = palette.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = colorAccent(),
        )
    }
}

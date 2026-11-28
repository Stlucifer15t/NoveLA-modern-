package my.dreamora.settings.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
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
import my.dreamora.coreui.theme.DreamoraSpacing
import my.dreamora.coreui.theme.DreamoraType
import my.dreamora.coreui.theme.colorAccent
import my.dreamora.coreui.theme.textPadding
import my.dreamora.settings.ReadingStats
import my.dreamora.settings.R
import my.dreamora.settings.displayCount
import my.dreamora.settings.watchMinutes
import my.dreamora.settings.ui.PremiumSectionHeader
import my.dreamora.settings.ui.PremiumSectionTitle
import my.dreamora.settings.ui.PremiumSettingsRow
import androidx.compose.ui.text.font.FontWeight

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SettingsStats(stats: ReadingStats) {
    var showMovies by rememberSaveable { mutableStateOf(false) }

    Column {
        PremiumSectionHeader(
            icon = Icons.Outlined.Insights,
            title = stringResource(R.string.settings_group_stats),
            summary = stringResource(R.string.settings_group_stats_hint),
            accent = MaterialTheme.colorScheme.primary,
        )

        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .textPadding(),
            horizontalArrangement = Arrangement.spacedBy(DreamoraSpacing.sm),
        ) {
            PremiumChip(
                selected = !showMovies,
                onClick = { showMovies = false },
                label = { Text(stringResource(R.string.stats_books), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium)) },
            )
            PremiumChip(
                selected = showMovies,
                onClick = { showMovies = true },
                label = { Text(stringResource(R.string.stats_movies), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium)) },
            )
        }

        if (!showMovies) {
            ReadingStatsPanel(stats = stats)
            Spacer(Modifier.height(DreamoraSpacing.md))
            PremiumStatRow(
                icon = Icons.Outlined.MenuBook,
                title = stringResource(R.string.stats_library_books),
                value = stats.libraryBooks.displayCount(),
            )
            PremiumStatRow(
                icon = Icons.Outlined.MenuBook,
                title = stringResource(R.string.stats_books_finished),
                value = stats.booksFinished.displayCount(),
            )
            PremiumStatRow(
                icon = Icons.Outlined.MenuBook,
                title = stringResource(R.string.stats_streak),
                value = stats.streakDays.displayCount(),
            )
        } else {
            PremiumStatRow(
                icon = Icons.Outlined.PlayCircle,
                title = stringResource(R.string.stats_movies_in_library),
                value = stats.moviesInLibrary.displayCount(),
            )
            PremiumStatRow(
                icon = Icons.Outlined.PlayCircle,
                title = stringResource(R.string.stats_episodes_watched),
                value = stats.episodesWatched.displayCount(),
            )
            PremiumStatRow(
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
                        .padding(top = DreamoraSpacing.xs),
                )
            }
        }

        Text(
            text = stringResource(
                if (showMovies) R.string.stats_note_movies else R.string.stats_note_books
            ),
            style = DreamoraType.bookMeta,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .textPadding()
                .padding(top = DreamoraSpacing.xs),
        )
    }
}

internal fun watchTimeLabel(minutes: Long): String = when {
    minutes < 0L -> "…"
    minutes < 60L -> stringResource(R.string.stats_minute, minutes.toInt())
    else -> stringResource(R.string.stats_hour, (minutes / 60).toInt(), (minutes % 60).toInt())
}

@Composable
private fun PremiumStatRow(
    icon: ImageVector,
    title: String,
    value: String,
) {
    val palette = MaterialTheme.colorScheme
    PremiumSettingsRow(
        modifier = Modifier.textPadding().padding(vertical = DreamoraSpacing.xs),
        headlineContent = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DreamoraSpacing.md),
            ) {
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = RoundedCornerShape(DreamoraSpacing.xl),
                    color = palette.primary.copy(alpha = 0.12f),
                ) {
                    Icon(
                        icon,
                        null,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(20.dp),
                        tint = palette.primary,
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = palette.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = colorAccent(),
                )
            }
        },
    )
}

package my.dreamora.settings.sections

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import my.dreamora.coreui.components.ImageView
import my.dreamora.settings.MostReadBook
import my.dreamora.settings.R
import my.dreamora.settings.ReadingStats
import my.dreamora.settings.displayCount
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Our own targets for the three achievement cards. They are thresholds the app sets, not
 * numbers it measured, so each card shows real progress toward a stated goal.
 */
private const val BOOKWORM_CHAPTERS = 10
private const val WEEK_WARRIOR_DAYS = 7
private const val MONTHLY_MASTER_DAYS = 20

/**
 * Reading Stats, Books view. Every number comes from the reader's measured log or the history
 * table; a value that has not loaded yet shows an ellipsis instead of a confident zero.
 */
@Composable
internal fun ReadingStatsPanel(stats: ReadingStats) {
    val context = LocalContext.current
    val todayLabel = readingTimeLabel(stats.readSecondsToday)
    val weekLabel = readingTimeLabel(stats.readSecondsThisWeek)
    val monthLabel = readingTimeLabel(stats.readSecondsThisMonth)
    val shareText = stringResource(
        R.string.stats_share_text,
        todayLabel,
        weekLabel,
        stats.chaptersRead.coerceAtLeast(0),
    )

    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.stats_reading_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = {
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareText)
                }
                context.startActivity(Intent.createChooser(send, null))
            }) {
                Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.stats_share))
            }
        }

        SectionTitle(Icons.Filled.DateRange, stringResource(R.string.stats_this_week_title))
        WeekChart(dailySeconds = stats.weekDailySeconds)

        SectionTitle(Icons.Outlined.Timer, stringResource(R.string.stats_reading_time))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            PeriodCard(
                value = todayLabel,
                label = stringResource(R.string.stats_today),
                container = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier.weight(1f),
            )
            PeriodCard(
                value = weekLabel,
                label = stringResource(R.string.stats_period_week),
                container = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.weight(1f),
            )
            PeriodCard(
                value = monthLabel,
                label = stringResource(R.string.stats_period_month),
                container = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.weight(1f),
            )
        }

        SectionTitle(Icons.Outlined.MenuBook, stringResource(R.string.stats_most_read))
        MostReadList(books = stats.mostRead)

        SectionTitle(Icons.Filled.Star, stringResource(R.string.stats_achievements))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AchievementCard(
                icon = Icons.Outlined.MenuBook,
                name = stringResource(R.string.stats_ach_bookworm),
                current = stats.chaptersRead,
                target = BOOKWORM_CHAPTERS,
            )
            AchievementCard(
                icon = Icons.Filled.DateRange,
                name = stringResource(R.string.stats_ach_week),
                current = stats.daysWithReadingWeek,
                target = WEEK_WARRIOR_DAYS,
            )
            AchievementCard(
                icon = Icons.Outlined.Timer,
                name = stringResource(R.string.stats_ach_month),
                current = stats.daysWithReadingMonth,
                target = MONTHLY_MASTER_DAYS,
            )
        }

        SectionTitle(Icons.Filled.Star, stringResource(R.string.stats_all_time))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            AllTimeCard(
                icon = Icons.Outlined.MenuBook,
                value = stats.chaptersRead.displayCount(),
                label = stringResource(R.string.stats_chapters_read),
                modifier = Modifier.weight(1f),
            )
            AllTimeCard(
                icon = Icons.Filled.DateRange,
                value = stats.daysActive.displayCount(),
                label = stringResource(R.string.stats_days_active),
                modifier = Modifier.weight(1f),
            )
            AllTimeCard(
                icon = Icons.Outlined.Timer,
                value = if (stats.readSecondsAllTime < 0) "…"
                else stringResource(R.string.stats_hours_short, stats.readSecondsAllTime / 3600),
                label = stringResource(R.string.stats_total),
                modifier = Modifier.weight(1f),
            )
        }

        Text(
            text = stringResource(R.string.stats_reading_note),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Minutes below an hour, hours and minutes above. -1 is the not-loaded state. */
@Composable
private fun readingTimeLabel(seconds: Long): String {
    if (seconds < 0) return "…"
    val minutes = seconds / 60
    return if (minutes < 60) stringResource(R.string.stats_minute, minutes)
    else stringResource(R.string.stats_hour, minutes / 60, minutes % 60)
}

@Composable
private fun SectionTitle(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(text = text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

/** Seven bars, Monday first. Heights scale to the busiest day; today is highlighted. */
@Composable
private fun WeekChart(dailySeconds: List<Long>) {
    val letters = remember { weekdayLetters() }
    val todayIndex = remember { mondayBasedToday() }
    val values = if (dailySeconds.size == 7) dailySeconds else List(7) { 0L }
    val busiest = values.maxOrNull() ?: 0L

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                values.forEachIndexed { index, seconds ->
                    val fraction = if (busiest > 0L) seconds.toFloat() / busiest else 0f
                    Box(
                        modifier = Modifier
                            .width(22.dp)
                            .height(4.dp + 88.dp * fraction)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (index == todayIndex) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                letters.forEachIndexed { index, letter ->
                    Text(
                        text = letter,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (index == todayIndex) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(22.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun PeriodCard(value: String, label: String, container: Color, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(20.dp), color = container) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MostReadList(books: List<MostReadBook>) {
    if (books.isEmpty()) {
        Text(
            text = stringResource(R.string.stats_most_read_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        books.forEachIndexed { index, book ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RankPill(rank = index + 1)
                    Spacer(Modifier.width(12.dp))
                    ImageView(
                        imageModel = book.coverUrl,
                        modifier = Modifier
                            .size(width = 48.dp, height = 68.dp)
                            .clip(RoundedCornerShape(8.dp)),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = book.title,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val detail = listOfNotNull(readingTimeLabel(book.seconds), book.sourceName)
                            .joinToString(" · ")
                        Text(
                            text = detail,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RankPill(rank: Int) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(
                if (rank == 1) MaterialTheme.colorScheme.tertiaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "#$rank",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun AchievementCard(icon: ImageVector, name: String, current: Int, target: Int) {
    val shown = current.coerceAtLeast(0)
    Surface(
        modifier = Modifier.width(150.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (shown.toFloat() / target).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "$shown / $target",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AllTimeCard(icon: ImageVector, value: String, label: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Single-letter weekday names, Monday first, in the device language. */
private fun weekdayLetters(): List<String> {
    val format = SimpleDateFormat("EEEEE", Locale.getDefault())
    val monday = Calendar.getInstance().apply {
        firstDayOfWeek = Calendar.MONDAY
        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
    }
    return List(7) { offset ->
        val day = (monday.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, offset) }
        format.format(day.time)
    }
}

/** Index of today in a Monday-first week: Monday = 0 … Sunday = 6. */
private fun mondayBasedToday(): Int =
    (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) + 5) % 7

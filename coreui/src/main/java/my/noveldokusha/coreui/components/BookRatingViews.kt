package my.noveldokusha.coreui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.StarHalf
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import my.noveldokusha.coreui.R
import my.noveldokusha.coreui.theme.NovelaRadius
import my.noveldokusha.coreui.theme.NovelaTheme
import java.util.Locale
import kotlin.math.floor

internal enum class BookRatingType { Score, Rank }

internal data class ParsedBookRating(
    val type: BookRatingType,
    val value: String,
    val score: Float? = null,
)

private val RANK_WORD_REGEX = Regex("""(?i)\brank\b\D*(\d+(?:[.,]\d+)?)""")
private val RANK_HASH_REGEX = Regex("""#\s*(\d+(?:[.,]\d+)?)""")
private val FRACTION_REGEX = Regex("""(\d+(?:[.,]\d+)?)\s*/\s*(\d+(?:[.,]\d+)?)""")
private val NUMBER_REGEX = Regex("""(\d+(?:[.,]\d+)?)""")

internal fun parseBookRating(raw: String?): ParsedBookRating? {
    val text = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null

    (RANK_WORD_REGEX.find(text) ?: RANK_HASH_REGEX.find(text))?.let { match ->
        val rawNum = match.groupValues[1]
        if (rawNum.contains('.') || rawNum.contains(',')) return null
        val rank = rawNum.toIntOrNull() ?: return null
        if (rank <= 0) return null
        return ParsedBookRating(type = BookRatingType.Rank, value = rank.toString())
    }

    FRACTION_REGEX.find(text)?.let { match ->
        val numerator = match.groupValues[1].replace(',', '.').toFloatOrNull() ?: return null
        val scale = match.groupValues[2].replace(',', '.').toFloatOrNull() ?: return null
        if (scale <= 0f || numerator < 0f || numerator > scale) return null
        val normalized = (numerator / scale) * 5f
        return ParsedBookRating(
            type = BookRatingType.Score,
            value = formatScore(normalized),
            score = normalized,
        )
    }

    val value = NUMBER_REGEX.find(text)
        ?.groupValues?.get(1)
        ?.replace(',', '.')
        ?.toFloatOrNull() ?: return null
    if (value < 0f || value > 5f) return null

    return ParsedBookRating(
        type = BookRatingType.Score,
        value = formatScore(value),
        score = value,
    )
}

private fun formatScore(score: Float): String {
    val truncated = floor((score + 1e-4f) * 10f) / 10f
    val formatted = String.format(Locale.US, "%.1f", truncated)
    return formatted.removeSuffix(".0")
}

/**
 * Compact WebNovel-style frosted gold rating/rank capsule for book covers.
 * Uses a dark smoked backdrop so the score remains crisp over any cover artwork.
 */
@Composable
fun BookRatingBadge(
    rating: String,
    modifier: Modifier = Modifier,
) {
    val parsed = remember(rating) { parseBookRating(rating) } ?: return
    val colors = NovelaTheme.colors
    val gold = Color(0xFFFFBC1F)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(NovelaRadius.pill),
        color = colors.coverBadgeBg,
        border = BorderStroke(0.5.dp, colors.coverBadgeBorder),
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.5.dp),
        ) {
            Icon(
                imageVector = when (parsed.type) {
                    BookRatingType.Score -> Icons.Rounded.Star
                    BookRatingType.Rank -> Icons.Rounded.EmojiEvents
                },
                contentDescription = null,
                modifier = Modifier.size(11.dp),
                tint = gold,
            )
            Text(
                text = when (parsed.type) {
                    BookRatingType.Score -> parsed.value
                    BookRatingType.Rank -> "#${parsed.value}"
                },
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = Color.White,
                maxLines = 1,
            )
        }
    }
}

/**
 * WebNovel Book Detail star-bar + score pill (`★★★★★ 4.8` or `🏆 Rank #3`).
 */
@Composable
fun BookRatingChip(
    rating: String,
    modifier: Modifier = Modifier,
) {
    val parsed = remember(rating) { parseBookRating(rating) } ?: return
    val colors = NovelaTheme.colors

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(NovelaRadius.pill),
        color = colors.starGoldSoft,
        border = BorderStroke(0.75.dp, colors.starGold.copy(alpha = 0.35f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            when (parsed.type) {
                BookRatingType.Score -> {
                    val scoreVal = parsed.score ?: 0f
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(1.dp),
                    ) {
                        for (starIndex in 1..5) {
                            val icon = when {
                                scoreVal >= starIndex - 0.2f -> Icons.Rounded.Star
                                scoreVal >= starIndex - 0.75f -> Icons.AutoMirrored.Rounded.StarHalf
                                else -> Icons.Rounded.StarOutline
                            }
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = colors.starGold,
                            )
                        }
                    }
                    Text(
                        text = parsed.value,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.textPrimary,
                        maxLines = 1,
                    )
                }
                BookRatingType.Rank -> {
                    Icon(
                        imageVector = Icons.Rounded.EmojiEvents,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = colors.starGold,
                    )
                    Text(
                        text = stringResource(id = R.string.book_rank_format, "#${parsed.value}"),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

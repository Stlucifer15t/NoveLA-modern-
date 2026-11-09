package my.noveldokusha.coreui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.CollectionsBookmark
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import my.noveldokusha.core.utils.normalizeBookUrl
import my.noveldokusha.coreui.theme.NovelaTheme

/**
 * WebNovel-style top-left cover ribbon indicator for books already saved in the user's bookshelf.
 *
 * @param inSameSource true = crimson bookmark ribbon (added from this source),
 *                     false = smoked frosted badge (added from a different source)
 * @param sourceCount number of sources this book is in the library from (>1 shows count)
 */
@Composable
fun InLibraryBadge(
    inSameSource: Boolean,
    sourceCount: Int,
    modifier: Modifier = Modifier,
) {
    val colors = NovelaTheme.colors
    val description = if (sourceCount > 1) {
        "In library from $sourceCount sources"
    } else {
        "In library"
    }

    if (inSameSource) {
        val ribbonColor = colors.favoriteCrimson
        Box(
            modifier = modifier
                .padding(start = 6.dp)
                .width(20.dp)
                .height(30.dp)
                .drawBehind {
                    val notch = size.height * 0.22f
                    val path = Path().apply {
                        moveTo(0f, 0f)
                        lineTo(size.width, 0f)
                        lineTo(size.width, size.height)
                        lineTo(size.width / 2f, size.height - notch)
                        lineTo(0f, size.height)
                        close()
                    }
                    drawPath(path, color = ribbonColor)
                }
                .semantics(mergeDescendants = true) { contentDescription = description },
            contentAlignment = Alignment.TopCenter,
        ) {
            Box(
                modifier = Modifier.padding(top = 5.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (sourceCount > 1) {
                    Text(
                        text = if (sourceCount > 99) "99+" else sourceCount.toString(),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                        ),
                        maxLines = 1,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Bookmark,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(13.dp),
                    )
                }
            }
        }
    } else {
        Surface(
            modifier = modifier
                .padding(6.dp)
                .semantics(mergeDescendants = true) { contentDescription = description },
            shape = RoundedCornerShape(6.dp),
            color = colors.coverBadgeBg,
            border = BorderStroke(0.5.dp, colors.coverBadgeBorder),
            shadowElevation = 2.dp,
        ) {
            Box(
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (sourceCount > 1) {
                    Text(
                        text = if (sourceCount > 99) "99+" else sourceCount.toString(),
                        color = colors.accent,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                        ),
                        maxLines = 1,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.CollectionsBookmark,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(12.dp),
                    )
                }
            }
        }
    }
}

/**
 * Badge data maps: normalized URL -> count, title -> count.
 * Used by ViewModels to build reactive badge lookups.
 */
data class LibraryBadgeMaps(
    val urls: Map<String, Int> = emptyMap(),
    val titles: Map<String, Int> = emptyMap(),
)

/**
 * Determines the in-library badge state for a book.
 */
data class LibraryBadgeState(
    val inSameSource: Boolean,
    val sourceCount: Int,
)

/**
 * Checks if a book is in the library and returns badge state.
 * Priority: URL match (same source) > title match (different source).
 */
fun getLibraryBadgeState(
    bookUrl: String,
    bookTitle: String,
    libraryUrls: Map<String, Int>,
    libraryTitles: Map<String, Int>,
): LibraryBadgeState? {
    val urlMatch = libraryUrls[normalizeBookUrl(bookUrl)]
    val titleCount = libraryTitles[bookTitle] ?: 0

    if (urlMatch != null) {
        return LibraryBadgeState(
            inSameSource = true,
            sourceCount = maxOf(1, titleCount),
        )
    }

    if (titleCount > 0) {
        return LibraryBadgeState(inSameSource = false, sourceCount = titleCount)
    }

    return null
}

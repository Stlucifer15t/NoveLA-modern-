package my.noveldokusha.coreui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.CollectionsBookmark
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import my.noveldokusha.coreui.R
import my.noveldokusha.coreui.theme.NovelaTheme
import my.noveldokusha.feature.local_database.tables.Book
import java.util.Locale

@Immutable
data class LibraryBadgeMaps(
    val inLibraryUrls: Set<String> = emptySet(),
    val inLibraryTitles: Set<String> = emptySet(),
) {
    companion object {
        val Empty = LibraryBadgeMaps()

        fun from(books: List<Book>): LibraryBadgeMaps {
            val urls = HashSet<String>(books.size * 2)
            val titles = HashSet<String>(books.size * 2)
            for (book in books) {
                if (book.inLibrary) {
                    urls.add(book.url)
                    titles.add(normalizeTitle(book.title))
                }
            }
            return LibraryBadgeMaps(urls, titles)
        }

        internal fun normalizeTitle(title: String): String =
            title.trim().lowercase(Locale.ROOT)
    }
}

enum class LibraryBadgeState {
    None,
    SameSource,
    OtherSource,
}

fun LibraryBadgeMaps.getLibraryBadgeState(bookUrl: String, bookTitle: String): LibraryBadgeState {
    if (bookUrl in inLibraryUrls) return LibraryBadgeState.SameSource
    if (LibraryBadgeMaps.normalizeTitle(bookTitle) in inLibraryTitles) return LibraryBadgeState.OtherSource
    return LibraryBadgeState.None
}

/**
 * WebNovel-style top-left cover ribbon indicator for books already saved in the user's bookshelf.
 */
@Composable
fun InLibraryBadge(
    state: LibraryBadgeState,
    modifier: Modifier = Modifier,
) {
    if (state == LibraryBadgeState.None) return
    val colors = NovelaTheme.colors

    val containerColor = when (state) {
        LibraryBadgeState.SameSource -> colors.favoriteCrimson
        LibraryBadgeState.OtherSource -> colors.coverBadgeBg
        LibraryBadgeState.None -> return
    }
    val iconTint = when (state) {
        LibraryBadgeState.SameSource -> Color.White
        LibraryBadgeState.OtherSource -> colors.accent
        LibraryBadgeState.None -> return
    }
    val icon = when (state) {
        LibraryBadgeState.SameSource -> Icons.Rounded.Bookmark
        LibraryBadgeState.OtherSource -> Icons.Rounded.CollectionsBookmark
        LibraryBadgeState.None -> return
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(
            topStart = 2.dp,
            topEnd = 6.dp,
            bottomStart = 6.dp,
            bottomEnd = 6.dp,
        ),
        color = containerColor,
        border = if (state == LibraryBadgeState.OtherSource) {
            BorderStroke(0.5.dp, colors.coverBadgeBorder)
        } else {
            null
        },
        shadowElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = stringResource(id = R.string.in_library),
                modifier = Modifier.size(12.dp),
                tint = iconTint,
            )
        }
    }
}

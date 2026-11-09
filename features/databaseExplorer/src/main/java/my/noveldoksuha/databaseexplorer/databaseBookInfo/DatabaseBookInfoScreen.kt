package my.noveldokusha.databaseexplorer.databaseBookInfo

import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Public
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import my.noveldokusha.coreui.theme.InternalTheme
import my.noveldokusha.coreui.theme.NovelaSize
import my.noveldokusha.coreui.theme.NovelaTheme
import my.noveldokusha.coreui.theme.isAtTop
import my.noveldokusha.databaseexplorer.R
import my.noveldokusha.feature.local_database.BookMetadata
import my.noveldokusha.scraper.DatabaseInterface
import my.noveldokusha.scraper.DatabaseInterface.BookData
import my.noveldokusha.scraper.SearchGenre
import my.noveldokusha.scraper.domain.BookResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DatabaseBookInfoScreen(
    state: DatabaseBookInfoState,
    onSourcesClick: () -> Unit,
    onGenresClick: (List<SearchGenre>) -> Unit,
    onBookClick: (BookMetadata) -> Unit,
    onOpenInWeb: () -> Unit,
    onPressBack: () -> Unit,
) {
    val colors = NovelaTheme.colors
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val scrollState = rememberScrollState()
    val isAtTop by scrollState.isAtTop(threshold = 56.dp)

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            val alpha by animateFloatAsState(
                targetValue = if (isAtTop) 0f else 0.96f,
                label = "dbBookBarAlpha",
            )
            val backgroundColor by animateColorAsState(
                targetValue = MaterialTheme.colorScheme.background.copy(alpha = alpha),
                label = "dbBookBarBg",
            )
            val titleColor by animateColorAsState(
                targetValue = colors.textPrimary.copy(alpha = alpha),
                label = "dbBookBarTitle",
            )

            Surface(color = backgroundColor) {
                Column {
                    TopAppBar(
                        scrollBehavior = scrollBehavior,
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            scrolledContainerColor = Color.Transparent,
                        ),
                        title = {
                            Text(
                                text = state.book.value.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = titleColor,
                            )
                        },
                        navigationIcon = {
                            if (isAtTop) {
                                Surface(
                                    onClick = onPressBack,
                                    shape = CircleShape,
                                    color = colors.coverBadgeBg,
                                    border = BorderStroke(0.5.dp, colors.coverBadgeBorder),
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .size(38.dp),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }
                                }
                            } else {
                                IconButton(onClick = onPressBack) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                        contentDescription = null,
                                        tint = colors.textPrimary,
                                    )
                                }
                            }
                        },
                        actions = {
                            if (isAtTop) {
                                Surface(
                                    onClick = onOpenInWeb,
                                    shape = CircleShape,
                                    color = colors.coverBadgeBg,
                                    border = BorderStroke(0.5.dp, colors.coverBadgeBorder),
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .size(38.dp),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Outlined.Public,
                                            contentDescription = stringResource(R.string.open_in_browser),
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }
                                }
                                Spacer(Modifier.width(4.dp))
                            } else {
                                IconButton(onClick = onOpenInWeb) {
                                    Icon(
                                        imageVector = Icons.Outlined.Public,
                                        contentDescription = stringResource(R.string.open_in_browser),
                                        tint = colors.textPrimary,
                                    )
                                }
                            }
                        },
                    )
                    HorizontalDivider(
                        thickness = NovelaSize.hairline,
                        color = colors.borderSubtle,
                        modifier = Modifier.alpha(alpha),
                    )
                }
            }
        },
        content = { innerPadding ->
            DatabaseBookInfoScreenBody(
                state = state,
                scrollState = scrollState,
                onSourcesClick = onSourcesClick,
                onGenresClick = onGenresClick,
                onBookClick = onBookClick,
                innerPadding = innerPadding,
            )
        },
    )
}

@Preview(heightDp = 1500)
@Preview(heightDp = 1500, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewView() {
    val state = remember {
        DatabaseBookInfoState(
            databaseNameStrId = mutableIntStateOf(R.string.database_name_baka_updates),
            book = BookData(
                title = "Novel title",
                description = "Novel description goes here and here too and a little more to fill lines",
                coverImageUrl = "",
                alternativeTitles = listOf("Title 1", "Title 2", "Title 3"),
                authors = (1..3).map {
                    DatabaseInterface.AuthorMetadata("Author $it", "page url")
                } + DatabaseInterface.AuthorMetadata("Author", null),
                tags = (1..20).map { "tag $it" },
                genres = (1..8).map {
                    SearchGenre(
                        genreName = "genre $it",
                        id = "$it",
                    )
                },
                bookType = "Web novel",
                relatedBooks = (1..3).map {
                    BookResult(
                        "novel name $it",
                        "ulr",
                        "coverUrl",
                        "novel description $it",
                    )
                },
                similarRecommended = (1..6).map {
                    BookResult(
                        "novel name $it",
                        "ulr",
                        "coverUrl",
                        "novel description $it",
                    )
                },
            ).let(::mutableStateOf),
        )
    }
    InternalTheme {
        DatabaseBookInfoScreenBody(
            state = state,
            scrollState = rememberScrollState(),
            onSourcesClick = {},
            onGenresClick = {},
            onBookClick = {},
            innerPadding = PaddingValues(),
        )
        Spacer(modifier = Modifier.height(200.dp))
    }
}

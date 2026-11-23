package my.dreamora.databaseexplorer.databaseBookInfo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.activity.OnBackPressedCallback
import androidx.core.view.WindowCompat
import dagger.hilt.android.AndroidEntryPoint
import my.dreamora.coreui.BaseActivity
import my.dreamora.coreui.composableActions.SetSystemBarTransparent
import my.dreamora.coreui.theme.Theme
import my.dreamora.databaseexplorer.databaseSearch.DatabaseSearchActivity
import my.dreamora.databaseexplorer.databaseSearch.DatabaseSearchExtras
import my.dreamora.core.utils.Extra_String
import my.dreamora.navigation.NavigationRoutes
import my.dreamora.scraper.SearchGenre
import my.dreamora.feature.local_database.BookMetadata
import javax.inject.Inject

@AndroidEntryPoint
class DatabaseBookInfoActivity : BaseActivity() {
    class IntentData : Intent, DatabaseBookInfoStateBundle {
        override var databaseUrlBase by Extra_String()
        override var bookUrl by Extra_String()
        override var bookTitle by Extra_String()

        constructor(intent: Intent) : super(intent)
        constructor(ctx: Context, databaseUrlBase: String, bookMetadata: BookMetadata) : super(
            ctx,
            DatabaseBookInfoActivity::class.java
        ) {
            this.databaseUrlBase = databaseUrlBase
            this.bookUrl = bookMetadata.url
            this.bookTitle = bookMetadata.title
        }
    }

    @Inject
    internal lateinit var navigationRoutes: NavigationRoutes

    private val viewModel by viewModels<DatabaseBookInfoViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val backPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Обработка нажатия кнопки "назад"
                finish()
            }
        }
        addOnBackPressedCallback(backPressedCallback)

        setContent {
            Theme(themeProvider = themeProvider) {
                SetSystemBarTransparent()
                DatabaseBookInfoScreen(
                    state = viewModel.state,
                    onSourcesClick = ::openGlobalSearchPage,
                    onGenresClick = ::openSearchPageByGenres,
                    onBookClick = ::openBookInfo,
                    onOpenInWeb = { navigationRoutes.webView(this, viewModel.bookUrl, bookUrl = viewModel.bookUrl).let(::startActivity) },
                    onPressBack = { backPressedCallback.handleOnBackPressed() }
                )
            }
        }
    }

    private fun openGlobalSearchPage() = navigationRoutes.globalSearch(
        context = this,
        text = viewModel.state.book.value.title
    ).let(::startActivity)

    private fun openSearchPageByGenres(
        genres: List<SearchGenre>
    ) = DatabaseSearchActivity.IntentData(
        ctx = this,
        extras = DatabaseSearchExtras.Genres(
            databaseBaseUrl = viewModel.database.baseUrl,
            includedGenresIds = genres.map { it.id },
            excludedGenresIds = emptyList()
        )
    ).let(::startActivity)

    private fun openBookInfo(book: BookMetadata) = IntentData(
        ctx = this,
        bookMetadata = book,
        databaseUrlBase = viewModel.database.baseUrl
    ).let(::startActivity)
}
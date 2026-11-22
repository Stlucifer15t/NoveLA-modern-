package my.noveldokusha.settings.foryou

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import my.noveldokusha.core.appPreferences.AppPreferences
import my.noveldokusha.feature.local_database.DAOs.LibraryDao
import javax.inject.Inject

/** Why the rail is empty, when it is. */
enum class ForYouNotice { NoSources, NoAnswers }

data class ForYouState(
    val books: List<ForYouBook> = emptyList(),
    val loading: Boolean = false,
    val genres: List<NovelaGenre> = emptyList(),
    val notice: ForYouNotice? = null,
)

/**
 * The "For you" rail's state: which genres the reader picked, and what the installed sources
 * said about them. Fetching is deliberately coarse — one pass, page 0 of each source — and
 * reruns only when the genre set changes, so returning to Home never re-hits the network.
 */
@HiltViewModel
class ForYouViewModel @Inject constructor(
    private val appPreferences: AppPreferences,
    private val repository: ForYouRepository,
    private val libraryDao: LibraryDao,
) : ViewModel() {

    private val _state = mutableStateOf(ForYouState())
    val state: ForYouState get() = _state.value

    private var fetchJob: Job? = null
    private var libraryUrls: Set<String> = emptySet()

    init {
        _state.value = _state.value.copy(
            genres = NovelaGenre.fromSlugs(appPreferences.FOR_YOU_GENRES.value),
        )
        viewModelScope.launch {
            libraryDao.booksInLibraryFlow().collect { books ->
                libraryUrls = books.map { it.url }.toSet()
                // Re-tag the rail from the shelf instead of refetching it from the network.
                _state.value = _state.value.copy(
                    books = _state.value.books.map { it.copy(inLibrary = it.key in libraryUrls) },
                )
            }
        }
        viewModelScope.launch {
            appPreferences.FOR_YOU_GENRES.flow().collect { slugs ->
                _state.value = _state.value.copy(genres = NovelaGenre.fromSlugs(slugs))
                refresh()
            }
        }
        refresh()
    }

    /** One pass. Reruns when the genre set changes, never on mere recomposition. */
    fun refresh() {
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, notice = null)
            val batch = try {
                repository.fetch(genres = state.genres, libraryUrls = libraryUrls)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
            if (batch == null) {
                _state.value = _state.value.copy(
                    loading = false,
                    notice = if (state.books.isEmpty()) ForYouNotice.NoAnswers else null,
                )
                return@launch
            }
            _state.value = _state.value.copy(
                books = batch.books.map { it.copy(inLibrary = it.key in libraryUrls) },
                loading = false,
                notice = when {
                    batch.noSources -> ForYouNotice.NoSources
                    batch.books.isEmpty() -> ForYouNotice.NoAnswers
                    else -> null
                },
            )
        }
    }
}

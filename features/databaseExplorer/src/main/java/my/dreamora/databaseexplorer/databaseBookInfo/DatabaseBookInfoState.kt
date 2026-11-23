package my.dreamora.databaseexplorer.databaseBookInfo

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import my.dreamora.scraper.DatabaseInterface

internal data class DatabaseBookInfoState(
    val databaseNameStrId: State<Int>,
    val book: MutableState<DatabaseInterface.BookData>,
)
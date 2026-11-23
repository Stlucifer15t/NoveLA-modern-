package my.dreamora.interactor

import kotlinx.coroutines.flow.Flow
import my.dreamora.core.domain.LibraryCategory

interface WorkersInteractions {
    fun checkForLibraryUpdates(libraryCategory: LibraryCategory)
    fun cancelLibraryUpdates()
    fun isManualUpdateRunning(): Flow<Boolean>
}

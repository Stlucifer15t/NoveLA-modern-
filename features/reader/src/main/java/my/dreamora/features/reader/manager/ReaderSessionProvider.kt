package my.dreamora.features.reader.manager

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import my.dreamora.data.AppRepository
import my.dreamora.core.appPreferences.AppPreferences
import my.dreamora.core.appPreferences.TranslationSettingsResolver
import my.dreamora.features.reader.ReaderRepository
import my.dreamora.features.reader.ui.ReaderViewHandlersActions
import my.dreamora.feature.local_database.DAOs.ChapterTranslationDao
import my.dreamora.text_translator.domain.TranslationManager
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class ReaderSessionProvider @Inject constructor(
    private val appRepository: AppRepository,
    private val appPreferences: AppPreferences,
    @ApplicationContext private val context: Context,
    private val translationManager: TranslationManager,
    private val translationSettingsResolver: TranslationSettingsResolver,
    private val readerRepository: ReaderRepository,
    private val readerViewHandlersActions: ReaderViewHandlersActions,
    private val chapterTranslationDao: ChapterTranslationDao,
) {
    fun create(
        bookUrl: String,
        initialChapterUrl: String,
    ): ReaderSession {
        Timber.d("ReaderStart: ReaderSessionProvider.create bookUrl=$bookUrl initialChapterUrl=$initialChapterUrl")
        return ReaderSession(
            bookUrl = bookUrl,
            initialChapterUrl = initialChapterUrl,
            appRepository = appRepository,
            translationManager = translationManager,
            translationSettingsResolver = translationSettingsResolver,
            appPreferences = appPreferences,
            context = context,
            readerRepository = readerRepository,
            readerViewHandlersActions = readerViewHandlersActions,
            chapterTranslationDao = chapterTranslationDao,
        )
    }
}

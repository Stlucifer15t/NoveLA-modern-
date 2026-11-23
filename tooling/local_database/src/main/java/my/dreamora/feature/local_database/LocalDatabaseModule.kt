package my.dreamora.feature.local_database

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import my.dreamora.feature.local_database.DAOs.BookTranslationDao
import my.dreamora.feature.local_database.DAOs.ChapterBodyDao
import my.dreamora.feature.local_database.DAOs.ChapterDao
import my.dreamora.feature.local_database.DAOs.ChapterPagesDao
import my.dreamora.feature.local_database.DAOs.ChapterTranslationDao
import my.dreamora.feature.local_database.DAOs.DownloadedPageChaptersDao
import my.dreamora.feature.local_database.DAOs.DownloadTaskDao
import my.dreamora.feature.local_database.DAOs.LibraryDao
import my.dreamora.feature.local_database.DAOs.AuthorPreferenceDao
import my.dreamora.feature.local_database.DAOs.NetworkBudgetDao
import my.dreamora.feature.local_database.DAOs.NovelMigrationDao
import my.dreamora.feature.local_database.DAOs.RecommendationDao
import my.dreamora.feature.local_database.DAOs.UserFilterDao
import my.dreamora.feature.local_database.DAOs.ReadingHistoryDao
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
abstract class LocalDatabaseModule {

    companion object {

        @Provides
        @Singleton
        internal fun provideAppRoomDatabase(@ApplicationContext context: Context): AppDatabase {
            return AppDatabase.createRoom(
                context,
                name = "bookEntry"
            )
        }

        @Provides
        @Singleton
        fun provideLibraryDao(database: AppDatabase): LibraryDao = database.libraryDao()

        @Provides
        @Singleton
        fun provideChapterDao(database: AppDatabase): ChapterDao = database.chapterDao()

        @Provides
        @Singleton
        fun provideChapterBodyDao(database: AppDatabase): ChapterBodyDao = database.chapterBodyDao()

        @Provides
        @Singleton
        fun provideChapterPagesDao(database: AppDatabase): ChapterPagesDao = database.chapterPagesDao()

        @Provides
        @Singleton
        fun provideDownloadedPageChaptersDao(database: AppDatabase): DownloadedPageChaptersDao =
            database.downloadedPageChaptersDao()

        @Provides
        @Singleton
        fun provideChapterTranslationDao(database: AppDatabase): ChapterTranslationDao = database.chapterTranslationDao()

        @Provides
        @Singleton
        fun provideBookTranslationDao(database: AppDatabase): BookTranslationDao = database.bookTranslationDao()

        @Provides
        @Singleton
        fun provideDownloadTaskDao(database: AppDatabase): DownloadTaskDao = database.downloadTaskDao()

        @Provides
        @Singleton
        fun provideNovelMigrationDao(database: AppDatabase): NovelMigrationDao = database.novelMigrationDao()

        @Provides
        @Singleton
        fun provideReadingHistoryDao(database: AppDatabase): ReadingHistoryDao = database.readingHistoryDao()

        @Provides
        @Singleton
        fun provideAuthorPreferenceDao(database: AppDatabase): AuthorPreferenceDao = database.authorPreferenceDao()

        @Provides
        @Singleton
        fun provideRecommendationDao(database: AppDatabase): RecommendationDao = database.recommendationDao()

        @Provides
        @Singleton
        fun provideUserFilterDao(database: AppDatabase): UserFilterDao = database.userFilterDao()

        @Provides
        @Singleton
        fun provideNetworkBudgetDao(database: AppDatabase): NetworkBudgetDao = database.networkBudgetDao()

    }
}

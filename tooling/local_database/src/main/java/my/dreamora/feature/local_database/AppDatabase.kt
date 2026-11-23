package my.dreamora.feature.local_database

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import my.dreamora.feature.local_database.DAOs.BookTranslationDao
import my.dreamora.feature.local_database.DAOs.ChapterBodyDao
import my.dreamora.feature.local_database.DAOs.ChapterDao
import my.dreamora.feature.local_database.DAOs.ChapterPagesDao
import my.dreamora.feature.local_database.DAOs.ChapterTranslationDao
import my.dreamora.feature.local_database.DAOs.DownloadTaskDao
import my.dreamora.feature.local_database.DAOs.DownloadedPageChaptersDao
import my.dreamora.feature.local_database.DAOs.AuthorPreferenceDao
import my.dreamora.feature.local_database.DAOs.ExtensionDao
import my.dreamora.feature.local_database.DAOs.LibraryDao
import my.dreamora.feature.local_database.DAOs.NetworkBudgetDao
import my.dreamora.feature.local_database.DAOs.NovelMigrationDao
import my.dreamora.feature.local_database.DAOs.RecommendationDao
import my.dreamora.feature.local_database.DAOs.UserFilterDao
import my.dreamora.feature.local_database.DAOs.ReadingHistoryDao
import my.dreamora.feature.local_database.tables.AuthorPreferenceEntity
import my.dreamora.feature.local_database.tables.Book
import my.dreamora.feature.local_database.tables.BookTranslation
import my.dreamora.feature.local_database.tables.Chapter
import my.dreamora.feature.local_database.tables.ChapterBody
import my.dreamora.feature.local_database.tables.ChapterPages
import my.dreamora.feature.local_database.tables.ChapterTranslation
import my.dreamora.feature.local_database.tables.BlockedAuthorEntity
import my.dreamora.feature.local_database.tables.DiscoveredNovelEntity
import my.dreamora.feature.local_database.tables.DiscoveryChainEntity
import my.dreamora.feature.local_database.tables.DownloadTaskEntity
import my.dreamora.feature.local_database.tables.DownloadedPageChapter
import my.dreamora.feature.local_database.tables.Extension
import my.dreamora.feature.local_database.tables.HiddenNovelEntity
import my.dreamora.feature.local_database.tables.MigrationRecord
import my.dreamora.feature.local_database.tables.NetworkBudgetEntity
import my.dreamora.feature.local_database.tables.ReadingHistory
import my.dreamora.feature.local_database.tables.UserPreferenceEntity
import my.dreamora.feature.local_database.tables.UserTagFilterEntity
import java.io.InputStream


interface AppDatabase {
    fun libraryDao(): LibraryDao
    fun chapterDao(): ChapterDao
    fun chapterBodyDao(): ChapterBodyDao
    fun chapterPagesDao(): ChapterPagesDao
    fun downloadedPageChaptersDao(): DownloadedPageChaptersDao
    fun chapterTranslationDao(): ChapterTranslationDao
    fun bookTranslationDao(): BookTranslationDao
    fun downloadTaskDao(): DownloadTaskDao
    fun extensionDao(): ExtensionDao
    fun novelMigrationDao(): NovelMigrationDao
    fun readingHistoryDao(): ReadingHistoryDao
    fun authorPreferenceDao(): AuthorPreferenceDao
    fun recommendationDao(): RecommendationDao
    fun userFilterDao(): UserFilterDao
    fun networkBudgetDao(): NetworkBudgetDao
    val name: String

    fun closeDatabase()
    fun clearDatabase()
    suspend fun vacuum()
    suspend fun vacuumInto(targetPath: String)
    suspend fun integrityCheck(): String

    /**
     * Execute the whole database calls as an atomic operation
     */
    suspend fun <T> transaction(block: suspend () -> T): T

    companion object {
        fun createRoom(ctx: Context, name: String): AppDatabase = Room
            .databaseBuilder(ctx, AppRoomDatabase::class.java, name)
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    // Stability: Enable foreign keys for referential integrity
                    db.execSQL("PRAGMA foreign_keys = ON")
                    // Speed: Use NORMAL synchronous mode, optimal and safe under WAL mode
                    db.execSQL("PRAGMA synchronous = NORMAL")
                    // Speed: Store temporary tables in memory instead of disk
                    db.execSQL("PRAGMA temp_store = MEMORY")
                    // Safe Storage: Limit journal size to 8MB. Must use db.query().close() because
                    // SQLite PRAGMAs that return results (like journal_size_limit) throw SQLiteException when called via execSQL().
                    db.query("PRAGMA journal_size_limit = 8388608").close()
                    // Safe Memory: Bounded SQLite memory cache size. Must use db.query().close() because
                    // SQLite PRAGMAs that return results (like cache_size) throw SQLiteException when called via execSQL().
                    db.query("PRAGMA cache_size = -4000").close()
                }
            })
            .addMigrations(*databaseMigrations())
            .build()
            .also { it.name = name }

        suspend fun checkFileIntegrity(ctx: Context, filePath: String): String = withContext(Dispatchers.IO) {
            val db = Room.databaseBuilder(ctx, AppRoomDatabase::class.java, filePath)
                .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
                .build()
            db.openHelper.writableDatabase.query("PRAGMA integrity_check").use { cursor ->
                cursor.moveToFirst()
                cursor.getString(0)
            }.also { db.close() }
        }

        fun createRoomFromStream(
            ctx: Context,
            name: String,
            inputStream: InputStream
        ): AppDatabase = Room
            .databaseBuilder(ctx, AppRoomDatabase::class.java, name)
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .createFromInputStream { inputStream }
            .fallbackToDestructiveMigration(false) // Don't apply migrations, database is already at correct version
            .addCallback(object : RoomDatabase.Callback() {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    db.execSQL("PRAGMA foreign_keys = ON")
                    db.execSQL("PRAGMA synchronous = NORMAL")
                    db.execSQL("PRAGMA temp_store = MEMORY")
                    db.query("PRAGMA journal_size_limit = 8388608").close()
                    db.query("PRAGMA cache_size = -4000").close()
                }
            })
            .build()
            .also { it.name = name }
    }
}


@Database(
    entities = [
        Book::class,
        BookTranslation::class,
        Chapter::class,
        ChapterBody::class,
        ChapterPages::class,
        ChapterTranslation::class,
        DownloadTaskEntity::class,
        DownloadedPageChapter::class,
        Extension::class,
        MigrationRecord::class,
        ReadingHistory::class,
        AuthorPreferenceEntity::class,
        DiscoveredNovelEntity::class,
        UserPreferenceEntity::class,
        UserTagFilterEntity::class,
        HiddenNovelEntity::class,
        BlockedAuthorEntity::class,
        NetworkBudgetEntity::class,
        DiscoveryChainEntity::class
    ],
    version = 36,
    exportSchema = true,
    // v36 adds only new For You tables; Room generates the SQL and checks it against 36.json.
    autoMigrations = [AutoMigration(from = 35, to = 36)]
)
internal abstract class AppRoomDatabase : RoomDatabase(), AppDatabase {
    abstract override fun libraryDao(): LibraryDao
    abstract override fun chapterDao(): ChapterDao
    abstract override fun chapterBodyDao(): ChapterBodyDao
    abstract override fun chapterPagesDao(): ChapterPagesDao
    abstract override fun downloadedPageChaptersDao(): DownloadedPageChaptersDao
    abstract override fun chapterTranslationDao(): ChapterTranslationDao
    abstract override fun bookTranslationDao(): BookTranslationDao
    abstract override fun downloadTaskDao(): DownloadTaskDao
    abstract override fun extensionDao(): ExtensionDao
    abstract override fun novelMigrationDao(): NovelMigrationDao
    abstract override fun readingHistoryDao(): ReadingHistoryDao
    abstract override fun authorPreferenceDao(): AuthorPreferenceDao
    abstract override fun recommendationDao(): RecommendationDao
    abstract override fun userFilterDao(): UserFilterDao
    abstract override fun networkBudgetDao(): NetworkBudgetDao
    override lateinit var name: String

    override suspend fun <T> transaction(block: suspend () -> T): T = withTransaction(block)

    override fun closeDatabase() {
        close()
    }

    override fun clearDatabase() {
        clearAllTables()
    }

    override suspend fun vacuum() {
        withContext(Dispatchers.IO) {
            openHelper.writableDatabase.execSQL("VACUUM")
        }
    }

    override suspend fun vacuumInto(targetPath: String) {
        withContext(Dispatchers.IO) {
            openHelper.writableDatabase.execSQL("VACUUM INTO '$targetPath'")
        }
    }

    override suspend fun integrityCheck(): String {
        return withContext(Dispatchers.IO) {
            openHelper.writableDatabase.query("PRAGMA integrity_check").use { cursor ->
                cursor.moveToFirst()
                cursor.getString(0)
            }
        }
    }
}
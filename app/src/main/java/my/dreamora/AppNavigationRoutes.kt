package my.dreamora

import android.content.Context
import android.content.Intent
import my.dreamora.databaseexplorer.databaseSearch.DatabaseSearchActivity
import my.dreamora.databaseexplorer.databaseSearch.DatabaseSearchExtras

import my.dreamora.features.chapterslist.ChaptersActivity
import my.dreamora.features.main.MainActivity
import my.dreamora.features.reader.ReaderActivity
import my.dreamora.globalsourcesearch.GlobalSourceSearchActivity
import my.dreamora.navigation.NavigationRoutes
import my.dreamora.sourceexplorer.SourceCatalogActivity
import my.dreamora.feature.local_database.BookMetadata
import my.dreamora.tooling.novel_migration.MigrationActivity
import my.dreamora.webview.WebViewActivity
import my.dreamora.settings.RegexRulesActivity
import my.dreamora.settings.SettingsGroupActivity
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class AppNavigationRoutes @Inject constructor() : NavigationRoutes {

    override fun main(context: Context): Intent {
        return Intent(context, MainActivity::class.java)
    }

    override fun reader(
        context: Context,
        bookUrl: String,
        chapterUrl: String,
        scrollToSpeakingItem: Boolean
    ): Intent {
        return ReaderActivity.IntentData(
            context,
            bookUrl = bookUrl,
            chapterUrl = chapterUrl,
            scrollToSpeakingItem = scrollToSpeakingItem
        )
    }

    override fun chapters(context: Context, bookMetadata: BookMetadata): Intent {
        return ChaptersActivity.IntentData(context, bookMetadata)
    }

    override fun databaseSearch(
        context: Context,
        input: String,
        databaseUrlBase: String
    ): Intent {
        return DatabaseSearchActivity.IntentData(
            context,
            DatabaseSearchExtras.Title(databaseBaseUrl = databaseUrlBase, title = input)
        )
    }

    override fun databaseSearch(
        context: Context,
        databaseBaseUrl: String
    ): Intent {
        return DatabaseSearchActivity.IntentData(
            context,
            DatabaseSearchExtras.Catalog(databaseBaseUrl = databaseBaseUrl)
        )
    }

    override fun sourceCatalog(
        context: Context,
        sourceBaseUrl: String,
    ): Intent {
        return SourceCatalogActivity
            .IntentData(context, sourceBaseUrl = sourceBaseUrl)
    }

    override fun globalSearch(
        context: Context,
        text: String,
        contentType: String,
    ): Intent {
        return GlobalSourceSearchActivity.IntentData(context, text, contentType)
    }

    override fun webView(context: Context, url: String, bookUrl: String?): Intent {
        return Intent(context, WebViewActivity::class.java).apply {
            putExtra("url", url)
            if (bookUrl != null) putExtra("bookUrl", bookUrl)
        }
    }

    override fun novelMigration(context: Context, bookUrl: String, bookTitle: String): Intent {
        return MigrationActivity.IntentData(context, bookUrl = bookUrl, bookTitle = bookTitle)
    }

    override fun massMigration(context: Context, sourceBaseUrl: String?): Intent {
        return Intent(context, MigrationActivity::class.java).apply {
            if (sourceBaseUrl != null) {
                putExtra("massMigrationSourceUrl", sourceBaseUrl)
            } else {
                putExtra("massMigrationUnknownSource", true)
            }
        }
    }

    override fun migrationHistory(context: Context): Intent {
        return Intent(context, MigrationActivity::class.java).apply {
            putExtra("showHistory", true)
        }
    }

    override fun regexRules(context: Context, bookUrl: String?): Intent {
        return RegexRulesActivity.IntentData(context, bookUrl = bookUrl)
    }

    override fun settingsGroup(context: Context, groupKey: String): Intent =
        SettingsGroupActivity.IntentData(context, groupKey = groupKey)
}

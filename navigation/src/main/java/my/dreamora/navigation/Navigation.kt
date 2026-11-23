package my.dreamora.navigation

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import my.dreamora.feature.local_database.BookMetadata
import javax.inject.Inject

interface NavigationRoutes {

    fun main(context: Context): Intent

    fun chapters(context: Context, bookMetadata: BookMetadata): Intent

    fun webView(context: Context, url: String, bookUrl: String? = null): Intent

    fun reader(
        context: Context,
        bookUrl: String,
        chapterUrl: String,
        scrollToSpeakingItem: Boolean = false
    ): Intent

    fun databaseSearch(
        context: Context,
        input: String,
        databaseUrlBase: String = "https://www.novelupdates.com/"
    ): Intent

    fun databaseSearch(
        context: Context,
        databaseBaseUrl: String
    ): Intent

    // contentType: "" = every type, otherwise "novel"/"manga"/"video"
    fun globalSearch(context: Context, text: String, contentType: String = ""): Intent
    fun sourceCatalog(context: Context, sourceBaseUrl: String): Intent

    fun novelMigration(context: Context, bookUrl: String, bookTitle: String): Intent
    // sourceBaseUrl == null — "unknown source" mode (plugin removed or uninstalled)
    fun massMigration(context: Context, sourceBaseUrl: String?): Intent
    fun migrationHistory(context: Context): Intent

    fun regexRules(context: Context, bookUrl: String? = null): Intent

    /**
     * A single settings group as its own screen. `groupKey` is the name of
     * `my.dreamora.settings.ui.SettingsGroup`; the interface stays Intent-typed so
     * the navigation module does not depend on the settings feature.
     */
    fun settingsGroup(context: Context, groupKey: String): Intent
}

@HiltViewModel
class NavigationRouteViewModel @Inject constructor(
    private val appNavigationRoutes: NavigationRoutes
) : NavigationRoutes by appNavigationRoutes, ViewModel()

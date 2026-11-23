// Полный заменённый FossModule.kt

package my.dreamora.text_translator

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import my.dreamora.core.appPreferences.AppPreferences
import my.dreamora.network.ScraperNetworkClient
import my.dreamora.text_translator.domain.TranslationManager
import my.dreamora.text_translator.TranslationManagerComposite
import my.dreamora.text_translator.TranslationManagerGemini
import my.dreamora.text_translator.TranslationManagerGoogleFree
import my.dreamora.text_translator.TranslationManagerGooglePA
import my.dreamora.text_translator.TranslationManagerOpenAI
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object FossModule {

    @Provides
    @Singleton
    fun provideTranslationManager(
        appPreferences: AppPreferences,
        networkClient: ScraperNetworkClient
    ): TranslationManager {
        val geminiManager    = TranslationManagerGemini(networkClient, appPreferences)
        val googleFreeManager= TranslationManagerGoogleFree(appPreferences, networkClient)
        val googlePAManager  = TranslationManagerGooglePA(appPreferences, networkClient)
        val openAiManager    = TranslationManagerOpenAI(networkClient, appPreferences)

        return TranslationManagerComposite(
            geminiManager     = geminiManager,
            googleFreeManager = googleFreeManager,
            googlePAManager   = googlePAManager,
            openAiManager     = openAiManager,
            appPreferences    = appPreferences
        )
    }
}
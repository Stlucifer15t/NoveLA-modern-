package my.dreamora.di

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import my.dreamora.App
import my.dreamora.AppNavigationRoutes
import my.dreamora.BuildConfig
import my.dreamora.ExtensionRepository
import my.dreamora.core.AppInternalState
import my.dreamora.core.ExtensionManager
import my.dreamora.core.Toasty
import my.dreamora.core.ToastyToast
import my.dreamora.core.OfflineVideoCleaner
import my.dreamora.core.appPreferences.TranslationSettingsResolver
import my.dreamora.data.TranslationSettingsResolverImpl
import my.dreamora.feature.local_database.AppDatabase
import my.dreamora.features.reader.video.VideoDownloadManager
import my.dreamora.navigation.NavigationRoutes
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindAppNavigationRoutes(nav: AppNavigationRoutes): NavigationRoutes

    @Binds
    @Singleton
    abstract fun bindToasty(toast: ToastyToast): Toasty

    @Binds
    @Singleton
    abstract fun bindExtensionManager(extensionRepository: ExtensionRepository): ExtensionManager

    @Binds
    @Singleton
    abstract fun bindTranslationSettingsResolver(
        impl: TranslationSettingsResolverImpl
    ): TranslationSettingsResolver

    @Binds
    @Singleton
    abstract fun bindOfflineVideoCleaner(videoDownloadManager: VideoDownloadManager): OfflineVideoCleaner

    companion object {

        @Provides
        @Singleton
        fun providesApp(@ApplicationContext context: Context): App {
            return context as App
        }

        @Provides
        @Singleton
        fun providesAppInternalState(): AppInternalState = object : AppInternalState {
            override val isDebugMode = BuildConfig.DEBUG
            override val versionCode = BuildConfig.VERSION_CODE
            override val versionName = BuildConfig.VERSION_NAME
            override val gitCommitHash = BuildConfig.GIT_COMMIT_HASH
        }



    }
}

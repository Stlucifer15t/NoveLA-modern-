package my.noveldokusha.extension_compat.di

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import my.noveldokusha.extension_compat.core.CompatExtensionLoader
import my.noveldokusha.extension_compat.core.CompatLoaders
import my.noveldokusha.extension_compat.core.CompatSourceProviderImpl
import my.noveldokusha.extension_compat.core.CompatStorage
import my.noveldokusha.scraper.CompatSourceProvider
import java.io.File
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
abstract class CompatModule {

    @Binds
    @Singleton
    abstract fun bindCompatSourceProvider(impl: CompatSourceProviderImpl): CompatSourceProvider

    companion object {
        @Provides
        @Singleton
        fun provideCompatStorage(@ApplicationContext context: Context): CompatStorage =
            CompatStorage(File(context.filesDir, CompatStorage.DIR_NAME))

        @Provides
        @Singleton
        fun provideCompatExtensionLoaders(): Set<@JvmSuppressWildcards CompatExtensionLoader> = emptySet()
    }
}
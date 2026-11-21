package my.noveldokusha.extension_compat.di

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import my.noveldokusha.extension_compat.core.CompatExtensionLoader
import my.noveldokusha.extension_compat.core.CompatLoaders
import my.noveldokusha.extension_compat.core.CompatRepositoryManager
import my.noveldokusha.extension_compat.core.CompatSourceProviderImpl
import my.noveldokusha.extension_compat.core.CompatStorage
import my.noveldokusha.extension_compat.legado.LegadoExtensionLoader
import my.noveldokusha.extension_compat.lnreader.LNReaderExtensionLoader
import my.noveldokusha.scraper.CompatSourceProvider
import java.io.File
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
abstract class CompatModule {

    @Binds
    @Singleton
    abstract fun bindCompatSourceProvider(impl: CompatSourceProviderImpl): CompatSourceProvider

    @Binds
    @Singleton
    @CompatLoaders
    @IntoSet
    abstract fun bindLegadoLoader(impl: LegadoExtensionLoader): CompatExtensionLoader

    @Binds
    @Singleton
    @CompatLoaders
    @IntoSet
    abstract fun bindLNReaderLoader(impl: LNReaderExtensionLoader): CompatExtensionLoader

    companion object {

        @Provides
        @Singleton
        fun provideCompatStorage(@ApplicationContext context: Context): CompatStorage =
            CompatStorage(File(context.filesDir, CompatStorage.DIR_NAME))

        @Provides
        @Singleton
        fun provideCompatRepositoryManager(
            networkClient: my.noveldokusha.network.NetworkClient,
        ): CompatRepositoryManager = CompatRepositoryManager(networkClient)
    }
}
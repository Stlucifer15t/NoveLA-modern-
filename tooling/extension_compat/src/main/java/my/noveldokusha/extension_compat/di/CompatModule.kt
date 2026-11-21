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
import my.noveldokusha.extension_compat.legado.LegadoRepositoryHandler
import my.noveldokusha.extension_compat.lnreader.LNReaderExtensionLoader
import my.noveldokusha.extension_compat.lnreader.LNReaderRepositoryHandler
import my.noveldokusha.scraper.CompatSourceProvider
import java.io.File
import javax.inject.Singleton

/**
 * DI-модуль compat-слоя: провайдер источников + multibinding загрузчиков экосистем.
 * Подключается в app через dependency на :tooling:extension_compat.
 * 
 * Note: Tachiyomi, Mangayomi, CloudStream adapters are temporarily disabled
 * due to compilation errors. They can be re-enabled once fixed.
 */
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

    // TODO: Re-enable once compilation errors are fixed
    // @Binds @Singleton @CompatLoaders @IntoSet abstract fun bindTachiyomiLoader(impl: TachiyomiExtensionLoader): CompatExtensionLoader
    // @Binds @Singleton @CompatLoaders @IntoSet abstract fun bindMangayomiLoader(impl: MangayomiExtensionLoader): CompatExtensionLoader
    // @Binds @Singleton @CompatLoaders @IntoSet abstract fun bindCloudStreamLoader(impl: CloudStreamExtensionLoader): CompatExtensionLoader

    companion object {

        @Provides
        @Singleton
        fun provideCompatStorage(@ApplicationContext context: Context): CompatStorage =
            CompatStorage(File(context.filesDir, CompatStorage.DIR_NAME))

        @Provides
        @Singleton
        fun provideLegadoRepositoryHandler(
            networkClient: my.noveldokusha.network.NetworkClient,
            storage: CompatStorage,
        ): LegadoRepositoryHandler = LegadoRepositoryHandler(networkClient, storage)

        @Provides
        @Singleton
        fun provideLNReaderRepositoryHandler(
            networkClient: my.noveldokusha.network.NetworkClient,
            storage: CompatStorage,
        ): LNReaderRepositoryHandler = LNReaderRepositoryHandler(networkClient, storage)

        // TODO: Re-enable once compilation errors are fixed
        // @Provides @Singleton fun provideTachiyomiRepositoryHandler(...): TachiyomiRepositoryHandler = TachiyomiRepositoryHandler(networkClient, storage)
        // @Provides @Singleton fun provideMangayomiRepositoryHandler(...): MangayomiRepositoryHandler = MangayomiRepositoryHandler(networkClient, storage)
        // @Provides @Singleton fun provideCloudStreamRepositoryHandler(...): CloudStreamRepositoryHandler = CloudStreamRepositoryHandler(networkClient, storage)

        @Provides
        @Singleton
        fun provideCompatRepositoryManager(
            networkClient: my.noveldokusha.network.NetworkClient,
            legadoHandler: LegadoRepositoryHandler,
            lnReaderHandler: LNReaderRepositoryHandler,
        ): CompatRepositoryManager = CompatRepositoryManager(
            networkClient,
            legadoHandler,
            lnReaderHandler,
        )
    }
}
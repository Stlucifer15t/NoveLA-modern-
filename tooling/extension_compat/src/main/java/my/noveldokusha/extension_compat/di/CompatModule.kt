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
import my.noveldokusha.extension_compat.core.CompatSourceProviderImpl
import my.noveldokusha.extension_compat.core.CompatStorage
import my.noveldokusha.extension_compat.legado.LegadoExtensionLoader
import my.noveldokusha.extension_compat.tachiyomi.TachiyomiExtensionLoader
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

    /**
     * Mihon/Tachiyomi/Aniyomi (keiyoushi) ship APK extensions. The loader validates
     * the manifest and dex, but this ecosystem's source API is not part of the build
     * (it needs an eu.kanade.tachiyomi.source shim): the binding stays so an installed
     * package is reported with an explanation instead of "no loader".
     */
    @Binds
    @Singleton
    @CompatLoaders
    @IntoSet
    abstract fun bindTachiyomiLoader(impl: TachiyomiExtensionLoader): CompatExtensionLoader

    companion object {
        @Provides
        @Singleton
        fun provideCompatStorage(@ApplicationContext context: Context): CompatStorage =
            CompatStorage(File(context.filesDir, CompatStorage.DIR_NAME))
    }
}

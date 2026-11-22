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
import my.noveldokusha.extension_compat.legado.LegadoExtensionLoader
import my.noveldokusha.extension_compat.tachiyomi.TachiyomiExtensionLoader
import my.noveldokusha.scraper.CompatSourceProvider
import java.io.File
import javax.inject.Singleton

/**
 * The one binding that has to be a `@Binds`: [CompatSourceProviderImpl] is constructed
 * by Dagger itself. A Dagger module may not mix abstract (`@Binds`) and non-static
 * (`@Provides`) members, which is why the providers below live in [CompatProviderModule].
 */
@InstallIn(SingletonComponent::class)
@Module
abstract class CompatModule {

    @Binds
    @Singleton
    abstract fun bindCompatSourceProvider(impl: CompatSourceProviderImpl): CompatSourceProvider
}

/**
 * Hand-written providers for the compat layer.
 *
 * Loaders are handed over as a [CompatLoaders] registry rather than an injected `Set`:
 * `@Binds @IntoSet`, `@Multibinds` + `@Provides @IntoSet` and finally a plain
 * `@Provides Set<CompatExtensionLoader>` were each rejected by
 * `:app:hiltJavaCompileDebug` with `MissingBinding:
 * java.util.Set<? extends CompatExtensionLoader>` — Dagger always reads an injected
 * set as a wildcard and never matched any of them. A dedicated type has nothing to
 * mismatch. A new ecosystem is registered in [provideLoaders], one place to grep.
 */
@InstallIn(SingletonComponent::class)
@Module
object CompatProviderModule {

    @Provides
    @Singleton
    fun provideCompatStorage(@ApplicationContext context: Context): CompatStorage =
        CompatStorage(File(context.filesDir, CompatStorage.DIR_NAME))

    /**
     * Every ecosystem the app can load.
     *
     * Tachiyomi/Mihon/Aniyomi (keiyoushi) ship extensions as APKs with dex written
     * against host APIs (`eu.kanade.tachiyomi.source.model.*`, Injekt, RxJava). That
     * source API is not part of this build, so the loader only validates the manifest
     * and dex and then explains itself: registering it is what turns "installed but
     * unusable here" into a message instead of "no loader for this ecosystem".
     */
    @Provides
    @Singleton
    fun provideLoaders(
        legado: LegadoExtensionLoader,
        tachiyomi: TachiyomiExtensionLoader,
    ): CompatLoaders = CompatLoaders(listOf(legado, tachiyomi))
}

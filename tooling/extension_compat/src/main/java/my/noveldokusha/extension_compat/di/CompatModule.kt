package my.noveldokusha.extension_compat.di

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import my.noveldokusha.extension_compat.core.CompatExtensionLoader
import my.noveldokusha.extension_compat.core.CompatSourceProviderImpl
import my.noveldokusha.extension_compat.core.CompatStorage
import my.noveldokusha.extension_compat.legado.LegadoExtensionLoader
import my.noveldokusha.extension_compat.tachiyomi.TachiyomiExtensionLoader
import my.noveldokusha.scraper.CompatSourceProvider
import java.io.File
import javax.inject.Singleton

/**
 * Wires the compat layer into the application graph.
 *
 * The loader set is *provided* rather than multibound. Injecting `Set<T>` through
 * `@Multibinds`/`@Binds @IntoSet` kept failing in `:app:hiltJavaCompileDebug` with
 * `MissingBinding: java.util.Set<? extends CompatExtensionLoader>` across several CI
 * runs, while the same module's `@Provides` for [CompatStorage] was picked up
 * immediately — so the app graph is handed a concrete set and no wildcard has to match.
 * A new ecosystem is registered in [provideLoaders], which keeps the registry greppable.
 */
@InstallIn(SingletonComponent::class)
@Module
abstract class CompatModule {

    @Binds
    @Singleton
    abstract fun bindCompatSourceProvider(impl: CompatSourceProviderImpl): CompatSourceProvider

    @Module
    companion object {
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
        ): Set<CompatExtensionLoader> = setOf(legado, tachiyomi)
    }
}

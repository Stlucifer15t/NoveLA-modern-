package my.noveldokusha.extension_compat.core

import javax.inject.Qualifier

/**
 * Qualifier для multibinding'а загрузчиков экосистем compat-слоя.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CompatLoaders

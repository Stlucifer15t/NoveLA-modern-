package my.dreamora.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import my.dreamora.scraper.LuaSourceProvider
import my.dreamora.scraper.LuaSourceProviderImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LuaSourceModule {

    @Binds
    @Singleton
    abstract fun bindLuaSourceProvider(impl: LuaSourceProviderImpl): LuaSourceProvider
}
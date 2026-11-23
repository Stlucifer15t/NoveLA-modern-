package my.dreamora.di

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import my.dreamora.core.appPreferences.AppPreferences
import my.dreamora.tooling.application_workers.setup.AppWorkerFactory

@EntryPoint
@InstallIn(SingletonComponent::class)
interface HiltAppEntryPoint {
    fun workerFactory(): AppWorkerFactory
    fun appPreferences(): AppPreferences
}

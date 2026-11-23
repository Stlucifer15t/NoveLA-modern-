package my.dreamora.tooling.application_workers.setup

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import my.dreamora.core.appPreferences.AppPreferences
import my.dreamora.coreui.states.NotificationsCenter
import my.dreamora.data.AppRemoteRepository
import my.dreamora.interactor.LibraryUpdatesInteractions
import my.dreamora.scraper.LuaSourceProvider
import my.dreamora.tooling.application_workers.AutoBackupWorker
import my.dreamora.tooling.application_workers.BookExportWorker
import my.dreamora.tooling.application_workers.DatabaseMaintenanceWorker
import my.dreamora.tooling.application_workers.LibraryUpdatesWorker
import my.dreamora.tooling.application_workers.UpdatesCheckerWorker
import my.dreamora.tooling.application_workers.notifications.LibraryUpdateNotification
import timber.log.Timber
import javax.inject.Inject

class AppWorkerFactory @Inject internal constructor(
    private val appPreferences: AppPreferences,
    private val appRemoteRepository: AppRemoteRepository,
    private val notificationsCenter: NotificationsCenter,
    private val libraryUpdateNotification: LibraryUpdateNotification,
    private val libraryUpdatesInteractions: LibraryUpdatesInteractions,
    private val luaSourceProvider: LuaSourceProvider,
) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        Timber.d("AppWorkerFactory: createWorker called for '$workerClassName'")
        return when (workerClassName) {
            UpdatesCheckerWorker::class.java.name -> {
                Timber.d("AppWorkerFactory: creating UpdatesCheckerWorker")
                UpdatesCheckerWorker(
                    context = appContext,
                    workerParameters = workerParameters,
                    appRemoteRepository = appRemoteRepository,
                    notificationsCenter = notificationsCenter
                )
            }
            LibraryUpdatesWorker::class.java.name -> {
                Timber.d("AppWorkerFactory: creating LibraryUpdatesWorker")
                LibraryUpdatesWorker(
                    context = appContext,
                    workerParameters = workerParameters,
                    appPreferences = appPreferences,
                    libraryUpdateNotification = libraryUpdateNotification,
                    libraryUpdatesInteractions = libraryUpdatesInteractions,
                    luaSourceProvider = luaSourceProvider,
                )
            }
            AutoBackupWorker::class.java.name -> {
                Timber.d("AppWorkerFactory: creating AutoBackupWorker")
                AutoBackupWorker(
                    context = appContext,
                    workerParameters = workerParameters,
                )
            }
            DatabaseMaintenanceWorker::class.java.name -> {
                Timber.d("AppWorkerFactory: creating DatabaseMaintenanceWorker")
                DatabaseMaintenanceWorker(
                    context = appContext,
                    workerParameters = workerParameters,
                )
            }
            BookExportWorker::class.java.name -> {
                Timber.d("AppWorkerFactory: creating BookExportWorker")
                BookExportWorker(
                    context = appContext,
                    workerParameters = workerParameters,
                )
            }
            else -> {
                Timber.w("AppWorkerFactory: unknown worker '$workerClassName', returning null")
                null
            }
        }
    }
}
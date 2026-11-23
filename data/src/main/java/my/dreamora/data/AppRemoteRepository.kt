package my.dreamora.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import my.dreamora.core.AppInternalState
import my.dreamora.core.Response
import my.dreamora.core.domain.AppVersion
import my.dreamora.core.domain.RemoteAppVersion
import my.dreamora.network.NetworkClient
import my.dreamora.network.toJson
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRemoteRepository @Inject constructor(
    private val networkClient: NetworkClient,
    private val appInternalState: AppInternalState,
) {

    private val lastReleaseUrl =
        "https://api.github.com/repos/HnDK0/NoveLA/releases/latest"
    suspend fun getLastAppVersion(
    ): Response<RemoteAppVersion> = withContext(Dispatchers.IO) {
        return@withContext my.dreamora.network.tryConnect {
            val json = networkClient
                .get(lastReleaseUrl)
                .toJson()
                .asJsonObject

            RemoteAppVersion(
                version = AppVersion.fromString(json["tag_name"].asString),
                sourceUrl = json["html_url"].asString
            )
        }
    }

    fun getCurrentAppVersion() = AppVersion.fromString(appInternalState.versionName)

    fun getDisplayVersion(): String {
        val version = getCurrentAppVersion()
        val buildType = if (appInternalState.isDebugMode) "debug" else "release"
        return "$version-$buildType (${appInternalState.gitCommitHash})"
    }
}
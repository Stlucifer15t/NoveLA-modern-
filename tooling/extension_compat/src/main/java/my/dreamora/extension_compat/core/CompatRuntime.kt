package my.dreamora.extension_compat.core

/**
 * What this build can actually RUN, not just parse.
 *
 * Any ecosystem's index can be parsed and its payload downloaded, but a source
 * only shows up in the Finder when a runtime exists that can execute it. An
 * honest status here keeps the UI from looking broken: "downloaded but not
 * runnable because of X" beats a silent zero-source list.
 */
enum class CompatRuntimeState {
    /** Rules/adapter built in — an installed source works immediately. */
    READY,

    /** Payload is stored, but this ecosystem's runtime is not part of the build. */
    RUNTIME_NOT_BUNDLED,
}

data class CompatRuntimeInfo(
    val state: CompatRuntimeState,
    /** Short label for the UI chip. */
    val badge: String,
    /** The "why" plus what is needed to make it work. */
    val detail: String,
)

object CompatRuntime {

    private val READY = setOf(CompatEcosystem.LEGADO, CompatEcosystem.LNREADER, CompatEcosystem.MANGAYOMI)

    val runnableEcosystems: Set<CompatEcosystem> get() = READY

    fun isRunnable(ecosystem: CompatEcosystem): Boolean = ecosystem in READY

    fun of(ecosystem: CompatEcosystem): CompatRuntimeInfo = when (ecosystem) {
        CompatEcosystem.LEGADO -> CompatRuntimeInfo(
            state = CompatRuntimeState.READY,
            badge = "ready",
            detail = "Legado bookSource rules run on the built-in rule engine " +
                "(CSS/XPath/JSONPath/@js). Installed sources appear in Finder.",
        )

        CompatEcosystem.TACHIYOMI -> CompatRuntimeInfo(
            state = CompatRuntimeState.RUNTIME_NOT_BUNDLED,
            badge = "apk runtime",
            detail = "Mihon/Tachiyomi extensions are compiled Kotlin inside an APK. " +
                "The APK is downloaded and registered, and its metadata is read, but running " +
                "it needs the eu.kanade.tachiyomi source API shim in the app. " +
                "Use a JSON/JS source repo for now.",
        )

        CompatEcosystem.ANIYOMI -> CompatRuntimeInfo(
            state = CompatRuntimeState.RUNTIME_NOT_BUNDLED,
            badge = "apk runtime",
            detail = "Aniyomi extensions are Mihon APKs with video sources: same runtime " +
                "requirement as Tachiyomi (source API shim not bundled in this build).",
        )

        CompatEcosystem.LNREADER -> CompatRuntimeInfo(
            state = CompatRuntimeState.READY,
            badge = "ready",
            detail = "LNReader plugins run in a QuickJS runtime with the LNReader host " +
                "(@libs/fetch, storage, cheerio, dayjs). Installed plugins appear in Finder.",
        )

        CompatEcosystem.MANGAYOMI -> CompatRuntimeInfo(
            state = CompatRuntimeState.READY,
            badge = "ready",
            detail = "Mangayomi JavaScript sources run in a QuickJS runtime with the Client, " +
                "Document and MProvider globals. Dart sources and video sources are not " +
                "supported in this build and say so when installed.",
        )

        CompatEcosystem.CLOUDSTREAM -> CompatRuntimeInfo(
            state = CompatRuntimeState.RUNTIME_NOT_BUNDLED,
            badge = "video runtime",
            detail = "CloudStream providers are Kotlin/JS plugins for video. Package is stored; " +
                "the MainAPI/ExtractorApi host is not part of this build.",
        )
    }
}

package my.noveldokusha.extension_compat.core

/**
 * Что сборка умеет ИСПОЛНЯТЬ, а не только разбирать.
 *
 * Разобрать индекс и скачать payload можно для любой экосистемы, но источник
 * появляется в Finder только когда есть рантайм, который его выполнит. Честный
 * статус здесь нужен, чтобы UI не выглядел сломанным: «скачано, но не
 * запускается, потому что X» лучше, чем молчаливый ноль источников.
 */
enum class CompatRuntimeState {
    /** Правила/адаптер встроены — установленный источник сразу работает. */
    READY,

    /** Payload сохраняется, но рантайм этой экосистемы в сборку не вошёл. */
    RUNTIME_NOT_BUNDLED,
}

data class CompatRuntimeInfo(
    val state: CompatRuntimeState,
    /** Короткая метка для чипа в UI. */
    val badge: String,
    /** Объяснение «почему» + что нужно, чтобы заработало. */
    val detail: String,
)

object CompatRuntime {

    private val READY = setOf(CompatEcosystem.LEGADO)

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
            state = CompatRuntimeState.RUNTIME_NOT_BUNDLED,
            badge = "js runtime",
            detail = "LNReader plugins are JavaScript run against a QuickJS host with " +
                "Client/Document/Cheerio globals. The .js file is stored in the compat library; " +
                "it becomes a source once that runtime is bundled.",
        )

        CompatEcosystem.MANGAYOMI -> CompatRuntimeInfo(
            state = CompatRuntimeState.RUNTIME_NOT_BUNDLED,
            badge = "js runtime",
            detail = "Mangayomi sources are QuickJS/D4rt scripts. Code is stored; execution " +
                "needs the Client/MBridge runtime, which is not part of this build.",
        )

        CompatEcosystem.CLOUDSTREAM -> CompatRuntimeInfo(
            state = CompatRuntimeState.RUNTIME_NOT_BUNDLED,
            badge = "video runtime",
            detail = "CloudStream providers are Kotlin/JS plugins for video. Package is stored; " +
                "the MainAPI/ExtractorApi host is not part of this build.",
        )
    }
}

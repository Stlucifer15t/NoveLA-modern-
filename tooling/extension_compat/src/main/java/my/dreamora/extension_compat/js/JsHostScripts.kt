package my.dreamora.extension_compat.js

/**
 * The JavaScript the host runs before any plugin, read from `src/main/assets/js/`.
 *
 *  - [libs]: bundled npm libraries (cheerio, htmlparser2, dayjs), built from js-runtime/.
 *  - [common]: browser-like globals (fetch, Response, URL, Buffer, timers, require).
 *  - [lnReader] / [mangayomi]: the plugin contract of each ecosystem.
 */
class JsHostScripts(
    val libs: String,
    val common: String,
    val lnReader: String,
    val mangayomi: String,
) {
    companion object {
        /** [read] takes a path relative to the assets root, e.g. `js/host-common.js`. */
        fun load(read: (String) -> String): JsHostScripts = JsHostScripts(
            libs = read("js/js-libs.js"),
            common = read("js/host-common.js"),
            lnReader = read("js/lnreader-host.js"),
            mangayomi = read("js/mangayomi-host.js"),
        )
    }
}

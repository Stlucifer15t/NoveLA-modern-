package my.noveldokusha.extension_compat.legado

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * bookSource JSON model (Legado / 阅读 3.0 format).
 * Every field is optional — Legado exports wildly different field sets.
 */
@Serializable
data class LegadoBookSource(
    @SerialName("bookSourceName") val bookSourceName: String = "",
    @SerialName("bookSourceUrl") val bookSourceUrl: String = "",
    @SerialName("bookSourceGroup") val bookSourceGroup: String = "",
    /** 0 = text, 1 = audio, 2 = images (manga), 3 = file, 4 = video */
    @SerialName("bookSourceType") val bookSourceType: Int = 0,
    @SerialName("bookSourceComment") val bookSourceComment: String = "",
    @SerialName("enabled") val enabled: Boolean = true,
    @SerialName("enabledExplore") val enabledExplore: Boolean = true,
    @SerialName("header") val header: String = "",
    @SerialName("loginUrl") val loginUrl: String = "",
    @SerialName("jsLib") val jsLib: String = "",
    @SerialName("searchUrl") val searchUrl: String = "",
    @SerialName("exploreUrl") val exploreUrl: String = "",
    @SerialName("bookUrlPattern") val bookUrlPattern: String = "",
    @SerialName("ruleSearch") val ruleSearch: LegadoRuleSearch = LegadoRuleSearch(),
    @SerialName("ruleBookInfo") val ruleBookInfo: LegadoRuleBookInfo = LegadoRuleBookInfo(),
    @SerialName("ruleToc") val ruleToc: LegadoRuleToc = LegadoRuleToc(),
    @SerialName("ruleContent") val ruleContent: LegadoRuleContent = LegadoRuleContent(),
    @SerialName("ruleExplore") val ruleExplore: LegadoRuleExplore = LegadoRuleExplore(),
    @SerialName("charset") val charset: String = "",
)

@Serializable
data class LegadoRuleSearch(
    @SerialName("bookList") val bookList: String = "",
    @SerialName("bookName") val bookName: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("author") val author: String = "",
    @SerialName("coverUrl") val coverUrl: String = "",
    @SerialName("intro") val intro: String = "",
    @SerialName("kind") val kind: String = "",
    @SerialName("lastUpdate") val lastUpdate: String = "",
    @SerialName("lastChapter") val lastChapter: String = "",
    @SerialName("bookUrl") val bookUrl: String = "",
    @SerialName("wordCount") val wordCount: String = "",
)

@Serializable
data class LegadoRuleBookInfo(
    @SerialName("name") val name: String = "",
    @SerialName("author") val author: String = "",
    @SerialName("coverUrl") val coverUrl: String = "",
    @SerialName("intro") val intro: String = "",
    @SerialName("kind") val kind: String = "",
    @SerialName("lastUpdate") val lastUpdate: String = "",
    @SerialName("lastChapter") val lastChapter: String = "",
    @SerialName("tocUrl") val tocUrl: String = "",
    @SerialName("wordCount") val wordCount: String = "",
)

@Serializable
data class LegadoRuleToc(
    @SerialName("chapterList") val chapterList: String = "",
    @SerialName("chapterName") val chapterName: String = "",
    @SerialName("chapterUrl") val chapterUrl: String = "",
    @SerialName("isVolume") val isVolume: String = "",
    @SerialName("volumeName") val volumeName: String = "",
    @SerialName("nextTocUrl") val nextTocUrl: String = "",
    @SerialName("updateTime") val updateTime: String = "",
)

@Serializable
data class LegadoRuleContent(
    @SerialName("content") val content: String = "",
    @SerialName("nextContentUrl") val nextContentUrl: String = "",
    @SerialName("replaceRegex") val replaceRegex: String = "",
    @SerialName("imageStyle") val imageStyle: String = "",
    @SerialName("sourceRegex") val sourceRegex: String = "",
    @SerialName("webJs") val webJs: String = "",
    @SerialName("javaScript") val javaScript: String = "",
    @SerialName("style") val style: String = "",
)

@Serializable
data class LegadoRuleExplore(
    @SerialName("bookList") val bookList: String = "",
    @SerialName("bookName") val bookName: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("author") val author: String = "",
    @SerialName("coverUrl") val coverUrl: String = "",
    @SerialName("intro") val intro: String = "",
    @SerialName("kind") val kind: String = "",
    @SerialName("lastUpdate") val lastUpdate: String = "",
    @SerialName("bookUrl") val bookUrl: String = "",
)

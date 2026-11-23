package my.dreamora.scraper.fixtures

import my.dreamora.core.LanguageCode
import my.dreamora.core.PagedList
import my.dreamora.core.Response
import my.dreamora.scraper.R
import my.dreamora.scraper.SourceInterface
import my.dreamora.scraper.domain.BookResult
import my.dreamora.scraper.domain.ChapterResult

fun fixturesCatalogList(): List<SourceInterface.Catalog> = (0..7).map {
    object : SourceInterface.Catalog {
        override val catalogUrl = "catalogUrl$it"
        override val language = LanguageCode.ENGLISH
        override suspend fun getChapterList(bookUrl: String): Response<List<ChapterResult>> =
            Response.Success(listOf())

        override suspend fun getCatalogList(index: Int): Response<PagedList<BookResult>> =
            Response.Success(PagedList.createEmpty(0))

        override suspend fun getCatalogSearch(
            index: Int,
            input: String
        ): Response<PagedList<BookResult>> = Response.Success(PagedList.createEmpty(0))

        override val id = "id$it"
        override val nameStrId = R.string.source_name_local
        override val baseUrl = "baseUrl$it"
    }
}
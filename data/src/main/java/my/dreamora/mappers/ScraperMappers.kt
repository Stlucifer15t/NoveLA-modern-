package my.dreamora.mappers

import my.dreamora.core.PagedList
import my.dreamora.core.Response
import my.dreamora.core.syncMap
import my.dreamora.core.utils.normalizeBookUrl
import my.dreamora.feature.local_database.BookMetadata
import my.dreamora.feature.local_database.ChapterMetadata
import my.dreamora.scraper.domain.BookResult
import my.dreamora.scraper.domain.ChapterResult

fun ChapterResult.mapToChapterMetadata() = ChapterMetadata(
    title = this.title,
    url = this.url,
)

fun List<ChapterResult>.mapToChapterMetadata() = map { it.mapToChapterMetadata() }

@Suppress("unused")
fun Response<List<ChapterResult>>.mapToChapterMetadata() = syncMap { it.mapToChapterMetadata() }

fun BookResult.mapToBookMetadata() = BookMetadata(
    title = this.title,
    url = normalizeBookUrl(this.url),
    coverImageUrl = this.coverImageUrl,
    description = this.description,
    rating = this.rating.orEmpty(),
    contentType = this.contentType,
)

fun List<BookResult>.mapToBookMetadata() = map { it.mapToBookMetadata() }

fun PagedList<BookResult>.mapToBookMetadata() = PagedList(
    list = this.list.mapToBookMetadata(),
    index = this.index,
    isLastPage = this.isLastPage
)

fun Response<PagedList<BookResult>>.mapToBookMetadata() = syncMap { it.mapToBookMetadata() }


package my.dreamora.features.reader.domain

import my.dreamora.feature.local_database.tables.Chapter

internal data class ChapterStats(
    val itemsCount: Int,
    val chapter: Chapter,
    val orderedChaptersIndex: Int
)
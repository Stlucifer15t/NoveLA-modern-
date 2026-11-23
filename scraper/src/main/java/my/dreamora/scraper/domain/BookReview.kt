package my.dreamora.scraper.domain

/**
 * Отзыв читателя к книге. Всё берётся из источника: если поле не отдано, оно null/пустое.
 * [rating] — шкала 0..5 (скрипт сам приводит свою шкалу).
 */
data class BookReview(
    val id: String,
    val author: String,
    val rating: Float?,
    val time: String?,
    val content: String,
    val likes: Int,
    val replyCount: Int,
    val isSpoiler: Boolean,
    val isPinned: Boolean,
    val isEdited: Boolean,
)

/** Страница отзывов. [hasNext] — есть ли следующая страница. */
data class BookReviewsPage(
    val reviews: List<BookReview>,
    val hasNext: Boolean,
)

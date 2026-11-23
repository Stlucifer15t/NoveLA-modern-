package my.dreamora.scraper

import android.content.Context
import kotlinx.coroutines.runBlocking
import my.dreamora.core.Response
import my.dreamora.scraper.domain.BookReview
import my.dreamora.scraper.domain.BookReviewsPage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Проверяет отзывы из Lua-плагина. getBookReviews необязательна: без неё источник
 * честно «не публикует отзывы». Пустой текст отзыва отбрасывается, оценка — 0..5.
 */
class LuaSourceAdapterReviewsTest {

    private fun adapter(script: String): LuaSourceAdapter {
        val globals = createLuaSandboxGlobals()
        val result = globals.load(script).call()
        val luaScript = if (result.istable()) result else globals
        val luaEngine = mock<LuaEngine>()
        whenever(luaEngine.currentSourceId).thenReturn(ThreadLocal())
        return LuaSourceAdapter(
            context = mock<Context>(),
            luaScript = luaScript,
            luaEngine = luaEngine,
            fileName = null
        )
    }

    private val book = "http://example.com/book"

    @Test
    fun `script without getBookReviews reports no reviews support and an empty page`() = runBlocking {
        val a = adapter("-- no reviews function")
        assertFalse(a.hasBookReviews())
        assertEquals(
            Response.Success(BookReviewsPage(emptyList(), false)),
            a.getBookReviews(book, 0)
        )
    }

    @Test
    fun `script with getBookReviews reports support and maps every field`() = runBlocking {
        val a = adapter(
            """
            function getBookReviews(url, index)
                return {
                    items = {
                        {
                            id = 7, author = "Ann", rating = 4.5, time = "2 days ago",
                            content = "Great read", likes = 3, replies = 1,
                            spoiler = true, pinned = true, edited = false
                        }
                    },
                    hasNext = true
                }
            end
            """.trimIndent()
        )
        assertTrue(a.hasBookReviews())
        assertEquals(
            Response.Success(
                BookReviewsPage(
                    reviews = listOf(
                        BookReview(
                            id = "7",
                            author = "Ann",
                            rating = 4.5f,
                            time = "2 days ago",
                            content = "Great read",
                            likes = 3,
                            replyCount = 1,
                            isSpoiler = true,
                            isPinned = true,
                            isEdited = false,
                        )
                    ),
                    hasNext = true,
                )
            ),
            a.getBookReviews(book, 0)
        )
    }

    @Test
    fun `reviews without text are dropped and ratings are clamped to 0 to 5`() = runBlocking {
        val a = adapter(
            """
            function getBookReviews(url, index)
                return { items = {
                    { id = "a", author = "Empty", content = "   " },
                    { id = "b", author = "High", rating = 7, content = "x" },
                    { id = "c", author = "Text", rating = "3.5", content = "y" },
                    { id = "d", author = "None", content = "z" },
                }, hasNext = false }
            end
            """.trimIndent()
        )
        val page = (a.getBookReviews(book, 0) as Response.Success).data
        assertEquals(listOf("b", "c", "d"), page.reviews.map { it.id })
        assertEquals(5f, page.reviews[0].rating)
        assertEquals(3.5f, page.reviews[1].rating)
        assertEquals(null, page.reviews[2].rating)
        assertFalse(page.hasNext)
    }

    @Test
    fun `nil from getBookReviews is an empty page, not an error`() = runBlocking {
        val a = adapter("function getBookReviews(url, index) return nil end")
        assertEquals(
            Response.Success(BookReviewsPage(emptyList(), false)),
            a.getBookReviews(book, 0)
        )
    }

    @Test
    fun `a Lua error inside getBookReviews becomes Response Error`() = runBlocking {
        val a = adapter("function getBookReviews(url, index) error('boom') end")
        assertTrue(a.getBookReviews(book, 0) is Response.Error)
    }
}

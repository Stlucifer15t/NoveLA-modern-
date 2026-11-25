package my.dreamora.scraper

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import kotlinx.coroutines.runBlocking
import my.dreamora.core.appPreferences.AppPreferences
import my.dreamora.network.NetworkClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.luaj.vm2.LuaError
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import java.util.Locale

/**
 * require_lib: shared libraries of the extension repo (libs/<id>.lua) load into
 * the plugin's globals, are downloaded from the index on first use, verified by
 * sha256, cached per file, and fail with LuaError — never nil.
 */
class RequireLibTest {

    private val tempDir: File = Files.createTempDirectory("lua-require-lib").toFile()

    private val repoUrl = "https://raw.githubusercontent.com/example/external-sources/main/index.yaml"
    private val libUrl = "https://raw.githubusercontent.com/example/external-sources/main/libs/helpers/urls.lua"

    private val libCode = """
        local M = {}
        function M.hello() return "world" end
        return M
    """.trimIndent()

    private fun sha256Of(text: String): String =
        MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    private fun okResponse(request: Request, body: String): Response = Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(200)
        .message("OK")
        .body(body.toResponseBody("text/plain; charset=utf-8".toMediaType()))
        .build()

    private fun indexYaml(sha256: String, withLib: Boolean = true): String = if (withLib) {
        """
        languages: {}
        libraries:
          - id: "urls"
            url: "$libUrl"
            sha256: "$sha256"
        """.trimIndent()
    } else {
        "languages: {}\nlibraries: []\n"
    }

    private fun createEngine(
        libBody: String,
        index: String,
    ): Pair<LuaEngine, NetworkClient> {
        val networkClient = mock<NetworkClient>()
        runBlocking {
            whenever(networkClient.get(repoUrl)).thenAnswer {
                okResponse(Request.Builder().url(repoUrl).build(), index)
            }
            whenever(networkClient.get(libUrl)).thenAnswer {
                okResponse(Request.Builder().url(libUrl).build(), libBody)
            }
        }

        val context = mock<Context>()
        whenever(context.filesDir).thenReturn(tempDir)
        val resources = mock<Resources>()
        val configuration = Configuration()
        configuration.locale = Locale.US
        whenever(resources.configuration).thenReturn(configuration)
        whenever(context.resources).thenReturn(resources)

        val appPreferences = mock<AppPreferences>()
        val repoPref = with(appPreferences) {
            object : AppPreferences.Preference<String>("EXTENSIONS_REPOSITORY_URL") {
                override var value: String = repoUrl
            }
        }
        whenever(appPreferences.EXTENSIONS_REPOSITORY_URL).thenReturn(repoPref)

        return LuaEngine(context, networkClient, appPreferences) to networkClient
    }

    private fun pluginUsingLib(): String = """
        local urls = require_lib("urls")
        return { greeting = urls.hello() }
    """.trimIndent()

    @Test
    fun `require_lib downloads, verifies and loads a library into the plugin`() {
        val (engine, _) = createEngine(libCode, indexYaml(sha256Of(libCode)))

        val result = runBlocking { engine.loadScript(pluginUsingLib()) }

        assertEquals("world", result.get("greeting").tojstring())
        assertTrue(File(tempDir, "lua_extensions/lib/urls.lua").exists())
    }

    @Test
    fun `require_lib caches the library file across plugin loads`() {
        val (engine, networkClient) = createEngine(libCode, indexYaml(sha256Of(libCode)))

        runBlocking { engine.loadScript(pluginUsingLib()) }
        runBlocking { engine.loadScript(pluginUsingLib()) }

        runBlocking { verify(networkClient, times(1)).get(libUrl) }
    }

    @Test
    fun `checksum mismatch fails with LuaError`() {
        val (engine, _) = createEngine(libCode, indexYaml("deadbeef"))

        assertThrows(LuaError::class.java) {
            runBlocking { engine.loadScript(pluginUsingLib()) }
        }
    }

    @Test
    fun `library missing from the index fails with LuaError`() {
        val (engine, _) = createEngine(libCode, indexYaml(sha256Of(libCode), withLib = false))

        assertThrows(LuaError::class.java) {
            runBlocking { engine.loadScript(pluginUsingLib()) }
        }
    }

    @Test
    fun `invalid library id fails with LuaError`() {
        val (engine, _) = createEngine(libCode, indexYaml(sha256Of(libCode)))

        assertThrows(LuaError::class.java) {
            runBlocking { engine.loadScript("""return require_lib("../escape")""") }
        }
    }
}

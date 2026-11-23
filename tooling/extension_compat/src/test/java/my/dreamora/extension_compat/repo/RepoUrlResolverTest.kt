package my.dreamora.extension_compat.repo

import my.dreamora.extension_compat.core.RepoUrlResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The URL resolver: users paste links any way they like, and we have to figure
 * out what to download. No network here — the plan is purely syntactic.
 */
class RepoUrlResolverTest {

    @Test
    fun `repo root url becomes a git plan`() {
        val plan = RepoUrlResolver.plan("https://github.com/keiyoushi/extensions")
        assertTrue(plan is RepoUrlResolver.Plan.Git)
        val target = (plan as RepoUrlResolver.Plan.Git).target
        assertEquals("keiyoushi", target.owner)
        assertEquals("extensions", target.repo)
        assertEquals(null, target.ref)
        assertEquals(null, target.path)
    }

    @Test
    fun `source-only keiyoushi repo is rewritten to the repo that holds the index`() {
        val rewritten = RepoUrlResolver.rewriteKnown("https://github.com/keiyoushi/extensions-source")
        assertEquals("https://github.com/keiyoushi/extensions", rewritten)
        // a link to a source subdirectory is not rewritten blindly
        val nested = RepoUrlResolver.rewriteKnown(
            "https://github.com/keiyoushi/extensions-source/tree/main/src/en"
        )
        assertEquals("https://github.com/keiyoushi/extensions-source/tree/main/src/en", nested)
    }

    @Test
    fun `blob url becomes a direct file`() {
        val plan = RepoUrlResolver.plan("https://github.com/o/r/blob/main/sources.json")
        assertTrue(plan is RepoUrlResolver.Plan.File)
        assertEquals(
            "https://raw.githubusercontent.com/o/r/main/sources.json",
            (plan as RepoUrlResolver.Plan.File).url,
        )
    }

    @Test
    fun `tree url with file path becomes a direct file`() {
        val plan = RepoUrlResolver.plan("https://github.com/keiyoushi/extensions/tree/repo/index.json")
        assertTrue(plan is RepoUrlResolver.Plan.File)
        assertEquals(
            "https://raw.githubusercontent.com/keiyoushi/extensions/repo/index.json",
            (plan as RepoUrlResolver.Plan.File).url,
        )
    }

    @Test
    fun `tree url with slashed branch keeps first segment as ref guess`() {
        // `plugins/v3.0.0` is a branch name, but from the URL it is not separable
        // from a path. The resolver guesses (ref = first segment) and the manager also
        // tries ref + next segment — see CompatRepositoryManager.resolveGit.
        val plan = RepoUrlResolver.plan("https://github.com/LNReader/lnreader-plugins/tree/plugins/v3.0.0/.dist")
            as RepoUrlResolver.Plan.Git
        assertEquals("LNReader", plan.target.owner)
        assertEquals("plugins", plan.target.ref)
        assertEquals("v3.0.0/.dist", plan.target.path)
    }

    @Test
    fun `raw url with slashed branch keeps the full path`() {
        val plan = RepoUrlResolver.plan(
            "https://raw.githubusercontent.com/LNReader/lnreader-plugins/plugins/v3.0.0/.dist/index.txt"
        ) as RepoUrlResolver.Plan.File
        assertEquals(
            "https://raw.githubusercontent.com/LNReader/lnreader-plugins/plugins/v3.0.0/.dist/index.txt",
            plan.url,
        )
    }

    @Test
    fun `release asset url is used as is`() {
        val plan = RepoUrlResolver.plan(
            "https://github.com/keiyoushi/extensions/releases/download/293eb3c/tachiyomi-all.comicklive-v1.6.8.apk"
        )
        assertTrue(plan is RepoUrlResolver.Plan.File)
        assertEquals(
            "https://github.com/keiyoushi/extensions/releases/download/293eb3c/tachiyomi-all.comicklive-v1.6.8.apk",
            (plan as RepoUrlResolver.Plan.File).url,
        )
    }

    @Test
    fun `releases page resolves to the repository`() {
        val plan = RepoUrlResolver.plan("https://github.com/o/r/releases/latest")
        assertTrue(plan is RepoUrlResolver.Plan.Git)
    }

    @Test
    fun `raw url of an index file is fetched directly`() {
        val plan = RepoUrlResolver.plan(
            "https://raw.githubusercontent.com/LNReader/lnreader-plugins/plugins/v3.0.0/.dist/plugins.min.json"
        )
        assertTrue(plan is RepoUrlResolver.Plan.File)
        assertEquals(
            "https://raw.githubusercontent.com/LNReader/lnreader-plugins/plugins/v3.0.0/.dist/plugins.min.json",
            (plan as RepoUrlResolver.Plan.File).url,
        )
    }

    @Test
    fun `custom domain file is a direct fetch`() {
        val plan = RepoUrlResolver.plan("https://flyml.xyz/bookSource.json")
        assertTrue(plan is RepoUrlResolver.Plan.File)
        assertEquals("https://flyml.xyz/bookSource.json", (plan as RepoUrlResolver.Plan.File).url)
    }

    @Test
    fun `custom domain directory probes known index names with the url itself first`() {
        val plan = RepoUrlResolver.plan("https://reader.example/sources") as RepoUrlResolver.Plan.Probe
        assertTrue(plan.urls.first() == "https://reader.example/sources")
        assertTrue(plan.urls.any { it.endsWith("bookSource.json") })
        assertTrue(plan.urls.all { it.startsWith("https://reader.example/sources/") || it == "https://reader.example/sources" })
        assertTrue(plan.urls.size <= RepoUrlResolver.MAX_PROBES + 1)
    }

    @Test
    fun `scheme-less input still resolves when it looks like a repo path`() {
        val plan = RepoUrlResolver.plan("github.com/o/r")
        assertTrue(plan is RepoUrlResolver.Plan.Git)
        assertEquals("o", (plan as RepoUrlResolver.Plan.Git).target.owner)
    }

    @Test
    fun `gitlab blob resolves to its raw url`() {
        val plan = RepoUrlResolver.plan("https://gitlab.com/group/proj/-/blob/main/bookSource.json")
        assertTrue(plan is RepoUrlResolver.Plan.File)
        assertEquals("https://gitlab.com/group/proj/-/raw/main/bookSource.json", (plan as RepoUrlResolver.Plan.File).url)
    }

    @Test
    fun `jsdelivr github cdn is understood`() {
        val plan = RepoUrlResolver.plan(
            "https://cdn.jsdelivr.net/gh/keiyoushi/extensions@repo/index.min.json"
        )
        assertTrue(plan is RepoUrlResolver.Plan.File)
        assertEquals(
            "https://cdn.jsdelivr.net/gh/keiyoushi/extensions@repo/index.min.json",
            (plan as RepoUrlResolver.Plan.File).url,
        )
    }

    @Test
    fun `empty and garbage input are rejected`() {
        assertTrue(RepoUrlResolver.plan("") is RepoUrlResolver.Plan.Invalid)
        assertTrue(RepoUrlResolver.plan("   ") is RepoUrlResolver.Plan.Invalid)
        assertTrue(RepoUrlResolver.plan("just words") is RepoUrlResolver.Plan.Invalid)
    }

    // ── index selection and API urls ────────────────────────────────────────────

    @Test
    fun `index priority prefers repo json and index json over the min stub`() {
        val files = listOf("README.md", "index.min.json", "index.html", "index.json", "repo.json", "icons")
        assertEquals("repo.json", RepoUrlResolver.findIndexName(files))
        assertEquals(
            "index.json",
            RepoUrlResolver.findIndexName(listOf("index.min.json", "index.json", "x.txt")),
        )
        // Mangayomi: the novel index is a candidate too
        assertEquals(
            "novel_index.json",
            RepoUrlResolver.findIndexName(listOf("novel_index.json", "random.json")),
        )
        assertEquals(null, RepoUrlResolver.findIndexName(listOf("README.md", "build.gradle")))
    }

    @Test
    fun `tree api url encodes refs with slashes and toggles recursion`() {
        assertEquals(
            "https://api.github.com/repos/o/r/git/trees/plugins%2Fv3.0.0",
            RepoUrlResolver.apiTreeUrl("o", "r", "plugins/v3.0.0", recursive = false),
        )
        assertEquals(
            "https://api.github.com/repos/o/r/git/trees/main?recursive=1",
            RepoUrlResolver.apiTreeUrl("o", "r", "main", recursive = true),
        )
        assertEquals("https://api.github.com/repos/o/r", RepoUrlResolver.apiDefaultBranchUrl("o", "r"))
    }

    @Test
    fun `tree file filter keeps source payloads only`() {
        val files = listOf(
            "src/a.json" to 1_000L,
            "src/b.js" to 2_000L,
            "src/c.dart" to 3_000L,
            "src/D.kt" to 4_000L,
            "notes.md" to 10L,
        )
        val kept = RepoUrlResolver.treeFiles(files).map { it.path }
        assertEquals(listOf("src/a.json", "src/b.js", "src/c.dart"), kept)
    }
}

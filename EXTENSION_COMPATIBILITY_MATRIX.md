# Dreamora Extension Compatibility Matrix

## Target Ecosystems Overview

| Ecosystem | Package Format | Runtime | API Style | Language | Dependencies | Feasibility |
|-----------|---------------|---------|-----------|----------|--------------|-------------|
| **Mihon/Tachiyomi** | APK (Android) | Native (ClassLoader) | Kotlin suspend `KeiSource` | Kotlin | tachiyomix lib, KSP | High |
| **Aniyomi** | APK (Android) | Native (ClassLoader) | Kotlin suspend `KeiSource` | Kotlin | tachiyomix lib, KSP | High |
| **Legado** | JSON bookSource | Custom Rule Engine | Declarative rules (CSS/XPath/JSONPath/JS) | JSON + JS | Rhino/QuickJS, JSoup | High (partial impl exists) |
| **LNReader** | .js (TypeScript) | QuickJS / Hermes | JS async class `PluginBase` | TypeScript/JS | Cheerio, fetch wrappers | High |
| **Mangayomi** | .js / .dart | QuickJS / D4rt | JS async `MProvider` | JS/Dart | flutter_qjs, MBridge | Medium |
| **CloudStream** | .cs3 (Kotlin/JS) | Native / Kotlin/JS | Kotlin suspend `MainAPI`/`ExtractorApi` | Kotlin | cloudstream lib | Medium |

---

## Detailed Compatibility Analysis

### 1. Mihon / Tachiyomi / Aniyomi

**Format**: Android APK with `tachiyomi.extension` feature flag
**Manifest metadata**:
- `tachiyomi.extension.class` - fully qualified Source/SourceFactory class
- `tachiyomi.extension.factory` - optional SourceFactory
- `tachiyomi.extension.nsfw` - 0/1
- `tachiyomix.extensionLib` - library version (1.6)

**Source API** (`KeiSource`):
- `suspend getPopularManga(page: Int): MangasPage`
- `suspend getLatestUpdates(page: Int): MangasPage`
- `suspend getSearchMangaList(page: Int, query: String, filters: FilterList): MangasPage`
- `suspend fetchMangaUpdate(manga, chapters, fetchDetails, fetchChapters): SMangaUpdate`
- `suspend getPageList(chapter): List<Page>`
- Optional: `getMangaByUrl`, `getMangasByUrl`, filters, related manga

**Content types**: Manga only (video via Aniyomi uses same API with `TvType.Anime`)

**Execution**: Load APK via `ChildFirstPathClassLoader` (isolated from app ClassLoader), instantiate Source class, call suspend methods.

**Security**: Verify signatures against trusted keys; NSFW gating; isolate ClassLoader.

**Repository format**: Index JSON with language → source list (name, id, version, url, icon, contentWarning)

**Dependencies**: `com.github.mihonapp:tachiyomix:1.6` (compileOnly stubs)

**Implementation approach**:
1. Add `tachiyomix` as compileOnly dependency
2. Implement `TachiyomiExtensionLoader` using `ChildFirstPathClassLoader`
3. Create `TachiyomiSourceAdapter` implementing `SourceInterface.Catalog` + `FilterableCatalog`
4. Map `KeiSource` methods → `SourceInterface` methods
5. Handle manga/video content type distinction

---

### 2. Legado (ReadEra / Legado 3.x)

**Format**: JSON bookSource (single object or array)
**Rule Engine**: Declarative rules with:
- CSS selectors (`##`, `$$`)
- XPath (`@xpath:`)
- JSONPath (`@json:`)
- Regex (`@regex:`)
- JavaScript (`@js:`)
- Variables: `{{key}}`, `{{page}}`, `{{baseURL}}`

**Source types** (`bookSourceType`):
- 0/1 = Novel (text)
- 2 = Manga (images)
- 3 = Audio
- 4 = Video

**Key rules**:
- `searchUrl` + `ruleSearch` (bookList, name, author, coverUrl, bookUrl, kind, lastChapter)
- `exploreUrl` + `ruleExplore`
- `ruleBookInfo` (name, intro, coverUrl, tocUrl, kind, lastChapter, lastUpdate)
- `ruleToc` (chapterList, chapterName, chapterUrl, isVolume, volumeName, nextTocUrl, updateTime)
- `ruleContent` (content, nextContentUrl, replaceRegex)
- `header` (JSON headers)
- `charset`

**Status**: Partial implementation exists in `tooling/extension_compat/legado/`
- `LegadoBookSource` model ✓
- `LegadoRuleParser` (CSS/XPath/JSONPath/regex/@js) ✓
- `LegadoRuleEngine` (27 tests) ✓
- `LegadoSourceAdapter` (search, explore, details, TOC, content, manga pages, video) ✓

**Missing**:
- `loginUrl` / auth handling
- `webJs` / WebView-based rules
- `payAction` / purchase handling
- Some `@js` edge cases
- Rule variable scoping improvements

---

### 3. LNReader

**Format**: TypeScript `.ts` files compiled to ES5 JavaScript (IIFE bundle)
**Runtime**: QuickJS (Android) / Hermes (iOS) / Node.js (desktop)
**Plugin API** (`PluginBase`):
- `id`, `name`, `icon`, `site`, `version`
- `filters` (Picker, TextInput, Switch, CheckboxGroup, ExcludableCheckboxGroup)
- `pluginSettings` (Text, Switch, Select, CheckboxGroup)
- `popularNovels(page, options): NovelItem[]`
- `parseNovel(path): SourceNovel` (with chapters + totalPages for pagination)
- `parseChapter(path): string` (HTML)
- `searchNovels(term, page): NovelItem[]`
- `resolveUrl(path, isNovel?): string` (optional)
- `parsePage(novelPath, page): SourcePage` (optional, for pagination)

**Utilities provided to plugins**:
- `fetchApi`, `fetchText`, `fetchProto` (with default headers)
- `Cheerio` for HTML parsing
- `defaultCover` fallback
- `NovelStatus` enum
- `dayjs` for dates

**Repository**: GitHub Pages `plugins.min.json` manifest with plugin metadata + code URLs

**Content types**: Novel (text), Manga (via `isManga` flag), Anime (via video extractors)

**Execution**: QuickJS sandbox per plugin; inject `Client`, `Document` (cheerio), utilities.

**Implementation approach**:
1. Add QuickJS dependency (`org.mozilla.javascript:rhino` or `com.github.quickjs-android:quickjs`)
2. Create `LNReaderExtensionLoader` that compiles/loads JS
3. Implement `LNReaderSourceAdapter` mapping JS API → `SourceInterface`
4. Provide required globals: `Client`, `Document`, `fetchApi`, `Cheerio`, etc.
5. Handle pagination via `parsePage` + `totalPages`

---

### 4. Mangayomi

**Format**: JavaScript (QuickJS) or Dart (D4rt)
**Runtime**: `flutter_qjs` (QuickJS) or `d4rt` interpreter
**Source API** (`ExtensionService`):
- `getPopular(page): MPages`
- `getLatestUpdates(page): MPages`
- `search(query, page, filters): MPages`
- `getDetail(url): MManga`
- `getPageList(url): List<PageUrl>`
- `getVideoList(url): List<Video>`
- `getHtmlContent(name, url): String`
- `cleanHtmlContent(html): String`
- `getFilterList(): FilterList`
- `getSourcePreferences(): List<SourcePreference>`

**JS Globals**: `Client` (HTTP), `Document` (cheerio-like), `MBridge` (XPath, crypto, video extractors)

**Video extractors built-in**: GogoCDN, DoodStream, StreamWish, Filemoon, Quark, UC, etc.

**Repository**: JSON index with source metadata + code URLs

**Content types**: Manga, Anime, Novel (via `isManga` flag)

**Execution**: QuickJS sandbox per source; bridge HTTP to Dart `MClient`.

**Implementation approach**:
1. Use same QuickJS runtime as LNReader
2. Create `MangayomiExtensionLoader` 
3. Implement `MangayomiSourceAdapter`
4. Provide `Client`, `Document`, `MBridge` globals
5. Handle manga/anime/novel content type via `isManga` flag

---

### 5. CloudStream

**Format**: Kotlin/JS plugin (compiled to `.cs3` or JAR)
**Runtime**: Native Kotlin (Android) or Kotlin/JS
**Plugin API**:
- `BasePlugin` with `load()`, `beforeUnload()`
- `registerMainAPI(MainAPI)` - content providers
- `registerExtractorAPI(ExtractorApi)` - video extractors

**MainAPI** (content sources):
- `search(query): List<SearchResponse>`
- `getMainPage(page, request): HomePageResponse`
- `load(url): LoadResponse` (episodes/seasons)
- `loadLinks(data, isCasting, subtitleCallback, callback): Boolean`

**ExtractorApi** (video extraction):
- `getUrl(url, referer, subtitleCallback, callback)`
- `mainUrl`, `name`, `requiresReferer`
- Returns `ExtractorLink` (VIDEO, M3U8, DASH, TORRENT, MAGNET)

**SearchResponse types**: `AnimeSearchResponse`, `MovieSearchResponse`, `TvSeriesSearchResponse`, etc.

**Repository**: Plugin manifest JSON with name, className, version, requiresResources

**Content types**: Anime, Movies, TV Shows (video only)

**Execution**: Load plugin class via ClassLoader, instantiate, call `load(context)`

**Implementation approach**:
1. Add CloudStream library as dependency
2. Create `CloudStreamExtensionLoader` for `.cs3`/JAR loading
3. Implement `CloudStreamSourceAdapter` for video-only sources
3. Map `MainAPI` → `SourceInterface` (video content type)
4. Integrate `ExtractorApi` with Dreamora's video player

---

## Unified Interface Mapping

| Dreamora `SourceInterface` | Tachiyomi `KeiSource` | Legado Rules | LNReader `PluginBase` | Mangayomi `ExtensionService` | CloudStream `MainAPI` |
|--------------------------|----------------------|--------------|----------------------|------------------------------|----------------------|
| `getCatalogList` | `getPopularManga` | `exploreUrl` + `ruleExplore` | `popularNovels` | `getPopular` | `getMainPage` |
| `getCatalogSearch` | `getSearchMangaList` | `searchUrl` + `ruleSearch` | `searchNovels` | `search` | `search` |
| `getBookTitle/Description/Cover` | `fetchMangaUpdate` (details) | `ruleBookInfo` | `parseNovel` | `getDetail` | `load` |
| `getChapterList` | `fetchMangaUpdate` (chapters) | `ruleToc` | `parseNovel.chapters` + `parsePage` | `getDetail.chapters` | `load` |
| `getChapterText` | N/A | `ruleContent` | `parseChapter` | `getHtmlContent` | N/A |
| `getChapterPages` | `getPageList` | `ruleContent` (images) | N/A (novel only) | `getPageList` | N/A |
| `getVideoList` | N/A (Aniyomi) | `ruleContent` (video) | N/A | `getVideoList` | `loadLinks` + `ExtractorApi` |
| `getFilterList` | `getFilterList` + `fetchFilterData` | N/A | `filters` | `getFilterList` | N/A |
| `getCatalogFiltered` | `getSearchMangaList` with filters | N/A | `popularNovels` with filters | `search` with filters | N/A |

---

## Implementation Priority

1. **Legado** - Already partially implemented; complete rule engine & edge cases
2. **LNReader** - Pure JS, QuickJS runtime, high-value novel sources
3. **Mihon/Tachiyomi** - APK loading, large manga ecosystem
4. **Mangayomi** - Same JS runtime as LNReader, Dart support optional
5. **Aniyomi** - Same as Mihon, video/anime sources
6. **CloudStream** - Video-only, different architecture

---

## Security Considerations

| Risk | Mitigation |
|------|------------|
| Arbitrary code execution (APK/JS) | Isolated ClassLoader / QuickJS sandbox; no access to app internals |
| Network abuse | Route all requests through app's `NetworkClient` with timeout/limits |
| Resource exhaustion | Per-extension timeouts (LOAD_MS=10s), memory limits |
| Malicious APKs | Verify signatures against trusted keys; NSFW gating |
| Data exfiltration | Extensions only receive URLs they request; no access to user data |
| Crash propagation | `SupervisorJob` + `runCatching` per extension; errors to `CompatDiagnostics` |

---

## File Structure Plan

```
tooling/extension_compat/
├── core/
│   ├── CompatEcosystem.kt ✓
│   ├── CompatCapability.kt ✓
│   ├── CompatException.kt ✓
│   ├── CompatInstallRecord.kt ✓
│   ├── CompatStorage.kt ✓
│   ├── CompatDiagnostics.kt ✓
│   ├── CompatLoaders.kt ✓
│   ├── CompatSourceProviderImpl.kt ✓
│   ├── CompatTimeouts.kt (new)
│   ├── CompatSourceInfo.kt (new)
│   ├── FormatDetector.kt ✓
│   └── di/CompatModule.kt ✓
├── legado/ ✓ (complete)
├── tachiyomi/
│   ├── TachiyomiExtensionLoader.kt (new)
│   ├── TachiyomiSourceAdapter.kt (new)
│   ├── TachiyomiManifestParser.kt (new)
│   └── TachiyomiRepositoryHandler.kt (new)
├── lnreader/
│   ├── LNReaderExtensionLoader.kt (new)
│   ├── LNReaderSourceAdapter.kt (new)
│   ├── LNReaderRuntime.kt (new) - QuickJS wrapper
│   ├── LNReaderGlobals.kt (new) - Client, Cheerio, etc.
│   └── LNReaderRepositoryHandler.kt (new)
├── mangayomi/
│   ├── MangayomiExtensionLoader.kt (new)
│   ├── MangayomiSourceAdapter.kt (new)
│   ├── MangayomiRuntime.kt (new) - QuickJS/D4rt
│   ├── MangayomiGlobals.kt (new) - Client, MBridge
│   └── MangayomiRepositoryHandler.kt (new)
├── cloudstream/
│   ├── CloudStreamExtensionLoader.kt (new)
│   ├── CloudStreamSourceAdapter.kt (new)
│   ├── CloudStreamExtractorAdapter.kt (new)
│   └── CloudStreamRepositoryHandler.kt (new)
├── aniyomi/
│   └── (reuse Tachiyomi loader, different repo handler)
└── test/
    ├── LegadoRuleEngineTest.kt ✓
    ├── TachiyomiAdapterTest.kt (new)
    ├── LNReaderAdapterTest.kt (new)
    ├── MangayomiAdapterTest.kt (new)
    └── CloudStreamAdapterTest.kt (new)
```

---

## Dependencies to Add

```toml
# gradle/libs.versions.toml
[versions]
tachiyomix = "1.6"
quickjs = "0.4.0"  # or rhino "1.7.15"
cloudstream = "4.8.0"
kotlinx-serialization-json = "1.6.3"
okio = "3.9.0"
jsoup = "1.18.1"

[libraries]
tachiyomix = { module = "com.github.mihonapp:tachiyomix", version.ref = "tachiyomix" }
quickjs = { module = "com.github.quickjs-android:quickjs", version.ref = "quickjs" }
rhino = { module = "org.mozilla:rhino", version.ref = "rhino" }
cloudstream = { module = "com.lagradost:cloudstream3", version.ref = "cloudstream" }
```

---

## Testing Strategy

1. **Unit tests** for each adapter (rule parsing, URL building, response mapping)
2. **Integration tests** with real extension fixtures:
   - Legado: 10+ bookSource JSONs (novel, manga, video)
   - LNReader: 5+ popular plugins (NobleMTL, WuxiaWorld, etc.)
   - Tachiyomi: 3+ APK extensions (MangaDex, MangaKakalot, etc.)
   - Mangayomi: 3+ JS sources
   - CloudStream: 2+ providers
3. **End-to-end tests**: Search → Details → Chapter/Video → Read/Play
4. **Compatibility reports**: Track which extensions work/fail per ecosystem

---

## UI Integration Points

1. **ExtensionsScreen** - Add ecosystem tabs / filter chips
2. **Repository management** - Unified repo URL input with ecosystem detection
3. **Extension list items** - Show ecosystem badge, capabilities, content type
4. **Install flow** - APK picker / JSON import / JS file import / repo browse
5. **Diagnostics screen** - View load errors, capability matrix per extension
6. **Settings** - Per-extension preferences (already supported via `Configurable`)
---

## Repository layer — status as of this pass (JSON indexes)

Multi-repository subscription layer over third-party **index JSON** files. It is a
separate concern from the per-ecosystem runtimes below: adding a repository never
requires a new parser in Dreamora, only a new file in `core/`.

### Implemented

| File | Role |
|------|------|
| `core/CompatRepository.kt` | `CompatRepository` (subscription), `CompatRepoEntry` (one source in an index), `RepoKind` (INDEX / TREE), `CompatPayloadKind`, `CompatRepoResult` |
| `core/CompatRepoParsers.kt` | format sniffing + one parser per ecosystem, raw-slice extraction for Legado payloads |
| `core/RepoUrlResolver.kt` | pasted URL → `File` / `Git` / `Probe` / `Invalid` plan; GitHub/GitLab/raw/jsDelivr/release assets |
| `core/CompatRepositoryManager.kt` | add / refresh / remove, index cache, install / update / uninstall, install state |
| `core/CompatStorage.kt` | `repos.json` + `indexes/<repoId>.{index,entries}.json` cache next to `registry.json` |
| `core/CompatRuntime.kt` | per-ecosystem capability report shown in UI instead of a silent failure |
| `tachiyomi/TachiyomiExtensionLoader.kt` | APK validation (manifest metadata + `DexClassLoader` probe); refuses to pretend the source API is present |
| `features/extensions/CompatExtensionsViewModel.kt` + `CompatExtensionsScreen.kt` | repository manager dialog, opened from the Extensions screen |

### What each ecosystem gets from an index

| Ecosystem | Indexes understood | Browse | Install | Runs in Finder |
|-----------|--------------------|:------:|:-------:|:--------------:|
| Legado | `bookSource.json` object/array/wrapped, one-JSON-per-file repos, `.txt`/array URL subscription lists | ✅ | ✅ | ✅ via `LegadoRuleEngine` |
| Mihon / Tachiyomi / keiyoushi | `repo.json` pointer → `index.json` (v2 `extensionList.extensions`), legacy `index.min.json` array | ✅ | ✅ (APK stored) | ❌ no bundled source API |
| LNReader | `.dist/plugins.min.json` (+ branch `plugins/v3.0.0`) | ✅ | payload stored | ❌ no `PluginBase` runtime |
| Mangayomi | `repo.json`, `index.json`, `novel_index.json` (JS *and* Dart payloads) | ✅ | payload stored | ❌ no `MProvider` runtime |
| CloudStream | `repo.json` → `pluginLists[].url` → nested `index.min.json` (`.cs3`) | ✅ | payload stored | ❌ no `MainAPI` runtime |

Index-priority rules learned from the live repositories (see `CompatRepoParsersTest`):

- keiyoushi migrated to a v2 format: `repo.json` names the real index, and
  `index.min.json` is a 765-byte "update your app" stub — parser reads the pointer
  first and drops `Outdated App` rows, so a stale URL still yields the full catalog.
- APK URLs are release assets (`releases/download/<tag>/…`), so plain HTTP download works.
- LNReader's usable index is not on `main` — hence `KNOWN_REWRITES` in the resolver.
- Repos with no index file at all (typical Legado collections) fall back to `TREE`
  mode: every `*.json`/`*.js`/`*.dart` in the tree becomes an entry.

### Deliberate limits

- Mihon/CloudStream/etc. entries are **stored**, not executed: `CompatRuntime` says
  why (no bundled runtime), and `install()` returns `runnable = false`. No fake
  "1 source installed" success message.
- Legado payload extraction keeps the **raw JSON slice** — unknown Legado fields
  (`customOrder`, `lastUpdateTime`, …) survive into `LegadoExtensionLoader`, which
  is what applies Legado's own rule defaults.
- `updateAll()` compares `versionCode` then `version` only; signature verification of
  Mihon `apkSignBlockBytes` is not performed (fingerprint is recorded for display).

### Follow-ups, in order of usefulness

1. Legado subscription-list import UX from Finder (share-sheet target) — the parser
   already accepts URL lists.
2. Mihon runtime: vendored `KeiSource` stubs + `TachiyomiSourceAdapter` (the loader
   already isolates a `DexClassLoader`; the adapter is the missing half).
3. `AppPreferences.EXTENSIONS_REPOSITORY_URL` (single Lua YAML string) can be
   migrated into `repos.json` so the Lua repo appears in the same list.

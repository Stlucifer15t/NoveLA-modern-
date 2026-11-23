# NoveLA Modern — UI Redesign Audit & Feature Preservation Checklist

Redesign scope: presentation layer only. The NoveLA engine (scraping, sources,
database, networking, translation, plugins, reader engine) is untouched.

---

## 1. Architecture (as audited)

**There is no Navigation Compose graph.** Navigation is Intent-based, one Activity
per screen. Arguments travel as Intent extras whose key is the property name
(`core/src/main/java/my/noveldokusha/core/utils/IntentExtraDelegates.kt`).

- `navigation/src/main/java/my/noveldokusha/navigation/Navigation.kt` — `NavigationRoutes`
  interface, 11 `fun` returning `Intent`, plus `NavigationRouteViewModel`.
- `app/src/main/java/my/noveldokusha/AppNavigationRoutes.kt` — the only implementation.

**App shell:** `app/src/main/java/my/noveldokusha/features/main/MainActivity.kt`.
Four tabs (Library, Finder, History, Settings) are **all kept in composition at once**
and switched by animating `graphicsLayer { alpha }` + `zIndex`, with a
`pointerInput` blocker on inactive layers. `activePageIndex` is `rememberSaveable`.
This is why tab switches are instant and why state survives switching.

**Modules (30):** `app`, `core`, `coreui`, `data`, `networking`, `scraper`, `strings`,
`navigation`, `features/*` (10), `tooling/*` (13).

Two structural facts that constrain the redesign:

1. `coreui` is a dependency of **`data` and `scraper`** — it cannot be removed or
   renamed, and it owns `NotificationsCenter` plus the Lua code editor.
2. `BookMetadata` / `BookWithContext` are `tooling:local_database` classes referenced
   directly by UI code. They are not UI models; do not rename them.

Note: databaseExplorer sources live on disk under a misspelled directory
(`features/databaseExplorer/src/main/java/my/noveldoksuha/`) while the package is
`my.noveldokusha.databaseexplorer`. Cosmetic; left alone.

---

## 2. Screen inventory

| Screen | Main file | Reached via | ViewModel |
|---|---|---|---|
| Library | `features/libraryExplorer/.../LibraryScreen.kt` | tab 0 of MainActivity | `LibraryViewModel`, `LibraryPageViewModel` |
| Library body | `features/libraryExplorer/.../LibraryScreenBody.kt` | (called by Library) | `LibraryPageViewModel` |
| Library grid | `features/libraryExplorer/.../LibraryPageBody.kt` | (called by body) | — |
| Finder / Catalog | `features/catalogExplorer/.../CatalogExplorerScreen.kt` | tab 1 | `CatalogExplorerViewModel`, `ExtensionsManagerViewModel` |
| History | `features/historyExplorer/.../HistoryScreen.kt` | tab 2 | `HistoryViewModel` |
| Settings | `features/settings/.../SettingsScreen.kt` | tab 3 | `SettingsViewModel` |
| Reader | `features/reader/.../ui/ReaderScreen.kt` | Intent from chapters | `ReaderViewModel` |
| Chapters list | `features/chaptersList/...` | Intent | `ChaptersViewModel` |
| Manga reader | `features/reader/.../manga/MangaReaderActivity.kt` | Intent | `MangaReaderViewModel` |
| Video player | `features/reader/.../video/VideoPlayerActivity.kt` | Intent | — |
| WebView | `features/webview/...` | Intent | — |
| Source catalog | `features/sourceExplorer/...` | Intent | — |
| Global search | `features/globalSourceSearch/...` | Intent | — |
| Database explorer | `features/databaseExplorer/...` | Intent | — |
| Extensions | `features/extensions/.../ExtensionsScreen.kt` | tab 1 sub-tab | `ExtensionsManagerViewModel` |
| Migration | `tooling/novel_migration/...` | tab 1 sub-tab | — |

---

## 3. Feature preservation checklist

Every item below existed before the redesign and still exists after it. The
redesign changed how things look, not what they do.

### Library
- [x] Category filter chips — All / Reading / Completed / custom categories, with counts
- [x] Multi-select category filtering (`toggleCategory`, `clearCategoryFilters`)
- [x] Create category, delete category (picker + confirm dialogs)
- [x] Search mode (toolbar swap) and search query state
- [x] Sort / filter bottom sheet
- [x] Pull to refresh + cancel update
- [x] Selection mode: select all, mark read, mark unread, delete, move to category
- [x] Book long-press actions sheet (delete, category, mark all read/unread, migrate)
- [x] Per-book unread count and source name (all three `SourceStripPosition` modes)
- [x] Cover badges: content-type badge, rating badge, in-library badge
- [x] Book fix / repair action
- [x] EPUB import (file picker + shared intent + `ACTION_VIEW`)
- [x] Cover image prefetching while scrolling
- [x] Translated titles
- [x] Grid column count and list/grid layout preference

### Finder / Catalog
- [x] Three tabs: Browse, Extensions, Migration
- [x] Language filter chips
- [x] Add by URL dialog
- [x] Global search
- [x] Extensions: refresh, repository dialog, import Lua from file
- [x] Migration: history, global search
- [x] Per-tab action sets

### History
- [x] Loading / Empty / Content states
- [x] Clear all history (dropdown)
- [x] Resume reading on tap

### Settings
- [x] All theme + dark mode selection (21 themes, AMOLED black)
- [x] Storage cleanup actions (database, images, novel cache, manga cache, video)
- [x] Backup create / restore
- [x] Update check
- [x] Translation backends (Gemini, OpenAI-compatible, Google)
- [x] Prompt presets and per-novel prompts
- [x] Language selection
- [x] Regex cleanup navigation
- [x] Auto-backup directory, interval, count, contents
- [x] Export logs

### Reader
- [x] All toolbar actions (TOC, settings, TTS, translation, autoscroll, etc.)
- [x] Autoscroll speed panel
- [x] Bottom quick toggles: keep screen on, full screen, text selection, single-tap
- [x] Chapter navigation and progress
- [x] Text-to-speech mini player
- [x] Live translation (parallel mode)
- [x] Manual highlights
- [x] Background presets / custom image
- [x] Style settings: font, size, line height, paragraph spacing, letter spacing
- [x] Regex rules
- [x] Manga and video readers (untouched)

---

## 4. Files that are safe to redesign

Presentation only — no business logic:

- `coreui/src/main/java/my/noveldokusha/coreui/theme/**` (tokens, typography, colors)
- `coreui/src/main/java/my/noveldokusha/coreui/components/**`
- `features/*/src/main/java/**/ui/**`
- Screen composables listed in the inventory above
- `app/src/main/java/my/noveldokusha/features/main/MainActivity.kt` (shell only)

## 5. Files that must NOT be touched

- `scraper/**` — source engine, Lua loader, selector transforms
- `networking/**`
- `data/**` — repositories
- `tooling/local_database/**` — Room entities, DAOs, migrations
- `tooling/epub_parser/**`, `tooling/text_translator/**`, `tooling/text_to_speech/**`,
  `tooling/backup_create/**`, `tooling/backup_restore/**`
- `core/ExtensionManager.kt`, `app/LuaSourceModule.kt`, `app/LuaSourceProviderImpl.kt`,
  `app/ExtensionRepository.kt` — plugin compatibility
- Any `*ViewModel.kt` — kept as-is; the redesign consumes existing state, never
  re-shapes it

---

## 6. Known issues found during the audit (not fixed — out of scope)

1. **Palette triplication.** Theme accents exist in `theme/colorscheme/*ColorScheme.kt`,
   again in `features/settings/.../sections/SettingsTheme.kt`, and again in
   `features/reader/.../settingDialogs/StyleSettingDialog.kt`. Adding one theme
   requires editing three files. This is why the extended palette in
   `NovelaColors.kt` derives everything from `MaterialTheme.colorScheme` instead.
   The settings screen no longer contributes to it: seven switches that each spelled
   out `SwitchDefaults.colors(checkedThumbColor = colorAccent(), …)` now go through
   `ExpressiveSwitch` in `features/settings/ui/SettingsKit.kt`.
2. **Dead components:** `Section`, `RoundedContentLayout`, `SingleChoiceToggle` have
   no call sites outside their own previews.
3. **Reader theme plumbing is inert.** `ReaderTheme.kt` and the eight reader colors on
   `BaseColorScheme` are wired but never read by the reader, which uses
   `ReaderBackgroundPresets.kt` and preferences instead.
4. **Direct layer access from UI:** `CatalogExplorerScreen.kt`, `ChaptersScreenBody.kt`,
   `DatabaseBookInfoActivity.kt`, `WebViewActivity.kt`, `SettingsScreen.kt` reach into
   `Scraper` / `DatabaseInterface` directly. Left intact deliberately — a visual
   refactor there is where behaviour would break.

---

## 7. Design system added

| File | Contents |
|---|---|
| `coreui/theme/NovelaTokens.kt` | `NovelaSpacing`, `NovelaRadius`, `NovelaSize`, `NovelaMotion` |
| `coreui/theme/Typography.kt` | Refined M3 type scale + `NovelaType` extras |
| `coreui/theme/NovelaColors.kt` | Semantic colors derived from the active theme |
| `coreui/components/NovelaBookCard.kt` | `NovelaBookCard`, `NovelaBookRow`, `RankBadge` |
| `coreui/components/NovelaScaffold.kt` | Headers, chips, empty state, buttons, progress |

All additive. Existing components keep their public signatures so untouched screens
continue to compile and behave identically.

---

## 8. Settings & Extensions pass (second rebrand round)

Research input: current Android 16 / Material 3 Expressive settings guidance — grouped
cards with a labelled icon chip instead of a flat divider list, larger round touch
targets, state shown by shape (check/cross in the switch thumb) and not by colour
alone, and search that filters whole groups once a page grows past ~30 destinations.

| File | Contents |
|---|---|
| `features/settings/ui/SettingsKit.kt` | `SettingsGroupCard` (tinted icon chip, title, optional summary, animated `visible`), `SettingsSearchField` (pill field + suggestion chips), `ExpressiveSwitch`, `SettingsCaret`, `SettingsInnerLabel`, `SettingsRowPadding` |
| `features/settings/SettingsScreenBody.kt` | 11 sections in 7 labelled cards; one search field filters at card granularity, visibility computed once so cards and the empty state cannot disagree |
| `features/extensions/ExtensionsScreen.kt` | `NovelaSectionHeader` with live counts and the "Update all" action in the header row, `NovelaFilterChip`, `NovelaTopProgress`; language/type/search filters are `remember`ed instead of recomputed per keystroke; two dead `.filter { true }` placeholders removed |
| `features/extensions/CompatExtensionsScreen.kt` | The repository dialog body is one `LazyColumn`: a Mihon index with a thousand sources no longer composes every row at once |

Behaviour was deliberately not touched: every section composable keeps its own
view-model wiring, and the compat entry point still opens the same dialog.

### 8b. Two-level settings, the tile board, and the row groups (third round)

Requested shape: Settings as "clickable boxes with the respective settings inside", and
Browse/Extensions as cards rather than a stock source list. The two references describe
two *different* structures, and they were applied to different screens on purpose: Browse
is a board of tiles (you pick a thing), the secondary screen is a header with counters and
labelled groups of rows (you act on things).

| Change | Notes |
|---|---|
| `SettingsGroupSpec` + one list, two renderings | `SettingsScreenBody` builds seven specs (title, icon, accent, keywords, sections-lambda) and renders them header-only on the Settings tab or expanded inside `SettingsGroupActivity`. Sections exist once, so a setting cannot be present on one level and missing on the other |
| `SettingsGroupActivity` | opened through `NavigationRoutes.settingsGroup(groupKey)`, the same intent-style route as `regexRules`; an unknown key finishes instead of rendering an empty page. Group title + summary come from `settings_group_*` (+ seven new `*_hint` lines) so both levels label a group identically |
| `coreui/components/NovelaTile.kt` | `novelaTileAccent(seed)` (four *scheme* colours, cycled by a stable hash — never literals, so themes repaint), `NovelaTileCard`, `NovelaIconChip` (square, "a thing you open"), `NovelaCircleIcon` (round, "a row you act on"), `NovelaTileBadge`, `NovelaTileActionButton`, `NovelaTileIconButton`, `NovelaStatCard`, `NovelaCardGroup` + `NovelaCardGroupDivider`, `NovelaHeroHeader`, `NovelaHeroPill` |
| Browse | 2-column `LazyVerticalGrid`: hero header with the source count, database tiles, then source tiles. Pinned sources sort first. Config / translate / pin live in the tile's corner, and the text reserves that corner instead of competing with a trailing button column |
| Extensions | hero header + a strip of three counters (installed / available / updates, computed from the same filtered lists the groups render) + "Installed"/"Available" groups as single cards with hairline rows. `ExtensionRow` keeps the old row's state machine exactly — installing / update / edit+reset+uninstall / install — and adds no fake actions: `ExtensionInfo` has no pin, so there is no pin button here |

### 8c. Debug vs release, measured

The complaint was lag on the debug install. `app/build.gradle.kts` explains part of it:
debug has no R8, no resource shrinking and runs debuggable, unoptimised bytecode.

`.github/workflows/release.yml` assembles the **release** variant from the same commit and
asserts the certificate is the bundled key. It exists next to `buildRelease.yml` /
`nightly.yml` rather than replacing them: those two are dispatch-only or default-branch
and pass `-PlocalPropertiesFilePath=custom.properties`, which no clone of this repository
has, so neither produced an APK for a branch build. First run of `release.yml`, commit
`ceb2032`:

| Artifact | Size |
|---|---|
| `NoveLA-ci-debug-…` | 40 278 101 B |
| `NoveLA-release-…` | 8 290 817 B |

So the release APK is 4.9× smaller than the debug one and is the build to judge smoothness
with. What this does *not* prove: that R8 keeps the runtime-loaded extension paths intact —
`assembleRelease` compiling and signing is not the same as an installed run, and the release
APK still needs a pass on the device (install a source, open a chapter).

Known gaps carried forward honestly: the references' "For You" tab, the "More" tab with
reading-stat chips, the level/streak pills, and the "57 genres" figure do not exist in this
codebase — there is no per-source genre count, reading-time or streak data to render. Nothing
was faked for them; the counters that *are* shown are real counts of what is listed below them.

### 8d. The "More" composition, the Reading group, and first-open genres (fourth round)

The reference app's More tab replaced the flat box list. `SettingsScreenBody` now has two
renderings from one `specs` list, so a group screen and the hub can never disagree about what
exists:

- **Hub** (Settings tab): `NovelaGreetingHeader` (hour-based greeting, gradient wash of the
  accent, no app bar) → three weighted stat surfaces → "Menu" label and one
  `NovelaMenuCard` per group (circle icon, title, one-line summary, chevron) → "About" group
  alone at the bottom. The search field sits behind a header icon instead of always showing.
- **Group screen**: unchanged — the same `specs`, one entry, expanded.

**What the counters are.** `libraryBooks` and `chaptersRead` come from
`LibraryDao.booksInLibraryFlow()` and `ReadingHistoryDao.getAllFlow()`; the cache figure is the
size scan the screen already ran. `streakDays` is `readingStreakDays()` over
`ReadingHistory.lastReadEpochTimeMilli` — consecutive *local* days, allowed to end yesterday,
unit-tested in `ReadingStreakTest`. A `−1` means "database hasn't answered yet" and renders as
an ellipsis, never as a confident zero. No level, no hours-read: the app does not measure time,
and a number nobody measured is the one thing this screen must not show.

**Reading group.** A ninth `SettingsGroup` (`Reading`) whose controls write the reader's own
preference objects — `ReadingPreferences` holds the very same `Preference.state(...)` instances
`ReaderActivity` writes, so the reader panel and this screen are two doors into one value and
cannot drift. Ranges, formats and labels are copied from `StyleSettingDialog` (text size 8–32,
line height 1.0–2.5, paragraph spacing 0–40 dp, margins 0–100 dp). Margins are one control here
(the reader keeps left/right apart; this writes both and shows the wider). Prefetch is a stepper
over `0,1,2,3,5,8` — a slider invites a number that only spends mobile data.

**First-open genres.** `NovelaGenre` (in `:core`, shared by the sheet and the tab) is a fixed,
curated list, *not* a union of the installed sources' vocabularies: a Lua/Legado source only
exposes what its own `getFilterList` happens to define, so a list built from them would
reshuffle on every install and ask about taxonomies most sources cannot answer. Chosen slugs
land in `AppPreferences.FOR_YOU_GENRES` (a `StringSet`), guarded by `FOR_YOU_GENRES_PICKED`,
which is what `MainActivity` checks in `onCreate` — `IS_FIRST_LAUNCH_DONE` is written inside
`App.onCreate`, before any activity can observe it, so it is useless as a "did we ask yet" flag.

**For You is a tab, not a rail.** Six pages in `MainActivity`: Library, For you, Feeds, Finder,
History, Settings. The content-type chips (Novel / Manga / Video) are not decoration —
`ForYouEngine.resolve` filters installed sources with the *same* rule the Finder uses
(`SourceInterface.Catalog.contentType`, empty meaning "novel"), so a Manga tab with no manga
source says so instead of showing novels. One source is read at a time: `FOR_YOU_SOURCE_ID`
binds it, empty means "auto", and auto prefers a source whose name mentions WebNovel and
otherwise fans out over `MAX_FANOUT = 8` sources merged round-robin. `ForYouSourceProvisioner`
makes one attempt per install to actually obtain that default — from the cached Lua index
(`EXTENSIONS_AVAILABLE_CACHE` → `LuaSourceLoader.downloadAndCacheScript`) or, failing that, a
subscribed third-party repository — and the header reports the outcome, so "not found" is a
visible state rather than an empty grid. Paging is per (source, lane): a
lane is one picked genre (or the source's front page, `ForYouLane.Catalog`), each with its own
`ForYouCursor` — page, then the next word of that genre, exhausted only after the last one — so
one pass answers every genre at once instead of retiring the source after the first. That per-lane
cursor is what makes the sideways rails possible without lying: a rail is the answer list of its
own query, not a shuffled slice of one pool. A source that fails to answer is retired for the
pass instead of being promised forever. The tab fetches when it is first looked
at (`activate()`), re-runs when the installed-source flow re-emits (installing an extension
used to leave the tab empty until restart), and keeps library/history re-tagging off the
network. `decorate()` normalizes through `mapToBookMetadata()` (so keys match the shelf's
normalized URLs), dedupes by url, and marks `inLibrary`; books already on the shelf go last
rather than being hidden.

The header is a greeting plus one line about what the tab does, and the
source status is a pill on its own row with **Change** beside it — the tab is
named by the navigation pill, so the header does not have to repeat it. Under
the reading card sits the rails themselves (`ForYouRailBlock`): one per picked genre, in the
order the genres were picked, each a sideways `LazyRow` of `railCardWidth` covers with the
genre's initial in a tinted chip, the count it holds, and an expand button that fills the
screen with the same books (`ForYouExpandedRail`, `DialogProperties(usePlatformDefaultWidth =
false)`) — a rail that shows five covers has to have somewhere to show all of them. A genre
whose query returned fewer than three books gets no rail (`railsFor(minItems = 3)`), and a
"Highest rated" pseudo-rail is appended when the pass has at least two scores worth calling
high: the same pool ordered by the `rating` string each source returns (`parseRatingScore`
accepts "4.5", "4.8/5", "9.2/10"; a "#12" or "RANK 216" is an ordering, not a score, and is
dropped). Rails and pool share ONE `LazyVerticalGrid`, rails as `GridItemSpan(maxLineSpan)`
items: two nested vertical scrollers fight over the fling, and that fight is most of what
"not smooth" meant. The pool below is what the rails did not claim (`ForYouUiState.pool`), so
nothing prints twice, and the last row gets `navBarClearance + lg` of bottom padding because the
navigation pill floats over it. `NovelaBookCard` presses on a spring
(`dampingRatio = 0.6f, stiffness = 500f`) rather than a fixed tween, and pool cells carry
`animateItem()`, so "Load more" grows the grid instead of snapping it.

No percentage-match badge and no "because you like this" score: nothing in this codebase can
compute one honestly. The one claim the tab does make — "shares a word with <your book>" — is a
literal keyword overlap between a returned title and a title in `ReadingHistory`.

**Third-party repositories: a screen, not a dialog.** `CompatRepositoriesActivity`
(`:features:extensions`) hosts `NovelaCompatRepositoriesScreen`; the Extensions list starts it
directly instead of opening an `AlertDialog` capped at 520 dp. A Legado or Mihon index carries
hundreds of rows — name, format, version, state, action — and through a dialog keyhole that is
unreadable, especially since a Legado `bookSourceName` is usually CJK and was clipped to one
12sp line. The names themselves were wrong twice over: GitHub's tree API percent-encodes
non-ASCII paths, so entries inherited `%E7%AC%94%E9%98%81%E5%AD%97` as a display name
(`CompatRepoParsers.decodeName` fixes listing and file-name fallbacks), and an installed record
was named after the file rather than the source, which made the installed list disagree with the
Finder (`buildRecord` now reads `bookSourceName`/`bookSourceUrl`/`bookSourceType` out of a
single-source payload — `legadoPayloadMeta`). Collections are deliberately left alone: naming a
many-source file after its first source would be a guess. Labels in that screen are string
resources (`compat_*`) with Russian translations, as everywhere else in the redesigned surface.

**Layout is a Settings group, and it is the same preference object the tabs read.**
`SettingsGroup.Layout` (in `sections/SettingsLayout.kt`, written through `LayoutPreferences`,
which is built exactly like `ReadingPreferences`: `appPreferences.X.state(scope)`, never a copy)
offers Display Mode for For You and for browse/search, Grid Columns for For You (Auto = the
screen decides, i.e. `GridCells.Adaptive(112.dp)`; otherwise 2..6) and for library/browse/search
(`BOOKS_GRID_COLUMNS`, which the library's own sort sheet also writes — one value, two doors),
and "Fade chapters you have read". The For You tab re-lays out while the settings screen is
open, because `ForYouViewModel` collects `FOR_YOU_LIST_LAYOUT_MODE.flow()` and
`FOR_YOU_GRID_COLUMNS.flow()`; the library and catalog already collected `BOOKS_GRID_COLUMNS`,
and `DatabaseSearchScreenState` now carries it too, which is what Search was missing. List mode
is a real rendering, not a smaller grid: `ForYouListRow` is cover-left text-right. What is
deliberately *not* offered is the reference's "UI Density": `NovelaSpacing` is a compile-time
object, so a density row would be a slider that moves nothing until every consumer in the app is
threaded through a scale — a toy, which is the one thing this screen was criticized for.

**Read chapters fade; watched episodes fade the same way.** `GREY_OUT_READ_CHAPTERS`
(default on) drives `ChaptersScreenState.greyOutReadChapters`, and `ChaptersScreenChapterItem`
animates the title to `onSurfaceVariant` at 55% alpha over 200 ms. It rides the existing
`Chapter.read` flag — the reader sets it when a chapter is opened (`ReaderRepository:105`), the
manga pager at `MangaReaderViewModel:294`, and `VideoPlayerActivity:954` sets it for an episode
past 95% — so there is one definition of "has been read" and no second flag to fall out of sync.
The switch exists because for some sources `read` arrives with the download, and a grey list
would then be a lie.

**Feeds is a tab, and it is the shelf's own unread counts.** `FeedsViewModel` combines
`LibraryDao.getBooksInLibraryWithContextFlow()` (the aggregate the library already uses) with
`WorkersInteractions.isManualUpdateRunning()`: one row per shelf title, `chaptersCount -
chaptersReadCount` unread, sorted unread-first then by when the book was last checked, with the
source's own `lastUpdateDate` text verbatim. "All / Books / Movies" segments filter on
`Book.contentType`. The check button calls `checkForLibraryUpdates(LibraryCategory.DEFAULT)` —
the library update worker, which is the only path in this app that asks sources for new chapters,
and the row that says it is checking is the worker's own state. What it is *not*: a push feed.
The app has no channel that receives anything unprompted, so no row claims an arrival time
beyond what the database carries.

**Reading stats live in Settings, split by kind.** `SettingsGroup.Stats` renders
`sections/SettingsStats.kt` over `ReadingStats`, which `SettingsViewModel` fills from
`LibraryDao.booksInLibraryFlow()` and `ReadingHistoryDao.getAllFlow()`: shelf size, chapters
read, finished, day streak (`readingStreakDays`) and days with reading (`activeDays`) on the
Books side; titles in library, episodes watched (`ChapterDao.countWatchedEpisodes`) and watch
time (`ChapterDao.sumWatchedMs`, each episode's stored position capped at its own reported
length) on the Movies side. A chapter and an episode are different units, so they are never
added together, and `-1` in any field still renders as an ellipsis while the first read is in
flight. There is no "minutes read": the app has never timed a page, so the one duration it can
show is the video player's own.

**Where the reference is `Novery`.** The app these screens are modeled on is
[`1Finn2me/Novery`](https://github.com/1Finn2me/Novery) (GPL-3.0), whose source was read for
this work — `RecommendationTab`/`RecommendationSection` for the rail anatomy (tinted chip, title,
subtitle, see-all button, sideways row of wide cover cards, 28 dp between sections),
`RecommendationCard` for the spring press-scale, and `SettingsScreen.kt` for how Layout is grouped
(UI density, per-screen display mode, per-screen column count). No code was taken from it: the
data path here is this repo's Lua/Legado sources, cursors and Room tables, and two things in the
reference are deliberately not copied — its "% match" badge (nothing here can compute an affinity
score honestly) and its UI-density scale (our spacing tokens are compile-time constants, so a
density row would move nothing).

Still not claimed: no per-book genre ranking (sources expose genres per *book* only, one request
each — the rail shows what the sources returned for the searched words, in source order), and no
similarity scoring of any kind.

## 9. Book page (Novery layout, app theme)

Source of the layout: the Novery book page. Only the layout is borrowed, not code.

- Header: cover on the left with title, source, status, last update, rating and category beside it. Below: a stats card with Chapters, Read, Saved and Rating, then a primary action (Continue or Start reading, which opens the last-read chapter) and Download next 100 chapters.
- Synopsis: titled section with expand and collapse, the translate, scroll-to-last-read and go-to-chapter actions on the same row. Genres sit below as chips.
- Bottom navigation: Chapters (with a real chapter count badge), Related and Reviews. Back on Related or Reviews returns to Chapters.
- Stats are real data only. Read and Saved come from the local database. Saved counts chapters that have a stored body or download size. Rating comes from the source and shows a dash when the source gives none.
- Related runs a search on the book's source using its first genre and drops the book itself from the results. A book without a genre, or a source without search, shows a plain message.
- Reviews: optional. A source shows reviews only when its extension defines `getBookReviews` (see `guides/REVIEWS_EXTENSION_API.md`). Otherwise the tab says the source does not publish reviews. Pages load on demand, with Load more and spoilers hidden until tapped.
- Colours all come from the app theme (surface, surfaceContainer, primary, outlineVariant). No white or cream surfaces.


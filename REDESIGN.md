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

**First-open genres + "For you".** `NovelaGenre` is a fixed, curated list, *not* a union of the
installed sources' vocabularies: a Lua/Legado source only exposes what its own `getFilterList`
happens to define, so a list built from them would reshuffle on every install and ask about
taxonomies most sources cannot answer. Chosen slugs land in `AppPreferences.FOR_YOU_GENRES`
(new `StringSet`), guarded by `FOR_YOU_GENRES_PICKED`, which is what `MainActivity` checks in
`onCreate` — `IS_FIRST_LAUNCH_DONE` is written inside `App.onCreate`, before any activity can
observe it, so it is useless as a "did we ask yet" flag. `ForYouRepository` fans the chosen
genre words out as `getCatalogSearch` per installed source (`getCatalogList(0)` when nothing was
picked), merges round-robin so one chatty source cannot fill the rail, dedupes by
`normalizeBookUrl`, and tags each row `inLibrary` from the shelf. `MAX_SOURCES = 6`: the rail is
a preview, the Finder is where the full fan-out belongs. A card opens
`navigationRoutes.chapters(...)`.

Still not claimed: no per-book genre ranking (sources expose genres per *book* only, one request
each — the rail shows what the sources returned for the searched words, in source order), and no
similarity scoring of any kind.

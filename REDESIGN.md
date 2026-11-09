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

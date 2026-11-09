# NoveLA Modern — Complete UI/UX Audit & WebNovel Redesign Architecture

## Primary Objective
Transform NoveLA's visual identity and mobile user experience into a premium, original **WebNovel-class mobile reading application** while **100% preserving the original NoveLA core engine** (scrapers, networking, Room database, repositories, LuaJ plugin engine, translation backends, TTS engine, EPUB/FB2 parser/exporter, and novel/manga/video readers).

---

## 1. Complete Project & Module Audit

NoveLA is a 30-module Android application written in Kotlin using Jetpack Compose, Material 3, Hilt, Coroutines/Flow, Room, OkHttp, Jsoup, Coil 3, LuaJ, Media3, and WorkManager.

### Module Classification

| Layer | Modules | Redesign Policy |
|---|---|---|
| **App Shell & Routing** | `:app`, `:navigation` | Shell UI (`MainActivity.kt`) redesigned; DI, `App.kt`, `LuaSource*`, `ExtensionRepository`, and Intent contracts preserved untouched. |
| **Design System & Shared UI** | `:coreui`, `:strings` | Extended additively with WebNovel design tokens, semantic colors, typography, book cards, headers, search pills, and settings cards. Existing public signatures preserved. |
| **Feature Presentation** | `:features:libraryExplorer`, `:features:chaptersList`, `:features:catalogExplorer`, `:features:extensions`, `:features:sourceExplorer`, `:features:globalSourceSearch`, `:features:databaseExplorer`, `:features:historyExplorer`, `:features:settings`, `:features:reader`, `:features:webview`, `:tooling:novel_migration` | Compose screens, headers, cards, sheets, and reader overlay chrome redesigned. All `*ViewModel` classes, state holders, repositories, and reader adapters/views preserved. |
| **Core Engine (Untouched)** | `:core`, `:data`, `:networking`, `:scraper`, `:tooling:local_database`, `:tooling:application_workers`, `:tooling:backup_create`, `:tooling:backup_restore`, `:tooling:epub_importer`, `:tooling:epub_parser`, `:tooling:local_source`, `:tooling:text_to_speech`, `:tooling:text_translator:domain`, `:tooling:text_translator:translator_nop` | **Strictly untouched.** Zero changes to Room schemas, DAOs, scrapers, LuaJ runtime, OkHttp interceptors, workers, backup archives, or translation providers. |

---

## 2. Complete Screen & Navigation Route Map

Navigation is **Activity + Intent-based** via `NavigationRoutes` (`navigation/.../Navigation.kt`) implemented by `AppNavigationRoutes` (`app/.../AppNavigationRoutes.kt`), using property-name Intent extras (`IntentExtraDelegates.kt`).

### Main Shell (`MainActivity` — 4 Instant-Switch Persistent Tabs)
All 4 root screens remain alive in composition simultaneously and switch via `graphicsLayer { alpha }` + `zIndex` + inactive `pointerInput` blocker so scroll positions and states never reset:

| Tab | Screen | Entry Composable | ViewModels |
|---|---|---|---|
| **0 — Library (Bookshelf)** | Bookshelf grid/list, category filters, batch selection, sort/filter sheet, book actions sheet | `LibraryScreen` → `LibraryScreenBody` → `LibraryPageBody` | `LibraryViewModel`, `LibraryPageViewModel`, `NavigationRouteViewModel` |
| **1 — Finder (Explore)** | Sub-tabs: **Browse** (`CatalogList`), **Extensions** (`ExtensionsScreen`), **Migration** (`MigrationTabContent`) + `AddByUrlDialog` + `LuaEditorDialog` + `PluginTranslationSettingsDialog` | `CatalogExplorerScreen` | `CatalogExplorerViewModel`, `ExtensionsManagerViewModel`, `MigrationTabViewModel`, `NavigationRouteViewModel` |
| **2 — History** | Chronological reading timeline, quick resume, delete item / clear all | `HistoryScreen` | `HistoryViewModel`, `NavigationRouteViewModel` |
| **3 — Settings** | Theme & dark mode, language, storage/cache, network/UA/Cloudflare, backup & auto-backup, AI/Google translation, regex cleanup, updates, logs, about | `SettingsScreen` → `SettingsScreenBody` | `SettingsViewModel` |

### Activity Destinations (`NavigationRoutes` — 11 Intent Routes + Internal Activities)

| Route Method | Target Activity | Compose Screen / View | ViewModels |
|---|---|---|---|
| `main(context)` | `MainActivity` | 4-tab shell + EPUB/FB2 `ACTION_SEND`/`ACTION_VIEW` handler | — |
| `chapters(context, bookMetadata)` | `ChaptersActivity` | `ChaptersScreen` → `ChaptersScreenHeader` + `ChaptersScreenBody` + `ChaptersScreenChapterItem` + `ChaptersBottomSheet` + `GoToChapterDialog` | `ChaptersViewModel` |
| `reader(context, bookUrl, chapterUrl, scrollToSpeakingItem)` | `ReaderActivity` (dispatches to `MangaReaderActivity` / `VideoPlayerActivity` by `ReaderType`) | `ReaderScreen` + XML `ReaderItemAdapter` list + `ReaderScreenBottomBarDialogs` (`StyleSettingDialog`, `TranslatorSettingDialog`, `VoiceReaderSettingDialog`, `MoreSettingDialog`) + `TtsMiniPlayer` + `ManualHighlightPill` | `ReaderViewModel` / `MangaReaderViewModel` / `VideoPlayerViewModel` |
| `sourceCatalog(context, sourceBaseUrl)` | `SourceCatalogActivity` | `SourceCatalogScreen` + `BooksVerticalView` + `FilterBottomSheet` | `SourceCatalogViewModel` |
| `globalSearch(context, text, contentType)` | `GlobalSourceSearchActivity` | `GlobalSourceSearchScreen` + `GlobalSourceSearchScreenBody` | `GlobalSourceSearchViewModel` |
| `databaseSearch(context, input, databaseUrlBase)` | `DatabaseSearchActivity` (Title mode) | `DatabaseSearchScreen` + `DatabaseSearchBottomSheet` | `DatabaseSearchViewModel` |
| `databaseSearch(context, databaseBaseUrl)` | `DatabaseSearchActivity` (Catalog mode) | `DatabaseSearchScreen` + `DatabaseSearchBottomSheet` | `DatabaseSearchViewModel` |
| *(internal from DatabaseSearch)* | `DatabaseBookInfoActivity` | `DatabaseBookInfoScreen` + `DatabaseBookInfoScreenBody` | `DatabaseBookInfoViewModel` |
| `webView(context, url, bookUrl)` | `WebViewActivity` | `WebViewScreen` + JS `NovelaTranslateBridge` | — |
| `novelMigration(context, bookUrl, bookTitle)` | `MigrationActivity` (Single mode) | `PickerScreen` / `MigrationResultsScreen` / `MigrationOptionsDialog` | `MigrationViewModel` |
| `massMigration(context, sourceBaseUrl)` | `MigrationActivity` (Mass / Unknown source mode) | `MigrationBookPickerScreen` + `MigrationResultsScreen` | `MassMigrationViewModel` |
| `migrationHistory(context)` | `MigrationActivity` (History mode) | `MigrationHistoryScreen` | `MigrationHistoryViewModel` |
| `regexRules(context, bookUrl)` | `RegexRulesActivity` | `RegexCleanupSettingsScreen` | `RegexCleanupSettingsViewModel` |

---

## 3. UI-to-Core Dependencies Identified

1. **Database Models in UI**: `Book`, `BookMetadata`, `BookWithContext`, `Chapter`, `ChapterWithContext` from `:tooling:local_database` are consumed directly by UI composables. Must never be renamed or restructured.
2. **Core Preferences & Enums**: `AppPreferences`, `AppTheme` (20 themes), `DarkMode` (`SYSTEM`, `LIGHT`, `DARK`, `BLACK`), `ListLayoutMode` (`VerticalGrid`, `VerticalList`), `SourceStripPosition` (`OnCover`, `BelowCover`, `InfoPanel`), `LibrarySortOption`, `SortDirection`, `TernaryState`, `AppLanguage`, `RegexRule` are persisted preferences and must retain exact enum names and semantics.
3. **Direct Scraper / Domain References in UI**:
   - `CatalogExplorerScreen` & `CatalogList` reference `Scraper`, `DatabaseInterface`, `SourceInterface`, `SourceInterface.Configurable.ScreenConfig()`, `CatalogItem`.
   - `ChaptersScreen` & `ChaptersScreenBody` use `Scraper.getCompatibleSource()` and `Scraper.coverReferer()`.
   - `SourceCatalogScreen` & `FilterBottomSheet` consume `LuaFilter` (`Sort`, `Select`, `CheckboxGroup`, `TriState`, `Switch`, `TextInput`, `TagInput`), `ActiveFilters`, `FilterPreset`, `FilterHistoryEntry`.
   - `ChaptersScreenChapterItem` consumes `ChapterDownloadUi` / `ChapterDownloadUiState` from `:features:reader`.
4. **Reverse Dependency on `:coreui`**: `:data` and `:scraper` depend on `:coreui` (for `NotificationsCenter` and Lua code editor `CodeEditorField`). All existing `:coreui` public classes/functions must remain intact.
5. **Unit-Tested UI Helpers**:
   - `parseBookRating()` in `BookRatingViews.kt` → tested by `BookRatingParserTest`
   - `String?.toContentTypeBadgeIcon()` in `BookImageButtonView.kt` → tested by `ContentTypeBadgeTest`
   - `buildChapterListItems()` & `lazyIndexForChapterUrl()` in `ChaptersScreenBody.kt` → tested by `ChapterListGroupingTest` (requires `item 0` of `ChaptersScreenBody`'s `LazyColumn` to remain the single book header item)
   - `pickContinueChapter()` in `ChaptersActivity.kt` → tested by `ChaptersActivityKtTest`

---

## 4. Reusable Components Inventory

### Existing Components Preserved & Upgraded
- `BookImageButtonView` — primary book cover grid card across Library, Source Catalog, Global Search, Database Search, Migration Picker (all 3 `SourceStripPosition` modes supported).
- `BooksVerticalView` — paginated grid & vertical list book browser with infinite scroll (`ListGridLoadWatcher`), cover overrides, translated titles, and Cloudflare/error handlers.
- `BookRatingBadge` & `BookRatingChip` — cover rating/rank capsule and book detail rating display.
- `InLibraryBadge` — ribbon bookmark indicator on catalog/search covers.
- `TopAppBarSearch` — expandable/inline search bar with keyboard focus & optional dropdown menu.
- `ExpandableText` — animated gradient-faded collapsible synopsis/text block.
- `LanguageFilterChips` & `LanguagePicker` — multi-language filter pills and language selector dialog.
- `AddByUrlDialog` — multi-URL novel importer dialog with live source validation.
- `PillSlider`, `TernaryStateToggle`, `PosNegCheckbox`, `SlimListItem`, `MyButton`, `MyOutlinedTextField`, `ErrorView`, `ImageView`, `CodeEditorField`.

### WebNovel Design System Components (`coreui`)
- `NovelaTokens.kt` (`NovelaSpacing`, `NovelaRadius`, `NovelaSize`, `NovelaMotion`)
- `NovelaColors.kt` (`NovelaColors`, `NovelaTheme.colors` — theme-adaptive accents + WebNovel gold star, crimson heart, status emerald, frosted glass, CTA gradients)
- `Typography.kt` (`typography` M3 editorial scale + `NovelaType` specialized book/stat/badge styles)
- `NovelaBookCard.kt` (`NovelaBookCard`, `NovelaBookRow`, `RankBadge`)
- `NovelaScaffold.kt` (`NovelaScreenHeader`, `NovelaSectionHeader`, `NovelaFilterChip`, `NovelaChipRow`, `NovelaSearchBarPill`, `NovelaSegmentedRow`, `NovelaEmptyState`, `NovelaPrimaryButton`, `NovelaSecondaryButton`, `NovelaTopProgress`)
- `NovelaSettingsComponents.kt` (`NovelaSettingsGroupCard`, `NovelaSettingsTile`, `NovelaIconBadge`)

---

## 5. Complete Feature Preservation Checklist

### App Shell
- [x] 4 persistent tabs (Library, Finder, History, Settings) with instant alpha switching & state retention
- [x] Back press from non-zero tab returns to Library tab
- [x] Notification permission prompt on Android 13+
- [x] Deferred WorkManager periodic initialization on first `ON_RESUME`
- [x] Shared & viewed EPUB/FB2 intent import (`ACTION_SEND`, `ACTION_VIEW`)

### Library (Bookshelf)
- [x] Category filter toggle + multi-select chips (All, Reading, Completed, custom categories with counts)
- [x] Create category dialog & Delete custom category picker + confirmation dialogs
- [x] Search mode & live query filtering
- [x] Pull-to-refresh library updates + Cancel ongoing update button
- [x] Multi-book selection mode: Select All, Move to Category, Delete, Fix/Repair books (with progress bar), Cancel
- [x] Long-press `BookActionsBottomSheet`: Cover + Title, Category switch, Mark all read, Mark all unread, Migrate source, Delete novel
- [x] `LibraryBottomSheet`: Reset filters, Grid columns (2..6), Card style (`OnCover`, `BelowCover`, `InfoPanel`), Content-type filter (`All`, `Novel`, `Manga`, `Video`), Sort options (`Title`, `Unread`, `Last Read`, `Last Update`, `Date Added`) with ASC/DESC toggle, Tri-state Read filter, Genre multi-filter, Source multi-filter
- [x] Cover badges: Content-type icon, Star/Rank rating badge, Unread chapter count & source strip
- [x] Translated book titles & scrolling cover image prefetcher
- [x] Import book (EPUB/FB2) action

### Book Detail & Chapters (`ChaptersScreen`)
- [x] Collapsible translucent top bar with Back, Favorite/In-Library heart toggle, Sort/Filter sheet button, and Overflow menu
- [x] Overflow menu: Open in Browser, Find in Database, Resume Reading, Change Cover, Download Next 100, Download All, Export to EPUB (with original/translation & folder picker dialog), Migrate, Delete Book Translations, Fix Book
- [x] Fullscreen cover zoom dialog on tap & cover long-click callback
- [x] Selectable title (tap opens Global Search scoped by content type)
- [x] Source name, Reading progress (`read / total` + `%`), Book status, Last update date, Rating/Rank chip, Category chip (tap opens category picker)
- [x] Expandable genres (`+N` toggle) & expandable selectable description
- [x] Translate / Clear translation button for novel title & synopsis
- [x] Scroll to last read chapter (with highlight pulse) & Go to Chapter search/jump dialog
- [x] Resume reading CTA & Floating Action Button
- [x] Sticky Volume / Season headers (`buildChapterListItems`) & exact scroll index math (`lazyIndexForChapterUrl`)
- [x] Per-chapter read/unread styling, `"Last Read"` badge, byte size label, translated chapter titles, and download/queued/downloading/paused/failed/cancel controls
- [x] Chapter multi-selection mode: Select All, Invert Selection, Close, Delete Downloads, Download Selected, Translate Selected, Delete Translations (local), Mark Read/Unread, Mark Read/Unread Up To Chapter

### Finder / Catalog / Extensions / Migration
- [x] Segmented 3-tab switcher: Browse, Extensions, Migration + per-tab header actions
- [x] Browse: Add by URL dialog, Global Search, Language filter chips, Content-type filter chips, Databases list, Sources list (with Configurable `ScreenConfig()` dialog, per-plugin Translation Settings dialog, Pin/Unpin toggle)
- [x] Extensions: Search bar, Content-type filter, Language filter, Repository URL dialog, Import local Lua file, Refresh, Update All, Install/Update/Uninstall, Edit Lua (`LuaEditorDialog`) & Reset Lua
- [x] Migration: Source list with book counts + Unknown source entry, Mass Migration book picker & target source filter, Single/Mass Migration results with alternative match picker & skip, Migration options dialog, Migration history screen

### Source Catalog, Global Search & Database Explorer
- [x] Source Catalog: Source title & content-type label, Open WebView, Search mode, Grid/List layout mode, Lua `FilterBottomSheet` (Sort, Select, CheckboxGroup, TriState, Switch, TextInput with history, TagInput, Presets save/load/delete), `InLibraryBadge`, Cloudflare bypass prompts
- [x] Global Search: Search bar, Content-type scope chips, multi-source progress bar, horizontal paginated source carousels with `InLibraryBadge`
- [x] Database Explorer: Catalog / Title / Tri-state Genre search (`DatabaseSearchScreen`) + `DatabaseBookInfoScreen` (cover, search for sources CTA, synopsis, alt titles, authors, clickable genres, tags, related & recommended books)

### History, Settings, Reader & WebView
- [x] History: Chronological items with cover, source, last chapter, progress bar, relative time, tap to open book, quick resume, long-press/button to delete item, Clear All History
- [x] Settings: Language picker (20 languages), Dark Mode (4 modes) + 20 App Themes, Storage cleanup (DB, images, novel cache, manga cache, offline video) with confirmation dialog, Network (UA presets/custom, Cloudflare bypass, download/add delays, battery optimization), Manual & Auto Backup, Translation providers (Google Free/Enhanced, Gemini, OpenAI-compatible, global/per-novel mode, system prompt presets, per-novel prompts, LLM batch/token sliders), Regex Cleanup rules manager, Library auto-update, App update checker, Export logs, About links
- [x] Reader: Immersive background layer (theme / gradient preset / custom image), floating top & bottom bars, live translation, TTS voice reader + floating mini-player overlay, typography/style dialog, per-novel regex panel, manual & TTS highlight controls, autoscroll speed bar, quick toggles (keep screen on, fullscreen, selectable text, single-tap), manga & video readers untouched
- [x] WebView: Editable URL bar, Done (cookie save), JS Page Translate/Original toggle, Paste, Copy, Reload, Clear Cookies

---

## 6. Safe Redesign File Map

- **Allowed & Redesigned**: `coreui/theme/**`, `coreui/components/**`, `app/.../features/main/MainActivity.kt`, `features/*/src/main/java/**` (UI composables only), `tooling/novel_migration/.../ui/**` (UI composables only), `MigrationActivity.kt` (Compose UI only).
- **Strictly Untouched**: `scraper/**`, `networking/**`, `data/**`, `core/**`, `tooling/local_database/**`, `tooling/epub_parser/**`, `tooling/epub_importer/**`, `tooling/text_translator/**`, `tooling/text_to_speech/**`, `tooling/backup_create/**`, `tooling/backup_restore/**`, `tooling/application_workers/**`, `tooling/local_source/**`, all `*ViewModel.kt`, all `*Repository.kt`, and reader `ReaderItemAdapter` / manga / video engines.

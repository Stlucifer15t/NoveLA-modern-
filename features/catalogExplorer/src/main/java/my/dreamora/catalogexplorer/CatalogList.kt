package my.dreamora.catalogexplorer

import my.dreamora.coreui.components.NovelaHeroPill
import my.dreamora.coreui.components.NovelaHeroHeader
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material3.Icon
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import my.dreamora.core.getLanguageDisplayName
import my.dreamora.coreui.components.AnimatedTransition
import my.dreamora.coreui.components.ImageView
import my.dreamora.coreui.components.NovelaIconChip
import my.dreamora.coreui.components.NovelaTileBadge
import my.dreamora.coreui.components.NovelaTileCard
import my.dreamora.coreui.components.NovelaTileSectionHeader
import my.dreamora.coreui.components.novelaTileAccent
import my.dreamora.coreui.theme.InternalTheme
import my.dreamora.coreui.theme.NovelaRadius
import my.dreamora.coreui.theme.NovelaSpacing
import my.dreamora.coreui.theme.NovelaTheme
import my.dreamora.coreui.theme.NovelaType
import my.dreamora.coreui.theme.PreviewThemes
import my.dreamora.data.CatalogItem
import my.dreamora.scraper.DatabaseInterface
import my.dreamora.scraper.SourceInterface
import my.dreamora.scraper.displayName
import my.dreamora.scraper.fixtures.fixturesCatalogList
import my.dreamora.scraper.fixtures.fixturesDatabaseList
import my.dreamora.strings.R as StringsR

@OptIn(ExperimentalAnimationApi::class, ExperimentalFoundationApi::class)
@Composable
internal fun CatalogList(
    innerPadding: PaddingValues,
    databasesList: List<DatabaseInterface>,
    sourcesList: List<CatalogItem>,
    onDatabaseClick: (DatabaseInterface) -> Unit,
    onSourceClick: (SourceInterface.Catalog) -> Unit,
    onSourceSetPinned: (id: String, pinned: Boolean) -> Unit,
    translationSettingsExtensionIds: Map<String, String> = emptyMap(), // catalog.id → extensionId
    onTranslationSettingsClick: (String) -> Unit = {}, // extensionId
    pluginEnabledMap: Map<String, Boolean> = emptyMap(),
) {
    // Pinned first, then the incoming order (which is already alphabetical): the grid is
    // two columns wide, so the pinned row is where a thumb reaches it.
    val ordered = remember(sourcesList) {
        sourcesList.sortedByDescending { it.pinned }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues = innerPadding),
        horizontalArrangement = Arrangement.spacedBy(NovelaSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(NovelaSpacing.sm),
        contentPadding = PaddingValues(
            start = NovelaSpacing.screenH,
            end = NovelaSpacing.screenH,
            bottom = 200.dp,
        ),
    ) {
        // The screen-opening block from the mockup: icon chip, title, one-line purpose,
        // and a count pill. It scrolls with the grid instead of being pinned, so the first
        // tiles are one flick away rather than below a fixed header.
        item(span = { GridItemSpan(maxLineSpan) }, contentType = "hero") {
            val accent = MaterialTheme.colorScheme.primary
            NovelaHeroHeader(
                title = stringResource(R.string.title_browse),
                subtitle = stringResource(R.string.browse_hero_subtitle),
                accent = accent,
                icon = {
                    Icon(
                        Icons.Outlined.Explore,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = accent,
                    )
                },
                pill = {
                    NovelaHeroPill(
                        value = sourcesList.size.toString(),
                        label = stringResource(R.string.sources),
                        accent = accent,
                    )
                },
            )
        }

        if (databasesList.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }, contentType = "header") {
                NovelaTileSectionHeader(
                    title = stringResource(id = R.string.database),
                    count = databasesList.size,
                )
            }
            itemsIndexed(
                items = databasesList,
                key = { _, it -> "db_" + it.baseUrl },
                contentType = { _, _ -> "database" },
            ) { _, database ->
                val accent = novelaTileAccent(database.baseUrl)
                NovelaTileCard(
                    accent = accent,
                    onClick = { onDatabaseClick(database) },
                ) {
                    NovelaIconChip(accent = accent) {
                        ImageView(
                            imageModel = database.iconUrl,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(NovelaRadius.coverShape),
                            error = R.drawable.default_icon,
                        )
                    }
                    Spacer(Modifier.height(NovelaSpacing.xxs))
                    Text(
                        text = stringResource(id = database.nameStrId),
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(R.string.english),
                        style = NovelaType.bookMeta,
                        color = NovelaTheme.colors.textMuted,
                    )
                }
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }, contentType = "header") {
            NovelaTileSectionHeader(
                title = stringResource(id = R.string.sources),
                count = ordered.size,
            )
        }

        itemsIndexed(
            items = ordered,
            key = { _, it -> it.catalog.id },
            contentType = { _, _ -> "source" },
        ) { _, item ->
            SourceTile(
                item = item,
                onOpen = { onSourceClick(item.catalog) },
                onTogglePin = { onSourceSetPinned(item.catalog.id, !item.pinned) },
                translationExtensionId = translationSettingsExtensionIds[item.catalog.id],
                translationEnabled = translationSettingsExtensionIds[item.catalog.id]
                    ?.let { pluginEnabledMap[it] == true } ?: false,
                onTranslationSettingsClick = onTranslationSettingsClick,
            )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun SourceTile(
    item: CatalogItem,
    onOpen: () -> Unit,
    onTogglePin: () -> Unit,
    translationExtensionId: String?,
    translationEnabled: Boolean,
    onTranslationSettingsClick: (String) -> Unit,
) {
    val catalog = item.catalog
    val accent = novelaTileAccent(catalog.id)
    val contentType = catalog.contentType
    val labelRes = when (contentType) {
        "manga" -> StringsR.string.content_type_manga
        "video" -> StringsR.string.content_type_video
        else -> StringsR.string.content_type_novel
    }

    NovelaTileCard(
        accent = accent,
        onClick = onOpen,
        // The actions live in the tile's top-right corner instead of a trailing column:
        // in a two-column grid there is no horizontal room for them, and a corner button
        // is a bigger target than a 16dp icon squeezed next to a name.
        overlay = {
            if (catalog is SourceInterface.Configurable) {
                var openConfig by rememberSaveable { mutableStateOf(false) }
                IconButton(onClick = { openConfig = !openConfig }, modifier = Modifier.size(30.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = stringResource(R.string.configuration),
                        modifier = Modifier.size(16.dp),
                    )
                }
                if (openConfig) {
                    AlertDialog(
                        onDismissRequest = { openConfig = false },
                        confirmButton = {
                            FilledTonalButton(
                                onClick = { openConfig = false },
                                shape = NovelaRadius.buttonShape,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(stringResource(R.string.close))
                            }
                        },
                        text = { catalog.ScreenConfig() },
                        icon = {
                            Icon(
                                Icons.Filled.Settings,
                                stringResource(id = R.string.configuration),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                    )
                }
            }
            if (translationExtensionId != null && contentType !in setOf("manga", "video")) {
                // Settings open under catalog.id (the cascade lookup key): for a Lua plugin
                // that equals metadata.id ("lua_<ext.id>" or a custom id). Manga and anime
                // have no text to translate, so the icon is not offered for them.
                IconButton(
                    onClick = { onTranslationSettingsClick(catalog.id) },
                    modifier = Modifier.size(30.dp),
                ) {
                    Icon(
                        Icons.Filled.Translate,
                        contentDescription = stringResource(StringsR.string.extension_translation_settings),
                        modifier = Modifier.size(16.dp),
                        tint = if (translationEnabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
            IconButton(onClick = onTogglePin, modifier = Modifier.size(30.dp)) {
                AnimatedTransition(targetState = item.pinned) { pinned ->
                    Icon(
                        imageVector = if (pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                        contentDescription = stringResource(R.string.pin_or_unpin_source),
                        modifier = Modifier.size(16.dp),
                        tint = if (pinned) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NovelaIconChip(accent = accent) {
                val iconResId = catalog.iconResId
                ImageView(
                    imageModel = iconResId ?: catalog.iconUrl,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(NovelaRadius.coverShape),
                    error = R.drawable.default_icon,
                )
            }
            // Reserve the corner the overlay buttons sit in, so a long name never
            // slides under them.
            Spacer(Modifier.width(if (catalog is SourceInterface.Configurable) 36.dp else 8.dp))
        }
        Text(
            text = catalog.displayName(),
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(NovelaSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = NovelaSpacing.xxs),
        ) {
            NovelaTileBadge(text = stringResource(labelRes), accent = accent)
            catalog.languageTag?.let { tag ->
                NovelaTileBadge(
                    text = getLanguageDisplayName(tag),
                    accent = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@PreviewThemes
@Composable
private fun PreviewView() {
    val catalogItemsList = fixturesCatalogList().mapIndexed { index, it ->
        CatalogItem(catalog = it, pinned = index % 2 == 0)
    }
    InternalTheme {
        CatalogList(
            innerPadding = PaddingValues(),
            databasesList = fixturesDatabaseList(),
            sourcesList = catalogItemsList,
            onDatabaseClick = {},
            onSourceClick = {},
            onSourceSetPinned = { _, _ -> },
        )
    }
}
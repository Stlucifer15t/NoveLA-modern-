package my.noveldokusha.catalogexplorer

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import my.noveldokusha.core.getLanguageDisplayName
import my.noveldokusha.coreui.components.AnimatedTransition
import my.noveldokusha.coreui.components.ImageView
import my.noveldokusha.coreui.components.NovelaSecondaryButton
import my.noveldokusha.coreui.components.NovelaSectionHeader
import my.noveldokusha.coreui.theme.InternalTheme
import my.noveldokusha.coreui.theme.NovelaRadius
import my.noveldokusha.coreui.theme.NovelaSize
import my.noveldokusha.coreui.theme.NovelaSpacing
import my.noveldokusha.coreui.theme.NovelaTheme
import my.noveldokusha.coreui.theme.NovelaType
import my.noveldokusha.coreui.theme.PreviewThemes
import my.noveldokusha.data.CatalogItem
import my.noveldokusha.scraper.DatabaseInterface
import my.noveldokusha.scraper.SourceInterface
import my.noveldokusha.scraper.displayName
import my.noveldokusha.scraper.fixtures.fixturesCatalogList
import my.noveldokusha.scraper.fixtures.fixturesDatabaseList
import my.noveldokusha.strings.R as StringsR

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
    val colors = NovelaTheme.colors

    LazyColumn(
        contentPadding = PaddingValues(top = 8.dp, bottom = 220.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(paddingValues = innerPadding),
    ) {
        if (databasesList.isNotEmpty()) {
            item(key = "databases_header") {
                NovelaSectionHeader(
                    title = stringResource(id = R.string.database),
                    badgeText = databasesList.size.toString(),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            items(
                items = databasesList,
                key = { it.baseUrl },
            ) { db ->
                Surface(
                    onClick = { onDatabaseClick(db) },
                    shape = RoundedCornerShape(NovelaRadius.card),
                    color = colors.surfaceCard,
                    border = BorderStroke(NovelaSize.hairline, colors.borderSubtle),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = NovelaSpacing.lg),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = colors.surfaceSubtle,
                            border = BorderStroke(NovelaSize.hairline, colors.borderSubtle),
                            modifier = Modifier.size(40.dp),
                        ) {
                            ImageView(
                                imageModel = db.iconUrl,
                                modifier = Modifier
                                    .padding(6.dp)
                                    .clip(RoundedCornerShape(6.dp)),
                                error = R.drawable.default_icon,
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = stringResource(id = db.nameStrId),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary,
                            )
                            Text(
                                text = stringResource(R.string.english),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textMuted,
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = null,
                            tint = colors.textMuted,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }

        item(key = "sources_header") {
            Spacer(Modifier.height(6.dp))
            NovelaSectionHeader(
                title = stringResource(id = R.string.sources),
                badgeText = sourcesList.size.toString(),
            )
        }

        items(
            items = sourcesList,
            key = { it.catalog.id },
        ) { item ->
            val catalog = item.catalog
            Surface(
                onClick = { onSourceClick(catalog) },
                shape = RoundedCornerShape(NovelaRadius.card),
                color = colors.surfaceCard,
                border = BorderStroke(
                    width = NovelaSize.hairline,
                    color = if (item.pinned) colors.accentLine else colors.borderSubtle,
                ),
                modifier = Modifier
                    .animateItem()
                    .fillMaxWidth()
                    .padding(horizontal = NovelaSpacing.lg),
            ) {
                Row(
                    modifier = Modifier.padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = colors.surfaceSubtle,
                        border = BorderStroke(NovelaSize.hairline, colors.borderSubtle),
                        modifier = Modifier.size(40.dp),
                    ) {
                        val iconResId = catalog.iconResId
                        ImageView(
                            imageModel = iconResId ?: catalog.iconUrl,
                            modifier = Modifier
                                .padding(6.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            error = R.drawable.default_icon,
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = catalog.displayName(),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )

                            val contentType = catalog.contentType
                            val labelRes = when (contentType) {
                                "manga" -> StringsR.string.content_type_manga
                                "video" -> StringsR.string.content_type_video
                                else -> StringsR.string.content_type_novel
                            }
                            val (bgColor, fgColor) = when (contentType) {
                                "manga" -> colors.accentSoft to colors.accent
                                "video" -> colors.goldStarSoft to colors.goldStar
                                else -> colors.surfaceSubtle to colors.textSecondary
                            }
                            Box(
                                modifier = Modifier
                                    .background(bgColor, RoundedCornerShape(NovelaRadius.pill))
                                    .padding(horizontal = 7.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    text = stringResource(labelRes),
                                    style = NovelaType.badge,
                                    color = fgColor,
                                    maxLines = 1,
                                )
                            }
                        }

                        val languageCode = catalog.languageTag
                        if (languageCode != null) {
                            Text(
                                text = getLanguageDisplayName(languageCode),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textMuted,
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (catalog is SourceInterface.Configurable) {
                            var openConfig by rememberSaveable { mutableStateOf(false) }
                            IconButton(onClick = { openConfig = !openConfig }) {
                                Icon(
                                    imageVector = Icons.Filled.Settings,
                                    contentDescription = stringResource(R.string.configuration),
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(19.dp),
                                )
                            }
                            if (openConfig) {
                                AlertDialog(
                                    onDismissRequest = { openConfig = false },
                                    shape = RoundedCornerShape(NovelaRadius.sheet),
                                    containerColor = colors.surfaceFloating,
                                    confirmButton = {
                                        NovelaSecondaryButton(
                                            text = stringResource(R.string.close),
                                            onClick = { openConfig = false },
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                    },
                                    text = { catalog.ScreenConfig() },
                                    icon = {
                                        Icon(
                                            Icons.Filled.Settings,
                                            stringResource(id = R.string.configuration),
                                            tint = colors.accent,
                                        )
                                    },
                                )
                            }
                        }

                        val extensionId = translationSettingsExtensionIds[catalog.id]
                        if (extensionId != null && catalog.contentType !in setOf("manga", "video")) {
                            val isPluginTranslated = pluginEnabledMap[extensionId] == true
                            IconButton(onClick = { onTranslationSettingsClick(catalog.id) }) {
                                Icon(
                                    imageVector = Icons.Filled.Translate,
                                    contentDescription = stringResource(StringsR.string.extension_translation_settings),
                                    tint = if (isPluginTranslated) colors.accent else colors.textMuted,
                                    modifier = Modifier.size(19.dp),
                                )
                            }
                        }

                        IconButton(
                            onClick = { onSourceSetPinned(catalog.id, !item.pinned) },
                        ) {
                            AnimatedTransition(targetState = item.pinned) { pinned ->
                                Icon(
                                    imageVector = if (pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                    contentDescription = stringResource(R.string.pin_or_unpin_source),
                                    tint = if (pinned) colors.accent else colors.textMuted,
                                    modifier = Modifier.size(19.dp),
                                )
                            }
                        }
                    }
                }
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

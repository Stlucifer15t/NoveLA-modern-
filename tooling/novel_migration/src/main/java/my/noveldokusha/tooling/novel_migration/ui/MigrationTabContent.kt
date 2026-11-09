package my.noveldokusha.tooling.novel_migration.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.AltRoute
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import my.noveldokusha.coreui.components.ImageView
import my.noveldokusha.coreui.components.NovelaEmptyState
import my.noveldokusha.coreui.components.NovelaSectionHeader
import my.noveldokusha.coreui.theme.NovelaRadius
import my.noveldokusha.coreui.theme.NovelaSize
import my.noveldokusha.coreui.theme.NovelaSpacing
import my.noveldokusha.coreui.theme.NovelaTheme
import my.noveldokusha.coreui.theme.NovelaType
import my.noveldokusha.scraper.SourceInterface
import my.noveldokusha.scraper.displayName
import my.noveldokusha.strings.R

@Composable
fun MigrationTabContent(
    innerPadding: PaddingValues,
    onSourceClick: (SourceInterface.Catalog) -> Unit,
    onUnknownSourceClick: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onHistoryClick: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onGlobalSearchClick: () -> Unit,
) {
    val colors = NovelaTheme.colors
    val viewModel: MigrationTabViewModel = viewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    if (state.loading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = colors.accent)
        }
    } else if (state.sourcesWithCounts.isEmpty() && state.unknownCount == 0) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            NovelaEmptyState(
                icon = Icons.AutoMirrored.Outlined.AltRoute,
                title = stringResource(R.string.migration_no_sources_with_books),
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(top = 8.dp, bottom = 220.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item {
                NovelaSectionHeader(
                    title = stringResource(R.string.sources),
                    badgeText = state.sourcesWithCounts.size.toString(),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            items(state.sourcesWithCounts, key = { (source, _) -> source.id }) { (source, bookCount) ->
                Surface(
                    onClick = { onSourceClick(source) },
                    shape = RoundedCornerShape(NovelaRadius.card),
                    color = colors.surfaceCard,
                    border = BorderStroke(NovelaSize.hairline, colors.borderSubtle),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = NovelaSpacing.lg),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = colors.surfaceSubtle,
                            border = BorderStroke(NovelaSize.hairline, colors.borderSubtle),
                            modifier = Modifier.size(40.dp),
                        ) {
                            ImageView(
                                imageModel = source.iconResId ?: source.iconUrl,
                                modifier = Modifier
                                    .padding(6.dp)
                                    .clip(RoundedCornerShape(6.dp)),
                            )
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = source.displayName(),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = source.baseUrl,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        Spacer(Modifier.width(8.dp))

                        Surface(
                            shape = RoundedCornerShape(NovelaRadius.pill),
                            color = colors.accentSoft,
                        ) {
                            Text(
                                text = "$bookCount",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = NovelaType.badge,
                                color = colors.accent,
                            )
                        }

                        Spacer(Modifier.width(4.dp))

                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = null,
                            tint = colors.textMuted,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            if (state.unknownCount > 0) {
                item(key = "unknown-source") {
                    Surface(
                        onClick = onUnknownSourceClick,
                        shape = RoundedCornerShape(NovelaRadius.card),
                        color = colors.surfaceCard,
                        border = BorderStroke(NovelaSize.hairline, colors.borderSubtle),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = NovelaSpacing.lg),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = colors.surfaceSubtle,
                                border = BorderStroke(NovelaSize.hairline, colors.borderSubtle),
                                modifier = Modifier.size(40.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.LinkOff,
                                        contentDescription = null,
                                        tint = colors.textMuted,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }

                            Spacer(Modifier.width(12.dp))

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(
                                    text = stringResource(R.string.migration_unknown_source),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = stringResource(R.string.migration_unknown_source_hint),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(NovelaRadius.pill),
                                color = colors.accentSoft,
                            ) {
                                Text(
                                    text = "${state.unknownCount}",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = NovelaType.badge,
                                    color = colors.accent,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

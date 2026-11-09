package my.noveldokusha.features.chapterslist

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import my.noveldokusha.chapterslist.R
import my.noveldokusha.coreui.components.TernaryStateToggle
import my.noveldokusha.coreui.theme.NovelaRadius
import my.noveldokusha.coreui.theme.NovelaSize
import my.noveldokusha.coreui.theme.NovelaSpacing
import my.noveldokusha.coreui.theme.NovelaTheme
import my.noveldokusha.coreui.theme.NovelaType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChaptersBottomSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    state: ChaptersScreenState,
) {
    if (!visible) return

    val colors = NovelaTheme.colors

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = NovelaRadius.sheet, topEnd = NovelaRadius.sheet),
        containerColor = colors.surfaceFloating,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = NovelaSpacing.lg)
                .padding(top = 8.dp, bottom = 44.dp),
        ) {
            Text(
                text = stringResource(id = R.string.sort).uppercase(),
                style = NovelaType.sectionEyebrow,
                color = colors.accent,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(id = R.string.chapters),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            Spacer(Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(NovelaRadius.card),
                color = colors.surfaceSubtle,
                border = BorderStroke(NovelaSize.hairline, colors.borderSubtle),
                modifier = Modifier.fillMaxWidth(),
            ) {
                TernaryStateToggle(
                    text = stringResource(R.string.by_chapter_name),
                    state = state.settingChapterSort.value,
                    onStateChange = { state.settingChapterSort.value = it.next() },
                    modifier = Modifier.fillMaxWidth(),
                    activeIcon = {
                        Icon(
                            imageVector = Icons.Filled.ArrowUpward,
                            contentDescription = null,
                            tint = colors.accent,
                        )
                    },
                    inverseIcon = {
                        Icon(
                            imageVector = Icons.Filled.ArrowDownward,
                            contentDescription = null,
                            tint = colors.accent,
                        )
                    },
                    inactiveIcon = {
                        Icon(
                            imageVector = Icons.Filled.SortByAlpha,
                            contentDescription = null,
                            tint = colors.textMuted,
                        )
                    },
                )
            }
        }
    }
}

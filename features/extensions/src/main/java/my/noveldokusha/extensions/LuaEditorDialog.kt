package my.noveldokusha.extensions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import my.noveldokusha.coreui.components.editor.CodeEditorField
import my.noveldokusha.coreui.theme.NovelaRadius
import my.noveldokusha.coreui.theme.NovelaSize
import my.noveldokusha.coreui.theme.NovelaTheme
import my.noveldokusha.strings.R as StringsR

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LuaEditorDialog(
    title: String,
    code: String,
    error: String?,
    onCodeChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    val colors = NovelaTheme.colors
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current
    val normalizedCode = remember(code) { code.replace("\r\n", "\n") }

    var wordWrap by remember { mutableStateOf(false) }
    var editorFontSize by remember { mutableStateOf(11) }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(NovelaRadius.card),
            color = colors.surfaceFloating,
            border = BorderStroke(NovelaSize.hairline, colors.borderSubtle),
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        EditorPillAction(
                            text = stringResource(StringsR.string.cancel),
                            isPrimary = false,
                            onClick = onDismiss,
                        )
                        EditorPillAction(
                            text = stringResource(StringsR.string.save),
                            isPrimary = true,
                            onClick = onSave,
                        )
                    }
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    EditorPillAction(
                        text = stringResource(StringsR.string.copy),
                        onClick = { clipboardManager.setText(AnnotatedString(normalizedCode)) },
                    )
                    EditorPillAction(
                        text = stringResource(StringsR.string.paste),
                        onClick = { clipboardManager.getText()?.text?.let(onCodeChange) },
                    )
                    EditorPillAction(
                        text = stringResource(StringsR.string.clear),
                        onClick = { onCodeChange("") },
                    )
                    EditorPillAction(
                        text = if (wordWrap) "No Wrap" else "Wrap",
                        onClick = { wordWrap = !wordWrap },
                    )
                    EditorPillAction(
                        text = "${editorFontSize}sp",
                        onClick = {
                            editorFontSize = when (editorFontSize) {
                                6 -> 7
                                7 -> 8
                                8 -> 9
                                9 -> 10
                                10 -> 11
                                11 -> 13
                                else -> 8
                            }
                        },
                    )
                }

                if (!error.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(NovelaRadius.md),
                        color = colors.danger.copy(alpha = 0.12f),
                        border = BorderStroke(NovelaSize.hairline, colors.danger.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = error,
                            color = colors.danger,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp),
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(
                            color = Color(0xFF161922),
                            shape = RoundedCornerShape(NovelaRadius.md),
                        )
                        .padding(6.dp),
                ) {
                    CodeEditorField(
                        text = normalizedCode,
                        onTextChange = onCodeChange,
                        fontSize = editorFontSize,
                        showLineNumbers = true,
                        wordWrap = wordWrap,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
private fun EditorPillAction(
    text: String,
    isPrimary: Boolean = false,
    onClick: () -> Unit,
) {
    val colors = NovelaTheme.colors
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(NovelaRadius.pill),
        color = if (isPrimary) colors.accent else colors.surfaceSubtle,
        border = if (isPrimary) null else BorderStroke(NovelaSize.hairline, colors.borderSubtle),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (isPrimary) colors.onAccent else colors.textPrimary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

package my.noveldokusha.webview

import android.content.ClipboardManager
import android.content.Context
import android.view.View
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import my.noveldokusha.coreui.theme.NovelaRadius
import my.noveldokusha.coreui.theme.NovelaSize
import my.noveldokusha.coreui.theme.NovelaTheme

@Composable
private fun BrowserActionChip(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    active: Boolean = false,
) {
    val colors = NovelaTheme.colors
    val alpha = if (enabled) 1f else 0.38f

    Surface(
        shape = RoundedCornerShape(NovelaRadius.pill),
        color = if (active) colors.accentSoft else colors.surfaceMuted,
        border = BorderStroke(
            NovelaSize.hairline,
            if (active) colors.accent.copy(alpha = 0.55f) else colors.borderSubtle,
        ),
        modifier = Modifier
            .alpha(alpha)
            .clip(RoundedCornerShape(NovelaRadius.pill))
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                onClick = onClick,
            ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (active) colors.accent else colors.textSecondary,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                color = if (active) colors.accent else colors.textSecondary,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun <T : View> WebViewScreen(
    toolbarTitle: String,
    isReady: Boolean,
    onDoneClicked: () -> Unit,
    onNavigateToUrl: (String) -> Unit,
    webViewFactory: (Context) -> T,
    onBackClicked: () -> Unit,
    onReloadClicked: () -> Unit,
    onClearCookiesClicked: () -> Unit,
    onCopyUrlClicked: () -> Unit,
    translationEnabled: Boolean = false,
    isTranslated: Boolean = false,
    onTranslateClicked: () -> Unit = {},
) {
    val colors = NovelaTheme.colors
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    var editingUrl by remember(toolbarTitle) { mutableStateOf(toolbarTitle) }
    var isFocused by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            Surface(
                color = colors.surfaceElevated,
                shadowElevation = 2.dp,
            ) {
                Column(modifier = Modifier.statusBarsPadding()) {
                    // Row 1: Close + Pill Omnibox + DONE CTA (when ready)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                    ) {
                        IconButton(
                            onClick = onBackClicked,
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close",
                                tint = colors.textPrimary,
                                modifier = Modifier.size(20.dp),
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(NovelaRadius.pill),
                            color = colors.surfaceMuted,
                            border = BorderStroke(
                                NovelaSize.hairline,
                                if (isFocused) colors.accent else colors.borderSubtle,
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp),
                            ) {
                                val isHttps = editingUrl.startsWith("https://", ignoreCase = true)
                                Icon(
                                    imageVector = if (isHttps) Icons.Rounded.Lock else Icons.Rounded.Public,
                                    contentDescription = null,
                                    tint = if (isHttps) colors.accent else colors.textMuted,
                                    modifier = Modifier.size(14.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                BasicTextField(
                                    value = editingUrl,
                                    onValueChange = { editingUrl = it },
                                    singleLine = true,
                                    cursorBrush = SolidColor(colors.accent),
                                    textStyle = MaterialTheme.typography.bodySmall.copy(
                                        color = colors.textPrimary,
                                    ),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                                    keyboardActions = KeyboardActions(
                                        onGo = {
                                            val url = editingUrl.trim().let {
                                                if (!it.startsWith("http")) "https://$it" else it
                                            }
                                            onNavigateToUrl(url)
                                            focusManager.clearFocus()
                                        }
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .onFocusChanged { isFocused = it.isFocused },
                                )
                            }
                        }

                        if (isReady) {
                            Button(
                                onClick = onDoneClicked,
                                shape = RoundedCornerShape(NovelaRadius.pill),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.accent,
                                    contentColor = colors.onAccent,
                                ),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                    horizontal = 12.dp,
                                    vertical = 0.dp,
                                ),
                                modifier = Modifier.height(36.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "DONE",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }

                    // Row 2: Browser Action Pills (Translate, Paste, Copy, Reload, Clear)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 8.dp),
                    ) {
                        BrowserActionChip(
                            icon = Icons.Rounded.Translate,
                            label = if (isTranslated) "Original" else "Translate",
                            onClick = onTranslateClicked,
                            enabled = translationEnabled,
                            active = isTranslated,
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        BrowserActionChip(
                            icon = Icons.Rounded.ContentPaste,
                            label = "Paste",
                            onClick = {
                                val clipboard =
                                    context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val pastedUrl = clipboard.primaryClip
                                    ?.getItemAt(0)
                                    ?.text
                                    ?.toString()
                                    ?.trim() ?: return@BrowserActionChip
                                val url =
                                    if (!pastedUrl.startsWith("http")) "https://$pastedUrl" else pastedUrl
                                editingUrl = url
                                onNavigateToUrl(url)
                            },
                        )
                        BrowserActionChip(
                            icon = Icons.Rounded.ContentCopy,
                            label = "Copy",
                            onClick = onCopyUrlClicked,
                        )
                        BrowserActionChip(
                            icon = Icons.Rounded.Refresh,
                            label = "Reload",
                            onClick = onReloadClicked,
                        )
                        BrowserActionChip(
                            icon = Icons.Rounded.DeleteOutline,
                            label = "Clear",
                            onClick = onClearCookiesClicked,
                        )
                    }

                    HorizontalDivider(
                        thickness = NovelaSize.hairline,
                        color = colors.borderSubtle,
                    )
                }
            }
        },
        content = { padding ->
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                factory = webViewFactory,
            )
        },
    )
}

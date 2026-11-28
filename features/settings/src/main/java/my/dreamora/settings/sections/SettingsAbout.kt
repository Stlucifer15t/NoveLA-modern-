package my.dreamora.settings.sections

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import my.dreamora.settings.ui.SettingsRow
import my.dreamora.coreui.theme.DreamoraSpacing
import my.dreamora.coreui.theme.textPadding
import my.dreamora.settings.R
import androidx.compose.ui.text.font.FontWeight

private const val REPO_URL = "https://github.com/Stlucifer15t/NoveLA-modern-"
private const val ISSUES_URL = "https://github.com/Stlucifer15t/NoveLA-modern-/issues"

@Composable
internal fun SettingsAbout(
    appVersion: String,
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val copiedMessage = stringResource(R.string.copied_to_clipboard)

    Column {
        Text(
            text = stringResource(R.string.about),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.textPadding().padding(bottom = DreamoraSpacing.sm),
            color = MaterialTheme.colorScheme.onSurface,
        )

        SettingsRow(
            headlineContent = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.version_format, appVersion),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    )
                }
            },
            leadingContent = {
                Icon(Icons.Outlined.Info, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            modifier = Modifier.textPadding().combinedClickable(
                onClick = {},
                onLongClick = {
                    clipboardManager.setText(AnnotatedString(appVersion))
                    Toast.makeText(context, copiedMessage, Toast.LENGTH_SHORT).show()
                }
            )
        )

        SettingsRow(
            headlineContent = {
                Text(text = stringResource(R.string.github_repository))
            },
            leadingContent = {
                Icon(Icons.Outlined.Code, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            modifier = Modifier.textPadding().clickable {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(REPO_URL)))
            }
        )

        SettingsRow(
            headlineContent = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.report_issue),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    )
                    Spacer(Modifier.width(DreamoraSpacing.sm))
                    Text(
                        text = stringResource(R.string.report_issue_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            leadingContent = {
                Icon(Icons.Outlined.NewReleases, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            modifier = Modifier.textPadding().clickable {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(ISSUES_URL)))
            }
        )
    }
}

package my.noveldokusha.settings.sections

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import my.noveldokusha.coreui.components.NovelaSettingsGroupCard
import my.noveldokusha.coreui.components.NovelaSettingsTile
import my.noveldokusha.settings.R

private const val REPO_URL = "https://github.com/HnDK0/NoveLA"
private const val ISSUES_URL = "https://github.com/HnDK0/NoveLA/issues"

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun SettingsAbout(
    appVersion: String,
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val copiedMessage = stringResource(R.string.copied_to_clipboard)

    NovelaSettingsGroupCard(title = stringResource(R.string.about)) {
        NovelaSettingsTile(
            title = stringResource(R.string.version_format, appVersion),
            icon = Icons.Outlined.Info,
            showDivider = true,
            modifier = Modifier.combinedClickable(
                onClick = {
                    clipboardManager.setText(AnnotatedString(appVersion))
                    Toast.makeText(context, copiedMessage, Toast.LENGTH_SHORT).show()
                },
                onLongClick = {
                    clipboardManager.setText(AnnotatedString(appVersion))
                    Toast.makeText(context, copiedMessage, Toast.LENGTH_SHORT).show()
                },
            ),
        )

        NovelaSettingsTile(
            title = stringResource(R.string.github_repository),
            subtitle = REPO_URL,
            icon = Icons.Outlined.Code,
            showChevron = true,
            showDivider = true,
            onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(REPO_URL)))
            },
        )

        NovelaSettingsTile(
            title = stringResource(R.string.report_issue),
            subtitle = stringResource(R.string.report_issue_description),
            icon = Icons.Outlined.NewReleases,
            showChevron = true,
            onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(ISSUES_URL)))
            },
        )
    }
}

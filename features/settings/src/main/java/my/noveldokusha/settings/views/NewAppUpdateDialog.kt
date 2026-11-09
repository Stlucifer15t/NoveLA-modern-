package my.noveldokusha.settings.views

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import my.noveldokusha.coreui.R as CoreUiR
import my.noveldokusha.coreui.components.ImageView
import my.noveldokusha.coreui.theme.NovelaRadius
import my.noveldokusha.coreui.theme.NovelaSize
import my.noveldokusha.coreui.theme.NovelaTheme
import my.noveldokusha.settings.R
import my.noveldokusha.settings.SettingsScreenState

@Composable
internal fun NewAppUpdateDialog(
    updateApp: SettingsScreenState.UpdateApp,
) {
    val newVersion = updateApp.showNewVersionDialog.value
    val context = LocalContext.current
    val colors = NovelaTheme.colors

    if (newVersion != null) {
        Dialog(onDismissRequest = { updateApp.showNewVersionDialog.value = null }) {
            Surface(
                shape = RoundedCornerShape(NovelaRadius.cardLarge),
                color = colors.surfaceElevated,
                border = BorderStroke(NovelaSize.hairline, colors.borderSubtle),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ImageView(
                        imageModel = CoreUiR.drawable.default_icon,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(14.dp)),
                        contentScale = ContentScale.Fit,
                    )
                    Text(
                        text = stringResource(
                            R.string.new_app_version_found_s,
                            newVersion.version.toString(),
                        ),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        textAlign = TextAlign.Center,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TextButton(
                            onClick = { updateApp.showNewVersionDialog.value = null },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                text = stringResource(R.string.cancel),
                                color = colors.textSecondary,
                            )
                        }
                        Button(
                            onClick = {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW).also {
                                        it.data = Uri.parse(newVersion.sourceUrl)
                                    }
                                )
                            },
                            shape = RoundedCornerShape(NovelaRadius.pill),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.accent,
                                contentColor = colors.onAccent,
                            ),
                            modifier = Modifier.weight(1.4f),
                        ) {
                            Text(
                                text = stringResource(R.string.download),
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }
    }
}

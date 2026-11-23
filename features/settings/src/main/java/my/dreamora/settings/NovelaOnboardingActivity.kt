package my.dreamora.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import my.dreamora.core.appPreferences.AppPreferences
import my.dreamora.coreui.BaseActivity
import androidx.compose.material.icons.outlined.Check
import my.dreamora.coreui.components.NovelaFilterChip
import my.dreamora.coreui.theme.NovelaSpacing
import my.dreamora.coreui.theme.NovelaType
import my.dreamora.coreui.theme.Theme
import my.dreamora.coreui.theme.colorAccent
import my.dreamora.core.foryou.NovelaGenre

/**
 * The first-open genre sheet. One screen, one question, and both exits are valid: "Skip" is
 * a real answer, not a dead end — the For You rail then reads plain catalog pages, and its
 * button brings this screen back any time.
 *
 * An Activity rather than a dialog because Home has no dialog host of its own, and
 * MainActivity starts it once, guarded by the preference this screen writes on the way out.
 */
@AndroidEntryPoint
class NovelaOnboardingActivity : BaseActivity() {

    @Inject
    lateinit var appPreferences: AppPreferences

    companion object {
        fun createIntent(context: Context): Intent =
            Intent(context, NovelaOnboardingActivity::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initial = appPreferences.FOR_YOU_GENRES.value.toList()
        setContent {
            Theme(themeProvider = themeProvider) {
                NovelaOnboardingScreen(
                    initialSlugs = initial,
                    onContinue = { slugs ->
                        appPreferences.FOR_YOU_GENRES.value = slugs.toSet()
                        appPreferences.FOR_YOU_GENRES_PICKED.value = true
                        finish()
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NovelaOnboardingScreen(
    initialSlugs: List<String>,
    onContinue: (List<String>) -> Unit,
) {
    val palette = MaterialTheme.colorScheme
    var selected by remember { mutableStateOf(initialSlugs.toSet()) }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Column(modifier = Modifier.statusBarsPadding()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = NovelaSpacing.xs,
                            end = NovelaSpacing.screenH,
                            top = NovelaSpacing.sm,
                            bottom = NovelaSpacing.xs,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { onContinue(selected.toList()) }, modifier = Modifier.size(40.dp)) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(R.string.onboarding_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            text = stringResource(R.string.onboarding_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        bottomBar = {
            Button(
                onClick = { onContinue(selected.toList()) },
                modifier = Modifier
                    .fillMaxWidth()
                    .systemBarsPadding()
                    .padding(horizontal = NovelaSpacing.screenH)
                    .padding(bottom = NovelaSpacing.lg),
            ) {
                Text(
                    text = stringResource(
                        if (selected.isEmpty()) R.string.onboarding_skip else R.string.onboarding_continue
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = NovelaSpacing.screenH),
            verticalArrangement = Arrangement.spacedBy(NovelaSpacing.md),
        ) {
            Spacer(modifier = Modifier.height(NovelaSpacing.xs))
            Text(
                text = stringResource(R.string.onboarding_headline),
                style = MaterialTheme.typography.headlineSmall,
                color = palette.onBackground,
            )
            Text(
                text = stringResource(R.string.onboarding_genres),
                style = NovelaType.sectionAction,
                color = colorAccent(),
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(NovelaSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(NovelaSpacing.sm),
            ) {
                NovelaGenre.entries.forEach { genre ->
                    NovelaFilterChip(
                        label = stringResource(genre.labelRes),
                        selected = genre.slug in selected,
                        leadingIcon = if (genre.slug in selected) Icons.Outlined.Check else null,
                        onClick = {
                            selected = if (genre.slug in selected) selected - genre.slug
                            else selected + genre.slug
                        },
                    )
                }
            }
            Text(
                text = stringResource(R.string.onboarding_footer),
                style = MaterialTheme.typography.bodySmall,
                color = palette.onSurfaceVariant,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(NovelaSpacing.huge))
        }
    }
}

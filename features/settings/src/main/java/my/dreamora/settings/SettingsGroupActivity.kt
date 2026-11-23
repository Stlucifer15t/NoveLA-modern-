package my.dreamora.settings

import my.dreamora.settings.ui.hint
import my.dreamora.settings.ui.label
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import my.dreamora.coreui.BaseActivity
import my.dreamora.coreui.theme.NovelaSpacing
import my.dreamora.coreui.theme.Theme
import my.dreamora.navigation.NavigationRoutes
import my.dreamora.settings.ui.SettingsGroup
import my.dreamora.strings.R

/**
 * One settings group, as its own screen.
 *
 * The Settings tab lists groups as boxes; tapping one opens this activity with the
 * group's key. It renders the very same `SettingsScreenBody`, filtered to that group and
 * expanded — so a setting cannot exist on one level and be missing on the other, and the
 * two screens share the view model's behaviour by construction (each activity owns its
 * instance, backed by the same DataStore/preferences).
 */
@AndroidEntryPoint
class SettingsGroupActivity : BaseActivity() {

    class IntentData : Intent {
        constructor(context: Context, groupKey: String) : super(
            context,
            SettingsGroupActivity::class.java,
        ) {
            putExtra(EXTRA_GROUP, groupKey)
        }
    }

    @Inject
    lateinit var navigationRoutes: NavigationRoutes

    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val group = SettingsGroup.fromKey(intent.getStringExtra(EXTRA_GROUP))
        if (group == null) {
            // A stale/unknown key (old caller, restored intent after a refactor) must not
            // show an empty screen: close and let the caller's list stand.
            finish()
            return
        }
        setContent {
            Theme(themeProvider = themeProvider) {
                Screen(
                    group = group,
                    viewModel = viewModel,
                    navigationRoutes = navigationRoutes,
                    onNavigateBack = { finish() },
                )
            }
        }
    }

    companion object {
        private const val EXTRA_GROUP = "settingsGroup"
    }
}

@Composable
private fun Screen(
    group: SettingsGroup,
    viewModel: SettingsViewModel,
    navigationRoutes: NavigationRoutes,
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    val directoryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
            onResult = { uri ->
                if (uri != null) {
                    val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    context.contentResolver.takePersistableUriPermission(uri, flags)
                    viewModel.onAutoBackupDirectoryUriChange(uri.toString())
                }
            },
        )

    Scaffold(
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
                    IconButton(onClick = onNavigateBack, modifier = Modifier.size(40.dp)) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.title_settings),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    // The name and the hint are on the hero below; the bar only needs a way back.
                }
            }
        },
        content = { innerPadding ->
            SettingsScreenBody(
                state = viewModel.state,
                modifier = Modifier.padding(innerPadding),
                groups = setOf(group),
                reading = viewModel.reading,
                layout = viewModel.layout,
                onRefreshSizes = viewModel::refreshSizes,
                onAppThemeSelected = viewModel::onAppThemeChange,
                onDarkModeSelected = viewModel::onDarkModeChange,
                onRequestCleanDatabase = viewModel::requestCleanDatabase,
                onRequestCleanImageFolder = viewModel::requestCleanImageFolder,
                onRequestCleanNovelCache = viewModel::requestCleanNovelCache,
                onRequestCleanMangaCache = viewModel::requestCleanMangaCache,
                onRequestCleanOfflineVideo = viewModel::requestCleanOfflineVideo,
                onConfirmClean = viewModel::confirmCleanAction,
                onDismissClean = viewModel::dismissCleanAction,
                onMassAddDelayChange = viewModel::onMassAddDelayChange,
                onDownloadDelayChange = viewModel::onDownloadDelayChange,
                onBackupData = my.dreamora.tooling.backup_create.onBackupCreate(),
                onRestoreData = my.dreamora.tooling.backup_restore.onBackupRestore(),
                onCheckForUpdatesManual = viewModel::onCheckForUpdatesManual,
                onGeminiApiKeyChange = viewModel::onGeminiApiKeyChange,
                onGeminiModelChange = viewModel::onGeminiModelChange,
                onTranslationProviderChange = viewModel::onTranslationProviderChange,
                onTranslationGlobalModeChange = viewModel::onTranslationGlobalModeChange,
                onGooglePaApiKeysChange = viewModel::onGooglePaApiKeysChange,
                onOpenAiBaseUrlChange = viewModel::onOpenAiBaseUrlChange,
                onOpenAiApiKeysChange = viewModel::onOpenAiApiKeysChange,
                onOpenAiModelChange = viewModel::onOpenAiModelChange,
                onActiveSystemPromptChange = viewModel::onActiveSystemPromptChange,
                onPromptUseEnglishLocaleChange = viewModel::onPromptUseEnglishLocaleChange,
                onSavePreset = viewModel::onSavePromptPreset,
                onDeletePreset = viewModel::onDeletePromptPreset,
                onLlmBatchSizeChange = viewModel::onLlmBatchSizeChange,
                onLlmMaxOutputTokensChange = viewModel::onLlmMaxOutputTokensChange,
                onLanguageChange = viewModel::onLanguageChange,
                onNavigateToRegexCleanup = {
                    context.startActivity(navigationRoutes.regexRules(context, null))
                },
                onDeleteNovelPrompt = viewModel::onDeleteNovelPrompt,
                onAutoBackupSelectDirectory = { directoryPicker.launch(null) },
                onAutoBackupMaxCountChange = viewModel::onAutoBackupMaxCountChange,
                onAutoBackupIntervalMinutesChange = viewModel::onAutoBackupIntervalMinutesChange,
                onAutoBackupEnabledChange = viewModel::onAutoBackupEnabledChange,
                onAutoBackupIncludeImagesChange = viewModel::onAutoBackupIncludeImagesChange,
                onAutoBackupIncludeSettingsChange = viewModel::onAutoBackupIncludeSettingsChange,
                onAutoBackupIncludePluginsChange = viewModel::onAutoBackupIncludePluginsChange,
                onExportLogs = viewModel::exportLogs,
            )
        },
    )
}

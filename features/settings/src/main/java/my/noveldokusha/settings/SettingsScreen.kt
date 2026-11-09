package my.noveldokusha.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import my.noveldokusha.coreui.components.NovelaScreenHeader
import my.noveldokusha.coreui.theme.LocalAppTheme
import my.noveldokusha.coreui.theme.LocalIsDark
import my.noveldokusha.coreui.theme.NovelaTheme
import my.noveldokusha.navigation.NavigationRoutes
import my.noveldokusha.tooling.backup_create.onBackupCreate
import my.noveldokusha.tooling.backup_restore.onBackupRestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navigationRoutes: NavigationRoutes,
    onRestartApp: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val viewModel: SettingsViewModel = viewModel()
    viewModel.onRestartApp = onRestartApp

    // Auto backup directory picker
    val directoryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = { uri: Uri? ->
            if (uri != null) {
                val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, flags)
                viewModel.onAutoBackupDirectoryUriChange(uri.toString())
            }
        }
    )

    val appTheme = LocalAppTheme.current
    val isDark = LocalIsDark.current

    key(appTheme, isDark) {
        val colors = NovelaTheme.colors
        Scaffold(
            containerColor = colors.background,
            topBar = {
                NovelaScreenHeader(
                    title = stringResource(id = R.string.title_settings),
                )
            },
            content = { innerPadding ->
                SettingsScreenBody(
                    state = viewModel.state,
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
                    onBackupData = onBackupCreate(),
                    onRestoreData = onBackupRestore(),
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
                    onAutoBackupSelectDirectory = {
                        directoryPicker.launch(null)
                    },
                    onAutoBackupMaxCountChange = viewModel::onAutoBackupMaxCountChange,
                    onAutoBackupIntervalMinutesChange = viewModel::onAutoBackupIntervalMinutesChange,
                    onAutoBackupEnabledChange = viewModel::onAutoBackupEnabledChange,
                    onAutoBackupIncludeImagesChange = viewModel::onAutoBackupIncludeImagesChange,
                    onAutoBackupIncludeSettingsChange = viewModel::onAutoBackupIncludeSettingsChange,
                    onAutoBackupIncludePluginsChange = viewModel::onAutoBackupIncludePluginsChange,
                    onDeleteNovelPrompt = viewModel::onDeleteNovelPrompt,
                    onExportLogs = viewModel::exportLogs,
                    modifier = Modifier.padding(innerPadding),
                )
            }
        )
    }
}

package my.dreamora.extensions

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint
import my.dreamora.coreui.BaseActivity
import my.dreamora.coreui.theme.Theme

/**
 * The third-party repository manager as its own screen.
 *
 * It used to be an AlertDialog opened from the Extensions list, which put a few hundred Legado
 * or Mihon sources into a 520dp box with a scroller inside a scroller, and clipped every
 * source name to one 12sp line — a Chinese bookSourceName turned into three characters and an
 * ellipsis. An Activity, because the list has its own state (subscriptions, a selected catalog,
 * per-entry install progress) that is worth keeping while the reader navigates away and back,
 * and because the extensions surface this opens from is itself embedded in a tab, so there is
 * no dialog host here to own it.
 */
@AndroidEntryPoint
class CompatRepositoriesActivity : BaseActivity() {

    companion object {
        fun createIntent(context: Context): Intent =
            Intent(context, CompatRepositoriesActivity::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Theme(themeProvider = themeProvider) {
                NovelaCompatRepositoriesScreen(onBack = { finish() })
            }
        }
    }
}

package app.nooklings

import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.nooklings.data.Player
import app.nooklings.data.Settings
import app.nooklings.ui.NookRoot
import app.nooklings.ui.theme.NookTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val t = android.graphics.Color.TRANSPARENT
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(t), navigationBarStyle = SystemBarStyle.dark(t))
        setContent {
            val settings: Settings? by container.settings.settings.collectAsStateWithLifecycle(initialValue = null)
            val seeding by container.seeding.collectAsStateWithLifecycle()
            // Wait for settings (and the demo pantry) so the welcome doesn't flash for someone already here.
            if (!seeding) settings?.let { s -> NookTheme { NookRoot(s) } }
        }
    }

    override fun onStop() {
        super.onStop()
        if (isFinishing) Player.stop()
    }
}

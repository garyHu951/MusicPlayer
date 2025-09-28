package ntou.android2025.musicplayeractivity

import android.media.MediaPlayer
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class MainActivity : ComponentActivity() {
    private var player: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MusicPlayerApp(
                context = this,
                playerProvider = { player },
                onPlayerUpdate = { player = it }
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
    }
}

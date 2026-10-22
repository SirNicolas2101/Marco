package com.marco.vinyl

import android.app.Application
import com.marco.vinyl.data.demo.DemoMusicRepository
import com.marco.vinyl.model.MusicRepository
import com.marco.vinyl.playback.PlayerController

/** Einfache manuelle Dependency Injection. Für SoundCloud wird hier später das Repository getauscht. */
class AppContainer(app: Application) {
    val repository: MusicRepository = DemoMusicRepository(app.cacheDir)
    val player = PlayerController(app)
}

class VinylApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

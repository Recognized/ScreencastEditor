package vladsaif.syncedit.plugin.audioplayer

import javafx.application.Application
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import javafx.stage.Stage
import java.io.File

class AudioPlayer {

}

class App : Application() {
    override fun start(primaryStage: Stage?) {
        val bip = "nocturne.mp3"
        val hit = Media(File(bip).toURI().toString())
        val mediaPlayer = MediaPlayer(hit)
        println("started")
        mediaPlayer.play()
        println("stopped")
    }
}

fun main(args: Array<String>) {
    Application.launch(App::class.java)
}
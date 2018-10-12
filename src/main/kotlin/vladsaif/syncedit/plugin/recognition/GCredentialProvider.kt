package vladsaif.syncedit.plugin.recognition

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.diagnostic.logger
import vladsaif.syncedit.plugin.LibrariesLoader
import java.nio.file.Path

@State(name = "SpeechToTextKeyPath", storages = [Storage("speechToTextKeyPath.xml")])
class GCredentialProvider : PersistentStateComponent<GCredentialProvider> {
  var gSettings: Path? = null
    private set

  @Synchronized
  fun setGCredentialsFile(file: Path) {
    LOG.info("Setting credentials: $file")
    LibrariesLoader.checkCredentials(file)
    gSettings = file
  }

  override fun loadState(state: GCredentialProvider) {
    LOG.info("Loading state: $state")
    Instance = state
    state.gSettings?.let {
      Instance.gSettings = it
    }
    LibrariesLoader.releaseClassloader()
  }

  override fun getState() = Instance

  override fun toString(): String {
    return "GCredentialProvider(gSettings=$gSettings)"
  }

  companion object {
    private val LOG = logger<GCredentialProvider>()
    var Instance = GCredentialProvider()
      private set
  }
}
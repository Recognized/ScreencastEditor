package vladsaif.syncedit.plugin.recognition

import com.intellij.openapi.components.PersistentStateComponent
import vladsaif.syncedit.plugin.LibrariesLoader
import java.nio.file.Path

class CredentialProvider private constructor() : PersistentStateComponent<CredentialProvider> {
  var gSettings: Path? = null
    private set

  @Synchronized
  fun setGCredentialsFile(file: Path) {
    LibrariesLoader.checkCredentials(file)
    gSettings = file
  }

  override fun loadState(state: CredentialProvider) {
    Instance = state
    println("Loaded state: $state")
    state.gSettings?.let {
      Instance.setGCredentialsFile(it)
    }
    LibrariesLoader.releaseClassloader()
  }

  override fun getState() = Instance

  override fun toString(): String {
    return "CredentialProvider(gSettings=$gSettings)"
  }

  companion object {
    var Instance = CredentialProvider()
      private set
  }
}
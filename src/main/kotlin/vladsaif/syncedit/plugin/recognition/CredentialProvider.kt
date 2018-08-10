package vladsaif.syncedit.plugin.recognition

import com.intellij.openapi.components.PersistentStateComponent
import vladsaif.syncedit.plugin.LibrariesLoader
import java.nio.file.Path

class CredentialProvider private constructor() : PersistentStateComponent<CredentialProvider> {
  var gSettings: Path? = null
    private set

  fun setGCredentialsFile(file: Path) {
    LibrariesLoader.checkCredentials(file)
    gSettings = file
  }

  override fun loadState(state: CredentialProvider) {
    Instance = state
    state.gSettings?.let {
      Instance.setGCredentialsFile(it)
    }
  }

  override fun getState() = Instance

  companion object {
    var Instance = CredentialProvider()
      private set
  }
}
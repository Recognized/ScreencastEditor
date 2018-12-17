package com.github.recognized.screencast.editor.recognition

import com.github.recognized.screencast.editor.util.LibrariesLoader
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Storage
import com.intellij.openapi.diagnostic.logger
import com.intellij.util.io.readText
import java.io.ByteArrayInputStream
import java.nio.file.Path

object CredentialsProvider {
  private val LOG = logger<CredentialsProvider>()
  private var STATE = CredentialsProvider.State()

  @Synchronized
  fun getCredentials(): String? = STATE.myCredentials

  val isCredentialsSet: Boolean
    @Synchronized
    get() = STATE.myCredentials != null

  @Synchronized
  fun setCredentials(path: Path) {
    setCredentials(path.readText())
  }

  @Synchronized
  fun setCredentials(key: String) {
    LOG.info("Setting credentials")
    LibrariesLoader.checkCredentials(ByteArrayInputStream(key.toByteArray(Charsets.UTF_8)))
    LibrariesLoader.releaseClassloader()
    STATE = State(key)
  }

  override fun toString(): String {
    return "CredentialsProvider(credentialsSet=$isCredentialsSet)"
  }

  @com.intellij.openapi.components.State(
    name = "googleCredentials.xml",
    storages = [Storage("googleCredentialsPath.xml")]
  )
  private class State(key: String? = null) : PersistentStateComponent<CredentialsProvider.State> {
    val myCredentials: String? = key

    override fun getState(): CredentialsProvider.State {
      synchronized(CredentialsProvider) {
        // TODO: do not keep secure data, but help somehow
        return State()
      }
    }

    override fun loadState(state: CredentialsProvider.State) {
      synchronized(CredentialsProvider) {
        LOG.info("Loading state: $state")
        setCredentials(state.myCredentials ?: return)
      }
    }
  }
}
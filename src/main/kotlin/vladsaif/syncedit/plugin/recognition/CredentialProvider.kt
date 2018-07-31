package vladsaif.syncedit.plugin.recognition

import com.google.api.gax.core.FixedCredentialsProvider
import com.google.auth.oauth2.ServiceAccountCredentials
import com.google.cloud.speech.v1p1beta1.SpeechSettings
import com.intellij.openapi.components.PersistentStateComponent
import java.nio.file.Files
import java.nio.file.Path

class CredentialProvider private constructor() : PersistentStateComponent<CredentialProvider> {
    var gSettings: SpeechSettings? = null
        private set

    fun setGCredentialsFile(file: Path) {
        Files.newInputStream(file).use {
            gSettings = SpeechSettings.newBuilder()
                    .setCredentialsProvider(FixedCredentialsProvider.create(ServiceAccountCredentials.fromStream(it)))
                    .build()
        }
    }

    override fun loadState(state: CredentialProvider) {
        Instance = state
    }

    override fun getState() = Instance

    companion object {
        var Instance = CredentialProvider()
            private set
    }
}
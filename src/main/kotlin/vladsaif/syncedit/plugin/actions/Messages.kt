package vladsaif.syncedit.plugin.actions

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import vladsaif.syncedit.plugin.model.Screencast
import java.nio.file.Path

const val GROUP_ID = "Screencast Editor"

fun showNotification(
  content: String,
  title: String = "Error",
  type: NotificationType = NotificationType.ERROR
) {
  Notifications.Bus.notify(Notification(GROUP_ID, title, content, type))
}

fun notifySuccessfullySaved(screencast: Screencast) {
  Notification(
    GROUP_ID,
    "Saved",
    "Successfully saved ${screencast.name}",
    NotificationType.INFORMATION
  ).notify(screencast.project)
}

fun notifyCannotReadImportedAudio(project: Project, vararg paths: Path?) {
  Notification(
    GROUP_ID,
    "Cannot read imported audio",
    "Imported audio cannot be read in following paths: (${paths.filterNotNull()
      .map { it.toAbsolutePath() }
      .distinct()
      .joinToString(separator = ",")})",
    NotificationType.WARNING
  ).notify(project)
}

fun errorWhileSaving(screencast: Screencast, throwable: Throwable) {
  Notification(
    GROUP_ID,
    "Not saved",
    "Error occurred while saving ${screencast.name}: ${throwable.message}",
    NotificationType.ERROR
  ).notify(screencast.project)
}

fun errorScriptContainsErrors(screencast: Screencast) {
  Notification(
    GROUP_ID,
    "Script contains errors",
    "Script of ${screencast.file} is malformed",
    NotificationType.ERROR
  ).notify(screencast.project)
}


fun errorUnsupportedAudioFile(project: Project, path: Path) {
  Messages.showErrorDialog(
    project,
    "Audio file format is not supported. File: $path",
    "Unsupported file format"
  )
}

fun errorIO(project: Project?, message: String?) {
  Messages.showErrorDialog(
    project,
    "I/O error occurred. ${message ?: ""}",
    "I/O error"
  )
}

fun errorRequirementsNotSatisfied(project: Project, ex: Exception) {
  Messages.showWarningDialog(
    project,
    ex.message ?: "Unknown error.",
    "Requirements not satisfied"
  )
}

fun infoCredentialsOK(project: Project?, file: VirtualFile) {
  Notification(
    GROUP_ID,
    "Credentials",
    "Credentials are successfully installed: \"${file.path}\"",
    NotificationType.INFORMATION
  ).notify(project)
}

fun errorCredentials(project: Project?, file: VirtualFile) {
  Messages.showErrorDialog(
    project,
    "Not valid credentials file: \"${file.path}\"",
    "Corrupted credentials"
  )
}

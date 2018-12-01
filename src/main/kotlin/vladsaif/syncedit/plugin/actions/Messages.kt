package vladsaif.syncedit.plugin.actions

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import vladsaif.syncedit.plugin.model.Screencast
import java.nio.file.Path

fun showNotification(
  content: String,
  title: String = "Error",
  type: NotificationType = NotificationType.ERROR
) {
  Notifications.Bus.notify(Notification("Screencast Editor", title, content, type))
}

fun notifySuccessfullySaved(screencast: Screencast) {
  Notification(
    "Screencast Editor",
    "Saved",
    "Successfully saved ${screencast.name}",
    NotificationType.INFORMATION
  ).notify(screencast.project)
}

fun errorWhileSaving(screencast: Screencast, throwable: Throwable) {
  Notification(
    "Screencast Editor",
    "Not saved",
    "Error occurred while saving ${screencast.name}: ${throwable.message}",
    NotificationType.ERROR
  ).notify(screencast.project)
}

fun errorScriptContainsErrors(screencast: Screencast) {
  Notification(
    "Screencast Editor",
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
    "Screencast Editor",
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

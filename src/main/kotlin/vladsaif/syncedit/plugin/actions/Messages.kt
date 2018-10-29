package vladsaif.syncedit.plugin.actions

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import vladsaif.syncedit.plugin.model.ScreencastFile
import java.nio.file.Path

fun notifySuccessfullySaved(screencastFile: ScreencastFile) {
  Notification(
    "Screencast Editor",
    "Saved",
    "Successfully saved ${screencastFile.name}",
    NotificationType.INFORMATION
  ).notify(screencastFile.project)
}

fun errorWhileSaving(screencastFile: ScreencastFile, throwable: Throwable) {
  Notification(
    "Screencast Editor",
    "Not saved",
    "Error occurred while saving ${screencastFile.name}: ${throwable.message}",
    NotificationType.ERROR
  ).notify(screencastFile.project)
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

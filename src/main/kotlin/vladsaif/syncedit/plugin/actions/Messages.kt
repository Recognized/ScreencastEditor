package vladsaif.syncedit.plugin.actions

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import java.io.IOException

fun errorNoModelForFile(project: Project, file: VirtualFile) {
  Messages.showErrorDialog(
      project,
      "File (\"$file\") is not bound with anything.",
      "No associations found"
  )
}

fun errorNoTranscriptBound(project: Project, file: VirtualFile) {
  Messages.showErrorDialog(
      project,
      "File (\"$file\") is not bound with any transcript.",
      "No transcript bound"
  )
}

fun errorNoScriptBound(project: Project, file: VirtualFile) {
  Messages.showErrorDialog(
      project,
      "File (\"$file\") is not bound with any script.",
      "No script bound"
  )
}

fun errorAlreadyBoundToDifferent(project: Project) {
  Messages.showErrorDialog(
      project,
      "Cannot bind files already bound to other files.",
      "Bind error"
  )
}

fun infoAlreadyBound(project: Project, script: VirtualFile, audio: VirtualFile) {
  Messages.showInfoMessage(
      project,
      "Audio: \"${audio.path}\" \n" +
          "Script: \"${script.path}\" \n" +
          "are already bound.",
      "Bind audio and script"
  )
}

fun notifySuccessfullyBound(project: Project, script: VirtualFile, audio: VirtualFile) {
  Notification(
      "Screencast Editor",
      "Audio and script are bound",
      "Successfully bound\n" +
          "Audio: \"${audio.path}\" \n" +
          "Script: \"${script.path}\" \n",
      NotificationType.INFORMATION
  ).notify(project)
}

fun errorUnsupportedAudioFile(project: Project, audio: VirtualFile) {
  Messages.showErrorDialog(
      project,
      "Audio file format is not supported. File: ${audio.path}",
      "Unsupported file format"
  )
}

fun errorIO(project: Project, ex: IOException) {
  Messages.showErrorDialog(
      project,
      "I/O error occurred. ${ex.message}",
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
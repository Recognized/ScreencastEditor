# <img src="https://raw.githubusercontent.com/Recognized/ScreencastEditor/master/demo/ScreencastLogo.png" alt="drawing" width="35m"/> ScreencastEditor
Intellij IDEA plugin for recording and editing IDE screencasts.

[Test GUI Framework](https://plugins.jetbrains.com/plugin/11114-test-gui-framework) plugin is required to be installed.
<hr>

## Overview

**Screencast** is a zip (but with **.scs** extension) of:
1. UI automation script
2. Recorded audio (*)
3. Imported audio (*) 
4. Transcript of zipped audio (*)

_(\*) - optional_

*ScreencastEditor* allows user to record their actions (in form of UI automation script marked with time offsets which 
show delay between actions) inside IDE along with recording sound from microphone, then user can edit screencast and 
reproduce it.

## Features

✅ Built-in audio editor and player. (Currently supports WAV and MP3 formats).

✅ Transcript based mode of audio editing.

✅ If transcript is absent it can be obtained by transcribing recorded speech using external speech recognition service.
(Now, only Google Speech-to-text API is supported).

✅ Visual editor of script's actions' timings.

✅ Script can be edited as regular Kotlin file without time offsets, *ScreencastEditor* will automatically try to guess 
missing offsets.


![editor1](https://raw.githubusercontent.com/Recognized/ScreencastEditor/master/demo/editor1.PNG)


## Usage
<hr>

### Recording

User interactions with IDE are recorded using [Test GUI Framework](https://plugins.jetbrains.com/plugin/11114-test-gui-framework)'s functionality.
Sound is recorded using java.sound package.

Current controls:
- Start: _Tools_ menu → <img src="https://raw.githubusercontent.com/Recognized/ScreencastEditor/master/demo/ScreencastLogo.png" alt="drawing" width="16"/> _Screencast Editor_ →  _Start recording_
- Stop: _Tools_ menu → <img src="https://raw.githubusercontent.com/Recognized/ScreencastEditor/master/demo/ScreencastLogo.png" alt="drawing" width="16"/> _Screencast Editor_ → _Stop recording_

After recording has been finished, you can san save or discard recorded screencast.

### Editing screencast

**Open screencast**:
1. _Tools_ menu → <img src="https://raw.githubusercontent.com/Recognized/ScreencastEditor/master/demo/ScreencastLogo.png" alt="drawing" width="16"/> _Screencast Editor_ → <img src="https://raw.githubusercontent.com/Recognized/ScreencastEditor/master/demo/ScreencastLogo.png" alt="drawing" width="16"/> _Open Screencast_
2. Right click on file in Project Tree → <img src="https://raw.githubusercontent.com/Recognized/ScreencastEditor/master/demo/ScreencastLogo.png" alt="drawing" width="16"/> _Open Screencast_

**Editor**:

- <img src="https://raw.githubusercontent.com/JetBrains/intellij-community/master/platform/icons/src/actions/undo.svg?sanitize=true" width="16" height="16"/> Undo. (Standard **Undo** shortcut)
- <img src="https://raw.githubusercontent.com/JetBrains/intellij-community/master/platform/icons/src/actions/redo.svg?sanitize=true" width="16" height="16"/> Redo. (Standard **Redo** shortcut)
- <img src="https://raw.githubusercontent.com/Recognized/ScreencastEditor/master/resources/icons/transcript@2x.png" alt="transcript" width="16"/> Open transcript. Start recognition if transcript is not yet known.
- <img src="https://raw.githubusercontent.com/JetBrains/kotlin/1.2.70/idea/resources/org/jetbrains/kotlin/idea/icons/kotlin_script%402x.png" alt="transcript" width="16"/> Open UI script.
- <img src="https://raw.githubusercontent.com/JetBrains/intellij-community/master/platform/icons/src/toolbarDecorator/import.svg?sanitize=true" width="16"/> Import audio. (It will be played instead of audio recorded by plugin during screencast replay).
- <img src="https://raw.githubusercontent.com/JetBrains/intellij-community/master/platform/icons/src/actions/menu-saveall.svg?sanitize=true" width="16"/> Save changes made to this screencast.

<hr/>

- <img src="https://raw.githubusercontent.com/Recognized/ScreencastEditor/master/resources/icons/play@2x.png" alt="play pause" width="16" height="16"/> Play whole audio file or selected range only. (**Control**+**P**)
- <img src="https://raw.githubusercontent.com/Recognized/ScreencastEditor/master/resources/icons/stop@2x.png" alt="stop" width="16" height="16"/> Stop playing audio. (**Control**+**Alt**+**P**)
- <img src="https://raw.githubusercontent.com/Recognized/ScreencastEditor/master/resources/icons/delete@2x.png" alt="cut" width="16" height="16"/> Cut selected range. (Standard **Cut** shortcut)
- <img src="https://raw.githubusercontent.com/Recognized/ScreencastEditor/master/resources/icons/volume_on@2x.png" alt="unmute" width="16" height="16"/> Unmute selected range.
- <img src="https://raw.githubusercontent.com/Recognized/ScreencastEditor/master/resources/icons/volume_off@2x.png" alt="mute" width="16" height="16"/> Mute selected range. (**Control**+**M**)
- <img src="https://raw.githubusercontent.com/JetBrains/intellij-community/master/platform/icons/src/graph/zoomIn.svg?sanitize=true" width="16" height="16"/> Zoom in. (**Control**+**Plus**)
- <img src="https://raw.githubusercontent.com/JetBrains/intellij-community/master/platform/icons/src/graph/zoomOut.svg?sanitize=true" width="16" height="16"/> Zoom out. (**Control**+**Minus**)
- <img src="https://raw.githubusercontent.com/JetBrains/intellij-community/master/platform/icons/src/general/arrowSplitCenterH.svg?sanitize=true" width="16" height="16"/> Toggle drag mode. (Drag whole audio or script along time axis).

<hr/>

- Hold **Control** and drag with **Left Mouse Button** or click to select audio range by words.
- Hold **Shift** and drag **Left Mouse Button** to select range precisely (not by words).
- Drag word's or script's borders with **Left Mouse Button** to change their time range.

Transcript in editor can be edited via refactoring actions:

- Exclude: **Control**+**Alt**+**E**
- Mute: **Control**+**Alt**+**M**
- Include: **Control**+**Alt**+**I**
- Concatenate (selected words): **Control**+**Alt**+**C**

### Recognition

In order to use speech recognition you need to obtain key to Service Account in Google Cloud Platform.

Visit this [page](https://cloud.google.com/speech-to-text/docs/quickstart-client-libraries) and do the first step 
(_Set up a GCP Console Project, create service account and download private JSON key_).

Before using recognition in plugin you will need to set this key.

**Setting key**:
_Tools_ menu → _Screencast Editor_ → _Google Speech Kit_ → _Set credentials_ (choose downloaded JSON key)

Service account must have rights to access Speech-to-Text API and to create files in Google Cloud Storage 
(in order to temporarily store long audio files).

You may manage accounts rights on this [page](https://cloud.google.com/iam/docs/granting-roles-to-service-accounts).



# <img src="https://raw.githubusercontent.com/Recognized/ScreencastEditor/master/demo/ScreencastLogo.png" alt="drawing" width="35m"/> ScreencastEditor

Intellij IDEA plugin for recording and editing IDE screencasts.

**Screencast** is a set of:
1. Recorded speech (optional)
2. Transcript of this speech (optional)
3. UI automation script

UI automation script is a Kotlin DSL script that contains information about user actions within IDE
(such as clicking on buttons, typing, invoking actions, etc.)

Usually, it looks like this:
```kotlin
ideFrame {
    invokeAction("com.intellij.ide.actions.CutAction")
    editor {
        typeText("Type some text")
    }
    toolsMenu {
        item("ScreencastEditor").click()
        chooseFile {
            button("Ok").click()
        }
    }
    //...
}
```

If transcript is absent it can be obtained by transcribing recorded speech (if it is present).
using external speech recognition service. Now, only Google Speech-to-text API is supported. (See section below)

## Features

This plugin has simple built-in audio editor. 
![audio-editor1](https://raw.githubusercontent.com/Recognized/ScreencastEditor/master/demo/audio_editor1.gif)

In order to maintain synchronization between audio and script during editing, 
plugin uses bindings between words in audio and statements/blocks of code in script.
Plugin allows to manually set them or use automatically generated ones.

![bindings-editor](https://raw.githubusercontent.com/Recognized/ScreencastEditor/master/demo/bindings1.png)

Modifications to the one part of binding are automatically applied to the other part.

Using this script and recorded audio we can reproduce screencast in IDE.

## Usage
<hr>

### Recording

- Start: **Control**+**Alt**+**Meta**+**R**
- Pause: **Control**+**Alt**+**Meta**+**P**
- Stop: **Control**+**Alt**+**Meta**+**S**

### Editing screencast

**Open screencast**:
1. _Tools_ menu → <img src="https://raw.githubusercontent.com/Recognized/ScreencastEditor/master/demo/ScreencastLogo.png" alt="drawing" width="16"/> _Screencast Editor_ → <img src="https://raw.githubusercontent.com/Recognized/ScreencastEditor/master/demo/ScreencastLogo.png" alt="drawing" width="16"/> _Open Screencast_
2. Right click on file in Project Tree → <img src="https://raw.githubusercontent.com/Recognized/ScreencastEditor/master/demo/ScreencastLogo.png" alt="drawing" width="16"/> _Open Screencast_

**Audio editor structure**:

- Use **Left Mouse Button** to select audio range by words.
- Hold **Control/Meta** and drag mouse with pressed **Left Mouse Button** 
to select range precisely (not by words). 
- Hold **Shift** to prevent resetting current selection.
- You may also drag word borders holding **Right Mouse Button**.

![audio-editor2](https://raw.githubusercontent.com/Recognized/ScreencastEditor/master/demo/audio_editor2.png)

* <img src="https://raw.githubusercontent.com/Recognized/ScreencastEditor/master/resources/icons/play@2x.png" alt="play" width="16"/> Play whole audio file or selected range only.
* <img src="https://raw.githubusercontent.com/Recognized/ScreencastEditor/master/resources/icons/pause@2x.png" alt="pause" width="16"/> Pause audio.
* <img src="https://raw.githubusercontent.com/Recognized/ScreencastEditor/master/resources/icons/stop@2x.png" alt="stop" width="16"/> Stop playing audio.
* <img src="https://raw.githubusercontent.com/JetBrains/intellij-community/master/platform/icons/src/actions/undo.svg?sanitize=true" width="16" height="16"></object> Remove cut or mute effect from selected range.
* <img src="https://raw.githubusercontent.com/Recognized/ScreencastEditor/master/resources/icons/delete@2x.png" alt="cut" width="16"/> Cut selected range.
* <img src="https://raw.githubusercontent.com/Recognized/ScreencastEditor/master/resources/icons/volume_off@2x.png" alt="mute" width="16"/> Mute selected range.
* <img src="https://raw.githubusercontent.com/JetBrains/intellij-community/master/platform/icons/src/graph/zoomIn.svg?sanitize=true" width="16" height="16"></object> Zoom in.
* <img src="https://raw.githubusercontent.com/JetBrains/intellij-community/master/platform/icons/src/graph/zoomOut.svg?sanitize=true" width="16" height="16"></object> Zoom out.
* <img src="https://raw.githubusercontent.com/Recognized/ScreencastEditor/master/resources/icons/transcript@2x.png" alt="transcript" width="16"/> Open transcript. Start recognition if transcript is not yet known.
* <img src="https://raw.githubusercontent.com/JetBrains/kotlin/1.2.70/idea/resources/org/jetbrains/kotlin/idea/icons/kotlin_script%402x.png" alt="transcript" width="16"/> Open UI script.
* <img src="https://raw.githubusercontent.com/JetBrains/intellij-community/master/platform/icons/src/actions/menu-saveall.svg?sanitize=true" width="16"></object> Save changes made to this screencast.


Editions that are made to an audio are synchronously applied to the script according to bindings.

### Recognition

In order to use speech recognition you need to obtain key to Service Account in Google Cloud Platform.

Visit this [page](https://cloud.google.com/speech-to-text/docs/quickstart-client-libraries) and do the first step 
(_Set up a GCP Console Project_).

Before using recognition in plugin you will need to set this key.

**Setting key**:
_Tools_ menu → _Screencast Editor_ -> _Google Speech Kit_ -> _Set credentials_ (choose downloaded JSON key)

### Edit script

// TODO: controls and logic

### Manage bindings

**Open editor**:
_Tools_ menu → _Screencast Editor_ → _Manage script-transcript relations_

**Bindings editor structure**:

Left part of the editor represents transcript associated with chosen audio, right part is a script that is also 
associated with this audio.

Element in list from the left is word (or concatenation of multiple words done by user).

Bindings are highlighted with green.

![bindings-editor](https://raw.githubusercontent.com/Recognized/ScreencastEditor/master/demo/bindings2.png)

Use Left Mouse Button to select word/line, drag it to select multiple words/lines.

1. Bind selected words to selected lines. 
2. Unbind selected words.
3. Undo last change.
4. Redo.
5. Reset all changes in bindings.

### Save changes

// TODO
# ScreencastEditor

Intellij IDEA plugin for recording and editing screencasts.

**Screencast** is a pair of simultaneously recorded GUI script and audio from microphone.

**GUI script** is a Kotlin DSL script that contains information about user interactions with IDE 
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
    ...
}
```

This plugin has simple built-in audio editor. 
![audio-editor1](https://raw.githubusercontent.com/Recognized/ScreencastEditor/master/demo/audio_editor1.gif)

It allows to use speech recognition service to transcribe audio 
and make audio editing more convenient. Now, only Google Speech-to-text API is supported. (See section below)

In order to maintain synchronization between audio and script during editing, 
plugin uses bindings between words in audio and statements/blocks of code in script.
Plugin allows to manually set them or use automatically generated ones.

![bindings-editor](https://raw.githubusercontent.com/Recognized/ScreencastEditor/master/demo/bindings1.png)

Modifications to the one part of binding are automatically applied to the other part.

Using this script and recorded audio we can reproduce screencast in IDE.

## Usage

### Start recording

// TODO: actions and rules

### Edit audio

**Open audio**: 
1. _Tools_ menu → _Screencast Editor_ → _Open Audio_ 
2. Right click on file in Project Tree → _Open in Audio Editor_

**Audio editor structure**:

- Use **Left Mouse Button** to select audio range by words.
- Hold **Control/Meta** and drag mouse with pressed **Left Mouse Button** 
to select range precisely (not by words). 
- Hold **Shift** to prevent resetting current selection.
- You may also drag word borders holding **Right Mouse Button**.

![audio-editor2](https://raw.githubusercontent.com/Recognized/ScreencastEditor/master/demo/audio_editor2.png)

1. Play audio file or selected range.
2. Pause audio.
3. Stop playing audio.
4. Remove cut or mute effect from selected range.
5. Cut selected range.
6. Mute selected range.
7. Zoom in.
8. Zoom out.
9. Open transcript. Start recognition if transcript is not yet known.
10. Audio file name.
11. Borders in which word is spoken.
12. Selected range.
13. Recognized word.

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
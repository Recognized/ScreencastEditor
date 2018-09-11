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
1. Tools menu → Screencast Editor → Open Audio 
2. Right click on file in Project Tree → Open in Audio Editor

**Audio editor structure**:
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

Editions made to an audio are synchronously applied to the script according to bindings.

### Edit script

// TODO: controls and logic

### Manage bindings

**Open editor**:
Tools menu → Screencast Editor → Manage script-transcript relations
// TODO: picture and structure

### Save changes

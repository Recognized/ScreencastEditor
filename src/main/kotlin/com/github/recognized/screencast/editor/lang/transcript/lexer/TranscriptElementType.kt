package com.github.recognized.screencast.editor.lang.transcript.lexer

import com.github.recognized.screencast.editor.lang.transcript.psi.TranscriptLanguage
import com.intellij.psi.tree.IElementType

class TranscriptElementType(debugName: String) : IElementType(debugName, TranscriptLanguage)
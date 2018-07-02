// This is a generated file. Not intended for manual editing.
package vladsaif.syncedit.plugin.lang.transcript.psi;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.PsiElement;
import com.intellij.lang.ASTNode;
import vladsaif.syncedit.plugin.lang.transcript.psi.impl.*;

public interface TranscriptTokenTypes {

  IElementType ID = new TranscriptElementType("ID");
  IElementType LINE = new TranscriptElementType("LINE");
  IElementType TIME_OFFSET = new TranscriptElementType("TIME_OFFSET");

  IElementType CLOSE_BRACKET = new TranscriptTokenType("CLOSE_BRACKET");
  IElementType COMMA = new TranscriptTokenType("COMMA");
  IElementType INTEGER_LITERAL = new TranscriptTokenType("INTEGER_LITERAL");
  IElementType OPEN_BRACKET = new TranscriptTokenType("OPEN_BRACKET");
  IElementType WORD = new TranscriptTokenType("WORD");

  class Factory {
    public static PsiElement createElement(ASTNode node) {
      IElementType type = node.getElementType();
       if (type == ID) {
        return new TranscriptIdImpl(node);
      }
      else if (type == LINE) {
        return new TranscriptLineImpl(node);
      }
      else if (type == TIME_OFFSET) {
        return new TranscriptTimeOffsetImpl(node);
      }
      throw new AssertionError("Unknown element type: " + type);
    }
  }
}

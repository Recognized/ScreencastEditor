// This is a generated file. Not intended for manual editing.
package vladsaif.syncedit.plugin.lang.dsl.psi;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.PsiElement;
import com.intellij.lang.ASTNode;
import vladsaif.syncedit.plugin.lang.dsl.psi.impl.*;

public interface DslTokenTypes {

  IElementType BLOCK_STATEMENT = new DslElementType("BLOCK_STATEMENT");
  IElementType ID = new DslElementType("ID");
  IElementType META_COMMENT = new DslElementType("META_COMMENT");
  IElementType SINGLE_STATEMENT = new DslElementType("SINGLE_STATEMENT");
  IElementType STATEMENT = new DslElementType("STATEMENT");
  IElementType TIME_OFFSET = new DslElementType("TIME_OFFSET");
  IElementType WORD = new DslElementType("WORD");

  IElementType CHAR = new DslTokenType("CHAR");
  IElementType CLOSE_BRACE = new DslTokenType("CLOSE_BRACE");
  IElementType CLOSE_BRACKET = new DslTokenType("CLOSE_BRACKET");
  IElementType COMMA = new DslTokenType("COMMA");
  IElementType CRLF = new DslTokenType("CRLF");
  IElementType EOL_COMMENT_START = new DslTokenType("EOL_COMMENT_START");
  IElementType INTEGER_LITERAL = new DslTokenType("INTEGER_LITERAL");
  IElementType OPEN_BRACE = new DslTokenType("OPEN_BRACE");
  IElementType OPEN_BRACKET = new DslTokenType("OPEN_BRACKET");
  IElementType STRING = new DslTokenType("STRING");

  class Factory {
    public static PsiElement createElement(ASTNode node) {
      IElementType type = node.getElementType();
       if (type == BLOCK_STATEMENT) {
        return new DslBlockStatementImpl(node);
      }
      else if (type == ID) {
        return new DslIdImpl(node);
      }
      else if (type == META_COMMENT) {
        return new DslMetaCommentImpl(node);
      }
      else if (type == SINGLE_STATEMENT) {
        return new DslSingleStatementImpl(node);
      }
      else if (type == STATEMENT) {
        return new DslStatementImpl(node);
      }
      else if (type == TIME_OFFSET) {
        return new DslTimeOffsetImpl(node);
      }
      else if (type == WORD) {
        return new DslWordImpl(node);
      }
      throw new AssertionError("Unknown element type: " + type);
    }
  }
}

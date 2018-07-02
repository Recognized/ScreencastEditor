// This is a generated file. Not intended for manual editing.
package vladsaif.syncedit.plugin.lang.transcript.parser;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilder.Marker;
import static vladsaif.syncedit.plugin.lang.transcript.psi.TranscriptTokenTypes.*;
import static com.intellij.lang.parser.GeneratedParserUtilBase.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.TokenSet;
import com.intellij.lang.PsiParser;
import com.intellij.lang.LightPsiParser;

@SuppressWarnings({"SimplifiableIfStatement", "UnusedAssignment"})
public class TranscriptParser implements PsiParser, LightPsiParser {

  public ASTNode parse(IElementType t, PsiBuilder b) {
    parseLight(t, b);
    return b.getTreeBuilt();
  }

  public void parseLight(IElementType t, PsiBuilder b) {
    boolean r;
    b = adapt_builder_(t, b, this, null);
    Marker m = enter_section_(b, 0, _COLLAPSE_, null);
    if (t == ID) {
      r = id(b, 0);
    }
    else if (t == LINE) {
      r = line(b, 0);
    }
    else if (t == TIME_OFFSET) {
      r = timeOffset(b, 0);
    }
    else {
      r = parse_root_(t, b, 0);
    }
    exit_section_(b, 0, m, t, r, true, TRUE_CONDITION);
  }

  protected boolean parse_root_(IElementType t, PsiBuilder b, int l) {
    return file(b, l + 1);
  }

  /* ********************************************************** */
  // line*
  static boolean file(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "file")) return false;
    int c = current_position_(b);
    while (true) {
      if (!line(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "file", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  /* ********************************************************** */
  // INTEGER_LITERAL
  public static boolean id(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "id")) return false;
    if (!nextTokenIs(b, INTEGER_LITERAL)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, INTEGER_LITERAL);
    exit_section_(b, m, ID, r);
    return r;
  }

  /* ********************************************************** */
  // WORD OPEN_BRACKET timeOffset COMMA timeOffset CLOSE_BRACKET OPEN_BRACKET id CLOSE_BRACKET
  public static boolean line(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "line")) return false;
    if (!nextTokenIs(b, WORD)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, WORD, OPEN_BRACKET);
    r = r && timeOffset(b, l + 1);
    r = r && consumeToken(b, COMMA);
    r = r && timeOffset(b, l + 1);
    r = r && consumeTokens(b, 0, CLOSE_BRACKET, OPEN_BRACKET);
    r = r && id(b, l + 1);
    r = r && consumeToken(b, CLOSE_BRACKET);
    exit_section_(b, m, LINE, r);
    return r;
  }

  /* ********************************************************** */
  // INTEGER_LITERAL
  public static boolean timeOffset(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "timeOffset")) return false;
    if (!nextTokenIs(b, INTEGER_LITERAL)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, INTEGER_LITERAL);
    exit_section_(b, m, TIME_OFFSET, r);
    return r;
  }

}

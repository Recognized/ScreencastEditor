// This is a generated file. Not intended for manual editing.
package vladsaif.syncedit.plugin.lang.dsl.parser;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilder.Marker;
import static vladsaif.syncedit.plugin.lang.dsl.psi.DslTokenTypes.*;
import static com.intellij.lang.parser.GeneratedParserUtilBase.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.TokenSet;
import com.intellij.lang.PsiParser;
import com.intellij.lang.LightPsiParser;

@SuppressWarnings({"SimplifiableIfStatement", "UnusedAssignment"})
public class DslParser implements PsiParser, LightPsiParser {

  public ASTNode parse(IElementType t, PsiBuilder b) {
    parseLight(t, b);
    return b.getTreeBuilt();
  }

  public void parseLight(IElementType t, PsiBuilder b) {
    boolean r;
    b = adapt_builder_(t, b, this, null);
    Marker m = enter_section_(b, 0, _COLLAPSE_, null);
    if (t == BLOCK_STATEMENT) {
      r = block_statement(b, 0);
    }
    else if (t == ID) {
      r = id(b, 0);
    }
    else if (t == META_COMMENT) {
      r = meta_comment(b, 0);
    }
    else if (t == SINGLE_STATEMENT) {
      r = single_statement(b, 0);
    }
    else if (t == STATEMENT) {
      r = statement(b, 0);
    }
    else if (t == TIME_OFFSET) {
      r = timeOffset(b, 0);
    }
    else if (t == WORD) {
      r = word(b, 0);
    }
    else {
      r = parse_root_(t, b, 0);
    }
    exit_section_(b, 0, m, t, r, true, TRUE_CONDITION);
  }

  protected boolean parse_root_(IElementType t, PsiBuilder b, int l) {
    return blocks(b, l + 1);
  }

  /* ********************************************************** */
  // word (OPEN_BRACE meta_comment CRLF+ (statement)* CLOSE_BRACE meta_comment CRLF+)
  public static boolean block_statement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "block_statement")) return false;
    if (!nextTokenIs(b, "<block statement>", CHAR, STRING)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, BLOCK_STATEMENT, "<block statement>");
    r = word(b, l + 1);
    r = r && block_statement_1(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // OPEN_BRACE meta_comment CRLF+ (statement)* CLOSE_BRACE meta_comment CRLF+
  private static boolean block_statement_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "block_statement_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, OPEN_BRACE);
    r = r && meta_comment(b, l + 1);
    r = r && block_statement_1_2(b, l + 1);
    r = r && block_statement_1_3(b, l + 1);
    r = r && consumeToken(b, CLOSE_BRACE);
    r = r && meta_comment(b, l + 1);
    r = r && block_statement_1_6(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // CRLF+
  private static boolean block_statement_1_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "block_statement_1_2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, CRLF);
    int c = current_position_(b);
    while (r) {
      if (!consumeToken(b, CRLF)) break;
      if (!empty_element_parsed_guard_(b, "block_statement_1_2", c)) break;
      c = current_position_(b);
    }
    exit_section_(b, m, null, r);
    return r;
  }

  // (statement)*
  private static boolean block_statement_1_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "block_statement_1_3")) return false;
    int c = current_position_(b);
    while (true) {
      if (!block_statement_1_3_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "block_statement_1_3", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  // (statement)
  private static boolean block_statement_1_3_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "block_statement_1_3_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = statement(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // CRLF+
  private static boolean block_statement_1_6(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "block_statement_1_6")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, CRLF);
    int c = current_position_(b);
    while (r) {
      if (!consumeToken(b, CRLF)) break;
      if (!empty_element_parsed_guard_(b, "block_statement_1_6", c)) break;
      c = current_position_(b);
    }
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // statement*
  static boolean blocks(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "blocks")) return false;
    int c = current_position_(b);
    while (true) {
      if (!statement(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "blocks", c)) break;
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
  // EOL_COMMENT_START timeOffset COMMA OPEN_BRACKET id CLOSE_BRACKET
  public static boolean meta_comment(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "meta_comment")) return false;
    if (!nextTokenIs(b, EOL_COMMENT_START)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, EOL_COMMENT_START);
    r = r && timeOffset(b, l + 1);
    r = r && consumeTokens(b, 0, COMMA, OPEN_BRACKET);
    r = r && id(b, l + 1);
    r = r && consumeToken(b, CLOSE_BRACKET);
    exit_section_(b, m, META_COMMENT, r);
    return r;
  }

  /* ********************************************************** */
  // word meta_comment CRLF+
  public static boolean single_statement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "single_statement")) return false;
    if (!nextTokenIs(b, "<single statement>", CHAR, STRING)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, SINGLE_STATEMENT, "<single statement>");
    r = word(b, l + 1);
    r = r && meta_comment(b, l + 1);
    r = r && single_statement_2(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // CRLF+
  private static boolean single_statement_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "single_statement_2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, CRLF);
    int c = current_position_(b);
    while (r) {
      if (!consumeToken(b, CRLF)) break;
      if (!empty_element_parsed_guard_(b, "single_statement_2", c)) break;
      c = current_position_(b);
    }
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // single_statement | block_statement
  public static boolean statement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "statement")) return false;
    if (!nextTokenIs(b, "<statement>", CHAR, STRING)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, STATEMENT, "<statement>");
    r = single_statement(b, l + 1);
    if (!r) r = block_statement(b, l + 1);
    exit_section_(b, l, m, r, false, null);
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

  /* ********************************************************** */
  // (CHAR | STRING)+
  public static boolean word(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "word")) return false;
    if (!nextTokenIs(b, "<word>", CHAR, STRING)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, WORD, "<word>");
    r = word_0(b, l + 1);
    int c = current_position_(b);
    while (r) {
      if (!word_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "word", c)) break;
      c = current_position_(b);
    }
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // CHAR | STRING
  private static boolean word_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "word_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, CHAR);
    if (!r) r = consumeToken(b, STRING);
    exit_section_(b, m, null, r);
    return r;
  }

}

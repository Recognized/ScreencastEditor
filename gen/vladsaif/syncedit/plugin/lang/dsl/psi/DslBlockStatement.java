// This is a generated file. Not intended for manual editing.
package vladsaif.syncedit.plugin.lang.dsl.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface DslBlockStatement extends PsiElement {

  @NotNull
  List<DslMetaComment> getMetaCommentList();

  @NotNull
  List<DslStatement> getStatementList();

  @NotNull
  DslWord getWord();

}

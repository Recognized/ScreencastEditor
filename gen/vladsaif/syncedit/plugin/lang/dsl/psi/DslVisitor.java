// This is a generated file. Not intended for manual editing.
package vladsaif.syncedit.plugin.lang.dsl.psi;

import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiElement;

public class DslVisitor extends PsiElementVisitor {

  public void visitBlockStatement(@NotNull DslBlockStatement o) {
    visitPsiElement(o);
  }

  public void visitId(@NotNull DslId o) {
    visitPsiElement(o);
  }

  public void visitMetaComment(@NotNull DslMetaComment o) {
    visitPsiElement(o);
  }

  public void visitSingleStatement(@NotNull DslSingleStatement o) {
    visitPsiElement(o);
  }

  public void visitStatement(@NotNull DslStatement o) {
    visitPsiElement(o);
  }

  public void visitTimeOffset(@NotNull DslTimeOffset o) {
    visitPsiElement(o);
  }

  public void visitWord(@NotNull DslWord o) {
    visitPsiElement(o);
  }

  public void visitPsiElement(@NotNull PsiElement o) {
    visitElement(o);
  }

}

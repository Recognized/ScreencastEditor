// This is a generated file. Not intended for manual editing.
package vladsaif.syncedit.plugin.lang.dsl.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static vladsaif.syncedit.plugin.lang.dsl.psi.DslTokenTypes.*;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import vladsaif.syncedit.plugin.lang.dsl.psi.*;

public class DslSingleStatementImpl extends ASTWrapperPsiElement implements DslSingleStatement {

  public DslSingleStatementImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull DslVisitor visitor) {
    visitor.visitSingleStatement(this);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof DslVisitor) accept((DslVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public DslMetaComment getMetaComment() {
    return findNotNullChildByClass(DslMetaComment.class);
  }

  @Override
  @NotNull
  public DslWord getWord() {
    return findNotNullChildByClass(DslWord.class);
  }

}

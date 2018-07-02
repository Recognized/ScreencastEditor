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

public class DslStatementImpl extends ASTWrapperPsiElement implements DslStatement {

  public DslStatementImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull DslVisitor visitor) {
    visitor.visitStatement(this);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof DslVisitor) accept((DslVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public DslBlockStatement getBlockStatement() {
    return findChildByClass(DslBlockStatement.class);
  }

  @Override
  @Nullable
  public DslSingleStatement getSingleStatement() {
    return findChildByClass(DslSingleStatement.class);
  }

}

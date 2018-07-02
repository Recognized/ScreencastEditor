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

public class DslMetaCommentImpl extends ASTWrapperPsiElement implements DslMetaComment {

  public DslMetaCommentImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull DslVisitor visitor) {
    visitor.visitMetaComment(this);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof DslVisitor) accept((DslVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public DslId getId() {
    return findNotNullChildByClass(DslId.class);
  }

  @Override
  @NotNull
  public DslTimeOffset getTimeOffset() {
    return findNotNullChildByClass(DslTimeOffset.class);
  }

}

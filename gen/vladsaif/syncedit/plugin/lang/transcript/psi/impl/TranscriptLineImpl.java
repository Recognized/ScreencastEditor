// This is a generated file. Not intended for manual editing.
package vladsaif.syncedit.plugin.lang.transcript.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static vladsaif.syncedit.plugin.lang.transcript.psi.TranscriptTokenTypes.*;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import vladsaif.syncedit.plugin.lang.transcript.psi.*;

public class TranscriptLineImpl extends ASTWrapperPsiElement implements TranscriptLine {

  public TranscriptLineImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull TranscriptVisitor visitor) {
    visitor.visitLine(this);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof TranscriptVisitor) accept((TranscriptVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public TranscriptId getId() {
    return findNotNullChildByClass(TranscriptId.class);
  }

  @Override
  @NotNull
  public List<TranscriptTimeOffset> getTimeOffsetList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, TranscriptTimeOffset.class);
  }

}

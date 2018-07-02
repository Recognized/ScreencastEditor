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

public class TranscriptIdImpl extends ASTWrapperPsiElement implements TranscriptId {

  public TranscriptIdImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull TranscriptVisitor visitor) {
    visitor.visitId(this);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof TranscriptVisitor) accept((TranscriptVisitor)visitor);
    else super.accept(visitor);
  }

}

package embedded.com.android.dx.dex.code.form;

import embedded.com.android.dx.dex.code.*;
import embedded.com.android.dx.util.*;

public final class Form10t extends InsnFormat
{
    public static final InsnFormat THE_ONE;
    
    @Override
    public String insnArgString(final DalvInsn insn) {
        return InsnFormat.branchString(insn);
    }
    
    @Override
    public String insnCommentString(final DalvInsn insn, final boolean noteIndices) {
        return InsnFormat.branchComment(insn);
    }
    
    @Override
    public int codeSize() {
        return 1;
    }
    
    @Override
    public boolean isCompatible(final DalvInsn insn) {
        if (!(insn instanceof TargetInsn) || insn.getRegisters().size() != 0) {
            return false;
        }
        final TargetInsn ti = (TargetInsn)insn;
        return !ti.hasTargetOffset() || this.branchFits(ti);
    }
    
    @Override
    public boolean branchFits(final TargetInsn insn) {
        final int offset = insn.getTargetOffset();
        return offset != 0 && InsnFormat.signedFitsInByte(offset);
    }
    
    @Override
    public void writeTo(final AnnotatedOutput out, final DalvInsn insn) {
        final int offset = ((TargetInsn)insn).getTargetOffset();
        InsnFormat.write(out, InsnFormat.opcodeUnit(insn, offset & 0xFF));
    }
    
    static {
        THE_ONE = new Form10t();
    }
}

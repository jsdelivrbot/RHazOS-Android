package embedded.com.android.dx.dex.code.form;

import embedded.com.android.dx.dex.code.*;
import embedded.com.android.dx.util.*;

public final class Form30t extends InsnFormat
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
        return 3;
    }
    
    @Override
    public boolean isCompatible(final DalvInsn insn) {
        return insn instanceof TargetInsn && insn.getRegisters().size() == 0;
    }
    
    @Override
    public boolean branchFits(final TargetInsn insn) {
        return true;
    }
    
    @Override
    public void writeTo(final AnnotatedOutput out, final DalvInsn insn) {
        final int offset = ((TargetInsn)insn).getTargetOffset();
        InsnFormat.write(out, InsnFormat.opcodeUnit(insn, 0), offset);
    }
    
    static {
        THE_ONE = new Form30t();
    }
}

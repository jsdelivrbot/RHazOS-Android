package embedded.com.android.dx.dex.code.form;

import embedded.com.android.dx.dex.code.*;
import embedded.com.android.dx.util.*;

public final class Form10x extends InsnFormat
{
    public static final InsnFormat THE_ONE;
    
    @Override
    public String insnArgString(final DalvInsn insn) {
        return "";
    }
    
    @Override
    public String insnCommentString(final DalvInsn insn, final boolean noteIndices) {
        return "";
    }
    
    @Override
    public int codeSize() {
        return 1;
    }
    
    @Override
    public boolean isCompatible(final DalvInsn insn) {
        return insn instanceof SimpleInsn && insn.getRegisters().size() == 0;
    }
    
    @Override
    public void writeTo(final AnnotatedOutput out, final DalvInsn insn) {
        InsnFormat.write(out, InsnFormat.opcodeUnit(insn, 0));
    }
    
    static {
        THE_ONE = new Form10x();
    }
}

package embedded.com.android.dx.dex.code.form;

import embedded.com.android.dx.dex.code.*;
import embedded.com.android.dx.dex.code.CstInsn;
import embedded.com.android.dx.rop.cst.*;
import embedded.com.android.dx.rop.code.*;
import embedded.com.android.dx.util.*;

public final class Form3rc extends InsnFormat
{
    public static final InsnFormat THE_ONE;
    
    @Override
    public String insnArgString(final DalvInsn insn) {
        return InsnFormat.regRangeString(insn.getRegisters()) + ", " + insn.cstString();
    }
    
    @Override
    public String insnCommentString(final DalvInsn insn, final boolean noteIndices) {
        if (noteIndices) {
            return insn.cstComment();
        }
        return "";
    }
    
    @Override
    public int codeSize() {
        return 3;
    }
    
    @Override
    public boolean isCompatible(final DalvInsn insn) {
        if (!(insn instanceof CstInsn)) {
            return false;
        }
        final CstInsn ci = (CstInsn)insn;
        final int cpi = ci.getIndex();
        final Constant cst = ci.getConstant();
        if (!InsnFormat.unsignedFitsInShort(cpi)) {
            return false;
        }
        if (!(cst instanceof CstMethodRef) && !(cst instanceof CstType) && !(cst instanceof CstCallSiteRef)) {
            return false;
        }
        final RegisterSpecList regs = ci.getRegisters();
        final int sz = regs.size();
        return regs.size() == 0 || (InsnFormat.isRegListSequential(regs) && InsnFormat.unsignedFitsInShort(regs.get(0).getReg()) && InsnFormat.unsignedFitsInByte(regs.getWordCount()));
    }
    
    @Override
    public void writeTo(final AnnotatedOutput out, final DalvInsn insn) {
        final RegisterSpecList regs = insn.getRegisters();
        final int cpi = ((CstInsn)insn).getIndex();
        final int firstReg = (regs.size() == 0) ? 0 : regs.get(0).getReg();
        final int count = regs.getWordCount();
        InsnFormat.write(out, InsnFormat.opcodeUnit(insn, count), (short)cpi, (short)firstReg);
    }
    
    static {
        THE_ONE = new Form3rc();
    }
}

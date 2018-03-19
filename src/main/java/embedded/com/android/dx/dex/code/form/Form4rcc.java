package embedded.com.android.dx.dex.code.form;

import embedded.com.android.dx.dex.code.*;
import embedded.com.android.dx.rop.cst.*;
import embedded.com.android.dx.rop.code.*;
import embedded.com.android.dx.util.*;

public final class Form4rcc extends InsnFormat
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
        return 4;
    }
    
    @Override
    public boolean isCompatible(final DalvInsn insn) {
        if (!(insn instanceof MultiCstInsn)) {
            return false;
        }
        final MultiCstInsn mci = (MultiCstInsn)insn;
        final int methodIdx = mci.getIndex(0);
        final int protoIdx = mci.getIndex(1);
        if (!InsnFormat.unsignedFitsInShort(methodIdx) || !InsnFormat.unsignedFitsInShort(protoIdx)) {
            return false;
        }
        final Constant methodRef = mci.getConstant(0);
        if (!(methodRef instanceof CstMethodRef)) {
            return false;
        }
        final Constant protoRef = mci.getConstant(1);
        if (!(protoRef instanceof CstProtoRef)) {
            return false;
        }
        final RegisterSpecList regs = mci.getRegisters();
        final int sz = regs.size();
        return sz == 0 || (InsnFormat.unsignedFitsInByte(regs.getWordCount()) && InsnFormat.unsignedFitsInShort(sz) && InsnFormat.unsignedFitsInShort(regs.get(0).getReg()) && InsnFormat.isRegListSequential(regs));
    }
    
    @Override
    public void writeTo(final AnnotatedOutput out, final DalvInsn insn) {
        final MultiCstInsn mci = (MultiCstInsn)insn;
        final short regB = (short)mci.getIndex(0);
        final short regH = (short)mci.getIndex(1);
        final RegisterSpecList regs = insn.getRegisters();
        short regC = 0;
        if (regs.size() > 0) {
            regC = (short)regs.get(0).getReg();
        }
        final int regA = regs.getWordCount();
        InsnFormat.write(out, InsnFormat.opcodeUnit(insn, regA), regB, regC, regH);
    }
    
    static {
        THE_ONE = new Form4rcc();
    }
}

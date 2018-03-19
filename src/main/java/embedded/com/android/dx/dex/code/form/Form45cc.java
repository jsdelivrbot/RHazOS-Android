package embedded.com.android.dx.dex.code.form;

import embedded.com.android.dx.dex.code.*;
import embedded.com.android.dx.rop.cst.*;
import java.util.*;
import embedded.com.android.dx.rop.code.*;
import embedded.com.android.dx.util.*;
import embedded.com.android.dx.rop.type.*;

public final class Form45cc extends InsnFormat
{
    public static final InsnFormat THE_ONE;
    private static final int MAX_NUM_OPS = 5;
    
    @Override
    public String insnArgString(final DalvInsn insn) {
        final RegisterSpecList regs = explicitize(insn.getRegisters());
        return InsnFormat.regListString(regs) + ", " + insn.cstString();
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
        if (mci.getNumberOfConstants() != 2) {
            return false;
        }
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
        return wordCount(regs) >= 0;
    }
    
    @Override
    public BitSet compatibleRegs(final DalvInsn insn) {
        final RegisterSpecList regs = insn.getRegisters();
        final int sz = regs.size();
        final BitSet bits = new BitSet(sz);
        for (int i = 0; i < sz; ++i) {
            final RegisterSpec reg = regs.get(i);
            bits.set(i, InsnFormat.unsignedFitsInNibble(reg.getReg() + reg.getCategory() - 1));
        }
        return bits;
    }
    
    @Override
    public void writeTo(final AnnotatedOutput out, final DalvInsn insn) {
        final MultiCstInsn mci = (MultiCstInsn)insn;
        final short regB = (short)mci.getIndex(0);
        final short regH = (short)mci.getIndex(1);
        final RegisterSpecList regs = explicitize(insn.getRegisters());
        final int regA = regs.size();
        final int regC = (regA > 0) ? regs.get(0).getReg() : 0;
        final int regD = (regA > 1) ? regs.get(1).getReg() : 0;
        final int regE = (regA > 2) ? regs.get(2).getReg() : 0;
        final int regF = (regA > 3) ? regs.get(3).getReg() : 0;
        final int regG = (regA > 4) ? regs.get(4).getReg() : 0;
        InsnFormat.write(out, InsnFormat.opcodeUnit(insn, InsnFormat.makeByte(regG, regA)), regB, InsnFormat.codeUnit(regC, regD, regE, regF), regH);
    }
    
    private static int wordCount(final RegisterSpecList regs) {
        final int sz = regs.size();
        if (sz > 5) {
            return -1;
        }
        int result = 0;
        for (int i = 0; i < sz; ++i) {
            final RegisterSpec one = regs.get(i);
            result += one.getCategory();
            if (!InsnFormat.unsignedFitsInNibble(one.getReg() + one.getCategory() - 1)) {
                return -1;
            }
        }
        return (result <= 5) ? result : -1;
    }
    
    private static RegisterSpecList explicitize(final RegisterSpecList orig) {
        final int wordCount = wordCount(orig);
        final int sz = orig.size();
        if (wordCount == sz) {
            return orig;
        }
        final RegisterSpecList result = new RegisterSpecList(wordCount);
        int wordAt = 0;
        for (int i = 0; i < sz; ++i) {
            final RegisterSpec one = orig.get(i);
            result.set(wordAt, one);
            if (one.getCategory() == 2) {
                result.set(wordAt + 1, RegisterSpec.make(one.getReg() + 1, Type.VOID));
                wordAt += 2;
            }
            else {
                ++wordAt;
            }
        }
        result.setImmutable();
        return result;
    }
    
    static {
        THE_ONE = new Form45cc();
    }
}

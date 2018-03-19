package embedded.com.android.dx.dex.code.form;

import embedded.com.android.dx.dex.code.*;
import embedded.com.android.dx.dex.code.CstInsn;
import embedded.com.android.dx.rop.cst.*;
import java.util.*;
import embedded.com.android.dx.rop.code.*;
import embedded.com.android.dx.util.*;
import embedded.com.android.dx.rop.type.*;

public final class Form35c extends InsnFormat
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
        return 3;
    }
    
    @Override
    public boolean isCompatible(final DalvInsn insn) {
        if (!(insn instanceof CstInsn)) {
            return false;
        }
        final CstInsn ci = (CstInsn)insn;
        final int cpi = ci.getIndex();
        if (!InsnFormat.unsignedFitsInShort(cpi)) {
            return false;
        }
        final Constant cst = ci.getConstant();
        if (!(cst instanceof CstMethodRef) && !(cst instanceof CstType) && !(cst instanceof CstCallSiteRef)) {
            return false;
        }
        final RegisterSpecList regs = ci.getRegisters();
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
        final int cpi = ((CstInsn)insn).getIndex();
        final RegisterSpecList regs = explicitize(insn.getRegisters());
        final int sz = regs.size();
        final int r0 = (sz > 0) ? regs.get(0).getReg() : 0;
        final int r2 = (sz > 1) ? regs.get(1).getReg() : 0;
        final int r3 = (sz > 2) ? regs.get(2).getReg() : 0;
        final int r4 = (sz > 3) ? regs.get(3).getReg() : 0;
        final int r5 = (sz > 4) ? regs.get(4).getReg() : 0;
        InsnFormat.write(out, InsnFormat.opcodeUnit(insn, InsnFormat.makeByte(r5, sz)), (short)cpi, InsnFormat.codeUnit(r0, r2, r3, r4));
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
        THE_ONE = new Form35c();
    }
}

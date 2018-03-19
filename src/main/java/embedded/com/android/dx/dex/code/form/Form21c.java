package embedded.com.android.dx.dex.code.form;

import embedded.com.android.dx.dex.code.*;
import embedded.com.android.dx.dex.code.CstInsn;
import embedded.com.android.dx.rop.code.*;
import embedded.com.android.dx.rop.cst.*;
import java.util.*;
import embedded.com.android.dx.util.*;

public final class Form21c extends InsnFormat
{
    public static final InsnFormat THE_ONE;
    
    @Override
    public String insnArgString(final DalvInsn insn) {
        final RegisterSpecList regs = insn.getRegisters();
        return regs.get(0).regString() + ", " + insn.cstString();
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
        return 2;
    }
    
    @Override
    public boolean isCompatible(final DalvInsn insn) {
        if (!(insn instanceof CstInsn)) {
            return false;
        }
        final RegisterSpecList regs = insn.getRegisters();
        RegisterSpec reg = null;
        switch (regs.size()) {
            case 1: {
                reg = regs.get(0);
                break;
            }
            case 2: {
                reg = regs.get(0);
                if (reg.getReg() != regs.get(1).getReg()) {
                    return false;
                }
                break;
            }
            default: {
                return false;
            }
        }
        if (!InsnFormat.unsignedFitsInByte(reg.getReg())) {
            return false;
        }
        final CstInsn ci = (CstInsn)insn;
        final int cpi = ci.getIndex();
        final Constant cst = ci.getConstant();
        return InsnFormat.unsignedFitsInShort(cpi) && (cst instanceof CstType || cst instanceof CstFieldRef || cst instanceof CstString);
    }
    
    @Override
    public BitSet compatibleRegs(final DalvInsn insn) {
        final RegisterSpecList regs = insn.getRegisters();
        final int sz = regs.size();
        final BitSet bits = new BitSet(sz);
        final boolean compat = InsnFormat.unsignedFitsInByte(regs.get(0).getReg());
        if (sz == 1) {
            bits.set(0, compat);
        }
        else if (regs.get(0).getReg() == regs.get(1).getReg()) {
            bits.set(0, compat);
            bits.set(1, compat);
        }
        return bits;
    }
    
    @Override
    public void writeTo(final AnnotatedOutput out, final DalvInsn insn) {
        final RegisterSpecList regs = insn.getRegisters();
        final int cpi = ((CstInsn)insn).getIndex();
        InsnFormat.write(out, InsnFormat.opcodeUnit(insn, regs.get(0).getReg()), (short)cpi);
    }
    
    static {
        THE_ONE = new Form21c();
    }
}

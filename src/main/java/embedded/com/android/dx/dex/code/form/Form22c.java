package embedded.com.android.dx.dex.code.form;

import embedded.com.android.dx.dex.code.CstInsn;
import embedded.com.android.dx.rop.code.*;
import embedded.com.android.dx.dex.code.*;
import embedded.com.android.dx.rop.cst.*;
import java.util.*;
import embedded.com.android.dx.util.*;

public final class Form22c extends InsnFormat
{
    public static final InsnFormat THE_ONE;
    
    @Override
    public String insnArgString(final DalvInsn insn) {
        final RegisterSpecList regs = insn.getRegisters();
        return regs.get(0).regString() + ", " + regs.get(1).regString() + ", " + insn.cstString();
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
        final RegisterSpecList regs = insn.getRegisters();
        if (!(insn instanceof CstInsn) || regs.size() != 2 || !InsnFormat.unsignedFitsInNibble(regs.get(0).getReg()) || !InsnFormat.unsignedFitsInNibble(regs.get(1).getReg())) {
            return false;
        }
        final CstInsn ci = (CstInsn)insn;
        final int cpi = ci.getIndex();
        if (!InsnFormat.unsignedFitsInShort(cpi)) {
            return false;
        }
        final Constant cst = ci.getConstant();
        return cst instanceof CstType || cst instanceof CstFieldRef;
    }
    
    @Override
    public BitSet compatibleRegs(final DalvInsn insn) {
        final RegisterSpecList regs = insn.getRegisters();
        final BitSet bits = new BitSet(2);
        bits.set(0, InsnFormat.unsignedFitsInNibble(regs.get(0).getReg()));
        bits.set(1, InsnFormat.unsignedFitsInNibble(regs.get(1).getReg()));
        return bits;
    }
    
    @Override
    public void writeTo(final AnnotatedOutput out, final DalvInsn insn) {
        final RegisterSpecList regs = insn.getRegisters();
        final int cpi = ((CstInsn)insn).getIndex();
        InsnFormat.write(out, InsnFormat.opcodeUnit(insn, InsnFormat.makeByte(regs.get(0).getReg(), regs.get(1).getReg())), (short)cpi);
    }
    
    static {
        THE_ONE = new Form22c();
    }
}

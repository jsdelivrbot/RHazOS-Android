package embedded.com.android.dx.dex.code.form;

import embedded.com.android.dx.dex.code.*;
import embedded.com.android.dx.dex.code.CstInsn;
import embedded.com.android.dx.rop.code.*;
import embedded.com.android.dx.rop.cst.*;
import java.util.*;
import embedded.com.android.dx.util.*;

public final class Form22b extends InsnFormat
{
    public static final InsnFormat THE_ONE;
    
    @Override
    public String insnArgString(final DalvInsn insn) {
        final RegisterSpecList regs = insn.getRegisters();
        final CstLiteralBits value = (CstLiteralBits)((CstInsn)insn).getConstant();
        return regs.get(0).regString() + ", " + regs.get(1).regString() + ", " + InsnFormat.literalBitsString(value);
    }
    
    @Override
    public String insnCommentString(final DalvInsn insn, final boolean noteIndices) {
        final CstLiteralBits value = (CstLiteralBits)((CstInsn)insn).getConstant();
        return InsnFormat.literalBitsComment(value, 8);
    }
    
    @Override
    public int codeSize() {
        return 2;
    }
    
    @Override
    public boolean isCompatible(final DalvInsn insn) {
        final RegisterSpecList regs = insn.getRegisters();
        if (!(insn instanceof CstInsn) || regs.size() != 2 || !InsnFormat.unsignedFitsInByte(regs.get(0).getReg()) || !InsnFormat.unsignedFitsInByte(regs.get(1).getReg())) {
            return false;
        }
        final CstInsn ci = (CstInsn)insn;
        final Constant cst = ci.getConstant();
        if (!(cst instanceof CstLiteralBits)) {
            return false;
        }
        final CstLiteralBits cb = (CstLiteralBits)cst;
        return cb.fitsInInt() && InsnFormat.signedFitsInByte(cb.getIntBits());
    }
    
    @Override
    public BitSet compatibleRegs(final DalvInsn insn) {
        final RegisterSpecList regs = insn.getRegisters();
        final BitSet bits = new BitSet(2);
        bits.set(0, InsnFormat.unsignedFitsInByte(regs.get(0).getReg()));
        bits.set(1, InsnFormat.unsignedFitsInByte(regs.get(1).getReg()));
        return bits;
    }
    
    @Override
    public void writeTo(final AnnotatedOutput out, final DalvInsn insn) {
        final RegisterSpecList regs = insn.getRegisters();
        final int value = ((CstLiteralBits)((CstInsn)insn).getConstant()).getIntBits();
        InsnFormat.write(out, InsnFormat.opcodeUnit(insn, regs.get(0).getReg()), InsnFormat.codeUnit(regs.get(1).getReg(), value & 0xFF));
    }
    
    static {
        THE_ONE = new Form22b();
    }
}

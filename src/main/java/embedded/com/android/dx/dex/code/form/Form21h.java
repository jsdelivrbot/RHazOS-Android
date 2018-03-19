package embedded.com.android.dx.dex.code.form;

import embedded.com.android.dx.dex.code.*;
import embedded.com.android.dx.dex.code.CstInsn;
import embedded.com.android.dx.rop.code.*;
import embedded.com.android.dx.rop.cst.*;
import java.util.*;
import embedded.com.android.dx.util.*;

public final class Form21h extends InsnFormat
{
    public static final InsnFormat THE_ONE;
    
    @Override
    public String insnArgString(final DalvInsn insn) {
        final RegisterSpecList regs = insn.getRegisters();
        final CstLiteralBits value = (CstLiteralBits)((CstInsn)insn).getConstant();
        return regs.get(0).regString() + ", " + InsnFormat.literalBitsString(value);
    }
    
    @Override
    public String insnCommentString(final DalvInsn insn, final boolean noteIndices) {
        final RegisterSpecList regs = insn.getRegisters();
        final CstLiteralBits value = (CstLiteralBits)((CstInsn)insn).getConstant();
        return InsnFormat.literalBitsComment(value, (regs.get(0).getCategory() == 1) ? 32 : 64);
    }
    
    @Override
    public int codeSize() {
        return 2;
    }
    
    @Override
    public boolean isCompatible(final DalvInsn insn) {
        final RegisterSpecList regs = insn.getRegisters();
        if (!(insn instanceof CstInsn) || regs.size() != 1 || !InsnFormat.unsignedFitsInByte(regs.get(0).getReg())) {
            return false;
        }
        final CstInsn ci = (CstInsn)insn;
        final Constant cst = ci.getConstant();
        if (!(cst instanceof CstLiteralBits)) {
            return false;
        }
        final CstLiteralBits cb = (CstLiteralBits)cst;
        if (regs.get(0).getCategory() == 1) {
            final int bits = cb.getIntBits();
            return (bits & 0xFFFF) == 0x0;
        }
        final long bits2 = cb.getLongBits();
        return (bits2 & 0xFFFFFFFFFFFFL) == 0x0L;
    }
    
    @Override
    public BitSet compatibleRegs(final DalvInsn insn) {
        final RegisterSpecList regs = insn.getRegisters();
        final BitSet bits = new BitSet(1);
        bits.set(0, InsnFormat.unsignedFitsInByte(regs.get(0).getReg()));
        return bits;
    }
    
    @Override
    public void writeTo(final AnnotatedOutput out, final DalvInsn insn) {
        final RegisterSpecList regs = insn.getRegisters();
        final CstLiteralBits cb = (CstLiteralBits)((CstInsn)insn).getConstant();
        short bits;
        if (regs.get(0).getCategory() == 1) {
            bits = (short)(cb.getIntBits() >>> 16);
        }
        else {
            bits = (short)(cb.getLongBits() >>> 48);
        }
        InsnFormat.write(out, InsnFormat.opcodeUnit(insn, regs.get(0).getReg()), bits);
    }
    
    static {
        THE_ONE = new Form21h();
    }
}

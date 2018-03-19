package embedded.com.android.dx.dex.code.form;

import embedded.com.android.dx.dex.code.*;
import embedded.com.android.dx.dex.code.CstInsn;
import embedded.com.android.dx.rop.code.*;
import embedded.com.android.dx.rop.cst.*;
import java.util.*;
import embedded.com.android.dx.util.*;

public final class Form11n extends InsnFormat
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
        final CstLiteralBits value = (CstLiteralBits)((CstInsn)insn).getConstant();
        return InsnFormat.literalBitsComment(value, 4);
    }
    
    @Override
    public int codeSize() {
        return 1;
    }
    
    @Override
    public boolean isCompatible(final DalvInsn insn) {
        final RegisterSpecList regs = insn.getRegisters();
        if (!(insn instanceof CstInsn) || regs.size() != 1 || !InsnFormat.unsignedFitsInNibble(regs.get(0).getReg())) {
            return false;
        }
        final CstInsn ci = (CstInsn)insn;
        final Constant cst = ci.getConstant();
        if (!(cst instanceof CstLiteralBits)) {
            return false;
        }
        final CstLiteralBits cb = (CstLiteralBits)cst;
        return cb.fitsInInt() && InsnFormat.signedFitsInNibble(cb.getIntBits());
    }
    
    @Override
    public BitSet compatibleRegs(final DalvInsn insn) {
        final RegisterSpecList regs = insn.getRegisters();
        final BitSet bits = new BitSet(1);
        bits.set(0, InsnFormat.unsignedFitsInNibble(regs.get(0).getReg()));
        return bits;
    }
    
    @Override
    public void writeTo(final AnnotatedOutput out, final DalvInsn insn) {
        final RegisterSpecList regs = insn.getRegisters();
        final int value = ((CstLiteralBits)((CstInsn)insn).getConstant()).getIntBits();
        InsnFormat.write(out, InsnFormat.opcodeUnit(insn, InsnFormat.makeByte(regs.get(0).getReg(), value & 0xF)));
    }
    
    static {
        THE_ONE = new Form11n();
    }
}

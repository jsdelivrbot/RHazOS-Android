package embedded.com.android.dx.dex.code.form;

import embedded.com.android.dx.rop.code.*;
import embedded.com.android.dx.dex.code.*;
import java.util.*;
import embedded.com.android.dx.util.*;

public final class Form23x extends InsnFormat
{
    public static final InsnFormat THE_ONE;
    
    @Override
    public String insnArgString(final DalvInsn insn) {
        final RegisterSpecList regs = insn.getRegisters();
        return regs.get(0).regString() + ", " + regs.get(1).regString() + ", " + regs.get(2).regString();
    }
    
    @Override
    public String insnCommentString(final DalvInsn insn, final boolean noteIndices) {
        return "";
    }
    
    @Override
    public int codeSize() {
        return 2;
    }
    
    @Override
    public boolean isCompatible(final DalvInsn insn) {
        final RegisterSpecList regs = insn.getRegisters();
        return insn instanceof SimpleInsn && regs.size() == 3 && InsnFormat.unsignedFitsInByte(regs.get(0).getReg()) && InsnFormat.unsignedFitsInByte(regs.get(1).getReg()) && InsnFormat.unsignedFitsInByte(regs.get(2).getReg());
    }
    
    @Override
    public BitSet compatibleRegs(final DalvInsn insn) {
        final RegisterSpecList regs = insn.getRegisters();
        final BitSet bits = new BitSet(3);
        bits.set(0, InsnFormat.unsignedFitsInByte(regs.get(0).getReg()));
        bits.set(1, InsnFormat.unsignedFitsInByte(regs.get(1).getReg()));
        bits.set(2, InsnFormat.unsignedFitsInByte(regs.get(2).getReg()));
        return bits;
    }
    
    @Override
    public void writeTo(final AnnotatedOutput out, final DalvInsn insn) {
        final RegisterSpecList regs = insn.getRegisters();
        InsnFormat.write(out, InsnFormat.opcodeUnit(insn, regs.get(0).getReg()), InsnFormat.codeUnit(regs.get(1).getReg(), regs.get(2).getReg()));
    }
    
    static {
        THE_ONE = new Form23x();
    }
}

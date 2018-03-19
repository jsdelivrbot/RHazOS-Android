package embedded.com.android.dx.dex.code.form;

import embedded.com.android.dx.dex.code.*;
import embedded.com.android.dx.rop.code.*;
import java.util.*;
import embedded.com.android.dx.util.*;

public final class Form12x extends InsnFormat
{
    public static final InsnFormat THE_ONE;
    
    @Override
    public String insnArgString(final DalvInsn insn) {
        final RegisterSpecList regs = insn.getRegisters();
        final int sz = regs.size();
        return regs.get(sz - 2).regString() + ", " + regs.get(sz - 1).regString();
    }
    
    @Override
    public String insnCommentString(final DalvInsn insn, final boolean noteIndices) {
        return "";
    }
    
    @Override
    public int codeSize() {
        return 1;
    }
    
    @Override
    public boolean isCompatible(final DalvInsn insn) {
        if (!(insn instanceof SimpleInsn)) {
            return false;
        }
        final RegisterSpecList regs = insn.getRegisters();
        RegisterSpec rs1 = null;
        RegisterSpec rs2 = null;
        switch (regs.size()) {
            case 2: {
                rs1 = regs.get(0);
                rs2 = regs.get(1);
                break;
            }
            case 3: {
                rs1 = regs.get(1);
                rs2 = regs.get(2);
                if (rs1.getReg() != regs.get(0).getReg()) {
                    return false;
                }
                break;
            }
            default: {
                return false;
            }
        }
        return InsnFormat.unsignedFitsInNibble(rs1.getReg()) && InsnFormat.unsignedFitsInNibble(rs2.getReg());
    }
    
    @Override
    public BitSet compatibleRegs(final DalvInsn insn) {
        final RegisterSpecList regs = insn.getRegisters();
        final BitSet bits = new BitSet(2);
        final int r0 = regs.get(0).getReg();
        final int r2 = regs.get(1).getReg();
        switch (regs.size()) {
            case 2: {
                bits.set(0, InsnFormat.unsignedFitsInNibble(r0));
                bits.set(1, InsnFormat.unsignedFitsInNibble(r2));
                break;
            }
            case 3: {
                if (r0 != r2) {
                    bits.set(0, false);
                    bits.set(1, false);
                }
                else {
                    final boolean dstRegComp = InsnFormat.unsignedFitsInNibble(r2);
                    bits.set(0, dstRegComp);
                    bits.set(1, dstRegComp);
                }
                bits.set(2, InsnFormat.unsignedFitsInNibble(regs.get(2).getReg()));
                break;
            }
            default: {
                throw new AssertionError();
            }
        }
        return bits;
    }
    
    @Override
    public void writeTo(final AnnotatedOutput out, final DalvInsn insn) {
        final RegisterSpecList regs = insn.getRegisters();
        final int sz = regs.size();
        InsnFormat.write(out, InsnFormat.opcodeUnit(insn, InsnFormat.makeByte(regs.get(sz - 2).getReg(), regs.get(sz - 1).getReg())));
    }
    
    static {
        THE_ONE = new Form12x();
    }
}

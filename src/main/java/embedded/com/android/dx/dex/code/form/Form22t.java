package embedded.com.android.dx.dex.code.form;

import embedded.com.android.dx.rop.code.*;
import embedded.com.android.dx.dex.code.*;
import java.util.*;
import embedded.com.android.dx.util.*;

public final class Form22t extends InsnFormat
{
    public static final InsnFormat THE_ONE;
    
    @Override
    public String insnArgString(final DalvInsn insn) {
        final RegisterSpecList regs = insn.getRegisters();
        return regs.get(0).regString() + ", " + regs.get(1).regString() + ", " + InsnFormat.branchString(insn);
    }
    
    @Override
    public String insnCommentString(final DalvInsn insn, final boolean noteIndices) {
        return InsnFormat.branchComment(insn);
    }
    
    @Override
    public int codeSize() {
        return 2;
    }
    
    @Override
    public boolean isCompatible(final DalvInsn insn) {
        final RegisterSpecList regs = insn.getRegisters();
        if (!(insn instanceof TargetInsn) || regs.size() != 2 || !InsnFormat.unsignedFitsInNibble(regs.get(0).getReg()) || !InsnFormat.unsignedFitsInNibble(regs.get(1).getReg())) {
            return false;
        }
        final TargetInsn ti = (TargetInsn)insn;
        return !ti.hasTargetOffset() || this.branchFits(ti);
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
    public boolean branchFits(final TargetInsn insn) {
        final int offset = insn.getTargetOffset();
        return offset != 0 && InsnFormat.signedFitsInShort(offset);
    }
    
    @Override
    public void writeTo(final AnnotatedOutput out, final DalvInsn insn) {
        final RegisterSpecList regs = insn.getRegisters();
        final int offset = ((TargetInsn)insn).getTargetOffset();
        InsnFormat.write(out, InsnFormat.opcodeUnit(insn, InsnFormat.makeByte(regs.get(0).getReg(), regs.get(1).getReg())), (short)offset);
    }
    
    static {
        THE_ONE = new Form22t();
    }
}

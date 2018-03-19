package embedded.com.android.dx.dex.code;

import embedded.com.android.dx.rop.code.*;
import java.util.*;
import embedded.com.android.dx.ssa.*;
import embedded.com.android.dx.util.*;

public abstract class DalvInsn
{
    private int address;
    private final Dop opcode;
    private final SourcePosition position;
    private final RegisterSpecList registers;
    
    public static SimpleInsn makeMove(final SourcePosition position, final RegisterSpec dest, final RegisterSpec src) {
        final boolean category1 = dest.getCategory() == 1;
        final boolean reference = dest.getType().isReference();
        final int destReg = dest.getReg();
        final int srcReg = src.getReg();
        Dop opcode;
        if ((srcReg | destReg) < 16) {
            opcode = (reference ? Dops.MOVE_OBJECT : (category1 ? Dops.MOVE : Dops.MOVE_WIDE));
        }
        else if (destReg < 256) {
            opcode = (reference ? Dops.MOVE_OBJECT_FROM16 : (category1 ? Dops.MOVE_FROM16 : Dops.MOVE_WIDE_FROM16));
        }
        else {
            opcode = (reference ? Dops.MOVE_OBJECT_16 : (category1 ? Dops.MOVE_16 : Dops.MOVE_WIDE_16));
        }
        return new SimpleInsn(opcode, position, RegisterSpecList.make(dest, src));
    }
    
    public DalvInsn(final Dop opcode, final SourcePosition position, final RegisterSpecList registers) {
        if (opcode == null) {
            throw new NullPointerException("opcode == null");
        }
        if (position == null) {
            throw new NullPointerException("position == null");
        }
        if (registers == null) {
            throw new NullPointerException("registers == null");
        }
        this.address = -1;
        this.opcode = opcode;
        this.position = position;
        this.registers = registers;
    }
    
    @Override
    public final String toString() {
        final StringBuffer sb = new StringBuffer(100);
        sb.append(this.identifierString());
        sb.append(' ');
        sb.append(this.position);
        sb.append(": ");
        sb.append(this.opcode.getName());
        boolean needComma = false;
        if (this.registers.size() != 0) {
            sb.append(this.registers.toHuman(" ", ", ", null));
            needComma = true;
        }
        final String extra = this.argString();
        if (extra != null) {
            if (needComma) {
                sb.append(',');
            }
            sb.append(' ');
            sb.append(extra);
        }
        return sb.toString();
    }
    
    public final boolean hasAddress() {
        return this.address >= 0;
    }
    
    public final int getAddress() {
        if (this.address < 0) {
            throw new RuntimeException("address not yet known");
        }
        return this.address;
    }
    
    public final Dop getOpcode() {
        return this.opcode;
    }
    
    public final SourcePosition getPosition() {
        return this.position;
    }
    
    public final RegisterSpecList getRegisters() {
        return this.registers;
    }
    
    public final boolean hasResult() {
        return this.opcode.hasResult();
    }
    
    public final int getMinimumRegisterRequirement(final BitSet compatRegs) {
        final boolean hasResult = this.hasResult();
        final int regSz = this.registers.size();
        int resultRequirement = 0;
        int sourceRequirement = 0;
        if (hasResult && !compatRegs.get(0)) {
            resultRequirement = this.registers.get(0).getCategory();
        }
        for (int i = hasResult ? 1 : 0; i < regSz; ++i) {
            if (!compatRegs.get(i)) {
                sourceRequirement += this.registers.get(i).getCategory();
            }
        }
        return Math.max(sourceRequirement, resultRequirement);
    }
    
    public DalvInsn getLowRegVersion() {
        final RegisterSpecList regs = this.registers.withExpandedRegisters(0, this.hasResult(), null);
        return this.withRegisters(regs);
    }
    
    public DalvInsn expandedPrefix(final BitSet compatRegs) {
        RegisterSpecList regs = this.registers;
        final boolean firstBit = compatRegs.get(0);
        if (this.hasResult()) {
            compatRegs.set(0);
        }
        regs = regs.subset(compatRegs);
        if (this.hasResult()) {
            compatRegs.set(0, firstBit);
        }
        if (regs.size() == 0) {
            return null;
        }
        return new HighRegisterPrefix(this.position, regs);
    }
    
    public DalvInsn expandedSuffix(final BitSet compatRegs) {
        if (this.hasResult() && !compatRegs.get(0)) {
            final RegisterSpec r = this.registers.get(0);
            return makeMove(this.position, r, r.withReg(0));
        }
        return null;
    }
    
    public DalvInsn expandedVersion(final BitSet compatRegs) {
        final RegisterSpecList regs = this.registers.withExpandedRegisters(0, this.hasResult(), compatRegs);
        return this.withRegisters(regs);
    }
    
    public final String identifierString() {
        if (this.address != -1) {
            return String.format("%04x", this.address);
        }
        return Hex.u4(System.identityHashCode(this));
    }
    
    public final String listingString(final String prefix, final int width, final boolean noteIndices) {
        final String insnPerSe = this.listingString0(noteIndices);
        if (insnPerSe == null) {
            return null;
        }
        final String addr = prefix + this.identifierString() + ": ";
        final int w1 = addr.length();
        final int w2 = (width == 0) ? insnPerSe.length() : (width - w1);
        return TwoColumnOutput.toString(addr, w1, "", insnPerSe, w2);
    }
    
    public final void setAddress(final int address) {
        if (address < 0) {
            throw new IllegalArgumentException("address < 0");
        }
        this.address = address;
    }
    
    public final int getNextAddress() {
        return this.getAddress() + this.codeSize();
    }
    
    public DalvInsn withMapper(final RegisterMapper mapper) {
        return this.withRegisters(mapper.map(this.getRegisters()));
    }
    
    public abstract int codeSize();
    
    public abstract void writeTo(final AnnotatedOutput p0);
    
    public abstract DalvInsn withOpcode(final Dop p0);
    
    public abstract DalvInsn withRegisterOffset(final int p0);
    
    public abstract DalvInsn withRegisters(final RegisterSpecList p0);
    
    protected abstract String argString();
    
    protected abstract String listingString0(final boolean p0);
    
    public String cstString() {
        throw new UnsupportedOperationException("Not supported.");
    }
    
    public String cstComment() {
        throw new UnsupportedOperationException("Not supported.");
    }
}

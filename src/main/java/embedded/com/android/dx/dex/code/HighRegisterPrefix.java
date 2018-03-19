package embedded.com.android.dx.dex.code;

import embedded.com.android.dx.util.*;
import embedded.com.android.dx.rop.code.*;
import embedded.com.android.dx.rop.type.*;

public final class HighRegisterPrefix extends VariableSizeInsn
{
    private SimpleInsn[] insns;
    
    public HighRegisterPrefix(final SourcePosition position, final RegisterSpecList registers) {
        super(position, registers);
        if (registers.size() == 0) {
            throw new IllegalArgumentException("registers.size() == 0");
        }
        this.insns = null;
    }
    
    @Override
    public int codeSize() {
        int result = 0;
        this.calculateInsnsIfNecessary();
        for (final SimpleInsn insn : this.insns) {
            result += insn.codeSize();
        }
        return result;
    }
    
    @Override
    public void writeTo(final AnnotatedOutput out) {
        this.calculateInsnsIfNecessary();
        for (final SimpleInsn insn : this.insns) {
            insn.writeTo(out);
        }
    }
    
    private void calculateInsnsIfNecessary() {
        if (this.insns != null) {
            return;
        }
        final RegisterSpecList registers = this.getRegisters();
        final int sz = registers.size();
        this.insns = new SimpleInsn[sz];
        int i = 0;
        int outAt = 0;
        while (i < sz) {
            final RegisterSpec src = registers.get(i);
            this.insns[i] = moveInsnFor(src, outAt);
            outAt += src.getCategory();
            ++i;
        }
    }
    
    @Override
    public DalvInsn withRegisters(final RegisterSpecList registers) {
        return new HighRegisterPrefix(this.getPosition(), registers);
    }
    
    @Override
    protected String argString() {
        return null;
    }
    
    @Override
    protected String listingString0(final boolean noteIndices) {
        final RegisterSpecList registers = this.getRegisters();
        final int sz = registers.size();
        final StringBuffer sb = new StringBuffer(100);
        int i = 0;
        int outAt = 0;
        while (i < sz) {
            final RegisterSpec src = registers.get(i);
            final SimpleInsn insn = moveInsnFor(src, outAt);
            if (i != 0) {
                sb.append('\n');
            }
            sb.append(insn.listingString0(noteIndices));
            outAt += src.getCategory();
            ++i;
        }
        return sb.toString();
    }
    
    private static SimpleInsn moveInsnFor(final RegisterSpec src, final int destIndex) {
        return DalvInsn.makeMove(SourcePosition.NO_INFO, RegisterSpec.make(destIndex, src.getType()), src);
    }
}

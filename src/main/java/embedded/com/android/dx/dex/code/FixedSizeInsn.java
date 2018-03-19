package embedded.com.android.dx.dex.code;

import embedded.com.android.dx.rop.code.*;
import embedded.com.android.dx.util.*;

public abstract class FixedSizeInsn extends DalvInsn
{
    public FixedSizeInsn(final Dop opcode, final SourcePosition position, final RegisterSpecList registers) {
        super(opcode, position, registers);
    }
    
    @Override
    public final int codeSize() {
        return this.getOpcode().getFormat().codeSize();
    }
    
    @Override
    public final void writeTo(final AnnotatedOutput out) {
        this.getOpcode().getFormat().writeTo(out, this);
    }
    
    @Override
    public final DalvInsn withRegisterOffset(final int delta) {
        return this.withRegisters(this.getRegisters().withOffset(delta));
    }
    
    @Override
    protected final String listingString0(final boolean noteIndices) {
        return this.getOpcode().getFormat().listingString(this, noteIndices);
    }
}

package embedded.com.android.dx.dex.code;

import embedded.com.android.dx.rop.code.*;
import embedded.com.android.dx.util.*;

public final class OddSpacer extends VariableSizeInsn
{
    public OddSpacer(final SourcePosition position) {
        super(position, RegisterSpecList.EMPTY);
    }
    
    @Override
    public int codeSize() {
        return this.getAddress() & 0x1;
    }
    
    @Override
    public void writeTo(final AnnotatedOutput out) {
        if (this.codeSize() != 0) {
            out.writeShort(InsnFormat.codeUnit(0, 0));
        }
    }
    
    @Override
    public DalvInsn withRegisters(final RegisterSpecList registers) {
        return new OddSpacer(this.getPosition());
    }
    
    @Override
    protected String argString() {
        return null;
    }
    
    @Override
    protected String listingString0(final boolean noteIndices) {
        if (this.codeSize() == 0) {
            return null;
        }
        return "nop // spacer";
    }
}

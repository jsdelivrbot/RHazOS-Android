package embedded.com.android.dx.dex.code;

import embedded.com.android.dx.rop.code.*;
import embedded.com.android.dx.util.*;

public abstract class ZeroSizeInsn extends DalvInsn
{
    public ZeroSizeInsn(final SourcePosition position) {
        super(Dops.SPECIAL_FORMAT, position, RegisterSpecList.EMPTY);
    }
    
    @Override
    public final int codeSize() {
        return 0;
    }
    
    @Override
    public final void writeTo(final AnnotatedOutput out) {
    }
    
    @Override
    public final DalvInsn withOpcode(final Dop opcode) {
        throw new RuntimeException("unsupported");
    }
    
    @Override
    public DalvInsn withRegisterOffset(final int delta) {
        return this.withRegisters(this.getRegisters().withOffset(delta));
    }
}

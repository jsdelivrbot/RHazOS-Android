package embedded.com.android.dx.dex.code;

import embedded.com.android.dx.rop.code.*;

public abstract class VariableSizeInsn extends DalvInsn
{
    public VariableSizeInsn(final SourcePosition position, final RegisterSpecList registers) {
        super(Dops.SPECIAL_FORMAT, position, registers);
    }
    
    @Override
    public final DalvInsn withOpcode(final Dop opcode) {
        throw new RuntimeException("unsupported");
    }
    
    @Override
    public final DalvInsn withRegisterOffset(final int delta) {
        return this.withRegisters(this.getRegisters().withOffset(delta));
    }
}

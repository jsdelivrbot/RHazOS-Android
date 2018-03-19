package embedded.com.android.dx.dex.code;

import embedded.com.android.dx.rop.code.*;

public final class SimpleInsn extends FixedSizeInsn
{
    public SimpleInsn(final Dop opcode, final SourcePosition position, final RegisterSpecList registers) {
        super(opcode, position, registers);
    }
    
    @Override
    public DalvInsn withOpcode(final Dop opcode) {
        return new SimpleInsn(opcode, this.getPosition(), this.getRegisters());
    }
    
    @Override
    public DalvInsn withRegisters(final RegisterSpecList registers) {
        return new SimpleInsn(this.getOpcode(), this.getPosition(), registers);
    }
    
    @Override
    protected String argString() {
        return null;
    }
}

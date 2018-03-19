package embedded.com.android.dx.dex.code;

import embedded.com.android.dx.rop.code.*;

public final class TargetInsn extends FixedSizeInsn
{
    private CodeAddress target;
    
    public TargetInsn(final Dop opcode, final SourcePosition position, final RegisterSpecList registers, final CodeAddress target) {
        super(opcode, position, registers);
        if (target == null) {
            throw new NullPointerException("target == null");
        }
        this.target = target;
    }
    
    @Override
    public DalvInsn withOpcode(final Dop opcode) {
        return new TargetInsn(opcode, this.getPosition(), this.getRegisters(), this.target);
    }
    
    @Override
    public DalvInsn withRegisters(final RegisterSpecList registers) {
        return new TargetInsn(this.getOpcode(), this.getPosition(), registers, this.target);
    }
    
    public TargetInsn withNewTargetAndReversed(final CodeAddress target) {
        final Dop opcode = this.getOpcode().getOppositeTest();
        return new TargetInsn(opcode, this.getPosition(), this.getRegisters(), target);
    }
    
    public CodeAddress getTarget() {
        return this.target;
    }
    
    public int getTargetAddress() {
        return this.target.getAddress();
    }
    
    public int getTargetOffset() {
        return this.target.getAddress() - this.getAddress();
    }
    
    public boolean hasTargetOffset() {
        return this.hasAddress() && this.target.hasAddress();
    }
    
    @Override
    protected String argString() {
        if (this.target == null) {
            return "????";
        }
        return this.target.identifierString();
    }
}

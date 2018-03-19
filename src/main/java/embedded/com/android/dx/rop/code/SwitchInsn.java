package embedded.com.android.dx.rop.code;

import embedded.com.android.dx.util.*;
import embedded.com.android.dx.rop.type.*;

public final class SwitchInsn extends Insn
{
    private final IntList cases;
    
    public SwitchInsn(final Rop opcode, final SourcePosition position, final RegisterSpec result, final RegisterSpecList sources, final IntList cases) {
        super(opcode, position, result, sources);
        if (opcode.getBranchingness() != 5) {
            throw new IllegalArgumentException("bogus branchingness");
        }
        if (cases == null) {
            throw new NullPointerException("cases == null");
        }
        this.cases = cases;
    }
    
    @Override
    public String getInlineString() {
        return this.cases.toString();
    }
    
    @Override
    public TypeList getCatches() {
        return StdTypeList.EMPTY;
    }
    
    @Override
    public void accept(final Visitor visitor) {
        visitor.visitSwitchInsn(this);
    }
    
    @Override
    public Insn withAddedCatch(final Type type) {
        throw new UnsupportedOperationException("unsupported");
    }
    
    @Override
    public Insn withRegisterOffset(final int delta) {
        return new SwitchInsn(this.getOpcode(), this.getPosition(), this.getResult().withOffset(delta), this.getSources().withOffset(delta), this.cases);
    }
    
    @Override
    public boolean contentEquals(final Insn b) {
        return false;
    }
    
    @Override
    public Insn withNewRegisters(final RegisterSpec result, final RegisterSpecList sources) {
        return new SwitchInsn(this.getOpcode(), this.getPosition(), result, sources, this.cases);
    }
    
    public IntList getCases() {
        return this.cases;
    }
}

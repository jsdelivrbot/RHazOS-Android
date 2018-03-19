package embedded.com.android.dx.rop.code;

import embedded.com.android.dx.rop.cst.*;
import embedded.com.android.dx.rop.type.*;

public final class PlainCstInsn extends CstInsn
{
    public PlainCstInsn(final Rop opcode, final SourcePosition position, final RegisterSpec result, final RegisterSpecList sources, final Constant cst) {
        super(opcode, position, result, sources, cst);
        if (opcode.getBranchingness() != 1) {
            throw new IllegalArgumentException("opcode with invalid branchingness: " + opcode.getBranchingness());
        }
    }
    
    @Override
    public TypeList getCatches() {
        return StdTypeList.EMPTY;
    }
    
    @Override
    public void accept(final Visitor visitor) {
        visitor.visitPlainCstInsn(this);
    }
    
    @Override
    public Insn withAddedCatch(final Type type) {
        throw new UnsupportedOperationException("unsupported");
    }
    
    @Override
    public Insn withRegisterOffset(final int delta) {
        return new PlainCstInsn(this.getOpcode(), this.getPosition(), this.getResult().withOffset(delta), this.getSources().withOffset(delta), this.getConstant());
    }
    
    @Override
    public Insn withNewRegisters(final RegisterSpec result, final RegisterSpecList sources) {
        return new PlainCstInsn(this.getOpcode(), this.getPosition(), result, sources, this.getConstant());
    }
}

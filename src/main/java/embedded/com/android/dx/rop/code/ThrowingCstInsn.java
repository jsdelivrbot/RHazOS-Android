package embedded.com.android.dx.rop.code;

import embedded.com.android.dx.rop.cst.*;
import embedded.com.android.dx.rop.type.*;

public final class ThrowingCstInsn extends CstInsn
{
    private final TypeList catches;
    
    public ThrowingCstInsn(final Rop opcode, final SourcePosition position, final RegisterSpecList sources, final TypeList catches, final Constant cst) {
        super(opcode, position, null, sources, cst);
        if (opcode.getBranchingness() != 6) {
            throw new IllegalArgumentException("opcode with invalid branchingness: " + opcode.getBranchingness());
        }
        if (catches == null) {
            throw new NullPointerException("catches == null");
        }
        this.catches = catches;
    }
    
    @Override
    public String getInlineString() {
        final Constant cst = this.getConstant();
        String constantString = cst.toHuman();
        if (cst instanceof CstString) {
            constantString = ((CstString)cst).toQuoted();
        }
        return constantString + " " + ThrowingInsn.toCatchString(this.catches);
    }
    
    @Override
    public TypeList getCatches() {
        return this.catches;
    }
    
    @Override
    public void accept(final Visitor visitor) {
        visitor.visitThrowingCstInsn(this);
    }
    
    @Override
    public Insn withAddedCatch(final Type type) {
        return new ThrowingCstInsn(this.getOpcode(), this.getPosition(), this.getSources(), this.catches.withAddedType(type), this.getConstant());
    }
    
    @Override
    public Insn withRegisterOffset(final int delta) {
        return new ThrowingCstInsn(this.getOpcode(), this.getPosition(), this.getSources().withOffset(delta), this.catches, this.getConstant());
    }
    
    @Override
    public Insn withNewRegisters(final RegisterSpec result, final RegisterSpecList sources) {
        return new ThrowingCstInsn(this.getOpcode(), this.getPosition(), sources, this.catches, this.getConstant());
    }
}

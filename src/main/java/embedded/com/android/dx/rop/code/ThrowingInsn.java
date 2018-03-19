package embedded.com.android.dx.rop.code;

import embedded.com.android.dx.rop.type.*;

public final class ThrowingInsn extends Insn
{
    private final TypeList catches;
    
    public static String toCatchString(final TypeList catches) {
        final StringBuffer sb = new StringBuffer(100);
        sb.append("catch");
        for (int sz = catches.size(), i = 0; i < sz; ++i) {
            sb.append(" ");
            sb.append(catches.getType(i).toHuman());
        }
        return sb.toString();
    }
    
    public ThrowingInsn(final Rop opcode, final SourcePosition position, final RegisterSpecList sources, final TypeList catches) {
        super(opcode, position, null, sources);
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
        return toCatchString(this.catches);
    }
    
    @Override
    public TypeList getCatches() {
        return this.catches;
    }
    
    @Override
    public void accept(final Visitor visitor) {
        visitor.visitThrowingInsn(this);
    }
    
    @Override
    public Insn withAddedCatch(final Type type) {
        return new ThrowingInsn(this.getOpcode(), this.getPosition(), this.getSources(), this.catches.withAddedType(type));
    }
    
    @Override
    public Insn withRegisterOffset(final int delta) {
        return new ThrowingInsn(this.getOpcode(), this.getPosition(), this.getSources().withOffset(delta), this.catches);
    }
    
    @Override
    public Insn withNewRegisters(final RegisterSpec result, final RegisterSpecList sources) {
        return new ThrowingInsn(this.getOpcode(), this.getPosition(), sources, this.catches);
    }
}

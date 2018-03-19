package embedded.com.android.dx.rop.code;

import embedded.com.android.dx.rop.cst.*;
import embedded.com.android.dx.rop.type.*;

public final class PlainInsn extends Insn
{
    public PlainInsn(final Rop opcode, final SourcePosition position, final RegisterSpec result, final RegisterSpecList sources) {
        super(opcode, position, result, sources);
        switch (opcode.getBranchingness()) {
            case 5:
            case 6: {
                throw new IllegalArgumentException("opcode with invalid branchingness: " + opcode.getBranchingness());
            }
            default: {
                if (result != null && opcode.getBranchingness() != 1) {
                    throw new IllegalArgumentException("can't mix branchingness with result");
                }
            }
        }
    }
    
    public PlainInsn(final Rop opcode, final SourcePosition position, final RegisterSpec result, final RegisterSpec source) {
        this(opcode, position, result, RegisterSpecList.make(source));
    }
    
    @Override
    public TypeList getCatches() {
        return StdTypeList.EMPTY;
    }
    
    @Override
    public void accept(final Visitor visitor) {
        visitor.visitPlainInsn(this);
    }
    
    @Override
    public Insn withAddedCatch(final Type type) {
        throw new UnsupportedOperationException("unsupported");
    }
    
    @Override
    public Insn withRegisterOffset(final int delta) {
        return new PlainInsn(this.getOpcode(), this.getPosition(), this.getResult().withOffset(delta), this.getSources().withOffset(delta));
    }
    
    @Override
    public Insn withSourceLiteral() {
        final RegisterSpecList sources = this.getSources();
        final int szSources = sources.size();
        if (szSources == 0) {
            return this;
        }
        final TypeBearer lastType = sources.get(szSources - 1).getTypeBearer();
        if (lastType.isConstant()) {
            Constant cst = (Constant)lastType;
            final RegisterSpecList newSources = sources.withoutLast();
            Rop newRop;
            try {
                int opcode = this.getOpcode().getOpcode();
                if (opcode == 15 && cst instanceof CstInteger) {
                    opcode = 14;
                    cst = CstInteger.make(-((CstInteger)cst).getValue());
                }
                newRop = Rops.ropFor(opcode, this.getResult(), newSources, cst);
            }
            catch (IllegalArgumentException ex) {
                return this;
            }
            return new PlainCstInsn(newRop, this.getPosition(), this.getResult(), newSources, cst);
        }
        final TypeBearer firstType = sources.get(0).getTypeBearer();
        if (szSources == 2 && firstType.isConstant()) {
            final Constant cst2 = (Constant)firstType;
            final RegisterSpecList newSources2 = sources.withoutFirst();
            final Rop newRop2 = Rops.ropFor(this.getOpcode().getOpcode(), this.getResult(), newSources2, cst2);
            return new PlainCstInsn(newRop2, this.getPosition(), this.getResult(), newSources2, cst2);
        }
        return this;
    }
    
    @Override
    public Insn withNewRegisters(final RegisterSpec result, final RegisterSpecList sources) {
        return new PlainInsn(this.getOpcode(), this.getPosition(), result, sources);
    }
}

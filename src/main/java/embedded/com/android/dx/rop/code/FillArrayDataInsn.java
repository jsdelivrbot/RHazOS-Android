package embedded.com.android.dx.rop.code;

import java.util.*;
import embedded.com.android.dx.rop.cst.*;
import embedded.com.android.dx.rop.type.*;

public final class FillArrayDataInsn extends Insn
{
    private final ArrayList<Constant> initValues;
    private final Constant arrayType;
    
    public FillArrayDataInsn(final Rop opcode, final SourcePosition position, final RegisterSpecList sources, final ArrayList<Constant> initValues, final Constant cst) {
        super(opcode, position, null, sources);
        if (opcode.getBranchingness() != 1) {
            throw new IllegalArgumentException("opcode with invalid branchingness: " + opcode.getBranchingness());
        }
        this.initValues = initValues;
        this.arrayType = cst;
    }
    
    @Override
    public TypeList getCatches() {
        return StdTypeList.EMPTY;
    }
    
    public ArrayList<Constant> getInitValues() {
        return this.initValues;
    }
    
    public Constant getConstant() {
        return this.arrayType;
    }
    
    @Override
    public void accept(final Visitor visitor) {
        visitor.visitFillArrayDataInsn(this);
    }
    
    @Override
    public Insn withAddedCatch(final Type type) {
        throw new UnsupportedOperationException("unsupported");
    }
    
    @Override
    public Insn withRegisterOffset(final int delta) {
        return new FillArrayDataInsn(this.getOpcode(), this.getPosition(), this.getSources().withOffset(delta), this.initValues, this.arrayType);
    }
    
    @Override
    public Insn withNewRegisters(final RegisterSpec result, final RegisterSpecList sources) {
        return new FillArrayDataInsn(this.getOpcode(), this.getPosition(), sources, this.initValues, this.arrayType);
    }
}

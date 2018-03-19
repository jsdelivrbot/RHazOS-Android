package embedded.com.android.dx.rop.code;

import embedded.com.android.dx.rop.cst.*;

public abstract class CstInsn extends Insn
{
    private final Constant cst;
    
    public CstInsn(final Rop opcode, final SourcePosition position, final RegisterSpec result, final RegisterSpecList sources, final Constant cst) {
        super(opcode, position, result, sources);
        if (cst == null) {
            throw new NullPointerException("cst == null");
        }
        this.cst = cst;
    }
    
    @Override
    public String getInlineString() {
        return this.cst.toHuman();
    }
    
    public Constant getConstant() {
        return this.cst;
    }
    
    @Override
    public boolean contentEquals(final Insn b) {
        return super.contentEquals(b) && this.cst.equals(((CstInsn)b).getConstant());
    }
}

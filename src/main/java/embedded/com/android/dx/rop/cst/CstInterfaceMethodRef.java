package embedded.com.android.dx.rop.cst;

public final class CstInterfaceMethodRef extends CstBaseMethodRef
{
    private CstMethodRef methodRef;
    
    public CstInterfaceMethodRef(final CstType definingClass, final CstNat nat) {
        super(definingClass, nat);
        this.methodRef = null;
    }
    
    @Override
    public String typeName() {
        return "ifaceMethod";
    }
    
    public CstMethodRef toMethodRef() {
        if (this.methodRef == null) {
            this.methodRef = new CstMethodRef(this.getDefiningClass(), this.getNat());
        }
        return this.methodRef;
    }
}

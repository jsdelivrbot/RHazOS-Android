package embedded.com.android.dx.rop.cst;

public final class CstMethodRef extends CstBaseMethodRef
{
    public CstMethodRef(final CstType definingClass, final CstNat nat) {
        super(definingClass, nat);
    }
    
    @Override
    public String typeName() {
        return "method";
    }
}

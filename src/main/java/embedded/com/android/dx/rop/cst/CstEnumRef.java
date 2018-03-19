package embedded.com.android.dx.rop.cst;

import embedded.com.android.dx.rop.type.*;

public final class CstEnumRef extends CstMemberRef
{
    private CstFieldRef fieldRef;
    
    public CstEnumRef(final CstNat nat) {
        super(new CstType(nat.getFieldType()), nat);
        this.fieldRef = null;
    }
    
    @Override
    public String typeName() {
        return "enum";
    }
    
    @Override
    public Type getType() {
        return this.getDefiningClass().getClassType();
    }
    
    public CstFieldRef getFieldRef() {
        if (this.fieldRef == null) {
            this.fieldRef = new CstFieldRef(this.getDefiningClass(), this.getNat());
        }
        return this.fieldRef;
    }
}

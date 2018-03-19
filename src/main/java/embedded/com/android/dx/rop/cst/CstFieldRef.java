package embedded.com.android.dx.rop.cst;

import embedded.com.android.dx.rop.type.*;

public final class CstFieldRef extends CstMemberRef
{
    public static CstFieldRef forPrimitiveType(final Type primitiveType) {
        return new CstFieldRef(CstType.forBoxedPrimitiveType(primitiveType), CstNat.PRIMITIVE_TYPE_NAT);
    }
    
    public CstFieldRef(final CstType definingClass, final CstNat nat) {
        super(definingClass, nat);
    }
    
    @Override
    public String typeName() {
        return "field";
    }
    
    @Override
    public Type getType() {
        return this.getNat().getFieldType();
    }
    
    @Override
    protected int compareTo0(final Constant other) {
        final int cmp = super.compareTo0(other);
        if (cmp != 0) {
            return cmp;
        }
        final CstFieldRef otherField = (CstFieldRef)other;
        final CstString thisDescriptor = this.getNat().getDescriptor();
        final CstString otherDescriptor = otherField.getNat().getDescriptor();
        return thisDescriptor.compareTo((Constant)otherDescriptor);
    }
}

package embedded.com.android.dx.rop.cst;

public abstract class CstMemberRef extends TypedConstant
{
    private final CstType definingClass;
    private final CstNat nat;
    
    CstMemberRef(final CstType definingClass, final CstNat nat) {
        if (definingClass == null) {
            throw new NullPointerException("definingClass == null");
        }
        if (nat == null) {
            throw new NullPointerException("nat == null");
        }
        this.definingClass = definingClass;
        this.nat = nat;
    }
    
    @Override
    public final boolean equals(final Object other) {
        if (other == null || this.getClass() != other.getClass()) {
            return false;
        }
        final CstMemberRef otherRef = (CstMemberRef)other;
        return this.definingClass.equals(otherRef.definingClass) && this.nat.equals(otherRef.nat);
    }
    
    @Override
    public final int hashCode() {
        return this.definingClass.hashCode() * 31 ^ this.nat.hashCode();
    }
    
    @Override
    protected int compareTo0(final Constant other) {
        final CstMemberRef otherMember = (CstMemberRef)other;
        final int cmp = this.definingClass.compareTo((Constant)otherMember.definingClass);
        if (cmp != 0) {
            return cmp;
        }
        final CstString thisName = this.nat.getName();
        final CstString otherName = otherMember.nat.getName();
        return thisName.compareTo((Constant)otherName);
    }
    
    @Override
    public final String toString() {
        return this.typeName() + '{' + this.toHuman() + '}';
    }
    
    @Override
    public final boolean isCategory2() {
        return false;
    }
    
    @Override
    public final String toHuman() {
        return this.definingClass.toHuman() + '.' + this.nat.toHuman();
    }
    
    public final CstType getDefiningClass() {
        return this.definingClass;
    }
    
    public final CstNat getNat() {
        return this.nat;
    }
}

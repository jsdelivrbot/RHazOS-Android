package embedded.com.android.dx.rop.cst;

import embedded.com.android.dx.rop.type.*;

public abstract class CstBaseMethodRef extends CstMemberRef
{
    private final Prototype prototype;
    private Prototype instancePrototype;
    
    CstBaseMethodRef(final CstType definingClass, final CstNat nat) {
        super(definingClass, nat);
        final String descriptor = this.getNat().getDescriptor().getString();
        if (this.isSignaturePolymorphic()) {
            this.prototype = Prototype.fromDescriptor(descriptor);
        }
        else {
            this.prototype = Prototype.intern(descriptor);
        }
        this.instancePrototype = null;
    }
    
    public final Prototype getPrototype() {
        return this.prototype;
    }
    
    public final Prototype getPrototype(final boolean isStatic) {
        if (isStatic) {
            return this.prototype;
        }
        if (this.instancePrototype == null) {
            final Type thisType = this.getDefiningClass().getClassType();
            this.instancePrototype = this.prototype.withFirstParameter(thisType);
        }
        return this.instancePrototype;
    }
    
    @Override
    protected final int compareTo0(final Constant other) {
        final int cmp = super.compareTo0(other);
        if (cmp != 0) {
            return cmp;
        }
        final CstBaseMethodRef otherMethod = (CstBaseMethodRef)other;
        return this.prototype.compareTo(otherMethod.prototype);
    }
    
    @Override
    public final Type getType() {
        return this.prototype.getReturnType();
    }
    
    public final int getParameterWordCount(final boolean isStatic) {
        return this.getPrototype(isStatic).getParameterTypes().getWordCount();
    }
    
    public final boolean isInstanceInit() {
        return this.getNat().isInstanceInit();
    }
    
    public final boolean isClassInit() {
        return this.getNat().isClassInit();
    }
    
    public final boolean isSignaturePolymorphic() {
        return this.getDefiningClass().equals(CstType.METHOD_HANDLE) && this.getNat().isSignaturePolymorphic();
    }
}

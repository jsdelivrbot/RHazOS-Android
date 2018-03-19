package embedded.com.android.dx.rop.cst;

import embedded.com.android.dx.rop.type.*;

public final class CstProtoRef extends Constant
{
    private final Prototype prototype;
    
    public CstProtoRef(final Prototype prototype) {
        this.prototype = prototype;
    }
    
    public static CstProtoRef make(final CstString descriptor) {
        final Prototype prototype = Prototype.fromDescriptor(descriptor.getString());
        return new CstProtoRef(prototype);
    }
    
    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof CstProtoRef)) {
            return false;
        }
        final CstProtoRef otherCstProtoRef = (CstProtoRef)other;
        return this.getPrototype().equals(otherCstProtoRef.getPrototype());
    }
    
    @Override
    public int hashCode() {
        return this.prototype.hashCode();
    }
    
    @Override
    public boolean isCategory2() {
        return false;
    }
    
    @Override
    public String typeName() {
        return "proto";
    }
    
    @Override
    protected int compareTo0(final Constant other) {
        final CstProtoRef otherCstProtoRef = (CstProtoRef)other;
        return this.prototype.compareTo(otherCstProtoRef.getPrototype());
    }
    
    @Override
    public String toHuman() {
        return this.prototype.getDescriptor();
    }
    
    @Override
    public final String toString() {
        return this.typeName() + "{" + this.toHuman() + '}';
    }
    
    public Prototype getPrototype() {
        return this.prototype;
    }
}

package embedded.com.android.dx.rop.cst;

import embedded.com.android.dx.rop.type.*;

public final class CstNat extends Constant
{
    public static final CstNat PRIMITIVE_TYPE_NAT;
    private final CstString name;
    private final CstString descriptor;
    
    public CstNat(final CstString name, final CstString descriptor) {
        if (name == null) {
            throw new NullPointerException("name == null");
        }
        if (descriptor == null) {
            throw new NullPointerException("descriptor == null");
        }
        this.name = name;
        this.descriptor = descriptor;
    }
    
    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof CstNat)) {
            return false;
        }
        final CstNat otherNat = (CstNat)other;
        return this.name.equals(otherNat.name) && this.descriptor.equals(otherNat.descriptor);
    }
    
    @Override
    public int hashCode() {
        return this.name.hashCode() * 31 ^ this.descriptor.hashCode();
    }
    
    @Override
    protected int compareTo0(final Constant other) {
        final CstNat otherNat = (CstNat)other;
        final int cmp = this.name.compareTo((Constant)otherNat.name);
        if (cmp != 0) {
            return cmp;
        }
        return this.descriptor.compareTo((Constant)otherNat.descriptor);
    }
    
    @Override
    public String toString() {
        return "nat{" + this.toHuman() + '}';
    }
    
    @Override
    public String typeName() {
        return "nat";
    }
    
    @Override
    public boolean isCategory2() {
        return false;
    }
    
    public CstString getName() {
        return this.name;
    }
    
    public CstString getDescriptor() {
        return this.descriptor;
    }
    
    @Override
    public String toHuman() {
        return this.name.toHuman() + ':' + this.descriptor.toHuman();
    }
    
    public Type getFieldType() {
        return Type.intern(this.descriptor.getString());
    }
    
    public final boolean isInstanceInit() {
        return this.name.getString().equals("<init>");
    }
    
    public final boolean isClassInit() {
        return this.name.getString().equals("<clinit>");
    }
    
    public final boolean isSignaturePolymorphic() {
        final String INVOKE = "invoke";
        final String INVOKE_EXACT = "invokeExact";
        final int nameLength = this.name.getUtf8Size();
        if (nameLength == "invoke".length()) {
            return this.name.getString().equals("invoke");
        }
        return nameLength == "invokeExact".length() && this.name.getString().equals("invokeExact");
    }
    
    static {
        PRIMITIVE_TYPE_NAT = new CstNat(new CstString("TYPE"), new CstString("Ljava/lang/Class;"));
    }
}

package embedded.com.android.dx.rop.cst;

import java.util.*;
import embedded.com.android.dx.rop.type.*;

public final class CstInvokeDynamic extends Constant
{
    private final int bootstrapMethodIndex;
    private final CstNat nat;
    private final Prototype prototype;
    private CstType declaringClass;
    private CstCallSite callSite;
    private final List<CstCallSiteRef> references;
    
    public static CstInvokeDynamic make(final int bootstrapMethodIndex, final CstNat nat) {
        return new CstInvokeDynamic(bootstrapMethodIndex, nat);
    }
    
    private CstInvokeDynamic(final int bootstrapMethodIndex, final CstNat nat) {
        this.bootstrapMethodIndex = bootstrapMethodIndex;
        this.nat = nat;
        this.prototype = Prototype.fromDescriptor(nat.getDescriptor().toHuman());
        this.references = new ArrayList<CstCallSiteRef>();
    }
    
    public CstCallSiteRef addReference() {
        final CstCallSiteRef ref = new CstCallSiteRef(this, this.references.size());
        this.references.add(ref);
        return ref;
    }
    
    public List<CstCallSiteRef> getReferences() {
        return this.references;
    }
    
    @Override
    public String toString() {
        return this.toHuman();
    }
    
    @Override
    public String typeName() {
        return "InvokeDynamic";
    }
    
    @Override
    public String toHuman() {
        final String klass = (this.declaringClass != null) ? this.declaringClass.toHuman() : "Unknown";
        return "InvokeDynamic(" + klass + ":" + this.bootstrapMethodIndex + ", " + this.nat.toHuman() + ")";
    }
    
    @Override
    public boolean isCategory2() {
        return false;
    }
    
    @Override
    protected int compareTo0(final Constant other) {
        final CstInvokeDynamic otherInvoke = (CstInvokeDynamic)other;
        int result = Integer.compare(this.bootstrapMethodIndex, otherInvoke.getBootstrapMethodIndex());
        if (result != 0) {
            return result;
        }
        result = this.nat.compareTo((Constant)otherInvoke.getNat());
        if (result != 0) {
            return result;
        }
        result = this.declaringClass.compareTo((Constant)otherInvoke.getDeclaringClass());
        if (result != 0) {
            return result;
        }
        return this.callSite.compareTo((Constant)otherInvoke.getCallSite());
    }
    
    public int getBootstrapMethodIndex() {
        return this.bootstrapMethodIndex;
    }
    
    public CstNat getNat() {
        return this.nat;
    }
    
    public Prototype getPrototype() {
        return this.prototype;
    }
    
    public Type getReturnType() {
        return this.prototype.getReturnType();
    }
    
    public void setDeclaringClass(final CstType declaringClass) {
        if (this.declaringClass != null) {
            throw new IllegalArgumentException("already added declaring class");
        }
        if (declaringClass == null) {
            throw new NullPointerException("declaringClass == null");
        }
        this.declaringClass = declaringClass;
    }
    
    public CstType getDeclaringClass() {
        return this.declaringClass;
    }
    
    public void setCallSite(final CstCallSite callSite) {
        if (this.callSite != null) {
            throw new IllegalArgumentException("already added call site");
        }
        if (callSite == null) {
            throw new NullPointerException("callSite == null");
        }
        this.callSite = callSite;
    }
    
    public CstCallSite getCallSite() {
        return this.callSite;
    }
}

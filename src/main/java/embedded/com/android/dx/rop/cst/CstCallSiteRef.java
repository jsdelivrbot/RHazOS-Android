package embedded.com.android.dx.rop.cst;

import embedded.com.android.dx.rop.type.*;

public class CstCallSiteRef extends Constant
{
    private final CstInvokeDynamic invokeDynamic;
    private final int id;
    
    CstCallSiteRef(final CstInvokeDynamic invokeDynamic, final int id) {
        if (invokeDynamic == null) {
            throw new NullPointerException("invokeDynamic == null");
        }
        this.invokeDynamic = invokeDynamic;
        this.id = id;
    }
    
    @Override
    public boolean isCategory2() {
        return false;
    }
    
    @Override
    public String typeName() {
        return "CallSiteRef";
    }
    
    @Override
    protected int compareTo0(final Constant other) {
        final CstCallSiteRef o = (CstCallSiteRef)other;
        final int result = this.invokeDynamic.compareTo((Constant)o.invokeDynamic);
        if (result != 0) {
            return result;
        }
        return Integer.compare(this.id, o.id);
    }
    
    @Override
    public String toHuman() {
        return this.getCallSite().toHuman();
    }
    
    @Override
    public String toString() {
        return this.getCallSite().toString();
    }
    
    public Prototype getPrototype() {
        return this.invokeDynamic.getPrototype();
    }
    
    public Type getReturnType() {
        return this.invokeDynamic.getReturnType();
    }
    
    public CstCallSite getCallSite() {
        return this.invokeDynamic.getCallSite();
    }
}

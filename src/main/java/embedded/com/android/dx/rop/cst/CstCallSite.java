package embedded.com.android.dx.rop.cst;

import embedded.com.android.dx.cf.code.*;
import embedded.com.android.dx.rop.type.*;

public final class CstCallSite extends CstArray
{
    public static CstCallSite make(final CstMethodHandle bootstrapHandle, final CstNat nat, final BootstrapMethodArgumentsList optionalArguments) {
        if (bootstrapHandle == null) {
            throw new NullPointerException("bootstrapMethodHandle == null");
        }
        if (nat == null) {
            throw new NullPointerException("nat == null");
        }
        final List list = new List(3 + optionalArguments.size());
        list.set(0, bootstrapHandle);
        list.set(1, nat.getName());
        list.set(2, new CstProtoRef(Prototype.fromDescriptor(nat.getDescriptor().getString())));
        if (optionalArguments != null) {
            for (int i = 0; i < optionalArguments.size(); ++i) {
                list.set(i + 3, optionalArguments.get(i));
            }
        }
        list.setImmutable();
        return new CstCallSite(list);
    }
    
    private CstCallSite(final List list) {
        super(list);
    }
    
    @Override
    public boolean equals(final Object other) {
        return other instanceof CstCallSite && this.getList().equals(((CstCallSite)other).getList());
    }
    
    @Override
    public int hashCode() {
        return this.getList().hashCode();
    }
    
    @Override
    protected int compareTo0(final Constant other) {
        return this.getList().compareTo(((CstCallSite)other).getList());
    }
    
    @Override
    public String toString() {
        return this.getList().toString("call site{", ", ", "}");
    }
    
    @Override
    public String typeName() {
        return "call site";
    }
    
    @Override
    public boolean isCategory2() {
        return false;
    }
    
    @Override
    public String toHuman() {
        return this.getList().toHuman("{", ", ", "}");
    }
}

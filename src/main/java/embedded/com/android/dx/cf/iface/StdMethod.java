package embedded.com.android.dx.cf.iface;

import embedded.com.android.dx.rop.type.*;
import embedded.com.android.dx.rop.cst.*;
import embedded.com.android.dx.rop.code.*;

public final class StdMethod extends StdMember implements Method
{
    private final Prototype effectiveDescriptor;
    
    public StdMethod(final CstType definingClass, final int accessFlags, final CstNat nat, final AttributeList attributes) {
        super(definingClass, accessFlags, nat, attributes);
        final String descStr = this.getDescriptor().getString();
        this.effectiveDescriptor = Prototype.intern(descStr, definingClass.getClassType(), AccessFlags.isStatic(accessFlags), nat.isInstanceInit());
    }
    
    @Override
    public Prototype getEffectiveDescriptor() {
        return this.effectiveDescriptor;
    }
}

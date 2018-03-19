package embedded.com.android.dx.cf.direct;

import embedded.com.android.dx.rop.code.*;
import embedded.com.android.dx.rop.cst.*;
import embedded.com.android.dx.cf.iface.*;

final class MethodListParser extends MemberListParser
{
    private final StdMethodList methods;
    
    public MethodListParser(final DirectClassFile cf, final CstType definer, final int offset, final AttributeFactory attributeFactory) {
        super(cf, definer, offset, attributeFactory);
        this.methods = new StdMethodList(this.getCount());
    }
    
    public StdMethodList getList() {
        this.parseIfNecessary();
        return this.methods;
    }
    
    @Override
    protected String humanName() {
        return "method";
    }
    
    @Override
    protected String humanAccessFlags(final int accessFlags) {
        return AccessFlags.methodString(accessFlags);
    }
    
    @Override
    protected int getAttributeContext() {
        return 2;
    }
    
    @Override
    protected Member set(final int n, final int accessFlags, final CstNat nat, final AttributeList attributes) {
        final StdMethod meth = new StdMethod(this.getDefiner(), accessFlags, nat, attributes);
        this.methods.set(n, meth);
        return meth;
    }
}

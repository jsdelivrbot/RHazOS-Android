package embedded.com.android.dx.cf.direct;

import embedded.com.android.dx.rop.code.*;
import embedded.com.android.dx.rop.cst.*;
import embedded.com.android.dx.cf.iface.*;

final class FieldListParser extends MemberListParser
{
    private final StdFieldList fields;
    
    public FieldListParser(final DirectClassFile cf, final CstType definer, final int offset, final AttributeFactory attributeFactory) {
        super(cf, definer, offset, attributeFactory);
        this.fields = new StdFieldList(this.getCount());
    }
    
    public StdFieldList getList() {
        this.parseIfNecessary();
        return this.fields;
    }
    
    @Override
    protected String humanName() {
        return "field";
    }
    
    @Override
    protected String humanAccessFlags(final int accessFlags) {
        return AccessFlags.fieldString(accessFlags);
    }
    
    @Override
    protected int getAttributeContext() {
        return 1;
    }
    
    @Override
    protected Member set(final int n, final int accessFlags, final CstNat nat, final AttributeList attributes) {
        final StdField field = new StdField(this.getDefiner(), accessFlags, nat, attributes);
        this.fields.set(n, field);
        return field;
    }
}

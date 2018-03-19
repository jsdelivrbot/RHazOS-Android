package embedded.com.android.dx.cf.iface;

import embedded.com.android.dx.rop.cst.*;
import embedded.com.android.dx.cf.attrib.*;

public final class StdField extends StdMember implements Field
{
    public StdField(final CstType definingClass, final int accessFlags, final CstNat nat, final AttributeList attributes) {
        super(definingClass, accessFlags, nat, attributes);
    }
    
    @Override
    public TypedConstant getConstantValue() {
        final AttributeList attribs = this.getAttributes();
        final AttConstantValue cval = (AttConstantValue)attribs.findFirst("ConstantValue");
        if (cval == null) {
            return null;
        }
        return cval.getConstantValue();
    }
}

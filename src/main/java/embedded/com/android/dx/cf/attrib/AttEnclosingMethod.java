package embedded.com.android.dx.cf.attrib;

import embedded.com.android.dx.rop.cst.*;

public final class AttEnclosingMethod extends BaseAttribute
{
    public static final String ATTRIBUTE_NAME = "EnclosingMethod";
    private final CstType type;
    private final CstNat method;
    
    public AttEnclosingMethod(final CstType type, final CstNat method) {
        super("EnclosingMethod");
        if (type == null) {
            throw new NullPointerException("type == null");
        }
        this.type = type;
        this.method = method;
    }
    
    @Override
    public int byteLength() {
        return 10;
    }
    
    public CstType getEnclosingClass() {
        return this.type;
    }
    
    public CstNat getMethod() {
        return this.method;
    }
}

package embedded.com.android.dx.cf.attrib;

import embedded.com.android.dx.rop.cst.*;

public final class AttSignature extends BaseAttribute
{
    public static final String ATTRIBUTE_NAME = "Signature";
    private final CstString signature;
    
    public AttSignature(final CstString signature) {
        super("Signature");
        if (signature == null) {
            throw new NullPointerException("signature == null");
        }
        this.signature = signature;
    }
    
    @Override
    public int byteLength() {
        return 8;
    }
    
    public CstString getSignature() {
        return this.signature;
    }
}

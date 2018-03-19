package embedded.com.android.dx.cf.attrib;

import embedded.com.android.dx.rop.cst.*;

public final class AttSourceDebugExtension extends BaseAttribute
{
    public static final String ATTRIBUTE_NAME = "SourceDebugExtension";
    private final CstString smapString;
    
    public AttSourceDebugExtension(final CstString smapString) {
        super("SourceDebugExtension");
        if (smapString == null) {
            throw new NullPointerException("smapString == null");
        }
        this.smapString = smapString;
    }
    
    @Override
    public int byteLength() {
        return 6 + this.smapString.getUtf8Size();
    }
    
    public CstString getSmapString() {
        return this.smapString;
    }
}

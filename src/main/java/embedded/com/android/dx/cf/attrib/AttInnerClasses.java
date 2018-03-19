package embedded.com.android.dx.cf.attrib;

import embedded.com.android.dx.util.*;

public final class AttInnerClasses extends BaseAttribute
{
    public static final String ATTRIBUTE_NAME = "InnerClasses";
    private final InnerClassList innerClasses;
    
    public AttInnerClasses(final InnerClassList innerClasses) {
        super("InnerClasses");
        try {
            if (innerClasses.isMutable()) {
                throw new MutabilityException("innerClasses.isMutable()");
            }
        }
        catch (NullPointerException ex) {
            throw new NullPointerException("innerClasses == null");
        }
        this.innerClasses = innerClasses;
    }
    
    @Override
    public int byteLength() {
        return 8 + this.innerClasses.size() * 8;
    }
    
    public InnerClassList getInnerClasses() {
        return this.innerClasses;
    }
}

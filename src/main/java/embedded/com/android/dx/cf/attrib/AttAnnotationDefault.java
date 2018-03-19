package embedded.com.android.dx.cf.attrib;

import embedded.com.android.dx.rop.cst.*;

public final class AttAnnotationDefault extends BaseAttribute
{
    public static final String ATTRIBUTE_NAME = "AnnotationDefault";
    private final Constant value;
    private final int byteLength;
    
    public AttAnnotationDefault(final Constant value, final int byteLength) {
        super("AnnotationDefault");
        if (value == null) {
            throw new NullPointerException("value == null");
        }
        this.value = value;
        this.byteLength = byteLength;
    }
    
    @Override
    public int byteLength() {
        return this.byteLength + 6;
    }
    
    public Constant getValue() {
        return this.value;
    }
}

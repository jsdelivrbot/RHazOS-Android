package embedded.com.android.dx.cf.attrib;

import embedded.com.android.dx.rop.annotation.*;

public final class AttRuntimeInvisibleAnnotations extends BaseAnnotations
{
    public static final String ATTRIBUTE_NAME = "RuntimeInvisibleAnnotations";
    
    public AttRuntimeInvisibleAnnotations(final Annotations annotations, final int byteLength) {
        super("RuntimeInvisibleAnnotations", annotations, byteLength);
    }
}

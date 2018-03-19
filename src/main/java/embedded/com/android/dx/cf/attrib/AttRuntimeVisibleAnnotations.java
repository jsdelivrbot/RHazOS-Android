package embedded.com.android.dx.cf.attrib;

import embedded.com.android.dx.rop.annotation.*;

public final class AttRuntimeVisibleAnnotations extends BaseAnnotations
{
    public static final String ATTRIBUTE_NAME = "RuntimeVisibleAnnotations";
    
    public AttRuntimeVisibleAnnotations(final Annotations annotations, final int byteLength) {
        super("RuntimeVisibleAnnotations", annotations, byteLength);
    }
}

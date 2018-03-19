package embedded.com.android.dx.cf.attrib;

import embedded.com.android.dx.rop.annotation.*;

public final class AttRuntimeVisibleParameterAnnotations extends BaseParameterAnnotations
{
    public static final String ATTRIBUTE_NAME = "RuntimeVisibleParameterAnnotations";
    
    public AttRuntimeVisibleParameterAnnotations(final AnnotationsList annotations, final int byteLength) {
        super("RuntimeVisibleParameterAnnotations", annotations, byteLength);
    }
}

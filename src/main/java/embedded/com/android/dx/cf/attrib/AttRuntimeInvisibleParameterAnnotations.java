package embedded.com.android.dx.cf.attrib;

import embedded.com.android.dx.rop.annotation.*;

public final class AttRuntimeInvisibleParameterAnnotations extends BaseParameterAnnotations
{
    public static final String ATTRIBUTE_NAME = "RuntimeInvisibleParameterAnnotations";
    
    public AttRuntimeInvisibleParameterAnnotations(final AnnotationsList parameterAnnotations, final int byteLength) {
        super("RuntimeInvisibleParameterAnnotations", parameterAnnotations, byteLength);
    }
}

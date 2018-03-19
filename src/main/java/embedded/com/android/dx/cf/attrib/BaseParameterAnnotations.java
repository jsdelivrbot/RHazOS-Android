package embedded.com.android.dx.cf.attrib;

import embedded.com.android.dx.rop.annotation.*;
import embedded.com.android.dx.util.*;

public abstract class BaseParameterAnnotations extends BaseAttribute
{
    private final AnnotationsList parameterAnnotations;
    private final int byteLength;
    
    public BaseParameterAnnotations(final String attributeName, final AnnotationsList parameterAnnotations, final int byteLength) {
        super(attributeName);
        try {
            if (parameterAnnotations.isMutable()) {
                throw new MutabilityException("parameterAnnotations.isMutable()");
            }
        }
        catch (NullPointerException ex) {
            throw new NullPointerException("parameterAnnotations == null");
        }
        this.parameterAnnotations = parameterAnnotations;
        this.byteLength = byteLength;
    }
    
    @Override
    public final int byteLength() {
        return this.byteLength + 6;
    }
    
    public final AnnotationsList getParameterAnnotations() {
        return this.parameterAnnotations;
    }
}

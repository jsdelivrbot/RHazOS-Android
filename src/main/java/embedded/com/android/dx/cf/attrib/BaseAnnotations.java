package embedded.com.android.dx.cf.attrib;

import embedded.com.android.dx.rop.annotation.*;
import embedded.com.android.dx.util.*;

public abstract class BaseAnnotations extends BaseAttribute
{
    private final Annotations annotations;
    private final int byteLength;
    
    public BaseAnnotations(final String attributeName, final Annotations annotations, final int byteLength) {
        super(attributeName);
        try {
            if (annotations.isMutable()) {
                throw new MutabilityException("annotations.isMutable()");
            }
        }
        catch (NullPointerException ex) {
            throw new NullPointerException("annotations == null");
        }
        this.annotations = annotations;
        this.byteLength = byteLength;
    }
    
    @Override
    public final int byteLength() {
        return this.byteLength + 6;
    }
    
    public final Annotations getAnnotations() {
        return this.annotations;
    }
}

package embedded.com.android.dx.cf.attrib;

import embedded.com.android.dx.rop.type.*;
import embedded.com.android.dx.util.*;

public final class AttExceptions extends BaseAttribute
{
    public static final String ATTRIBUTE_NAME = "Exceptions";
    private final TypeList exceptions;
    
    public AttExceptions(final TypeList exceptions) {
        super("Exceptions");
        try {
            if (exceptions.isMutable()) {
                throw new MutabilityException("exceptions.isMutable()");
            }
        }
        catch (NullPointerException ex) {
            throw new NullPointerException("exceptions == null");
        }
        this.exceptions = exceptions;
    }
    
    @Override
    public int byteLength() {
        return 8 + this.exceptions.size() * 2;
    }
    
    public TypeList getExceptions() {
        return this.exceptions;
    }
}

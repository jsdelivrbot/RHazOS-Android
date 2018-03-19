package embedded.com.android.dx.util;

import embedded.com.android.dex.util.*;

public class MutabilityException extends ExceptionWithContext
{
    public MutabilityException(final String message) {
        super(message);
    }
    
    public MutabilityException(final Throwable cause) {
        super(cause);
    }
    
    public MutabilityException(final String message, final Throwable cause) {
        super(message, cause);
    }
}

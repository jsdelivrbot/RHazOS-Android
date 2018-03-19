package embedded.com.android.dx.cf.code;

import embedded.com.android.dex.util.*;

public class SimException extends ExceptionWithContext
{
    public SimException(final String message) {
        super(message);
    }
    
    public SimException(final Throwable cause) {
        super(cause);
    }
    
    public SimException(final String message, final Throwable cause) {
        super(message, cause);
    }
}

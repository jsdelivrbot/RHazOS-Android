package embedded.com.android.dx.cf.iface;

import embedded.com.android.dex.util.*;

public class ParseException extends ExceptionWithContext
{
    public ParseException(final String message) {
        super(message);
    }
    
    public ParseException(final Throwable cause) {
        super(cause);
    }
    
    public ParseException(final String message, final Throwable cause) {
        super(message, cause);
    }
}

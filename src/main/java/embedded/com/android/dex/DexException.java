package embedded.com.android.dex;

import embedded.com.android.dex.util.*;

public class DexException extends ExceptionWithContext
{
    public DexException(final String message) {
        super(message);
    }
    
    public DexException(final Throwable cause) {
        super(cause);
    }
}

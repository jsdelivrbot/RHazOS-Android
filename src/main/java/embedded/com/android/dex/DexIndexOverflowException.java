package embedded.com.android.dex;

public final class DexIndexOverflowException extends DexException
{
    public DexIndexOverflowException(final String message) {
        super(message);
    }
    
    public DexIndexOverflowException(final Throwable cause) {
        super(cause);
    }
}

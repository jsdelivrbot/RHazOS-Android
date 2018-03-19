package embedded.com.android.dx.util;

public class MutabilityControl
{
    private boolean mutable;
    
    public MutabilityControl() {
        this.mutable = true;
    }
    
    public MutabilityControl(final boolean mutable) {
        this.mutable = mutable;
    }
    
    public void setImmutable() {
        this.mutable = false;
    }
    
    public final boolean isImmutable() {
        return !this.mutable;
    }
    
    public final boolean isMutable() {
        return this.mutable;
    }
    
    public final void throwIfImmutable() {
        if (!this.mutable) {
            throw new MutabilityException("immutable instance");
        }
    }
    
    public final void throwIfMutable() {
        if (this.mutable) {
            throw new MutabilityException("mutable instance");
        }
    }
}

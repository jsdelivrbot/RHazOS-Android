package embedded.com.android.dx.cf.code;

import embedded.com.android.dx.util.*;
import embedded.com.android.dex.util.*;
import embedded.com.android.dx.rop.type.*;
import embedded.com.android.dx.rop.code.*;

public abstract class LocalsArray extends MutabilityControl implements ToHuman
{
    protected LocalsArray(final boolean mutable) {
        super(mutable);
    }
    
    public abstract LocalsArray copy();
    
    public abstract void annotate(final ExceptionWithContext p0);
    
    public abstract void makeInitialized(final Type p0);
    
    public abstract int getMaxLocals();
    
    public abstract void set(final int p0, final TypeBearer p1);
    
    public abstract void set(final RegisterSpec p0);
    
    public abstract void invalidate(final int p0);
    
    public abstract TypeBearer getOrNull(final int p0);
    
    public abstract TypeBearer get(final int p0);
    
    public abstract TypeBearer getCategory1(final int p0);
    
    public abstract TypeBearer getCategory2(final int p0);
    
    public abstract LocalsArray merge(final LocalsArray p0);
    
    public abstract LocalsArraySet mergeWithSubroutineCaller(final LocalsArray p0, final int p1);
    
    protected abstract OneLocalsArray getPrimary();
}

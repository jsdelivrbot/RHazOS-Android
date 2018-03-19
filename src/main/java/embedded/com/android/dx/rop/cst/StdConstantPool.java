package embedded.com.android.dx.rop.cst;

import embedded.com.android.dx.util.*;
import embedded.com.android.dex.util.*;

public final class StdConstantPool extends MutabilityControl implements ConstantPool
{
    private final Constant[] entries;
    
    public StdConstantPool(final int size) {
        super(size > 1);
        if (size < 1) {
            throw new IllegalArgumentException("size < 1");
        }
        this.entries = new Constant[size];
    }
    
    @Override
    public int size() {
        return this.entries.length;
    }
    
    @Override
    public Constant getOrNull(final int n) {
        try {
            return this.entries[n];
        }
        catch (IndexOutOfBoundsException ex) {
            return throwInvalid(n);
        }
    }
    
    @Override
    public Constant get0Ok(final int n) {
        if (n == 0) {
            return null;
        }
        return this.get(n);
    }
    
    @Override
    public Constant get(final int n) {
        try {
            final Constant result = this.entries[n];
            if (result == null) {
                throwInvalid(n);
            }
            return result;
        }
        catch (IndexOutOfBoundsException ex) {
            return throwInvalid(n);
        }
    }
    
    @Override
    public Constant[] getEntries() {
        return this.entries;
    }
    
    public void set(final int n, final Constant cst) {
        this.throwIfImmutable();
        final boolean cat2 = cst != null && cst.isCategory2();
        if (n < 1) {
            throw new IllegalArgumentException("n < 1");
        }
        if (cat2) {
            if (n == this.entries.length - 1) {
                throw new IllegalArgumentException("(n == size - 1) && cst.isCategory2()");
            }
            this.entries[n + 1] = null;
        }
        if (cst != null && this.entries[n] == null) {
            final Constant prev = this.entries[n - 1];
            if (prev != null && prev.isCategory2()) {
                this.entries[n - 1] = null;
            }
        }
        this.entries[n] = cst;
    }
    
    private static Constant throwInvalid(final int idx) {
        throw new ExceptionWithContext("invalid constant pool index " + Hex.u2(idx));
    }
}

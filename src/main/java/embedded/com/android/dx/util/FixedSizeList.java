package embedded.com.android.dx.util;

import java.util.*;

public class FixedSizeList extends MutabilityControl implements ToHuman
{
    private Object[] arr;
    
    public FixedSizeList(final int size) {
        super(size != 0);
        try {
            this.arr = new Object[size];
        }
        catch (NegativeArraySizeException ex) {
            throw new IllegalArgumentException("size < 0");
        }
    }
    
    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || this.getClass() != other.getClass()) {
            return false;
        }
        final FixedSizeList list = (FixedSizeList)other;
        return Arrays.equals(this.arr, list.arr);
    }
    
    @Override
    public int hashCode() {
        return Arrays.hashCode(this.arr);
    }
    
    @Override
    public String toString() {
        final String name = this.getClass().getName();
        return this.toString0(name.substring(name.lastIndexOf(46) + 1) + '{', ", ", "}", false);
    }
    
    @Override
    public String toHuman() {
        final String name = this.getClass().getName();
        return this.toString0(name.substring(name.lastIndexOf(46) + 1) + '{', ", ", "}", true);
    }
    
    public String toString(final String prefix, final String separator, final String suffix) {
        return this.toString0(prefix, separator, suffix, false);
    }
    
    public String toHuman(final String prefix, final String separator, final String suffix) {
        return this.toString0(prefix, separator, suffix, true);
    }
    
    public final int size() {
        return this.arr.length;
    }
    
    public void shrinkToFit() {
        final int sz = this.arr.length;
        int newSz = 0;
        for (int i = 0; i < sz; ++i) {
            if (this.arr[i] != null) {
                ++newSz;
            }
        }
        if (sz == newSz) {
            return;
        }
        this.throwIfImmutable();
        final Object[] newa = new Object[newSz];
        int at = 0;
        for (int j = 0; j < sz; ++j) {
            final Object one = this.arr[j];
            if (one != null) {
                newa[at] = one;
                ++at;
            }
        }
        this.arr = newa;
        if (newSz == 0) {
            this.setImmutable();
        }
    }
    
    protected final Object get0(final int n) {
        try {
            final Object result = this.arr[n];
            if (result == null) {
                throw new NullPointerException("unset: " + n);
            }
            return result;
        }
        catch (ArrayIndexOutOfBoundsException ex) {
            return this.throwIndex(n);
        }
    }
    
    protected final Object getOrNull0(final int n) {
        return this.arr[n];
    }
    
    protected final void set0(final int n, final Object obj) {
        this.throwIfImmutable();
        try {
            this.arr[n] = obj;
        }
        catch (ArrayIndexOutOfBoundsException ex) {
            this.throwIndex(n);
        }
    }
    
    private Object throwIndex(final int n) {
        if (n < 0) {
            throw new IndexOutOfBoundsException("n < 0");
        }
        throw new IndexOutOfBoundsException("n >= size()");
    }
    
    private String toString0(final String prefix, final String separator, final String suffix, final boolean human) {
        final int len = this.arr.length;
        final StringBuffer sb = new StringBuffer(len * 10 + 10);
        if (prefix != null) {
            sb.append(prefix);
        }
        for (int i = 0; i < len; ++i) {
            if (i != 0 && separator != null) {
                sb.append(separator);
            }
            if (human) {
                sb.append(((ToHuman)this.arr[i]).toHuman());
            }
            else {
                sb.append(this.arr[i]);
            }
        }
        if (suffix != null) {
            sb.append(suffix);
        }
        return sb.toString();
    }
}

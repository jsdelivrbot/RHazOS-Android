package embedded.com.android.dx.dex.code;

import embedded.com.android.dx.util.*;
import embedded.com.android.dx.rop.cst.*;

public final class CatchHandlerList extends FixedSizeList implements Comparable<CatchHandlerList>
{
    public static final CatchHandlerList EMPTY;
    
    public CatchHandlerList(final int size) {
        super(size);
    }
    
    public Entry get(final int n) {
        return (Entry)this.get0(n);
    }
    
    @Override
    public String toHuman() {
        return this.toHuman("", "");
    }
    
    public String toHuman(final String prefix, final String header) {
        final StringBuilder sb = new StringBuilder(100);
        final int size = this.size();
        sb.append(prefix);
        sb.append(header);
        sb.append("catch ");
        for (int i = 0; i < size; ++i) {
            final Entry entry = this.get(i);
            if (i != 0) {
                sb.append(",\n");
                sb.append(prefix);
                sb.append("  ");
            }
            if (i == size - 1 && this.catchesAll()) {
                sb.append("<any>");
            }
            else {
                sb.append(entry.getExceptionType().toHuman());
            }
            sb.append(" -> ");
            sb.append(Hex.u2or4(entry.getHandler()));
        }
        return sb.toString();
    }
    
    public boolean catchesAll() {
        final int size = this.size();
        if (size == 0) {
            return false;
        }
        final Entry last = this.get(size - 1);
        return last.getExceptionType().equals(CstType.OBJECT);
    }
    
    public void set(final int n, final CstType exceptionType, final int handler) {
        this.set0(n, new Entry(exceptionType, handler));
    }
    
    public void set(final int n, final Entry entry) {
        this.set0(n, entry);
    }
    
    @Override
    public int compareTo(final CatchHandlerList other) {
        if (this == other) {
            return 0;
        }
        final int thisSize = this.size();
        final int otherSize = other.size();
        for (int checkSize = Math.min(thisSize, otherSize), i = 0; i < checkSize; ++i) {
            final Entry thisEntry = this.get(i);
            final Entry otherEntry = other.get(i);
            final int compare = thisEntry.compareTo(otherEntry);
            if (compare != 0) {
                return compare;
            }
        }
        if (thisSize < otherSize) {
            return -1;
        }
        if (thisSize > otherSize) {
            return 1;
        }
        return 0;
    }
    
    static {
        EMPTY = new CatchHandlerList(0);
    }
    
    public static class Entry implements Comparable<Entry>
    {
        private final CstType exceptionType;
        private final int handler;
        
        public Entry(final CstType exceptionType, final int handler) {
            if (handler < 0) {
                throw new IllegalArgumentException("handler < 0");
            }
            if (exceptionType == null) {
                throw new NullPointerException("exceptionType == null");
            }
            this.handler = handler;
            this.exceptionType = exceptionType;
        }
        
        @Override
        public int hashCode() {
            return this.handler * 31 + this.exceptionType.hashCode();
        }
        
        @Override
        public boolean equals(final Object other) {
            return other instanceof Entry && this.compareTo((Entry)other) == 0;
        }
        
        @Override
        public int compareTo(final Entry other) {
            if (this.handler < other.handler) {
                return -1;
            }
            if (this.handler > other.handler) {
                return 1;
            }
            return this.exceptionType.compareTo((Constant)other.exceptionType);
        }
        
        public CstType getExceptionType() {
            return this.exceptionType;
        }
        
        public int getHandler() {
            return this.handler;
        }
    }
}

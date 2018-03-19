package embedded.com.android.dx.dex.code;

import embedded.com.android.dx.util.*;

public final class CatchTable extends FixedSizeList implements Comparable<CatchTable>
{
    public static final CatchTable EMPTY;
    
    public CatchTable(final int size) {
        super(size);
    }
    
    public Entry get(final int n) {
        return (Entry)this.get0(n);
    }
    
    public void set(final int n, final Entry entry) {
        this.set0(n, entry);
    }
    
    @Override
    public int compareTo(final CatchTable other) {
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
        EMPTY = new CatchTable(0);
    }
    
    public static class Entry implements Comparable<Entry>
    {
        private final int start;
        private final int end;
        private final CatchHandlerList handlers;
        
        public Entry(final int start, final int end, final CatchHandlerList handlers) {
            if (start < 0) {
                throw new IllegalArgumentException("start < 0");
            }
            if (end <= start) {
                throw new IllegalArgumentException("end <= start");
            }
            if (handlers.isMutable()) {
                throw new IllegalArgumentException("handlers.isMutable()");
            }
            this.start = start;
            this.end = end;
            this.handlers = handlers;
        }
        
        @Override
        public int hashCode() {
            int hash = this.start * 31 + this.end;
            hash = hash * 31 + this.handlers.hashCode();
            return hash;
        }
        
        @Override
        public boolean equals(final Object other) {
            return other instanceof Entry && this.compareTo((Entry)other) == 0;
        }
        
        @Override
        public int compareTo(final Entry other) {
            if (this.start < other.start) {
                return -1;
            }
            if (this.start > other.start) {
                return 1;
            }
            if (this.end < other.end) {
                return -1;
            }
            if (this.end > other.end) {
                return 1;
            }
            return this.handlers.compareTo(other.handlers);
        }
        
        public int getStart() {
            return this.start;
        }
        
        public int getEnd() {
            return this.end;
        }
        
        public CatchHandlerList getHandlers() {
            return this.handlers;
        }
    }
}

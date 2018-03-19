package embedded.com.android.dx.rop.cst;

import embedded.com.android.dx.util.*;

public class CstArray extends Constant
{
    private final List list;
    
    public CstArray(final List list) {
        if (list == null) {
            throw new NullPointerException("list == null");
        }
        list.throwIfMutable();
        this.list = list;
    }
    
    @Override
    public boolean equals(final Object other) {
        return other instanceof CstArray && this.list.equals(((CstArray)other).list);
    }
    
    @Override
    public int hashCode() {
        return this.list.hashCode();
    }
    
    @Override
    protected int compareTo0(final Constant other) {
        return this.list.compareTo(((CstArray)other).list);
    }
    
    @Override
    public String toString() {
        return this.list.toString("array{", ", ", "}");
    }
    
    @Override
    public String typeName() {
        return "array";
    }
    
    @Override
    public boolean isCategory2() {
        return false;
    }
    
    @Override
    public String toHuman() {
        return this.list.toHuman("{", ", ", "}");
    }
    
    public List getList() {
        return this.list;
    }
    
    public static final class List extends FixedSizeList implements Comparable<List>
    {
        public List(final int size) {
            super(size);
        }
        
        @Override
        public int compareTo(final List other) {
            final int thisSize = this.size();
            final int otherSize = other.size();
            for (int compareSize = (thisSize < otherSize) ? thisSize : otherSize, i = 0; i < compareSize; ++i) {
                final Constant thisItem = (Constant)this.get0(i);
                final Constant otherItem = (Constant)other.get0(i);
                final int compare = thisItem.compareTo(otherItem);
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
        
        public Constant get(final int n) {
            return (Constant)this.get0(n);
        }
        
        public void set(final int n, final Constant a) {
            this.set0(n, a);
        }
    }
}

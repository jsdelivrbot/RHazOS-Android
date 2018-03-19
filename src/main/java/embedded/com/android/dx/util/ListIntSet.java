package embedded.com.android.dx.util;

import java.util.*;

public class ListIntSet implements IntSet
{
    final IntList ints;
    
    public ListIntSet() {
        (this.ints = new IntList()).sort();
    }
    
    @Override
    public void add(final int value) {
        final int index = this.ints.binarysearch(value);
        if (index < 0) {
            this.ints.insert(-(index + 1), value);
        }
    }
    
    @Override
    public void remove(final int value) {
        final int index = this.ints.indexOf(value);
        if (index >= 0) {
            this.ints.removeIndex(index);
        }
    }
    
    @Override
    public boolean has(final int value) {
        return this.ints.indexOf(value) >= 0;
    }
    
    @Override
    public void merge(final IntSet other) {
        if (other instanceof ListIntSet) {
            final ListIntSet o = (ListIntSet)other;
            final int szThis = this.ints.size();
            final int szOther = o.ints.size();
            int i = 0;
            int j = 0;
            while (j < szOther && i < szThis) {
                while (j < szOther && o.ints.get(j) < this.ints.get(i)) {
                    this.add(o.ints.get(j++));
                }
                if (j == szOther) {
                    break;
                }
                while (i < szThis && o.ints.get(j) >= this.ints.get(i)) {
                    ++i;
                }
            }
            while (j < szOther) {
                this.add(o.ints.get(j++));
            }
            this.ints.sort();
        }
        else if (other instanceof BitIntSet) {
            final BitIntSet o2 = (BitIntSet)other;
            for (int k = 0; k >= 0; k = Bits.findFirst(o2.bits, k + 1)) {
                this.ints.add(k);
            }
            this.ints.sort();
        }
        else {
            final IntIterator iter = other.iterator();
            while (iter.hasNext()) {
                this.add(iter.next());
            }
        }
    }
    
    @Override
    public int elements() {
        return this.ints.size();
    }
    
    @Override
    public IntIterator iterator() {
        return new IntIterator() {
            private int idx = 0;
            
            @Override
            public boolean hasNext() {
                return this.idx < ListIntSet.this.ints.size();
            }
            
            @Override
            public int next() {
                if (!this.hasNext()) {
                    throw new NoSuchElementException();
                }
                return ListIntSet.this.ints.get(this.idx++);
            }
        };
    }
    
    @Override
    public String toString() {
        return this.ints.toString();
    }
}

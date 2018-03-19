package embedded.com.android.dx.util;

import java.util.*;

public class BitIntSet implements IntSet
{
    int[] bits;
    
    public BitIntSet(final int max) {
        this.bits = Bits.makeBitSet(max);
    }
    
    @Override
    public void add(final int value) {
        this.ensureCapacity(value);
        Bits.set(this.bits, value, true);
    }
    
    private void ensureCapacity(final int value) {
        if (value >= Bits.getMax(this.bits)) {
            final int[] newBits = Bits.makeBitSet(Math.max(value + 1, 2 * Bits.getMax(this.bits)));
            System.arraycopy(this.bits, 0, newBits, 0, this.bits.length);
            this.bits = newBits;
        }
    }
    
    @Override
    public void remove(final int value) {
        if (value < Bits.getMax(this.bits)) {
            Bits.set(this.bits, value, false);
        }
    }
    
    @Override
    public boolean has(final int value) {
        return value < Bits.getMax(this.bits) && Bits.get(this.bits, value);
    }
    
    @Override
    public void merge(final IntSet other) {
        if (other instanceof BitIntSet) {
            final BitIntSet o = (BitIntSet)other;
            this.ensureCapacity(Bits.getMax(o.bits) + 1);
            Bits.or(this.bits, o.bits);
        }
        else if (other instanceof ListIntSet) {
            final ListIntSet o2 = (ListIntSet)other;
            final int sz = o2.ints.size();
            if (sz > 0) {
                this.ensureCapacity(o2.ints.get(sz - 1));
            }
            for (int i = 0; i < o2.ints.size(); ++i) {
                Bits.set(this.bits, o2.ints.get(i), true);
            }
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
        return Bits.bitCount(this.bits);
    }
    
    @Override
    public IntIterator iterator() {
        return new IntIterator() {
            private int idx = Bits.findFirst(BitIntSet.this.bits, 0);
            
            @Override
            public boolean hasNext() {
                return this.idx >= 0;
            }
            
            @Override
            public int next() {
                if (!this.hasNext()) {
                    throw new NoSuchElementException();
                }
                final int ret = this.idx;
                this.idx = Bits.findFirst(BitIntSet.this.bits, this.idx + 1);
                return ret;
            }
        };
    }
    
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append('{');
        boolean first = true;
        for (int i = Bits.findFirst(this.bits, 0); i >= 0; i = Bits.findFirst(this.bits, i + 1)) {
            if (!first) {
                sb.append(", ");
            }
            first = false;
            sb.append(i);
        }
        sb.append('}');
        return sb.toString();
    }
}

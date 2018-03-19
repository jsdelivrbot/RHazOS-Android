package embedded.com.android.dx.util;

import java.util.*;

public final class IntList extends MutabilityControl
{
    public static final IntList EMPTY;
    private int[] values;
    private int size;
    private boolean sorted;
    
    public static IntList makeImmutable(final int value) {
        final IntList result = new IntList(1);
        result.add(value);
        result.setImmutable();
        return result;
    }
    
    public static IntList makeImmutable(final int value0, final int value1) {
        final IntList result = new IntList(2);
        result.add(value0);
        result.add(value1);
        result.setImmutable();
        return result;
    }
    
    public IntList() {
        this(4);
    }
    
    public IntList(final int initialCapacity) {
        super(true);
        try {
            this.values = new int[initialCapacity];
        }
        catch (NegativeArraySizeException ex) {
            throw new IllegalArgumentException("size < 0");
        }
        this.size = 0;
        this.sorted = true;
    }
    
    @Override
    public int hashCode() {
        int result = 0;
        for (int i = 0; i < this.size; ++i) {
            result = result * 31 + this.values[i];
        }
        return result;
    }
    
    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof IntList)) {
            return false;
        }
        final IntList otherList = (IntList)other;
        if (this.sorted != otherList.sorted) {
            return false;
        }
        if (this.size != otherList.size) {
            return false;
        }
        for (int i = 0; i < this.size; ++i) {
            if (this.values[i] != otherList.values[i]) {
                return false;
            }
        }
        return true;
    }
    
    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer(this.size * 5 + 10);
        sb.append('{');
        for (int i = 0; i < this.size; ++i) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append(this.values[i]);
        }
        sb.append('}');
        return sb.toString();
    }
    
    public int size() {
        return this.size;
    }
    
    public int get(final int n) {
        if (n >= this.size) {
            throw new IndexOutOfBoundsException("n >= size()");
        }
        try {
            return this.values[n];
        }
        catch (ArrayIndexOutOfBoundsException ex) {
            throw new IndexOutOfBoundsException("n < 0");
        }
    }
    
    public void set(final int n, final int value) {
        this.throwIfImmutable();
        if (n >= this.size) {
            throw new IndexOutOfBoundsException("n >= size()");
        }
        try {
            this.values[n] = value;
            this.sorted = false;
        }
        catch (ArrayIndexOutOfBoundsException ex) {
            if (n < 0) {
                throw new IllegalArgumentException("n < 0");
            }
        }
    }
    
    public void add(final int value) {
        this.throwIfImmutable();
        this.growIfNeeded();
        this.values[this.size++] = value;
        if (this.sorted && this.size > 1) {
            this.sorted = (value >= this.values[this.size - 2]);
        }
    }
    
    public void insert(final int n, final int value) {
        if (n > this.size) {
            throw new IndexOutOfBoundsException("n > size()");
        }
        this.growIfNeeded();
        System.arraycopy(this.values, n, this.values, n + 1, this.size - n);
        this.values[n] = value;
        ++this.size;
        this.sorted = (this.sorted && (n == 0 || value > this.values[n - 1]) && (n == this.size - 1 || value < this.values[n + 1]));
    }
    
    public void removeIndex(final int n) {
        if (n >= this.size) {
            throw new IndexOutOfBoundsException("n >= size()");
        }
        System.arraycopy(this.values, n + 1, this.values, n, this.size - n - 1);
        --this.size;
    }
    
    private void growIfNeeded() {
        if (this.size == this.values.length) {
            final int[] newv = new int[this.size * 3 / 2 + 10];
            System.arraycopy(this.values, 0, newv, 0, this.size);
            this.values = newv;
        }
    }
    
    public int top() {
        return this.get(this.size - 1);
    }
    
    public int pop() {
        this.throwIfImmutable();
        final int result = this.get(this.size - 1);
        --this.size;
        return result;
    }
    
    public void pop(final int n) {
        this.throwIfImmutable();
        this.size -= n;
    }
    
    public void shrink(final int newSize) {
        if (newSize < 0) {
            throw new IllegalArgumentException("newSize < 0");
        }
        if (newSize > this.size) {
            throw new IllegalArgumentException("newSize > size");
        }
        this.throwIfImmutable();
        this.size = newSize;
    }
    
    public IntList mutableCopy() {
        final int sz = this.size;
        final IntList result = new IntList(sz);
        for (int i = 0; i < sz; ++i) {
            result.add(this.values[i]);
        }
        return result;
    }
    
    public void sort() {
        this.throwIfImmutable();
        if (!this.sorted) {
            Arrays.sort(this.values, 0, this.size);
            this.sorted = true;
        }
    }
    
    public int indexOf(final int value) {
        final int ret = this.binarysearch(value);
        return (ret >= 0) ? ret : -1;
    }
    
    public int binarysearch(final int value) {
        final int sz = this.size;
        if (!this.sorted) {
            for (int i = 0; i < sz; ++i) {
                if (this.values[i] == value) {
                    return i;
                }
            }
            return -sz;
        }
        int min = -1;
        int max = sz;
        while (max > min + 1) {
            final int guessIdx = min + (max - min >> 1);
            final int guess = this.values[guessIdx];
            if (value <= guess) {
                max = guessIdx;
            }
            else {
                min = guessIdx;
            }
        }
        if (max != sz) {
            return (value == this.values[max]) ? max : (-max - 1);
        }
        return -sz - 1;
    }
    
    public boolean contains(final int value) {
        return this.indexOf(value) >= 0;
    }
    
    static {
        (EMPTY = new IntList(0)).setImmutable();
    }
}

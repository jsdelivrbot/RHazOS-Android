package embedded.com.android.dx.cf.code;

import embedded.com.android.dx.util.*;

public final class ByteBlockList extends LabeledList
{
    public ByteBlockList(final int size) {
        super(size);
    }
    
    public ByteBlock get(final int n) {
        return (ByteBlock)this.get0(n);
    }
    
    public ByteBlock labelToBlock(final int label) {
        final int idx = this.indexOfLabel(label);
        if (idx < 0) {
            throw new IllegalArgumentException("no such label: " + Hex.u2(label));
        }
        return this.get(idx);
    }
    
    public void set(final int n, final ByteBlock bb) {
        super.set(n, bb);
    }
}

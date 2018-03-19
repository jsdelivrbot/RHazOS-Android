package embedded.com.android.dx.cf.code;

import embedded.com.android.dx.util.*;

public final class LineNumberList extends FixedSizeList
{
    public static final LineNumberList EMPTY;
    
    public static LineNumberList concat(final LineNumberList list1, final LineNumberList list2) {
        if (list1 == LineNumberList.EMPTY) {
            return list2;
        }
        final int sz1 = list1.size();
        final int sz2 = list2.size();
        final LineNumberList result = new LineNumberList(sz1 + sz2);
        for (int i = 0; i < sz1; ++i) {
            result.set(i, list1.get(i));
        }
        for (int i = 0; i < sz2; ++i) {
            result.set(sz1 + i, list2.get(i));
        }
        return result;
    }
    
    public LineNumberList(final int count) {
        super(count);
    }
    
    public Item get(final int n) {
        return (Item)this.get0(n);
    }
    
    public void set(final int n, final Item item) {
        if (item == null) {
            throw new NullPointerException("item == null");
        }
        this.set0(n, item);
    }
    
    public void set(final int n, final int startPc, final int lineNumber) {
        this.set0(n, new Item(startPc, lineNumber));
    }
    
    public int pcToLine(final int pc) {
        final int sz = this.size();
        int bestPc = -1;
        int bestLine = -1;
        for (int i = 0; i < sz; ++i) {
            final Item one = this.get(i);
            final int onePc = one.getStartPc();
            if (onePc <= pc && onePc > bestPc) {
                bestPc = onePc;
                bestLine = one.getLineNumber();
                if (bestPc == pc) {
                    break;
                }
            }
        }
        return bestLine;
    }
    
    static {
        EMPTY = new LineNumberList(0);
    }
    
    public static class Item
    {
        private final int startPc;
        private final int lineNumber;
        
        public Item(final int startPc, final int lineNumber) {
            if (startPc < 0) {
                throw new IllegalArgumentException("startPc < 0");
            }
            if (lineNumber < 0) {
                throw new IllegalArgumentException("lineNumber < 0");
            }
            this.startPc = startPc;
            this.lineNumber = lineNumber;
        }
        
        public int getStartPc() {
            return this.startPc;
        }
        
        public int getLineNumber() {
            return this.lineNumber;
        }
    }
}

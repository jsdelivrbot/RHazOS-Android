package embedded.com.android.dx.dex.code;

import embedded.com.android.dx.util.*;
import embedded.com.android.dx.rop.code.*;

public final class PositionList extends FixedSizeList
{
    public static final PositionList EMPTY;
    public static final int NONE = 1;
    public static final int LINES = 2;
    public static final int IMPORTANT = 3;
    
    public static PositionList make(final DalvInsnList insns, final int howMuch) {
        switch (howMuch) {
            case 1: {
                return PositionList.EMPTY;
            }
            case 2:
            case 3: {
                SourcePosition cur;
                final SourcePosition noInfo = cur = SourcePosition.NO_INFO;
                final int sz = insns.size();
                final Entry[] arr = new Entry[sz];
                boolean lastWasTarget = false;
                int at = 0;
                for (int i = 0; i < sz; ++i) {
                    final DalvInsn insn = insns.get(i);
                    if (insn instanceof CodeAddress) {
                        lastWasTarget = true;
                    }
                    else {
                        final SourcePosition pos = insn.getPosition();
                        if (!pos.equals(noInfo)) {
                            if (!pos.sameLine(cur)) {
                                if (howMuch != 3 || lastWasTarget) {
                                    cur = pos;
                                    arr[at] = new Entry(insn.getAddress(), pos);
                                    ++at;
                                    lastWasTarget = false;
                                }
                            }
                        }
                    }
                }
                final PositionList result = new PositionList(at);
                for (int j = 0; j < at; ++j) {
                    result.set(j, arr[j]);
                }
                result.setImmutable();
                return result;
            }
            default: {
                throw new IllegalArgumentException("bogus howMuch");
            }
        }
    }
    
    public PositionList(final int size) {
        super(size);
    }
    
    public Entry get(final int n) {
        return (Entry)this.get0(n);
    }
    
    public void set(final int n, final Entry entry) {
        this.set0(n, entry);
    }
    
    static {
        EMPTY = new PositionList(0);
    }
    
    public static class Entry
    {
        private final int address;
        private final SourcePosition position;
        
        public Entry(final int address, final SourcePosition position) {
            if (address < 0) {
                throw new IllegalArgumentException("address < 0");
            }
            if (position == null) {
                throw new NullPointerException("position == null");
            }
            this.address = address;
            this.position = position;
        }
        
        public int getAddress() {
            return this.address;
        }
        
        public SourcePosition getPosition() {
            return this.position;
        }
    }
}

package embedded.com.android.dx.cf.code;

import embedded.com.android.dx.rop.cst.*;
import embedded.com.android.dx.util.*;
import embedded.com.android.dx.rop.type.*;

public final class ByteCatchList extends FixedSizeList
{
    public static final ByteCatchList EMPTY;
    
    public ByteCatchList(final int count) {
        super(count);
    }
    
    public int byteLength() {
        return 2 + this.size() * 8;
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
    
    public void set(final int n, final int startPc, final int endPc, final int handlerPc, final CstType exceptionClass) {
        this.set0(n, new Item(startPc, endPc, handlerPc, exceptionClass));
    }
    
    public ByteCatchList listFor(final int pc) {
        final int sz = this.size();
        final Item[] resultArr = new Item[sz];
        int resultSz = 0;
        for (int i = 0; i < sz; ++i) {
            final Item one = this.get(i);
            if (one.covers(pc) && typeNotFound(one, resultArr, resultSz)) {
                resultArr[resultSz] = one;
                ++resultSz;
            }
        }
        if (resultSz == 0) {
            return ByteCatchList.EMPTY;
        }
        final ByteCatchList result = new ByteCatchList(resultSz);
        for (int j = 0; j < resultSz; ++j) {
            result.set(j, resultArr[j]);
        }
        result.setImmutable();
        return result;
    }
    
    private static boolean typeNotFound(final Item item, final Item[] arr, final int count) {
        final CstType type = item.getExceptionClass();
        for (int i = 0; i < count; ++i) {
            final CstType one = arr[i].getExceptionClass();
            if (one == type || one == CstType.OBJECT) {
                return false;
            }
        }
        return true;
    }
    
    public IntList toTargetList(final int noException) {
        if (noException < -1) {
            throw new IllegalArgumentException("noException < -1");
        }
        final boolean hasDefault = noException >= 0;
        final int sz = this.size();
        if (sz != 0) {
            final IntList result = new IntList(sz + (hasDefault ? 1 : 0));
            for (int i = 0; i < sz; ++i) {
                result.add(this.get(i).getHandlerPc());
            }
            if (hasDefault) {
                result.add(noException);
            }
            result.setImmutable();
            return result;
        }
        if (hasDefault) {
            return IntList.makeImmutable(noException);
        }
        return IntList.EMPTY;
    }
    
    public TypeList toRopCatchList() {
        final int sz = this.size();
        if (sz == 0) {
            return StdTypeList.EMPTY;
        }
        final StdTypeList result = new StdTypeList(sz);
        for (int i = 0; i < sz; ++i) {
            result.set(i, this.get(i).getExceptionClass().getClassType());
        }
        result.setImmutable();
        return result;
    }
    
    static {
        EMPTY = new ByteCatchList(0);
    }
    
    public static class Item
    {
        private final int startPc;
        private final int endPc;
        private final int handlerPc;
        private final CstType exceptionClass;
        
        public Item(final int startPc, final int endPc, final int handlerPc, final CstType exceptionClass) {
            if (startPc < 0) {
                throw new IllegalArgumentException("startPc < 0");
            }
            if (endPc < startPc) {
                throw new IllegalArgumentException("endPc < startPc");
            }
            if (handlerPc < 0) {
                throw new IllegalArgumentException("handlerPc < 0");
            }
            this.startPc = startPc;
            this.endPc = endPc;
            this.handlerPc = handlerPc;
            this.exceptionClass = exceptionClass;
        }
        
        public int getStartPc() {
            return this.startPc;
        }
        
        public int getEndPc() {
            return this.endPc;
        }
        
        public int getHandlerPc() {
            return this.handlerPc;
        }
        
        public CstType getExceptionClass() {
            return (this.exceptionClass != null) ? this.exceptionClass : CstType.OBJECT;
        }
        
        public boolean covers(final int pc) {
            return pc >= this.startPc && pc < this.endPc;
        }
    }
}

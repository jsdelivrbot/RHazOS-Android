package embedded.com.android.dx.cf.attrib;

import embedded.com.android.dx.util.*;
import embedded.com.android.dx.rop.cst.*;

public final class InnerClassList extends FixedSizeList
{
    public InnerClassList(final int count) {
        super(count);
    }
    
    public Item get(final int n) {
        return (Item)this.get0(n);
    }
    
    public void set(final int n, final CstType innerClass, final CstType outerClass, final CstString innerName, final int accessFlags) {
        this.set0(n, new Item(innerClass, outerClass, innerName, accessFlags));
    }
    
    public static class Item
    {
        private final CstType innerClass;
        private final CstType outerClass;
        private final CstString innerName;
        private final int accessFlags;
        
        public Item(final CstType innerClass, final CstType outerClass, final CstString innerName, final int accessFlags) {
            if (innerClass == null) {
                throw new NullPointerException("innerClass == null");
            }
            this.innerClass = innerClass;
            this.outerClass = outerClass;
            this.innerName = innerName;
            this.accessFlags = accessFlags;
        }
        
        public CstType getInnerClass() {
            return this.innerClass;
        }
        
        public CstType getOuterClass() {
            return this.outerClass;
        }
        
        public CstString getInnerName() {
            return this.innerName;
        }
        
        public int getAccessFlags() {
            return this.accessFlags;
        }
    }
}

package embedded.com.android.dx.cf.code;

import embedded.com.android.dx.util.*;
import embedded.com.android.dx.rop.cst.*;

public class BootstrapMethodsList extends FixedSizeList
{
    public static final BootstrapMethodsList EMPTY;
    
    public BootstrapMethodsList(final int count) {
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
    
    public void set(final int n, final CstType declaringClass, final CstMethodHandle bootstrapMethodHandle, final BootstrapMethodArgumentsList arguments) {
        this.set(n, new Item(declaringClass, bootstrapMethodHandle, arguments));
    }
    
    public static BootstrapMethodsList concat(final BootstrapMethodsList list1, final BootstrapMethodsList list2) {
        if (list1 == BootstrapMethodsList.EMPTY) {
            return list2;
        }
        if (list2 == BootstrapMethodsList.EMPTY) {
            return list1;
        }
        final int sz1 = list1.size();
        final int sz2 = list2.size();
        final BootstrapMethodsList result = new BootstrapMethodsList(sz1 + sz2);
        for (int i = 0; i < sz1; ++i) {
            result.set(i, list1.get(i));
        }
        for (int i = 0; i < sz2; ++i) {
            result.set(sz1 + i, list2.get(i));
        }
        return result;
    }
    
    static {
        EMPTY = new BootstrapMethodsList(0);
    }
    
    public static class Item
    {
        private final BootstrapMethodArgumentsList bootstrapMethodArgumentsList;
        private final CstMethodHandle bootstrapMethodHandle;
        private final CstType declaringClass;
        
        public Item(final CstType declaringClass, final CstMethodHandle bootstrapMethodHandle, final BootstrapMethodArgumentsList bootstrapMethodArguments) {
            if (declaringClass == null) {
                throw new NullPointerException("declaringClass == null");
            }
            if (bootstrapMethodHandle == null) {
                throw new NullPointerException("bootstrapMethodHandle == null");
            }
            if (bootstrapMethodArguments == null) {
                throw new NullPointerException("bootstrapMethodArguments == null");
            }
            this.bootstrapMethodHandle = bootstrapMethodHandle;
            this.bootstrapMethodArgumentsList = bootstrapMethodArguments;
            this.declaringClass = declaringClass;
        }
        
        public CstMethodHandle getBootstrapMethodHandle() {
            return this.bootstrapMethodHandle;
        }
        
        public BootstrapMethodArgumentsList getBootstrapMethodArguments() {
            return this.bootstrapMethodArgumentsList;
        }
        
        public CstType getDeclaringClass() {
            return this.declaringClass;
        }
    }
}

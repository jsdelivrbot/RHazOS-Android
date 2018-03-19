package embedded.com.android.dx.cf.code;

import embedded.com.android.dx.util.*;
import embedded.com.android.dx.rop.cst.*;
import embedded.com.android.dx.rop.code.*;
import embedded.com.android.dx.rop.type.*;

public final class LocalVariableList extends FixedSizeList
{
    public static final LocalVariableList EMPTY;
    
    public static LocalVariableList concat(final LocalVariableList list1, final LocalVariableList list2) {
        if (list1 == LocalVariableList.EMPTY) {
            return list2;
        }
        final int sz1 = list1.size();
        final int sz2 = list2.size();
        final LocalVariableList result = new LocalVariableList(sz1 + sz2);
        for (int i = 0; i < sz1; ++i) {
            result.set(i, list1.get(i));
        }
        for (int i = 0; i < sz2; ++i) {
            result.set(sz1 + i, list2.get(i));
        }
        result.setImmutable();
        return result;
    }
    
    public static LocalVariableList mergeDescriptorsAndSignatures(final LocalVariableList descriptorList, final LocalVariableList signatureList) {
        final int descriptorSize = descriptorList.size();
        final LocalVariableList result = new LocalVariableList(descriptorSize);
        for (int i = 0; i < descriptorSize; ++i) {
            Item item = descriptorList.get(i);
            final Item signatureItem = signatureList.itemToLocal(item);
            if (signatureItem != null) {
                final CstString signature = signatureItem.getSignature();
                item = item.withSignature(signature);
            }
            result.set(i, item);
        }
        result.setImmutable();
        return result;
    }
    
    public LocalVariableList(final int count) {
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
    
    public void set(final int n, final int startPc, final int length, final CstString name, final CstString descriptor, final CstString signature, final int index) {
        this.set0(n, new Item(startPc, length, name, descriptor, signature, index));
    }
    
    public Item itemToLocal(final Item item) {
        for (int sz = this.size(), i = 0; i < sz; ++i) {
            final Item one = (Item)this.get0(i);
            if (one != null && one.matchesAllButType(item)) {
                return one;
            }
        }
        return null;
    }
    
    public Item pcAndIndexToLocal(final int pc, final int index) {
        for (int sz = this.size(), i = 0; i < sz; ++i) {
            final Item one = (Item)this.get0(i);
            if (one != null && one.matchesPcAndIndex(pc, index)) {
                return one;
            }
        }
        return null;
    }
    
    static {
        EMPTY = new LocalVariableList(0);
    }
    
    public static class Item
    {
        private final int startPc;
        private final int length;
        private final CstString name;
        private final CstString descriptor;
        private final CstString signature;
        private final int index;
        
        public Item(final int startPc, final int length, final CstString name, final CstString descriptor, final CstString signature, final int index) {
            if (startPc < 0) {
                throw new IllegalArgumentException("startPc < 0");
            }
            if (length < 0) {
                throw new IllegalArgumentException("length < 0");
            }
            if (name == null) {
                throw new NullPointerException("name == null");
            }
            if (descriptor == null && signature == null) {
                throw new NullPointerException("(descriptor == null) && (signature == null)");
            }
            if (index < 0) {
                throw new IllegalArgumentException("index < 0");
            }
            this.startPc = startPc;
            this.length = length;
            this.name = name;
            this.descriptor = descriptor;
            this.signature = signature;
            this.index = index;
        }
        
        public int getStartPc() {
            return this.startPc;
        }
        
        public int getLength() {
            return this.length;
        }
        
        public CstString getDescriptor() {
            return this.descriptor;
        }
        
        public LocalItem getLocalItem() {
            return LocalItem.make(this.name, this.signature);
        }
        
        private CstString getSignature() {
            return this.signature;
        }
        
        public int getIndex() {
            return this.index;
        }
        
        public Type getType() {
            return Type.intern(this.descriptor.getString());
        }
        
        public Item withSignature(final CstString newSignature) {
            return new Item(this.startPc, this.length, this.name, this.descriptor, newSignature, this.index);
        }
        
        public boolean matchesPcAndIndex(final int pc, final int index) {
            return index == this.index && pc >= this.startPc && pc < this.startPc + this.length;
        }
        
        public boolean matchesAllButType(final Item other) {
            return this.startPc == other.startPc && this.length == other.length && this.index == other.index && this.name.equals(other.name);
        }
    }
}

package embedded.com.android.dx.dex.file;

import embedded.com.android.dx.rop.type.*;
import embedded.com.android.dx.rop.cst.*;
import embedded.com.android.dex.*;
import embedded.com.android.dx.util.*;
import java.util.*;

public final class TypeIdsSection extends UniformItemSection
{
    private final TreeMap<Type, TypeIdItem> typeIds;
    
    public TypeIdsSection(final DexFile file) {
        super("type_ids", file, 4);
        this.typeIds = new TreeMap<Type, TypeIdItem>();
    }
    
    @Override
    public Collection<? extends Item> items() {
        return this.typeIds.values();
    }
    
    @Override
    public IndexedItem get(final Constant cst) {
        if (cst == null) {
            throw new NullPointerException("cst == null");
        }
        this.throwIfNotPrepared();
        final Type type = ((CstType)cst).getClassType();
        final IndexedItem result = this.typeIds.get(type);
        if (result == null) {
            throw new IllegalArgumentException("not found: " + cst);
        }
        return result;
    }
    
    public void writeHeaderPart(final AnnotatedOutput out) {
        this.throwIfNotPrepared();
        final int sz = this.typeIds.size();
        final int offset = (sz == 0) ? 0 : this.getFileOffset();
        if (sz > 65536) {
            throw new DexIndexOverflowException(String.format("Too many type identifiers to fit in one dex file: %1$d; max is %2$d.%nYou may try using multi-dex. If multi-dex is enabled then the list of classes for the main dex list is too large.", this.items().size(), 65536));
        }
        if (out.annotates()) {
            out.annotate(4, "type_ids_size:   " + Hex.u4(sz));
            out.annotate(4, "type_ids_off:    " + Hex.u4(offset));
        }
        out.writeInt(sz);
        out.writeInt(offset);
    }
    
    public synchronized TypeIdItem intern(final Type type) {
        if (type == null) {
            throw new NullPointerException("type == null");
        }
        this.throwIfPrepared();
        TypeIdItem result = this.typeIds.get(type);
        if (result == null) {
            result = new TypeIdItem(new CstType(type));
            this.typeIds.put(type, result);
        }
        return result;
    }
    
    public synchronized TypeIdItem intern(final CstType type) {
        if (type == null) {
            throw new NullPointerException("type == null");
        }
        this.throwIfPrepared();
        final Type typePerSe = type.getClassType();
        TypeIdItem result = this.typeIds.get(typePerSe);
        if (result == null) {
            result = new TypeIdItem(type);
            this.typeIds.put(typePerSe, result);
        }
        return result;
    }
    
    public int indexOf(final Type type) {
        if (type == null) {
            throw new NullPointerException("type == null");
        }
        this.throwIfNotPrepared();
        final TypeIdItem item = this.typeIds.get(type);
        if (item == null) {
            throw new IllegalArgumentException("not found: " + type);
        }
        return item.getIndex();
    }
    
    public int indexOf(final CstType type) {
        if (type == null) {
            throw new NullPointerException("type == null");
        }
        return this.indexOf(type.getClassType());
    }
    
    @Override
    protected void orderItems() {
        int idx = 0;
        for (final Object i : this.items()) {
            ((TypeIdItem)i).setIndex(idx);
            ++idx;
        }
    }
}

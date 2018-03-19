package embedded.com.android.dx.dex.file;

import java.util.*;
import embedded.com.android.dx.rop.cst.*;
import embedded.com.android.dx.util.*;

public final class FieldIdsSection extends MemberIdsSection
{
    private final TreeMap<CstFieldRef, FieldIdItem> fieldIds;
    
    public FieldIdsSection(final DexFile file) {
        super("field_ids", file);
        this.fieldIds = new TreeMap<CstFieldRef, FieldIdItem>();
    }
    
    @Override
    public Collection<? extends Item> items() {
        return this.fieldIds.values();
    }
    
    @Override
    public IndexedItem get(final Constant cst) {
        if (cst == null) {
            throw new NullPointerException("cst == null");
        }
        this.throwIfNotPrepared();
        final IndexedItem result = this.fieldIds.get(cst);
        if (result == null) {
            throw new IllegalArgumentException("not found");
        }
        return result;
    }
    
    public void writeHeaderPart(final AnnotatedOutput out) {
        this.throwIfNotPrepared();
        final int sz = this.fieldIds.size();
        final int offset = (sz == 0) ? 0 : this.getFileOffset();
        if (out.annotates()) {
            out.annotate(4, "field_ids_size:  " + Hex.u4(sz));
            out.annotate(4, "field_ids_off:   " + Hex.u4(offset));
        }
        out.writeInt(sz);
        out.writeInt(offset);
    }
    
    public synchronized FieldIdItem intern(final CstFieldRef field) {
        if (field == null) {
            throw new NullPointerException("field == null");
        }
        this.throwIfPrepared();
        FieldIdItem result = this.fieldIds.get(field);
        if (result == null) {
            result = new FieldIdItem(field);
            this.fieldIds.put(field, result);
        }
        return result;
    }
    
    public int indexOf(final CstFieldRef ref) {
        if (ref == null) {
            throw new NullPointerException("ref == null");
        }
        this.throwIfNotPrepared();
        final FieldIdItem item = this.fieldIds.get(ref);
        if (item == null) {
            throw new IllegalArgumentException("not found");
        }
        return item.getIndex();
    }
}

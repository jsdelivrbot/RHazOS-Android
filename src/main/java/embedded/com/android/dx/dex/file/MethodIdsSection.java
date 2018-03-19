package embedded.com.android.dx.dex.file;

import java.util.*;
import embedded.com.android.dx.rop.cst.*;
import embedded.com.android.dx.util.*;

public final class MethodIdsSection extends MemberIdsSection
{
    private final TreeMap<CstBaseMethodRef, MethodIdItem> methodIds;
    
    public MethodIdsSection(final DexFile file) {
        super("method_ids", file);
        this.methodIds = new TreeMap<CstBaseMethodRef, MethodIdItem>();
    }
    
    @Override
    public Collection<? extends Item> items() {
        return this.methodIds.values();
    }
    
    @Override
    public IndexedItem get(final Constant cst) {
        if (cst == null) {
            throw new NullPointerException("cst == null");
        }
        this.throwIfNotPrepared();
        final IndexedItem result = this.methodIds.get(cst);
        if (result == null) {
            throw new IllegalArgumentException("not found");
        }
        return result;
    }
    
    public void writeHeaderPart(final AnnotatedOutput out) {
        this.throwIfNotPrepared();
        final int sz = this.methodIds.size();
        final int offset = (sz == 0) ? 0 : this.getFileOffset();
        if (out.annotates()) {
            out.annotate(4, "method_ids_size: " + Hex.u4(sz));
            out.annotate(4, "method_ids_off:  " + Hex.u4(offset));
        }
        out.writeInt(sz);
        out.writeInt(offset);
    }
    
    public synchronized MethodIdItem intern(final CstBaseMethodRef method) {
        if (method == null) {
            throw new NullPointerException("method == null");
        }
        this.throwIfPrepared();
        MethodIdItem result = this.methodIds.get(method);
        if (result == null) {
            result = new MethodIdItem(method);
            this.methodIds.put(method, result);
        }
        return result;
    }
    
    public int indexOf(final CstBaseMethodRef ref) {
        if (ref == null) {
            throw new NullPointerException("ref == null");
        }
        this.throwIfNotPrepared();
        final MethodIdItem item = this.methodIds.get(ref);
        if (item == null) {
            throw new IllegalArgumentException("not found");
        }
        return item.getIndex();
    }
}

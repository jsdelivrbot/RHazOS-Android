package embedded.com.android.dx.dex.file;

import embedded.com.android.dx.rop.type.*;
import embedded.com.android.dx.rop.cst.*;
import embedded.com.android.dx.util.*;
import java.util.*;

public final class ProtoIdsSection extends UniformItemSection
{
    private final TreeMap<Prototype, ProtoIdItem> protoIds;
    
    public ProtoIdsSection(final DexFile file) {
        super("proto_ids", file, 4);
        this.protoIds = new TreeMap<Prototype, ProtoIdItem>();
    }
    
    @Override
    public Collection<? extends Item> items() {
        return this.protoIds.values();
    }
    
    @Override
    public IndexedItem get(final Constant cst) {
        if (cst == null) {
            throw new NullPointerException("cst == null");
        }
        if (!(cst instanceof CstProtoRef)) {
            throw new IllegalArgumentException("cst not instance of CstProtoRef");
        }
        this.throwIfNotPrepared();
        final CstProtoRef protoRef = (CstProtoRef)cst;
        final IndexedItem result = this.protoIds.get(protoRef.getPrototype());
        if (result == null) {
            throw new IllegalArgumentException("not found");
        }
        return result;
    }
    
    public void writeHeaderPart(final AnnotatedOutput out) {
        this.throwIfNotPrepared();
        final int sz = this.protoIds.size();
        final int offset = (sz == 0) ? 0 : this.getFileOffset();
        if (sz > 65536) {
            throw new UnsupportedOperationException("too many proto ids");
        }
        if (out.annotates()) {
            out.annotate(4, "proto_ids_size:  " + Hex.u4(sz));
            out.annotate(4, "proto_ids_off:   " + Hex.u4(offset));
        }
        out.writeInt(sz);
        out.writeInt(offset);
    }
    
    public synchronized ProtoIdItem intern(final Prototype prototype) {
        if (prototype == null) {
            throw new NullPointerException("prototype == null");
        }
        this.throwIfPrepared();
        ProtoIdItem result = this.protoIds.get(prototype);
        if (result == null) {
            result = new ProtoIdItem(prototype);
            this.protoIds.put(prototype, result);
        }
        return result;
    }
    
    public int indexOf(final Prototype prototype) {
        if (prototype == null) {
            throw new NullPointerException("prototype == null");
        }
        this.throwIfNotPrepared();
        final ProtoIdItem item = this.protoIds.get(prototype);
        if (item == null) {
            throw new IllegalArgumentException("not found");
        }
        return item.getIndex();
    }
    
    @Override
    protected void orderItems() {
        int idx = 0;
        for (final Object i : this.items()) {
            ((ProtoIdItem)i).setIndex(idx);
            ++idx;
        }
    }
}

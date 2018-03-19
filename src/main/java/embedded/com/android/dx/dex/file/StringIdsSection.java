package embedded.com.android.dx.dex.file;

import embedded.com.android.dx.util.*;
import embedded.com.android.dx.rop.cst.*;
import java.util.*;

public final class StringIdsSection extends UniformItemSection
{
    private final TreeMap<CstString, StringIdItem> strings;
    
    public StringIdsSection(final DexFile file) {
        super("string_ids", file, 4);
        this.strings = new TreeMap<CstString, StringIdItem>();
    }
    
    @Override
    public Collection<? extends Item> items() {
        return this.strings.values();
    }
    
    @Override
    public IndexedItem get(final Constant cst) {
        if (cst == null) {
            throw new NullPointerException("cst == null");
        }
        this.throwIfNotPrepared();
        final IndexedItem result = this.strings.get(cst);
        if (result == null) {
            throw new IllegalArgumentException("not found");
        }
        return result;
    }
    
    public void writeHeaderPart(final AnnotatedOutput out) {
        this.throwIfNotPrepared();
        final int sz = this.strings.size();
        final int offset = (sz == 0) ? 0 : this.getFileOffset();
        if (out.annotates()) {
            out.annotate(4, "string_ids_size: " + Hex.u4(sz));
            out.annotate(4, "string_ids_off:  " + Hex.u4(offset));
        }
        out.writeInt(sz);
        out.writeInt(offset);
    }
    
    public StringIdItem intern(final String string) {
        return this.intern(new StringIdItem(new CstString(string)));
    }
    
    public StringIdItem intern(final CstString string) {
        return this.intern(new StringIdItem(string));
    }
    
    public synchronized StringIdItem intern(final StringIdItem string) {
        if (string == null) {
            throw new NullPointerException("string == null");
        }
        this.throwIfPrepared();
        final CstString value = string.getValue();
        final StringIdItem already = this.strings.get(value);
        if (already != null) {
            return already;
        }
        this.strings.put(value, string);
        return string;
    }
    
    public synchronized void intern(final CstNat nat) {
        this.intern(nat.getName());
        this.intern(nat.getDescriptor());
    }
    
    public int indexOf(final CstString string) {
        if (string == null) {
            throw new NullPointerException("string == null");
        }
        this.throwIfNotPrepared();
        final StringIdItem s = this.strings.get(string);
        if (s == null) {
            throw new IllegalArgumentException("not found");
        }
        return s.getIndex();
    }
    
    @Override
    protected void orderItems() {
        int idx = 0;
        for (final StringIdItem s : this.strings.values()) {
            s.setIndex(idx);
            ++idx;
        }
    }
}

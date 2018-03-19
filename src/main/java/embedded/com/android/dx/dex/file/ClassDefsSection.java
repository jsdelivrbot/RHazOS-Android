package embedded.com.android.dx.dex.file;

import embedded.com.android.dx.rop.cst.*;
import embedded.com.android.dx.util.*;
import java.util.*;
import embedded.com.android.dx.rop.type.*;

public final class ClassDefsSection extends UniformItemSection
{
    private final TreeMap<Type, ClassDefItem> classDefs;
    private ArrayList<ClassDefItem> orderedDefs;
    
    public ClassDefsSection(final DexFile file) {
        super("class_defs", file, 4);
        this.classDefs = new TreeMap<Type, ClassDefItem>();
        this.orderedDefs = null;
    }
    
    @Override
    public Collection<? extends Item> items() {
        if (this.orderedDefs != null) {
            return this.orderedDefs;
        }
        return this.classDefs.values();
    }
    
    @Override
    public IndexedItem get(final Constant cst) {
        if (cst == null) {
            throw new NullPointerException("cst == null");
        }
        this.throwIfNotPrepared();
        final Type type = ((CstType)cst).getClassType();
        final IndexedItem result = this.classDefs.get(type);
        if (result == null) {
            throw new IllegalArgumentException("not found");
        }
        return result;
    }
    
    public void writeHeaderPart(final AnnotatedOutput out) {
        this.throwIfNotPrepared();
        final int sz = this.classDefs.size();
        final int offset = (sz == 0) ? 0 : this.getFileOffset();
        if (out.annotates()) {
            out.annotate(4, "class_defs_size: " + Hex.u4(sz));
            out.annotate(4, "class_defs_off:  " + Hex.u4(offset));
        }
        out.writeInt(sz);
        out.writeInt(offset);
    }
    
    public void add(final ClassDefItem clazz) {
        Type type;
        try {
            type = clazz.getThisClass().getClassType();
        }
        catch (NullPointerException ex) {
            throw new NullPointerException("clazz == null");
        }
        this.throwIfPrepared();
        if (this.classDefs.get(type) != null) {
            throw new IllegalArgumentException("already added: " + type);
        }
        this.classDefs.put(type, clazz);
    }
    
    @Override
    protected void orderItems() {
        final int sz = this.classDefs.size();
        int idx = 0;
        this.orderedDefs = new ArrayList<ClassDefItem>(sz);
        for (final Type type : this.classDefs.keySet()) {
            idx = this.orderItems0(type, idx, sz - idx);
        }
    }
    
    private int orderItems0(final Type type, int idx, int maxDepth) {
        final ClassDefItem c = this.classDefs.get(type);
        if (c == null || c.hasIndex()) {
            return idx;
        }
        if (maxDepth < 0) {
            throw new RuntimeException("class circularity with " + type);
        }
        --maxDepth;
        final CstType superclassCst = c.getSuperclass();
        if (superclassCst != null) {
            final Type superclass = superclassCst.getClassType();
            idx = this.orderItems0(superclass, idx, maxDepth);
        }
        final TypeList interfaces = c.getInterfaces();
        for (int sz = interfaces.size(), i = 0; i < sz; ++i) {
            idx = this.orderItems0(interfaces.getType(i), idx, maxDepth);
        }
        c.setIndex(idx);
        this.orderedDefs.add(c);
        return idx + 1;
    }
}

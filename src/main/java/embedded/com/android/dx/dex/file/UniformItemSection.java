package embedded.com.android.dx.dex.file;

import embedded.com.android.dx.rop.cst.*;
import java.util.*;
import embedded.com.android.dx.util.*;

public abstract class UniformItemSection extends Section
{
    public UniformItemSection(final String name, final DexFile file, final int alignment) {
        super(name, file, alignment);
    }
    
    @Override
    public final int writeSize() {
        final Collection<? extends Item> items = this.items();
        final int sz = items.size();
        if (sz == 0) {
            return 0;
        }
        return sz * ((Item)items.iterator().next()).writeSize();
    }
    
    public abstract IndexedItem get(final Constant p0);
    
    @Override
    protected final void prepare0() {
        final DexFile file = this.getFile();
        this.orderItems();
        for (final Item one : this.items()) {
            one.addContents(file);
        }
    }
    
    @Override
    protected final void writeTo0(final AnnotatedOutput out) {
        final DexFile file = this.getFile();
        final int alignment = this.getAlignment();
        for (final Item one : this.items()) {
            one.writeTo(file, out);
            out.alignTo(alignment);
        }
    }
    
    @Override
    public final int getAbsoluteItemOffset(final Item item) {
        final IndexedItem ii = (IndexedItem)item;
        final int relativeOffset = ii.getIndex() * ii.writeSize();
        return this.getAbsoluteOffset(relativeOffset);
    }
    
    protected abstract void orderItems();
}

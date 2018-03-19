package embedded.com.android.dx.dex.file;

import embedded.com.android.dx.util.*;
import embedded.com.android.dx.rop.cst.*;

public final class TypeIdItem extends IdItem
{
    public TypeIdItem(final CstType type) {
        super(type);
    }
    
    @Override
    public ItemType itemType() {
        return ItemType.TYPE_TYPE_ID_ITEM;
    }
    
    @Override
    public int writeSize() {
        return 4;
    }
    
    @Override
    public void addContents(final DexFile file) {
        file.getStringIds().intern(this.getDefiningClass().getDescriptor());
    }
    
    @Override
    public void writeTo(final DexFile file, final AnnotatedOutput out) {
        final CstType type = this.getDefiningClass();
        final CstString descriptor = type.getDescriptor();
        final int idx = file.getStringIds().indexOf(descriptor);
        if (out.annotates()) {
            out.annotate(0, this.indexString() + ' ' + descriptor.toHuman());
            out.annotate(4, "  descriptor_idx: " + Hex.u4(idx));
        }
        out.writeInt(idx);
    }
}

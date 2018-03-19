package embedded.com.android.dx.dex.file;

import embedded.com.android.dx.util.*;
import embedded.com.android.dx.rop.type.*;

public final class TypeListItem extends OffsettedItem
{
    private static final int ALIGNMENT = 4;
    private static final int ELEMENT_SIZE = 2;
    private static final int HEADER_SIZE = 4;
    private final TypeList list;
    
    public TypeListItem(final TypeList list) {
        super(4, list.size() * 2 + 4);
        this.list = list;
    }
    
    @Override
    public int hashCode() {
        return StdTypeList.hashContents(this.list);
    }
    
    @Override
    public ItemType itemType() {
        return ItemType.TYPE_TYPE_LIST;
    }
    
    @Override
    public void addContents(final DexFile file) {
        final TypeIdsSection typeIds = file.getTypeIds();
        for (int sz = this.list.size(), i = 0; i < sz; ++i) {
            typeIds.intern(this.list.getType(i));
        }
    }
    
    @Override
    public String toHuman() {
        throw new RuntimeException("unsupported");
    }
    
    public TypeList getList() {
        return this.list;
    }
    
    @Override
    protected void writeTo0(final DexFile file, final AnnotatedOutput out) {
        final TypeIdsSection typeIds = file.getTypeIds();
        final int sz = this.list.size();
        if (out.annotates()) {
            out.annotate(0, this.offsetString() + " type_list");
            out.annotate(4, "  size: " + Hex.u4(sz));
            for (int i = 0; i < sz; ++i) {
                final Type one = this.list.getType(i);
                final int idx = typeIds.indexOf(one);
                out.annotate(2, "  " + Hex.u2(idx) + " // " + one.toHuman());
            }
        }
        out.writeInt(sz);
        for (int i = 0; i < sz; ++i) {
            out.writeShort(typeIds.indexOf(this.list.getType(i)));
        }
    }
    
    @Override
    protected int compareTo0(final OffsettedItem other) {
        final TypeList thisList = this.list;
        final TypeList otherList = ((TypeListItem)other).list;
        return StdTypeList.compareContents(thisList, otherList);
    }
}

package embedded.com.android.dx.dex.file;

import embedded.com.android.dx.rop.cst.*;
import embedded.com.android.dx.util.*;

public final class StringIdItem extends IndexedItem implements Comparable
{
    private final CstString value;
    private StringDataItem data;
    
    public StringIdItem(final CstString value) {
        if (value == null) {
            throw new NullPointerException("value == null");
        }
        this.value = value;
        this.data = null;
    }
    
    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof StringIdItem)) {
            return false;
        }
        final StringIdItem otherString = (StringIdItem)other;
        return this.value.equals(otherString.value);
    }
    
    @Override
    public int hashCode() {
        return this.value.hashCode();
    }
    
    @Override
    public int compareTo(final Object other) {
        final StringIdItem otherString = (StringIdItem)other;
        return this.value.compareTo((Constant)otherString.value);
    }
    
    @Override
    public ItemType itemType() {
        return ItemType.TYPE_STRING_ID_ITEM;
    }
    
    @Override
    public int writeSize() {
        return 4;
    }
    
    @Override
    public void addContents(final DexFile file) {
        if (this.data == null) {
            final MixedItemSection stringData = file.getStringData();
            stringData.add(this.data = new StringDataItem(this.value));
        }
    }
    
    @Override
    public void writeTo(final DexFile file, final AnnotatedOutput out) {
        final int dataOff = this.data.getAbsoluteOffset();
        if (out.annotates()) {
            out.annotate(0, this.indexString() + ' ' + this.value.toQuoted(100));
            out.annotate(4, "  string_data_off: " + Hex.u4(dataOff));
        }
        out.writeInt(dataOff);
    }
    
    public CstString getValue() {
        return this.value;
    }
    
    public StringDataItem getData() {
        return this.data;
    }
}

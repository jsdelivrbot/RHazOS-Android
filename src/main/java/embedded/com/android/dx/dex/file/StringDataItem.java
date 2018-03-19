package embedded.com.android.dx.dex.file;

import embedded.com.android.dex.*;
import embedded.com.android.dx.util.*;
import embedded.com.android.dx.rop.cst.*;

public final class StringDataItem extends OffsettedItem
{
    private final CstString value;
    
    public StringDataItem(final CstString value) {
        super(1, writeSize(value));
        this.value = value;
    }
    
    private static int writeSize(final CstString value) {
        final int utf16Size = value.getUtf16Size();
        return Leb128.unsignedLeb128Size(utf16Size) + value.getUtf8Size() + 1;
    }
    
    @Override
    public ItemType itemType() {
        return ItemType.TYPE_STRING_DATA_ITEM;
    }
    
    @Override
    public void addContents(final DexFile file) {
    }
    
    public void writeTo0(final DexFile file, final AnnotatedOutput out) {
        final ByteArray bytes = this.value.getBytes();
        final int utf16Size = this.value.getUtf16Size();
        if (out.annotates()) {
            out.annotate(Leb128.unsignedLeb128Size(utf16Size), "utf16_size: " + Hex.u4(utf16Size));
            out.annotate(bytes.size() + 1, this.value.toQuoted());
        }
        out.writeUleb128(utf16Size);
        out.write(bytes);
        out.writeByte(0);
    }
    
    @Override
    public String toHuman() {
        return this.value.toQuoted();
    }
    
    @Override
    protected int compareTo0(final OffsettedItem other) {
        final StringDataItem otherData = (StringDataItem)other;
        return this.value.compareTo((Constant)otherData.value);
    }
}

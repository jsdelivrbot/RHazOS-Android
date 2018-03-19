package embedded.com.android.dx.dex.file;

import embedded.com.android.dx.rop.cst.*;
import embedded.com.android.dx.util.*;

public final class HeaderItem extends IndexedItem
{
    @Override
    public ItemType itemType() {
        return ItemType.TYPE_HEADER_ITEM;
    }
    
    @Override
    public int writeSize() {
        return 112;
    }
    
    @Override
    public void addContents(final DexFile file) {
    }
    
    @Override
    public void writeTo(final DexFile file, final AnnotatedOutput out) {
        final int mapOff = file.getMap().getFileOffset();
        final Section firstDataSection = file.getFirstDataSection();
        final Section lastDataSection = file.getLastDataSection();
        final int dataOff = firstDataSection.getFileOffset();
        final int dataSize = lastDataSection.getFileOffset() + lastDataSection.writeSize() - dataOff;
        final String magic = file.getDexOptions().getMagic();
        if (out.annotates()) {
            out.annotate(8, "magic: " + new CstString(magic).toQuoted());
            out.annotate(4, "checksum");
            out.annotate(20, "signature");
            out.annotate(4, "file_size:       " + Hex.u4(file.getFileSize()));
            out.annotate(4, "header_size:     " + Hex.u4(112));
            out.annotate(4, "endian_tag:      " + Hex.u4(305419896));
            out.annotate(4, "link_size:       0");
            out.annotate(4, "link_off:        0");
            out.annotate(4, "map_off:         " + Hex.u4(mapOff));
        }
        for (int i = 0; i < 8; ++i) {
            out.writeByte(magic.charAt(i));
        }
        out.writeZeroes(24);
        out.writeInt(file.getFileSize());
        out.writeInt(112);
        out.writeInt(305419896);
        out.writeZeroes(8);
        out.writeInt(mapOff);
        file.getStringIds().writeHeaderPart(out);
        file.getTypeIds().writeHeaderPart(out);
        file.getProtoIds().writeHeaderPart(out);
        file.getFieldIds().writeHeaderPart(out);
        file.getMethodIds().writeHeaderPart(out);
        file.getClassDefs().writeHeaderPart(out);
        if (out.annotates()) {
            out.annotate(4, "data_size:       " + Hex.u4(dataSize));
            out.annotate(4, "data_off:        " + Hex.u4(dataOff));
        }
        out.writeInt(dataSize);
        out.writeInt(dataOff);
    }
}

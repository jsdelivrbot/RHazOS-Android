package embedded.com.android.dex;

import java.io.*;
import java.util.*;

public final class TableOfContents
{
    public final Section header;
    public final Section stringIds;
    public final Section typeIds;
    public final Section protoIds;
    public final Section fieldIds;
    public final Section methodIds;
    public final Section classDefs;
    public final Section callSiteIds;
    public final Section methodHandles;
    public final Section mapList;
    public final Section typeLists;
    public final Section annotationSetRefLists;
    public final Section annotationSets;
    public final Section classDatas;
    public final Section codes;
    public final Section stringDatas;
    public final Section debugInfos;
    public final Section annotations;
    public final Section encodedArrays;
    public final Section annotationsDirectories;
    public final Section[] sections;
    public int apiLevel;
    public int checksum;
    public byte[] signature;
    public int fileSize;
    public int linkSize;
    public int linkOff;
    public int dataSize;
    public int dataOff;
    
    public TableOfContents() {
        this.header = new Section(0);
        this.stringIds = new Section(1);
        this.typeIds = new Section(2);
        this.protoIds = new Section(3);
        this.fieldIds = new Section(4);
        this.methodIds = new Section(5);
        this.classDefs = new Section(6);
        this.callSiteIds = new Section(7);
        this.methodHandles = new Section(8);
        this.mapList = new Section(4096);
        this.typeLists = new Section(4097);
        this.annotationSetRefLists = new Section(4098);
        this.annotationSets = new Section(4099);
        this.classDatas = new Section(8192);
        this.codes = new Section(8193);
        this.stringDatas = new Section(8194);
        this.debugInfos = new Section(8195);
        this.annotations = new Section(8196);
        this.encodedArrays = new Section(8197);
        this.annotationsDirectories = new Section(8198);
        this.sections = new Section[] { this.header, this.stringIds, this.typeIds, this.protoIds, this.fieldIds, this.methodIds, this.classDefs, this.mapList, this.callSiteIds, this.methodHandles, this.typeLists, this.annotationSetRefLists, this.annotationSets, this.classDatas, this.codes, this.stringDatas, this.debugInfos, this.annotations, this.encodedArrays, this.annotationsDirectories };
        this.signature = new byte[20];
    }
    
    public void readFrom(final Dex dex) throws IOException {
        this.readHeader(dex.open(0));
        this.readMap(dex.open(this.mapList.off));
        this.computeSizesFromOffsets();
    }
    
    private void readHeader(final Dex.Section headerIn) throws UnsupportedEncodingException {
        final byte[] magic = headerIn.readByteArray(8);
        if (!DexFormat.isSupportedDexMagic(magic)) {
            final String msg = String.format("Unexpected magic: [0x%02x, 0x%02x, 0x%02x, 0x%02x, 0x%02x, 0x%02x, 0x%02x, 0x%02x]", magic[0], magic[1], magic[2], magic[3], magic[4], magic[5], magic[6], magic[7]);
            throw new DexException(msg);
        }
        this.apiLevel = DexFormat.magicToApi(magic);
        this.checksum = headerIn.readInt();
        this.signature = headerIn.readByteArray(20);
        this.fileSize = headerIn.readInt();
        final int headerSize = headerIn.readInt();
        if (headerSize != 112) {
            throw new DexException("Unexpected header: 0x" + Integer.toHexString(headerSize));
        }
        final int endianTag = headerIn.readInt();
        if (endianTag != 305419896) {
            throw new DexException("Unexpected endian tag: 0x" + Integer.toHexString(endianTag));
        }
        this.linkSize = headerIn.readInt();
        this.linkOff = headerIn.readInt();
        this.mapList.off = headerIn.readInt();
        if (this.mapList.off == 0) {
            throw new DexException("Cannot merge dex files that do not contain a map");
        }
        this.stringIds.size = headerIn.readInt();
        this.stringIds.off = headerIn.readInt();
        this.typeIds.size = headerIn.readInt();
        this.typeIds.off = headerIn.readInt();
        this.protoIds.size = headerIn.readInt();
        this.protoIds.off = headerIn.readInt();
        this.fieldIds.size = headerIn.readInt();
        this.fieldIds.off = headerIn.readInt();
        this.methodIds.size = headerIn.readInt();
        this.methodIds.off = headerIn.readInt();
        this.classDefs.size = headerIn.readInt();
        this.classDefs.off = headerIn.readInt();
        this.dataSize = headerIn.readInt();
        this.dataOff = headerIn.readInt();
    }
    
    private void readMap(final Dex.Section in) throws IOException {
        final int mapSize = in.readInt();
        Section previous = null;
        for (int i = 0; i < mapSize; ++i) {
            final short type = in.readShort();
            in.readShort();
            final Section section = this.getSection(type);
            final int size = in.readInt();
            final int offset = in.readInt();
            if ((section.size != 0 && section.size != size) || (section.off != -1 && section.off != offset)) {
                throw new DexException("Unexpected map value for 0x" + Integer.toHexString(type));
            }
            section.size = size;
            section.off = offset;
            if (previous != null && previous.off > section.off) {
                throw new DexException("Map is unsorted at " + previous + ", " + section);
            }
            previous = section;
        }
        Arrays.sort(this.sections);
    }
    
    public void computeSizesFromOffsets() {
        int end = this.dataOff + this.dataSize;
        for (int i = this.sections.length - 1; i >= 0; --i) {
            final Section section = this.sections[i];
            if (section.off != -1) {
                if (section.off > end) {
                    throw new DexException("Map is unsorted at " + section);
                }
                section.byteCount = end - section.off;
                end = section.off;
            }
        }
    }
    
    private Section getSection(final short type) {
        for (final Section section : this.sections) {
            if (section.type == type) {
                return section;
            }
        }
        throw new IllegalArgumentException("No such map item: " + type);
    }
    
    public void writeHeader(final Dex.Section out, final int api) throws IOException {
        out.write(DexFormat.apiToMagic(api).getBytes("UTF-8"));
        out.writeInt(this.checksum);
        out.write(this.signature);
        out.writeInt(this.fileSize);
        out.writeInt(112);
        out.writeInt(305419896);
        out.writeInt(this.linkSize);
        out.writeInt(this.linkOff);
        out.writeInt(this.mapList.off);
        out.writeInt(this.stringIds.size);
        out.writeInt(this.stringIds.off);
        out.writeInt(this.typeIds.size);
        out.writeInt(this.typeIds.off);
        out.writeInt(this.protoIds.size);
        out.writeInt(this.protoIds.off);
        out.writeInt(this.fieldIds.size);
        out.writeInt(this.fieldIds.off);
        out.writeInt(this.methodIds.size);
        out.writeInt(this.methodIds.off);
        out.writeInt(this.classDefs.size);
        out.writeInt(this.classDefs.off);
        out.writeInt(this.dataSize);
        out.writeInt(this.dataOff);
    }
    
    public void writeMap(final Dex.Section out) throws IOException {
        int count = 0;
        for (final Section section : this.sections) {
            if (section.exists()) {
                ++count;
            }
        }
        out.writeInt(count);
        for (final Section section : this.sections) {
            if (section.exists()) {
                out.writeShort(section.type);
                out.writeShort((short)0);
                out.writeInt(section.size);
                out.writeInt(section.off);
            }
        }
    }
    
    public static class Section implements Comparable<Section>
    {
        public final short type;
        public int size;
        public int off;
        public int byteCount;
        
        public Section(final int type) {
            this.size = 0;
            this.off = -1;
            this.byteCount = 0;
            this.type = (short)type;
        }
        
        public boolean exists() {
            return this.size > 0;
        }
        
        @Override
        public int compareTo(final Section section) {
            if (this.off != section.off) {
                return (this.off < section.off) ? -1 : 1;
            }
            return 0;
        }
        
        @Override
        public String toString() {
            return String.format("Section[type=%#x,off=%#x,size=%#x]", this.type, this.off, this.size);
        }
    }
}

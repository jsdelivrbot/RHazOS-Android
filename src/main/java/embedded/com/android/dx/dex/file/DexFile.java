package embedded.com.android.dx.dex.file;

import embedded.com.android.dx.dex.*;
import embedded.com.android.dx.rop.type.*;
import java.io.*;
import embedded.com.android.dx.rop.cst.*;
import embedded.com.android.dex.util.*;
import embedded.com.android.dx.util.*;
import java.security.*;
import java.util.zip.*;

public final class DexFile
{
    private final DexOptions dexOptions;
    private final MixedItemSection wordData;
    private final MixedItemSection typeLists;
    private final MixedItemSection map;
    private final MixedItemSection stringData;
    private final StringIdsSection stringIds;
    private final TypeIdsSection typeIds;
    private final ProtoIdsSection protoIds;
    private final FieldIdsSection fieldIds;
    private final MethodIdsSection methodIds;
    private final ClassDefsSection classDefs;
    private final MixedItemSection classData;
    private final CallSiteIdsSection callSiteIds;
    private final MethodHandlesSection methodHandles;
    private final MixedItemSection byteData;
    private final HeaderSection header;
    private final Section[] sections;
    private int fileSize;
    private int dumpWidth;
    
    public DexFile(final DexOptions dexOptions) {
        this.dexOptions = dexOptions;
        this.header = new HeaderSection(this);
        this.typeLists = new MixedItemSection(null, this, 4, MixedItemSection.SortType.NONE);
        this.wordData = new MixedItemSection("word_data", this, 4, MixedItemSection.SortType.TYPE);
        this.stringData = new MixedItemSection("string_data", this, 1, MixedItemSection.SortType.INSTANCE);
        this.classData = new MixedItemSection(null, this, 1, MixedItemSection.SortType.NONE);
        this.byteData = new MixedItemSection("byte_data", this, 1, MixedItemSection.SortType.TYPE);
        this.stringIds = new StringIdsSection(this);
        this.typeIds = new TypeIdsSection(this);
        this.protoIds = new ProtoIdsSection(this);
        this.fieldIds = new FieldIdsSection(this);
        this.methodIds = new MethodIdsSection(this);
        this.classDefs = new ClassDefsSection(this);
        this.map = new MixedItemSection("map", this, 4, MixedItemSection.SortType.NONE);
        if (dexOptions.canUseInvokeCustom()) {
            this.callSiteIds = new CallSiteIdsSection(this);
            this.methodHandles = new MethodHandlesSection(this);
            this.sections = new Section[] { this.header, this.stringIds, this.typeIds, this.protoIds, this.fieldIds, this.methodIds, this.classDefs, this.callSiteIds, this.methodHandles, this.wordData, this.typeLists, this.stringData, this.byteData, this.classData, this.map };
        }
        else {
            this.callSiteIds = null;
            this.methodHandles = null;
            this.sections = new Section[] { this.header, this.stringIds, this.typeIds, this.protoIds, this.fieldIds, this.methodIds, this.classDefs, this.wordData, this.typeLists, this.stringData, this.byteData, this.classData, this.map };
        }
        this.fileSize = -1;
        this.dumpWidth = 79;
    }
    
    public boolean isEmpty() {
        return this.classDefs.items().isEmpty();
    }
    
    public DexOptions getDexOptions() {
        return this.dexOptions;
    }
    
    public void add(final ClassDefItem clazz) {
        this.classDefs.add(clazz);
    }
    
    public ClassDefItem getClassOrNull(final String name) {
        try {
            final Type type = Type.internClassName(name);
            return (ClassDefItem)this.classDefs.get(new CstType(type));
        }
        catch (IllegalArgumentException ex) {
            return null;
        }
    }
    
    public void writeTo(final OutputStream out, final Writer humanOut, final boolean verbose) throws IOException {
        final boolean annotate = humanOut != null;
        final ByteArrayAnnotatedOutput result = this.toDex0(annotate, verbose);
        if (out != null) {
            out.write(result.getArray());
        }
        if (annotate) {
            result.writeAnnotationsTo(humanOut);
        }
    }
    
    public byte[] toDex(final Writer humanOut, final boolean verbose) throws IOException {
        final boolean annotate = humanOut != null;
        final ByteArrayAnnotatedOutput result = this.toDex0(annotate, verbose);
        if (annotate) {
            result.writeAnnotationsTo(humanOut);
        }
        return result.getArray();
    }
    
    public void setDumpWidth(final int dumpWidth) {
        if (dumpWidth < 40) {
            throw new IllegalArgumentException("dumpWidth < 40");
        }
        this.dumpWidth = dumpWidth;
    }
    
    public int getFileSize() {
        if (this.fileSize < 0) {
            throw new RuntimeException("file size not yet known");
        }
        return this.fileSize;
    }
    
    MixedItemSection getStringData() {
        return this.stringData;
    }
    
    MixedItemSection getWordData() {
        return this.wordData;
    }
    
    MixedItemSection getTypeLists() {
        return this.typeLists;
    }
    
    MixedItemSection getMap() {
        return this.map;
    }
    
    StringIdsSection getStringIds() {
        return this.stringIds;
    }
    
    public ClassDefsSection getClassDefs() {
        return this.classDefs;
    }
    
    MixedItemSection getClassData() {
        return this.classData;
    }
    
    public TypeIdsSection getTypeIds() {
        return this.typeIds;
    }
    
    ProtoIdsSection getProtoIds() {
        return this.protoIds;
    }
    
    public FieldIdsSection getFieldIds() {
        return this.fieldIds;
    }
    
    public MethodIdsSection getMethodIds() {
        return this.methodIds;
    }
    
    public MethodHandlesSection getMethodHandles() {
        return this.methodHandles;
    }
    
    public CallSiteIdsSection getCallSiteIds() {
        return this.callSiteIds;
    }
    
    MixedItemSection getByteData() {
        return this.byteData;
    }
    
    Section getFirstDataSection() {
        return this.wordData;
    }
    
    Section getLastDataSection() {
        return this.map;
    }
    
    void internIfAppropriate(final Constant cst) {
        if (cst == null) {
            throw new NullPointerException("cst == null");
        }
        if (cst instanceof CstString) {
            this.stringIds.intern((CstString)cst);
        }
        else if (cst instanceof CstType) {
            this.typeIds.intern((CstType)cst);
        }
        else if (cst instanceof CstBaseMethodRef) {
            this.methodIds.intern((CstBaseMethodRef)cst);
        }
        else if (cst instanceof CstFieldRef) {
            this.fieldIds.intern((CstFieldRef)cst);
        }
        else if (cst instanceof CstEnumRef) {
            this.fieldIds.intern(((CstEnumRef)cst).getFieldRef());
        }
        else if (cst instanceof CstProtoRef) {
            this.protoIds.intern(((CstProtoRef)cst).getPrototype());
        }
        else if (cst instanceof CstMethodHandle) {
            this.methodHandles.intern((CstMethodHandle)cst);
        }
    }
    
    IndexedItem findItemOrNull(final Constant cst) {
        if (cst instanceof CstString) {
            return this.stringIds.get(cst);
        }
        if (cst instanceof CstType) {
            return this.typeIds.get(cst);
        }
        if (cst instanceof CstBaseMethodRef) {
            return this.methodIds.get(cst);
        }
        if (cst instanceof CstFieldRef) {
            return this.fieldIds.get(cst);
        }
        if (cst instanceof CstEnumRef) {
            return this.fieldIds.intern(((CstEnumRef)cst).getFieldRef());
        }
        if (cst instanceof CstProtoRef) {
            return this.protoIds.get(cst);
        }
        if (cst instanceof CstMethodHandle) {
            return this.methodHandles.get(cst);
        }
        if (cst instanceof CstCallSiteRef) {
            return this.callSiteIds.get(cst);
        }
        return null;
    }
    
    private ByteArrayAnnotatedOutput toDex0(final boolean annotate, final boolean verbose) {
        this.classDefs.prepare();
        this.classData.prepare();
        this.wordData.prepare();
        if (this.dexOptions.canUseInvokePolymorphic()) {
            this.callSiteIds.prepare();
        }
        this.byteData.prepare();
        if (this.dexOptions.canUseInvokePolymorphic()) {
            this.methodHandles.prepare();
        }
        this.methodIds.prepare();
        this.fieldIds.prepare();
        this.protoIds.prepare();
        this.typeLists.prepare();
        this.typeIds.prepare();
        this.stringIds.prepare();
        this.stringData.prepare();
        this.header.prepare();
        final int count = this.sections.length;
        int offset = 0;
        for (int i = 0; i < count; ++i) {
            final Section one = this.sections[i];
            if ((one != this.callSiteIds && one != this.methodHandles) || !one.items().isEmpty()) {
                final int placedAt = one.setFileOffset(offset);
                if (placedAt < offset) {
                    throw new RuntimeException("bogus placement for section " + i);
                }
                try {
                    if (one == this.map) {
                        MapItem.addMap(this.sections, this.map);
                        this.map.prepare();
                    }
                    if (one instanceof MixedItemSection) {
                        ((MixedItemSection)one).placeItems();
                    }
                    offset = placedAt + one.writeSize();
                }
                catch (RuntimeException ex) {
                    throw ExceptionWithContext.withContext(ex, "...while writing section " + i);
                }
            }
        }
        this.fileSize = offset;
        final byte[] barr = new byte[this.fileSize];
        final ByteArrayAnnotatedOutput out = new ByteArrayAnnotatedOutput(barr);
        if (annotate) {
            out.enableAnnotations(this.dumpWidth, verbose);
        }
        for (int j = 0; j < count; ++j) {
            try {
                final Section one2 = this.sections[j];
                if ((one2 != this.callSiteIds && one2 != this.methodHandles) || !one2.items().isEmpty()) {
                    final int zeroCount = one2.getFileOffset() - out.getCursor();
                    if (zeroCount < 0) {
                        throw new ExceptionWithContext("excess write of " + -zeroCount);
                    }
                    out.writeZeroes(zeroCount);
                    one2.writeTo(out);
                }
            }
            catch (RuntimeException ex) {
                ExceptionWithContext ec;
                if (ex instanceof ExceptionWithContext) {
                    ec = (ExceptionWithContext)ex;
                }
                else {
                    ec = new ExceptionWithContext(ex);
                }
                ec.addContext("...while writing section " + j);
                throw ec;
            }
        }
        if (out.getCursor() != this.fileSize) {
            throw new RuntimeException("foreshortened write");
        }
        calcSignature(barr);
        calcChecksum(barr);
        if (annotate) {
            this.wordData.writeIndexAnnotation(out, ItemType.TYPE_CODE_ITEM, "\nmethod code index:\n\n");
            this.getStatistics().writeAnnotation(out);
            out.finishAnnotating();
        }
        return out;
    }
    
    public Statistics getStatistics() {
        final Statistics stats = new Statistics();
        for (final Section s : this.sections) {
            stats.addAll(s);
        }
        return stats;
    }
    
    private static void calcSignature(final byte[] bytes) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-1");
        }
        catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
        md.update(bytes, 32, bytes.length - 32);
        try {
            final int amt = md.digest(bytes, 12, 20);
            if (amt != 20) {
                throw new RuntimeException("unexpected digest write: " + amt + " bytes");
            }
        }
        catch (DigestException ex2) {
            throw new RuntimeException(ex2);
        }
    }
    
    private static void calcChecksum(final byte[] bytes) {
        final Adler32 a32 = new Adler32();
        a32.update(bytes, 12, bytes.length - 12);
        final int sum = (int)a32.getValue();
        bytes[8] = (byte)sum;
        bytes[9] = (byte)(sum >> 8);
        bytes[10] = (byte)(sum >> 16);
        bytes[11] = (byte)(sum >> 24);
    }
}

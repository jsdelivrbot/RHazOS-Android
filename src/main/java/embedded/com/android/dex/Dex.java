package embedded.com.android.dex;

import java.nio.*;
import java.security.*;
import java.util.zip.*;
import embedded.com.android.dex.util.*;
import java.io.*;
import java.util.*;

public final class Dex
{
    private static final int CHECKSUM_OFFSET = 8;
    private static final int CHECKSUM_SIZE = 4;
    private static final int SIGNATURE_OFFSET = 12;
    private static final int SIGNATURE_SIZE = 20;
    static final short[] EMPTY_SHORT_ARRAY;
    private ByteBuffer data;
    private final TableOfContents tableOfContents;
    private int nextSectionStart;
    private final StringTable strings;
    private final TypeIndexToDescriptorIndexTable typeIds;
    private final TypeIndexToDescriptorTable typeNames;
    private final ProtoIdTable protoIds;
    private final FieldIdTable fieldIds;
    private final MethodIdTable methodIds;
    
    public Dex(final byte[] data) throws IOException {
        this(ByteBuffer.wrap(data));
    }
    
    private Dex(final ByteBuffer data) throws IOException {
        this.tableOfContents = new TableOfContents();
        this.nextSectionStart = 0;
        this.strings = new StringTable();
        this.typeIds = new TypeIndexToDescriptorIndexTable();
        this.typeNames = new TypeIndexToDescriptorTable();
        this.protoIds = new ProtoIdTable();
        this.fieldIds = new FieldIdTable();
        this.methodIds = new MethodIdTable();
        (this.data = data).order(ByteOrder.LITTLE_ENDIAN);
        this.tableOfContents.readFrom(this);
    }
    
    public Dex(final int byteCount) throws IOException {
        this.tableOfContents = new TableOfContents();
        this.nextSectionStart = 0;
        this.strings = new StringTable();
        this.typeIds = new TypeIndexToDescriptorIndexTable();
        this.typeNames = new TypeIndexToDescriptorTable();
        this.protoIds = new ProtoIdTable();
        this.fieldIds = new FieldIdTable();
        this.methodIds = new MethodIdTable();
        (this.data = ByteBuffer.wrap(new byte[byteCount])).order(ByteOrder.LITTLE_ENDIAN);
    }
    
    public Dex(final InputStream in) throws IOException {
        this.tableOfContents = new TableOfContents();
        this.nextSectionStart = 0;
        this.strings = new StringTable();
        this.typeIds = new TypeIndexToDescriptorIndexTable();
        this.typeNames = new TypeIndexToDescriptorTable();
        this.protoIds = new ProtoIdTable();
        this.fieldIds = new FieldIdTable();
        this.methodIds = new MethodIdTable();
        try {
            this.loadFrom(in);
        }
        finally {
            in.close();
        }
    }
    
    public Dex(final File file) throws IOException {
        this.tableOfContents = new TableOfContents();
        this.nextSectionStart = 0;
        this.strings = new StringTable();
        this.typeIds = new TypeIndexToDescriptorIndexTable();
        this.typeNames = new TypeIndexToDescriptorTable();
        this.protoIds = new ProtoIdTable();
        this.fieldIds = new FieldIdTable();
        this.methodIds = new MethodIdTable();
        if (FileUtils.hasArchiveSuffix(file.getName())) {
            final ZipFile zipFile = new ZipFile(file);
            final ZipEntry entry = zipFile.getEntry("classes.dex");
            if (entry == null) {
                throw new DexException("Expected classes.dex in " + file);
            }
            try (final InputStream inputStream = zipFile.getInputStream(entry)) {
                this.loadFrom(inputStream);
            }
            zipFile.close();
        }
        else {
            if (!file.getName().endsWith(".dex")) {
                throw new DexException("unknown output extension: " + file);
            }
            try (final InputStream inputStream2 = new FileInputStream(file)) {
                this.loadFrom(inputStream2);
            }
        }
    }
    
    private void loadFrom(final InputStream in) throws IOException {
        final ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
        final byte[] buffer = new byte[8192];
        int count;
        while ((count = in.read(buffer)) != -1) {
            bytesOut.write(buffer, 0, count);
        }
        (this.data = ByteBuffer.wrap(bytesOut.toByteArray())).order(ByteOrder.LITTLE_ENDIAN);
        this.tableOfContents.readFrom(this);
    }
    
    private static void checkBounds(final int index, final int length) {
        if (index < 0 || index >= length) {
            throw new IndexOutOfBoundsException("index:" + index + ", length=" + length);
        }
    }
    
    public void writeTo(final OutputStream out) throws IOException {
        final byte[] buffer = new byte[8192];
        final ByteBuffer data = this.data.duplicate();
        data.clear();
        while (data.hasRemaining()) {
            final int count = Math.min(buffer.length, data.remaining());
            data.get(buffer, 0, count);
            out.write(buffer, 0, count);
        }
    }
    
    public void writeTo(final File dexOut) throws IOException {
        try (final OutputStream out = new FileOutputStream(dexOut)) {
            this.writeTo(out);
        }
    }
    
    public TableOfContents getTableOfContents() {
        return this.tableOfContents;
    }
    
    public Section open(final int position) {
        if (position < 0 || position >= this.data.capacity()) {
            throw new IllegalArgumentException("position=" + position + " length=" + this.data.capacity());
        }
        final ByteBuffer sectionData = this.data.duplicate();
        sectionData.order(ByteOrder.LITTLE_ENDIAN);
        sectionData.position(position);
        sectionData.limit(this.data.capacity());
        return new Section("section", sectionData);
    }
    
    public Section appendSection(final int maxByteCount, final String name) {
        if ((maxByteCount & 0x3) != 0x0) {
            throw new IllegalStateException("Not four byte aligned!");
        }
        final int limit = this.nextSectionStart + maxByteCount;
        final ByteBuffer sectionData = this.data.duplicate();
        sectionData.order(ByteOrder.LITTLE_ENDIAN);
        sectionData.position(this.nextSectionStart);
        sectionData.limit(limit);
        final Section result = new Section(name, sectionData);
        this.nextSectionStart = limit;
        return result;
    }
    
    public int getLength() {
        return this.data.capacity();
    }
    
    public int getNextSectionStart() {
        return this.nextSectionStart;
    }
    
    public byte[] getBytes() {
        final ByteBuffer data = this.data.duplicate();
        final byte[] result = new byte[data.capacity()];
        data.position(0);
        data.get(result);
        return result;
    }
    
    public List<String> strings() {
        return this.strings;
    }
    
    public List<Integer> typeIds() {
        return this.typeIds;
    }
    
    public List<String> typeNames() {
        return this.typeNames;
    }
    
    public List<ProtoId> protoIds() {
        return this.protoIds;
    }
    
    public List<FieldId> fieldIds() {
        return this.fieldIds;
    }
    
    public List<MethodId> methodIds() {
        return this.methodIds;
    }
    
    public Iterable<ClassDef> classDefs() {
        return new ClassDefIterable();
    }
    
    public TypeList readTypeList(final int offset) {
        if (offset == 0) {
            return TypeList.EMPTY;
        }
        return this.open(offset).readTypeList();
    }
    
    public ClassData readClassData(final ClassDef classDef) {
        final int offset = classDef.getClassDataOffset();
        if (offset == 0) {
            throw new IllegalArgumentException("offset == 0");
        }
        return this.open(offset).readClassData();
    }
    
    public Code readCode(final ClassData.Method method) {
        final int offset = method.getCodeOffset();
        if (offset == 0) {
            throw new IllegalArgumentException("offset == 0");
        }
        return this.open(offset).readCode();
    }
    
    public byte[] computeSignature() throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-1");
        }
        catch (NoSuchAlgorithmException e) {
            throw new AssertionError();
        }
        final byte[] buffer = new byte[8192];
        final ByteBuffer data = this.data.duplicate();
        data.limit(data.capacity());
        data.position(32);
        while (data.hasRemaining()) {
            final int count = Math.min(buffer.length, data.remaining());
            data.get(buffer, 0, count);
            digest.update(buffer, 0, count);
        }
        return digest.digest();
    }
    
    public int computeChecksum() throws IOException {
        final Adler32 adler32 = new Adler32();
        final byte[] buffer = new byte[8192];
        final ByteBuffer data = this.data.duplicate();
        data.limit(data.capacity());
        data.position(12);
        while (data.hasRemaining()) {
            final int count = Math.min(buffer.length, data.remaining());
            data.get(buffer, 0, count);
            adler32.update(buffer, 0, count);
        }
        return (int)adler32.getValue();
    }
    
    public void writeHashes() throws IOException {
        this.open(12).write(this.computeSignature());
        this.open(8).writeInt(this.computeChecksum());
    }
    
    public int descriptorIndexFromTypeIndex(final int typeIndex) {
        checkBounds(typeIndex, this.tableOfContents.typeIds.size);
        final int position = this.tableOfContents.typeIds.off + 4 * typeIndex;
        return this.data.getInt(position);
    }
    
    static {
        EMPTY_SHORT_ARRAY = new short[0];
    }
    
    public final class Section implements ByteInput, ByteOutput
    {
        private final String name;
        private final ByteBuffer data;
        private final int initialPosition;
        
        private Section(final String name, final ByteBuffer data) {
            this.name = name;
            this.data = data;
            this.initialPosition = data.position();
        }
        
        public int getPosition() {
            return this.data.position();
        }
        
        public int readInt() {
            return this.data.getInt();
        }
        
        public short readShort() {
            return this.data.getShort();
        }
        
        public int readUnsignedShort() {
            return this.readShort() & 0xFFFF;
        }
        
        @Override
        public byte readByte() {
            return this.data.get();
        }
        
        public byte[] readByteArray(final int length) {
            final byte[] result = new byte[length];
            this.data.get(result);
            return result;
        }
        
        public short[] readShortArray(final int length) {
            if (length == 0) {
                return Dex.EMPTY_SHORT_ARRAY;
            }
            final short[] result = new short[length];
            for (int i = 0; i < length; ++i) {
                result[i] = this.readShort();
            }
            return result;
        }
        
        public int readUleb128() {
            return Leb128.readUnsignedLeb128(this);
        }
        
        public int readUleb128p1() {
            return Leb128.readUnsignedLeb128(this) - 1;
        }
        
        public int readSleb128() {
            return Leb128.readSignedLeb128(this);
        }
        
        public void writeUleb128p1(final int i) {
            this.writeUleb128(i + 1);
        }
        
        public TypeList readTypeList() {
            final int size = this.readInt();
            final short[] types = this.readShortArray(size);
            this.alignToFourBytes();
            return new TypeList(Dex.this, types);
        }
        
        public String readString() {
            final int offset = this.readInt();
            final int savedPosition = this.data.position();
            final int savedLimit = this.data.limit();
            this.data.position(offset);
            this.data.limit(this.data.capacity());
            try {
                final int expectedLength = this.readUleb128();
                final String result = Mutf8.decode(this, new char[expectedLength]);
                if (result.length() != expectedLength) {
                    throw new DexException("Declared length " + expectedLength + " doesn't match decoded length of " + result.length());
                }
                return result;
            }
            catch (UTFDataFormatException e) {
                throw new DexException(e);
            }
            finally {
                this.data.position(savedPosition);
                this.data.limit(savedLimit);
            }
        }
        
        public FieldId readFieldId() {
            final int declaringClassIndex = this.readUnsignedShort();
            final int typeIndex = this.readUnsignedShort();
            final int nameIndex = this.readInt();
            return new FieldId(Dex.this, declaringClassIndex, typeIndex, nameIndex);
        }
        
        public MethodId readMethodId() {
            final int declaringClassIndex = this.readUnsignedShort();
            final int protoIndex = this.readUnsignedShort();
            final int nameIndex = this.readInt();
            return new MethodId(Dex.this, declaringClassIndex, protoIndex, nameIndex);
        }
        
        public ProtoId readProtoId() {
            final int shortyIndex = this.readInt();
            final int returnTypeIndex = this.readInt();
            final int parametersOffset = this.readInt();
            return new ProtoId(Dex.this, shortyIndex, returnTypeIndex, parametersOffset);
        }
        
        public CallSiteId readCallSiteId() {
            final int offset = this.readInt();
            return new CallSiteId(Dex.this, offset);
        }
        
        public MethodHandle readMethodHandle() {
            final MethodHandle.MethodHandleType methodHandleType = MethodHandle.MethodHandleType.fromValue(this.readUnsignedShort());
            final int unused1 = this.readUnsignedShort();
            final int fieldOrMethodId = this.readUnsignedShort();
            final int unused2 = this.readUnsignedShort();
            return new MethodHandle(Dex.this, methodHandleType, unused1, fieldOrMethodId, unused2);
        }
        
        public ClassDef readClassDef() {
            final int offset = this.getPosition();
            final int type = this.readInt();
            final int accessFlags = this.readInt();
            final int supertype = this.readInt();
            final int interfacesOffset = this.readInt();
            final int sourceFileIndex = this.readInt();
            final int annotationsOffset = this.readInt();
            final int classDataOffset = this.readInt();
            final int staticValuesOffset = this.readInt();
            return new ClassDef(Dex.this, offset, type, accessFlags, supertype, interfacesOffset, sourceFileIndex, annotationsOffset, classDataOffset, staticValuesOffset);
        }
        
        private Code readCode() {
            final int registersSize = this.readUnsignedShort();
            final int insSize = this.readUnsignedShort();
            final int outsSize = this.readUnsignedShort();
            final int triesSize = this.readUnsignedShort();
            final int debugInfoOffset = this.readInt();
            final int instructionsSize = this.readInt();
            final short[] instructions = this.readShortArray(instructionsSize);
            Code.CatchHandler[] catchHandlers;
            Code.Try[] tries;
            if (triesSize > 0) {
                if (instructions.length % 2 == 1) {
                    this.readShort();
                }
                final Section triesSection = Dex.this.open(this.data.position());
                this.skip(triesSize * 8);
                catchHandlers = this.readCatchHandlers();
                tries = triesSection.readTries(triesSize, catchHandlers);
            }
            else {
                tries = new Code.Try[0];
                catchHandlers = new Code.CatchHandler[0];
            }
            return new Code(registersSize, insSize, outsSize, debugInfoOffset, instructions, tries, catchHandlers);
        }
        
        private Code.CatchHandler[] readCatchHandlers() {
            final int baseOffset = this.data.position();
            final int catchHandlersSize = this.readUleb128();
            final Code.CatchHandler[] result = new Code.CatchHandler[catchHandlersSize];
            for (int i = 0; i < catchHandlersSize; ++i) {
                final int offset = this.data.position() - baseOffset;
                result[i] = this.readCatchHandler(offset);
            }
            return result;
        }
        
        private Code.Try[] readTries(final int triesSize, final Code.CatchHandler[] catchHandlers) {
            final Code.Try[] result = new Code.Try[triesSize];
            for (int i = 0; i < triesSize; ++i) {
                final int startAddress = this.readInt();
                final int instructionCount = this.readUnsignedShort();
                final int handlerOffset = this.readUnsignedShort();
                final int catchHandlerIndex = this.findCatchHandlerIndex(catchHandlers, handlerOffset);
                result[i] = new Code.Try(startAddress, instructionCount, catchHandlerIndex);
            }
            return result;
        }
        
        private int findCatchHandlerIndex(final Code.CatchHandler[] catchHandlers, final int offset) {
            for (int i = 0; i < catchHandlers.length; ++i) {
                final Code.CatchHandler catchHandler = catchHandlers[i];
                if (catchHandler.getOffset() == offset) {
                    return i;
                }
            }
            throw new IllegalArgumentException();
        }
        
        private Code.CatchHandler readCatchHandler(final int offset) {
            final int size = this.readSleb128();
            final int handlersCount = Math.abs(size);
            final int[] typeIndexes = new int[handlersCount];
            final int[] addresses = new int[handlersCount];
            for (int i = 0; i < handlersCount; ++i) {
                typeIndexes[i] = this.readUleb128();
                addresses[i] = this.readUleb128();
            }
            final int catchAllAddress = (size <= 0) ? this.readUleb128() : -1;
            return new Code.CatchHandler(typeIndexes, addresses, catchAllAddress, offset);
        }
        
        private ClassData readClassData() {
            final int staticFieldsSize = this.readUleb128();
            final int instanceFieldsSize = this.readUleb128();
            final int directMethodsSize = this.readUleb128();
            final int virtualMethodsSize = this.readUleb128();
            final ClassData.Field[] staticFields = this.readFields(staticFieldsSize);
            final ClassData.Field[] instanceFields = this.readFields(instanceFieldsSize);
            final ClassData.Method[] directMethods = this.readMethods(directMethodsSize);
            final ClassData.Method[] virtualMethods = this.readMethods(virtualMethodsSize);
            return new ClassData(staticFields, instanceFields, directMethods, virtualMethods);
        }
        
        private ClassData.Field[] readFields(final int count) {
            final ClassData.Field[] result = new ClassData.Field[count];
            int fieldIndex = 0;
            for (int i = 0; i < count; ++i) {
                fieldIndex += this.readUleb128();
                final int accessFlags = this.readUleb128();
                result[i] = new ClassData.Field(fieldIndex, accessFlags);
            }
            return result;
        }
        
        private ClassData.Method[] readMethods(final int count) {
            final ClassData.Method[] result = new ClassData.Method[count];
            int methodIndex = 0;
            for (int i = 0; i < count; ++i) {
                methodIndex += this.readUleb128();
                final int accessFlags = this.readUleb128();
                final int codeOff = this.readUleb128();
                result[i] = new ClassData.Method(methodIndex, accessFlags, codeOff);
            }
            return result;
        }
        
        private byte[] getBytesFrom(final int start) {
            final int end = this.data.position();
            final byte[] result = new byte[end - start];
            this.data.position(start);
            this.data.get(result);
            return result;
        }
        
        public Annotation readAnnotation() {
            final byte visibility = this.readByte();
            final int start = this.data.position();
            new EncodedValueReader(this, 29).skipValue();
            return new Annotation(Dex.this, visibility, new EncodedValue(this.getBytesFrom(start)));
        }
        
        public EncodedValue readEncodedArray() {
            final int start = this.data.position();
            new EncodedValueReader(this, 28).skipValue();
            return new EncodedValue(this.getBytesFrom(start));
        }
        
        public void skip(final int count) {
            if (count < 0) {
                throw new IllegalArgumentException();
            }
            this.data.position(this.data.position() + count);
        }
        
        public void alignToFourBytes() {
            this.data.position(this.data.position() + 3 & 0xFFFFFFFC);
        }
        
        public void alignToFourBytesWithZeroFill() {
            while ((this.data.position() & 0x3) != 0x0) {
                this.data.put((byte)0);
            }
        }
        
        public void assertFourByteAligned() {
            if ((this.data.position() & 0x3) != 0x0) {
                throw new IllegalStateException("Not four byte aligned!");
            }
        }
        
        public void write(final byte[] bytes) {
            this.data.put(bytes);
        }
        
        @Override
        public void writeByte(final int b) {
            this.data.put((byte)b);
        }
        
        public void writeShort(final short i) {
            this.data.putShort(i);
        }
        
        public void writeUnsignedShort(final int i) {
            final short s = (short)i;
            if (i != (s & 0xFFFF)) {
                throw new IllegalArgumentException("Expected an unsigned short: " + i);
            }
            this.writeShort(s);
        }
        
        public void write(final short[] shorts) {
            for (final short s : shorts) {
                this.writeShort(s);
            }
        }
        
        public void writeInt(final int i) {
            this.data.putInt(i);
        }
        
        public void writeUleb128(final int i) {
            try {
                Leb128.writeUnsignedLeb128(this, i);
            }
            catch (ArrayIndexOutOfBoundsException e) {
                throw new DexException("Section limit " + this.data.limit() + " exceeded by " + this.name);
            }
        }
        
        public void writeSleb128(final int i) {
            try {
                Leb128.writeSignedLeb128(this, i);
            }
            catch (ArrayIndexOutOfBoundsException e) {
                throw new DexException("Section limit " + this.data.limit() + " exceeded by " + this.name);
            }
        }
        
        public void writeStringData(final String value) {
            try {
                final int length = value.length();
                this.writeUleb128(length);
                this.write(Mutf8.encode(value));
                this.writeByte(0);
            }
            catch (UTFDataFormatException e) {
                throw new AssertionError();
            }
        }
        
        public void writeTypeList(final TypeList typeList) {
            final short[] types = typeList.getTypes();
            this.writeInt(types.length);
            for (final short type : types) {
                this.writeShort(type);
            }
            this.alignToFourBytesWithZeroFill();
        }
        
        public int used() {
            return this.data.position() - this.initialPosition;
        }
    }
    
    private final class StringTable extends AbstractList<String> implements RandomAccess
    {
        @Override
        public String get(final int index) {
            checkBounds(index, Dex.this.tableOfContents.stringIds.size);
            return Dex.this.open(Dex.this.tableOfContents.stringIds.off + index * 4).readString();
        }
        
        @Override
        public int size() {
            return Dex.this.tableOfContents.stringIds.size;
        }
    }
    
    private final class TypeIndexToDescriptorIndexTable extends AbstractList<Integer> implements RandomAccess
    {
        @Override
        public Integer get(final int index) {
            return Dex.this.descriptorIndexFromTypeIndex(index);
        }
        
        @Override
        public int size() {
            return Dex.this.tableOfContents.typeIds.size;
        }
    }
    
    private final class TypeIndexToDescriptorTable extends AbstractList<String> implements RandomAccess
    {
        @Override
        public String get(final int index) {
            return Dex.this.strings.get(Dex.this.descriptorIndexFromTypeIndex(index));
        }
        
        @Override
        public int size() {
            return Dex.this.tableOfContents.typeIds.size;
        }
    }
    
    private final class ProtoIdTable extends AbstractList<ProtoId> implements RandomAccess
    {
        @Override
        public ProtoId get(final int index) {
            checkBounds(index, Dex.this.tableOfContents.protoIds.size);
            return Dex.this.open(Dex.this.tableOfContents.protoIds.off + 12 * index).readProtoId();
        }
        
        @Override
        public int size() {
            return Dex.this.tableOfContents.protoIds.size;
        }
    }
    
    private final class FieldIdTable extends AbstractList<FieldId> implements RandomAccess
    {
        @Override
        public FieldId get(final int index) {
            checkBounds(index, Dex.this.tableOfContents.fieldIds.size);
            return Dex.this.open(Dex.this.tableOfContents.fieldIds.off + 8 * index).readFieldId();
        }
        
        @Override
        public int size() {
            return Dex.this.tableOfContents.fieldIds.size;
        }
    }
    
    private final class MethodIdTable extends AbstractList<MethodId> implements RandomAccess
    {
        @Override
        public MethodId get(final int index) {
            checkBounds(index, Dex.this.tableOfContents.methodIds.size);
            return Dex.this.open(Dex.this.tableOfContents.methodIds.off + 8 * index).readMethodId();
        }
        
        @Override
        public int size() {
            return Dex.this.tableOfContents.methodIds.size;
        }
    }
    
    private final class ClassDefIterator implements Iterator<ClassDef>
    {
        private final Section in;
        private int count;
        
        private ClassDefIterator() {
            this.in = Dex.this.open(Dex.this.tableOfContents.classDefs.off);
            this.count = 0;
        }
        
        @Override
        public boolean hasNext() {
            return this.count < Dex.this.tableOfContents.classDefs.size;
        }
        
        @Override
        public ClassDef next() {
            if (!this.hasNext()) {
                throw new NoSuchElementException();
            }
            ++this.count;
            return this.in.readClassDef();
        }
        
        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
    
    private final class ClassDefIterable implements Iterable<ClassDef>
    {
        @Override
        public Iterator<ClassDef> iterator() {
            return Dex.this.tableOfContents.classDefs.exists() ? new ClassDefIterator() : Collections.<ClassDef>emptySet().iterator();
        }
    }
}

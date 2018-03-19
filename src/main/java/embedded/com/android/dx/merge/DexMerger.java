package embedded.com.android.dx.merge;

import embedded.com.android.dx.command.dexer.*;
import embedded.com.android.dex.*;
import java.io.*;
import java.util.*;

public final class DexMerger
{
    private final Dex[] dexes;
    private final IndexMap[] indexMaps;
    private final CollisionPolicy collisionPolicy;
    private final DxContext context;
    private final WriterSizes writerSizes;
    private final Dex dexOut;
    private final Dex.Section headerOut;
    private final Dex.Section idsDefsOut;
    private final Dex.Section mapListOut;
    private final Dex.Section typeListOut;
    private final Dex.Section classDataOut;
    private final Dex.Section codeOut;
    private final Dex.Section stringDataOut;
    private final Dex.Section debugInfoOut;
    private final Dex.Section encodedArrayOut;
    private final Dex.Section annotationsDirectoryOut;
    private final Dex.Section annotationSetOut;
    private final Dex.Section annotationSetRefListOut;
    private final Dex.Section annotationOut;
    private final TableOfContents contentsOut;
    private final InstructionTransformer instructionTransformer;
    private int compactWasteThreshold;
    private static final byte DBG_END_SEQUENCE = 0;
    private static final byte DBG_ADVANCE_PC = 1;
    private static final byte DBG_ADVANCE_LINE = 2;
    private static final byte DBG_START_LOCAL = 3;
    private static final byte DBG_START_LOCAL_EXTENDED = 4;
    private static final byte DBG_END_LOCAL = 5;
    private static final byte DBG_RESTART_LOCAL = 6;
    private static final byte DBG_SET_PROLOGUE_END = 7;
    private static final byte DBG_SET_EPILOGUE_BEGIN = 8;
    private static final byte DBG_SET_FILE = 9;
    
    public DexMerger(final Dex[] dexes, final CollisionPolicy collisionPolicy, final DxContext context) throws IOException {
        this(dexes, collisionPolicy, context, new WriterSizes(dexes));
    }
    
    private DexMerger(final Dex[] dexes, final CollisionPolicy collisionPolicy, final DxContext context, final WriterSizes writerSizes) throws IOException {
        this.compactWasteThreshold = 1048576;
        this.dexes = dexes;
        this.collisionPolicy = collisionPolicy;
        this.context = context;
        this.writerSizes = writerSizes;
        this.dexOut = new Dex(writerSizes.size());
        this.indexMaps = new IndexMap[dexes.length];
        for (int i = 0; i < dexes.length; ++i) {
            this.indexMaps[i] = new IndexMap(this.dexOut, dexes[i].getTableOfContents());
        }
        this.instructionTransformer = new InstructionTransformer();
        this.headerOut = this.dexOut.appendSection(writerSizes.header, "header");
        this.idsDefsOut = this.dexOut.appendSection(writerSizes.idsDefs, "ids defs");
        this.contentsOut = this.dexOut.getTableOfContents();
        this.contentsOut.dataOff = this.dexOut.getNextSectionStart();
        this.contentsOut.mapList.off = this.dexOut.getNextSectionStart();
        this.contentsOut.mapList.size = 1;
        this.mapListOut = this.dexOut.appendSection(writerSizes.mapList, "map list");
        this.contentsOut.typeLists.off = this.dexOut.getNextSectionStart();
        this.typeListOut = this.dexOut.appendSection(writerSizes.typeList, "type list");
        this.contentsOut.annotationSetRefLists.off = this.dexOut.getNextSectionStart();
        this.annotationSetRefListOut = this.dexOut.appendSection(writerSizes.annotationsSetRefList, "annotation set ref list");
        this.contentsOut.annotationSets.off = this.dexOut.getNextSectionStart();
        this.annotationSetOut = this.dexOut.appendSection(writerSizes.annotationsSet, "annotation sets");
        this.contentsOut.classDatas.off = this.dexOut.getNextSectionStart();
        this.classDataOut = this.dexOut.appendSection(writerSizes.classData, "class data");
        this.contentsOut.codes.off = this.dexOut.getNextSectionStart();
        this.codeOut = this.dexOut.appendSection(writerSizes.code, "code");
        this.contentsOut.stringDatas.off = this.dexOut.getNextSectionStart();
        this.stringDataOut = this.dexOut.appendSection(writerSizes.stringData, "string data");
        this.contentsOut.debugInfos.off = this.dexOut.getNextSectionStart();
        this.debugInfoOut = this.dexOut.appendSection(writerSizes.debugInfo, "debug info");
        this.contentsOut.annotations.off = this.dexOut.getNextSectionStart();
        this.annotationOut = this.dexOut.appendSection(writerSizes.annotation, "annotation");
        this.contentsOut.encodedArrays.off = this.dexOut.getNextSectionStart();
        this.encodedArrayOut = this.dexOut.appendSection(writerSizes.encodedArray, "encoded array");
        this.contentsOut.annotationsDirectories.off = this.dexOut.getNextSectionStart();
        this.annotationsDirectoryOut = this.dexOut.appendSection(writerSizes.annotationsDirectory, "annotations directory");
        this.contentsOut.dataSize = this.dexOut.getNextSectionStart() - this.contentsOut.dataOff;
    }
    
    public void setCompactWasteThreshold(final int compactWasteThreshold) {
        this.compactWasteThreshold = compactWasteThreshold;
    }
    
    private Dex mergeDexes() throws IOException {
        this.mergeStringIds();
        this.mergeTypeIds();
        this.mergeTypeLists();
        this.mergeProtoIds();
        this.mergeFieldIds();
        this.mergeMethodIds();
        this.mergeMethodHandles();
        this.mergeAnnotations();
        this.unionAnnotationSetsAndDirectories();
        this.mergeCallSiteIds();
        this.mergeClassDefs();
        Arrays.sort(this.contentsOut.sections);
        this.contentsOut.header.off = 0;
        this.contentsOut.header.size = 1;
        this.contentsOut.fileSize = this.dexOut.getLength();
        this.contentsOut.computeSizesFromOffsets();
        this.contentsOut.writeHeader(this.headerOut, this.mergeApiLevels());
        this.contentsOut.writeMap(this.mapListOut);
        this.dexOut.writeHashes();
        return this.dexOut;
    }
    
    public Dex merge() throws IOException {
        if (this.dexes.length == 1) {
            return this.dexes[0];
        }
        if (this.dexes.length == 0) {
            return null;
        }
        final long start = System.nanoTime();
        Dex result = this.mergeDexes();
        final WriterSizes compactedSizes = new WriterSizes(this);
        final int wastedByteCount = this.writerSizes.size() - compactedSizes.size();
        if (wastedByteCount > this.compactWasteThreshold) {
            final DexMerger compacter = new DexMerger(new Dex[] { this.dexOut, new Dex(0) }, CollisionPolicy.FAIL, this.context, compactedSizes);
            result = compacter.mergeDexes();
            this.context.out.printf("Result compacted from %.1fKiB to %.1fKiB to save %.1fKiB%n", this.dexOut.getLength() / 1024.0f, result.getLength() / 1024.0f, wastedByteCount / 1024.0f);
        }
        final long elapsed = System.nanoTime() - start;
        for (int i = 0; i < this.dexes.length; ++i) {
            this.context.out.printf("Merged dex #%d (%d defs/%.1fKiB)%n", i + 1, this.dexes[i].getTableOfContents().classDefs.size, this.dexes[i].getLength() / 1024.0f);
        }
        this.context.out.printf("Result is %d defs/%.1fKiB. Took %.1fs%n", result.getTableOfContents().classDefs.size, result.getLength() / 1024.0f, elapsed / 1.0E9f);
        return result;
    }
    
    private int mergeApiLevels() {
        int maxApi = -1;
        for (int i = 0; i < this.dexes.length; ++i) {
            final int dexMinApi = this.dexes[i].getTableOfContents().apiLevel;
            if (maxApi < dexMinApi) {
                maxApi = dexMinApi;
            }
        }
        return maxApi;
    }
    
    private void mergeStringIds() {
        new IdMerger<String>(this.idsDefsOut) {
            @Override
            TableOfContents.Section getSection(final TableOfContents tableOfContents) {
                return tableOfContents.stringIds;
            }
            
            @Override
            String read(final Dex.Section in, final IndexMap indexMap, final int index) {
                return in.readString();
            }
            
            @Override
            void updateIndex(final int offset, final IndexMap indexMap, final int oldIndex, final int newIndex) {
                indexMap.stringIds[oldIndex] = newIndex;
            }
            
            @Override
            void write(final String value) {
                final TableOfContents.Section stringDatas = DexMerger.this.contentsOut.stringDatas;
                ++stringDatas.size;
                DexMerger.this.idsDefsOut.writeInt(DexMerger.this.stringDataOut.getPosition());
                DexMerger.this.stringDataOut.writeStringData(value);
            }
        }.mergeSorted();
    }
    
    private void mergeTypeIds() {
        new IdMerger<Integer>(this.idsDefsOut) {
            @Override
            TableOfContents.Section getSection(final TableOfContents tableOfContents) {
                return tableOfContents.typeIds;
            }
            
            @Override
            Integer read(final Dex.Section in, final IndexMap indexMap, final int index) {
                final int stringIndex = in.readInt();
                return indexMap.adjustString(stringIndex);
            }
            
            @Override
            void updateIndex(final int offset, final IndexMap indexMap, final int oldIndex, final int newIndex) {
                if (newIndex < 0 || newIndex > 65535) {
                    throw new DexIndexOverflowException("type ID not in [0, 0xffff]: " + newIndex);
                }
                indexMap.typeIds[oldIndex] = (short)newIndex;
            }
            
            @Override
            void write(final Integer value) {
                DexMerger.this.idsDefsOut.writeInt(value);
            }
        }.mergeSorted();
    }
    
    private void mergeTypeLists() {
        new IdMerger<TypeList>(this.typeListOut) {
            @Override
            TableOfContents.Section getSection(final TableOfContents tableOfContents) {
                return tableOfContents.typeLists;
            }
            
            @Override
            TypeList read(final Dex.Section in, final IndexMap indexMap, final int index) {
                return indexMap.adjustTypeList(in.readTypeList());
            }
            
            @Override
            void updateIndex(final int offset, final IndexMap indexMap, final int oldIndex, final int newIndex) {
                indexMap.putTypeListOffset(offset, DexMerger.this.typeListOut.getPosition());
            }
            
            @Override
            void write(final TypeList value) {
                DexMerger.this.typeListOut.writeTypeList(value);
            }
        }.mergeUnsorted();
    }
    
    private void mergeProtoIds() {
        new IdMerger<ProtoId>(this.idsDefsOut) {
            @Override
            TableOfContents.Section getSection(final TableOfContents tableOfContents) {
                return tableOfContents.protoIds;
            }
            
            @Override
            ProtoId read(final Dex.Section in, final IndexMap indexMap, final int index) {
                return indexMap.adjust(in.readProtoId());
            }
            
            @Override
            void updateIndex(final int offset, final IndexMap indexMap, final int oldIndex, final int newIndex) {
                if (newIndex < 0 || newIndex > 65535) {
                    throw new DexIndexOverflowException("proto ID not in [0, 0xffff]: " + newIndex);
                }
                indexMap.protoIds[oldIndex] = (short)newIndex;
            }
            
            @Override
            void write(final ProtoId value) {
                value.writeTo(DexMerger.this.idsDefsOut);
            }
        }.mergeSorted();
    }
    
    private void mergeCallSiteIds() {
        new IdMerger<CallSiteId>(this.idsDefsOut) {
            @Override
            TableOfContents.Section getSection(final TableOfContents tableOfContents) {
                return tableOfContents.callSiteIds;
            }
            
            @Override
            CallSiteId read(final Dex.Section in, final IndexMap indexMap, final int index) {
                return indexMap.adjust(in.readCallSiteId());
            }
            
            @Override
            void updateIndex(final int offset, final IndexMap indexMap, final int oldIndex, final int newIndex) {
                indexMap.callSiteIds[oldIndex] = newIndex;
            }
            
            @Override
            void write(final CallSiteId value) {
                value.writeTo(DexMerger.this.idsDefsOut);
            }
        }.mergeSorted();
    }
    
    private void mergeMethodHandles() {
        new IdMerger<MethodHandle>(this.idsDefsOut) {
            @Override
            TableOfContents.Section getSection(final TableOfContents tableOfContents) {
                return tableOfContents.methodHandles;
            }
            
            @Override
            MethodHandle read(final Dex.Section in, final IndexMap indexMap, final int index) {
                return indexMap.adjust(in.readMethodHandle());
            }
            
            @Override
            void updateIndex(final int offset, final IndexMap indexMap, final int oldIndex, final int newIndex) {
                indexMap.methodHandleIds.put(oldIndex, indexMap.methodHandleIds.size());
            }
            
            @Override
            void write(final MethodHandle value) {
                value.writeTo(DexMerger.this.idsDefsOut);
            }
        }.mergeUnsorted();
    }
    
    private void mergeFieldIds() {
        new IdMerger<FieldId>(this.idsDefsOut) {
            @Override
            TableOfContents.Section getSection(final TableOfContents tableOfContents) {
                return tableOfContents.fieldIds;
            }
            
            @Override
            FieldId read(final Dex.Section in, final IndexMap indexMap, final int index) {
                return indexMap.adjust(in.readFieldId());
            }
            
            @Override
            void updateIndex(final int offset, final IndexMap indexMap, final int oldIndex, final int newIndex) {
                if (newIndex < 0 || newIndex > 65535) {
                    throw new DexIndexOverflowException("field ID not in [0, 0xffff]: " + newIndex);
                }
                indexMap.fieldIds[oldIndex] = (short)newIndex;
            }
            
            @Override
            void write(final FieldId value) {
                value.writeTo(DexMerger.this.idsDefsOut);
            }
        }.mergeSorted();
    }
    
    private void mergeMethodIds() {
        new IdMerger<MethodId>(this.idsDefsOut) {
            @Override
            TableOfContents.Section getSection(final TableOfContents tableOfContents) {
                return tableOfContents.methodIds;
            }
            
            @Override
            MethodId read(final Dex.Section in, final IndexMap indexMap, final int index) {
                return indexMap.adjust(in.readMethodId());
            }
            
            @Override
            void updateIndex(final int offset, final IndexMap indexMap, final int oldIndex, final int newIndex) {
                if (newIndex < 0 || newIndex > 65535) {
                    throw new DexIndexOverflowException("method ID not in [0, 0xffff]: " + newIndex);
                }
                indexMap.methodIds[oldIndex] = (short)newIndex;
            }
            
            @Override
            void write(final MethodId methodId) {
                methodId.writeTo(DexMerger.this.idsDefsOut);
            }
        }.mergeSorted();
    }
    
    private void mergeAnnotations() {
        new IdMerger<Annotation>(this.annotationOut) {
            @Override
            TableOfContents.Section getSection(final TableOfContents tableOfContents) {
                return tableOfContents.annotations;
            }
            
            @Override
            Annotation read(final Dex.Section in, final IndexMap indexMap, final int index) {
                return indexMap.adjust(in.readAnnotation());
            }
            
            @Override
            void updateIndex(final int offset, final IndexMap indexMap, final int oldIndex, final int newIndex) {
                indexMap.putAnnotationOffset(offset, DexMerger.this.annotationOut.getPosition());
            }
            
            @Override
            void write(final Annotation value) {
                value.writeTo(DexMerger.this.annotationOut);
            }
        }.mergeUnsorted();
    }
    
    private void mergeClassDefs() {
        final SortableType[] types = this.getSortedTypes();
        this.contentsOut.classDefs.off = this.idsDefsOut.getPosition();
        this.contentsOut.classDefs.size = types.length;
        for (final SortableType type : types) {
            final Dex in = type.getDex();
            this.transformClassDef(in, type.getClassDef(), type.getIndexMap());
        }
    }
    
    private SortableType[] getSortedTypes() {
        final SortableType[] sortableTypes = new SortableType[this.contentsOut.typeIds.size];
        for (int i = 0; i < this.dexes.length; ++i) {
            this.readSortableTypes(sortableTypes, this.dexes[i], this.indexMaps[i]);
        }
        boolean allDone;
        do {
            allDone = true;
            for (final SortableType sortableType : sortableTypes) {
                if (sortableType != null && !sortableType.isDepthAssigned()) {
                    allDone &= sortableType.tryAssignDepth(sortableTypes);
                }
            }
        } while (!allDone);
        Arrays.sort(sortableTypes, SortableType.NULLS_LAST_ORDER);
        final int firstNull = Arrays.asList(sortableTypes).indexOf(null);
        return (firstNull != -1) ? Arrays.copyOfRange(sortableTypes, 0, firstNull) : sortableTypes;
    }
    
    private void readSortableTypes(final SortableType[] sortableTypes, final Dex buffer, final IndexMap indexMap) {
        for (final ClassDef classDef : buffer.classDefs()) {
            final SortableType sortableType = indexMap.adjust(new SortableType(buffer, indexMap, classDef));
            final int t = sortableType.getTypeIndex();
            if (sortableTypes[t] == null) {
                sortableTypes[t] = sortableType;
            }
            else {
                if (this.collisionPolicy != CollisionPolicy.KEEP_FIRST) {
                    throw new DexException("Multiple dex files define " + buffer.typeNames().get(classDef.getTypeIndex()));
                }
                continue;
            }
        }
    }
    
    private void unionAnnotationSetsAndDirectories() {
        for (int i = 0; i < this.dexes.length; ++i) {
            this.transformAnnotationSets(this.dexes[i], this.indexMaps[i]);
        }
        for (int i = 0; i < this.dexes.length; ++i) {
            this.transformAnnotationSetRefLists(this.dexes[i], this.indexMaps[i]);
        }
        for (int i = 0; i < this.dexes.length; ++i) {
            this.transformAnnotationDirectories(this.dexes[i], this.indexMaps[i]);
        }
        for (int i = 0; i < this.dexes.length; ++i) {
            this.transformStaticValues(this.dexes[i], this.indexMaps[i]);
        }
    }
    
    private void transformAnnotationSets(final Dex in, final IndexMap indexMap) {
        final TableOfContents.Section section = in.getTableOfContents().annotationSets;
        if (section.exists()) {
            final Dex.Section setIn = in.open(section.off);
            for (int i = 0; i < section.size; ++i) {
                this.transformAnnotationSet(indexMap, setIn);
            }
        }
    }
    
    private void transformAnnotationSetRefLists(final Dex in, final IndexMap indexMap) {
        final TableOfContents.Section section = in.getTableOfContents().annotationSetRefLists;
        if (section.exists()) {
            final Dex.Section setIn = in.open(section.off);
            for (int i = 0; i < section.size; ++i) {
                this.transformAnnotationSetRefList(indexMap, setIn);
            }
        }
    }
    
    private void transformAnnotationDirectories(final Dex in, final IndexMap indexMap) {
        final TableOfContents.Section section = in.getTableOfContents().annotationsDirectories;
        if (section.exists()) {
            final Dex.Section directoryIn = in.open(section.off);
            for (int i = 0; i < section.size; ++i) {
                this.transformAnnotationDirectory(directoryIn, indexMap);
            }
        }
    }
    
    private void transformStaticValues(final Dex in, final IndexMap indexMap) {
        final TableOfContents.Section section = in.getTableOfContents().encodedArrays;
        if (section.exists()) {
            final Dex.Section staticValuesIn = in.open(section.off);
            for (int i = 0; i < section.size; ++i) {
                this.transformStaticValues(staticValuesIn, indexMap);
            }
        }
    }
    
    private void transformClassDef(final Dex in, final ClassDef classDef, final IndexMap indexMap) {
        this.idsDefsOut.assertFourByteAligned();
        this.idsDefsOut.writeInt(classDef.getTypeIndex());
        this.idsDefsOut.writeInt(classDef.getAccessFlags());
        this.idsDefsOut.writeInt(classDef.getSupertypeIndex());
        this.idsDefsOut.writeInt(classDef.getInterfacesOffset());
        final int sourceFileIndex = indexMap.adjustString(classDef.getSourceFileIndex());
        this.idsDefsOut.writeInt(sourceFileIndex);
        final int annotationsOff = classDef.getAnnotationsOffset();
        this.idsDefsOut.writeInt(indexMap.adjustAnnotationDirectory(annotationsOff));
        final int classDataOff = classDef.getClassDataOffset();
        if (classDataOff == 0) {
            this.idsDefsOut.writeInt(0);
        }
        else {
            this.idsDefsOut.writeInt(this.classDataOut.getPosition());
            final ClassData classData = in.readClassData(classDef);
            this.transformClassData(in, classData, indexMap);
        }
        final int staticValuesOff = classDef.getStaticValuesOffset();
        this.idsDefsOut.writeInt(indexMap.adjustEncodedArray(staticValuesOff));
    }
    
    private void transformAnnotationDirectory(final Dex.Section directoryIn, final IndexMap indexMap) {
        final TableOfContents.Section annotationsDirectories = this.contentsOut.annotationsDirectories;
        ++annotationsDirectories.size;
        this.annotationsDirectoryOut.assertFourByteAligned();
        indexMap.putAnnotationDirectoryOffset(directoryIn.getPosition(), this.annotationsDirectoryOut.getPosition());
        final int classAnnotationsOffset = indexMap.adjustAnnotationSet(directoryIn.readInt());
        this.annotationsDirectoryOut.writeInt(classAnnotationsOffset);
        final int fieldsSize = directoryIn.readInt();
        this.annotationsDirectoryOut.writeInt(fieldsSize);
        final int methodsSize = directoryIn.readInt();
        this.annotationsDirectoryOut.writeInt(methodsSize);
        final int parameterListSize = directoryIn.readInt();
        this.annotationsDirectoryOut.writeInt(parameterListSize);
        for (int i = 0; i < fieldsSize; ++i) {
            this.annotationsDirectoryOut.writeInt(indexMap.adjustField(directoryIn.readInt()));
            this.annotationsDirectoryOut.writeInt(indexMap.adjustAnnotationSet(directoryIn.readInt()));
        }
        for (int i = 0; i < methodsSize; ++i) {
            this.annotationsDirectoryOut.writeInt(indexMap.adjustMethod(directoryIn.readInt()));
            this.annotationsDirectoryOut.writeInt(indexMap.adjustAnnotationSet(directoryIn.readInt()));
        }
        for (int i = 0; i < parameterListSize; ++i) {
            this.annotationsDirectoryOut.writeInt(indexMap.adjustMethod(directoryIn.readInt()));
            this.annotationsDirectoryOut.writeInt(indexMap.adjustAnnotationSetRefList(directoryIn.readInt()));
        }
    }
    
    private void transformAnnotationSet(final IndexMap indexMap, final Dex.Section setIn) {
        final TableOfContents.Section annotationSets = this.contentsOut.annotationSets;
        ++annotationSets.size;
        this.annotationSetOut.assertFourByteAligned();
        indexMap.putAnnotationSetOffset(setIn.getPosition(), this.annotationSetOut.getPosition());
        final int size = setIn.readInt();
        this.annotationSetOut.writeInt(size);
        for (int j = 0; j < size; ++j) {
            this.annotationSetOut.writeInt(indexMap.adjustAnnotation(setIn.readInt()));
        }
    }
    
    private void transformAnnotationSetRefList(final IndexMap indexMap, final Dex.Section refListIn) {
        final TableOfContents.Section annotationSetRefLists = this.contentsOut.annotationSetRefLists;
        ++annotationSetRefLists.size;
        this.annotationSetRefListOut.assertFourByteAligned();
        indexMap.putAnnotationSetRefListOffset(refListIn.getPosition(), this.annotationSetRefListOut.getPosition());
        final int parameterCount = refListIn.readInt();
        this.annotationSetRefListOut.writeInt(parameterCount);
        for (int p = 0; p < parameterCount; ++p) {
            this.annotationSetRefListOut.writeInt(indexMap.adjustAnnotationSet(refListIn.readInt()));
        }
    }
    
    private void transformClassData(final Dex in, final ClassData classData, final IndexMap indexMap) {
        final TableOfContents.Section classDatas = this.contentsOut.classDatas;
        ++classDatas.size;
        final ClassData.Field[] staticFields = classData.getStaticFields();
        final ClassData.Field[] instanceFields = classData.getInstanceFields();
        final ClassData.Method[] directMethods = classData.getDirectMethods();
        final ClassData.Method[] virtualMethods = classData.getVirtualMethods();
        this.classDataOut.writeUleb128(staticFields.length);
        this.classDataOut.writeUleb128(instanceFields.length);
        this.classDataOut.writeUleb128(directMethods.length);
        this.classDataOut.writeUleb128(virtualMethods.length);
        this.transformFields(indexMap, staticFields);
        this.transformFields(indexMap, instanceFields);
        this.transformMethods(in, indexMap, directMethods);
        this.transformMethods(in, indexMap, virtualMethods);
    }
    
    private void transformFields(final IndexMap indexMap, final ClassData.Field[] fields) {
        int lastOutFieldIndex = 0;
        for (final ClassData.Field field : fields) {
            final int outFieldIndex = indexMap.adjustField(field.getFieldIndex());
            this.classDataOut.writeUleb128(outFieldIndex - lastOutFieldIndex);
            lastOutFieldIndex = outFieldIndex;
            this.classDataOut.writeUleb128(field.getAccessFlags());
        }
    }
    
    private void transformMethods(final Dex in, final IndexMap indexMap, final ClassData.Method[] methods) {
        int lastOutMethodIndex = 0;
        for (final ClassData.Method method : methods) {
            final int outMethodIndex = indexMap.adjustMethod(method.getMethodIndex());
            this.classDataOut.writeUleb128(outMethodIndex - lastOutMethodIndex);
            lastOutMethodIndex = outMethodIndex;
            this.classDataOut.writeUleb128(method.getAccessFlags());
            if (method.getCodeOffset() == 0) {
                this.classDataOut.writeUleb128(0);
            }
            else {
                this.codeOut.alignToFourBytesWithZeroFill();
                this.classDataOut.writeUleb128(this.codeOut.getPosition());
                this.transformCode(in, in.readCode(method), indexMap);
            }
        }
    }
    
    private void transformCode(final Dex in, final Code code, final IndexMap indexMap) {
        final TableOfContents.Section codes = this.contentsOut.codes;
        ++codes.size;
        this.codeOut.assertFourByteAligned();
        this.codeOut.writeUnsignedShort(code.getRegistersSize());
        this.codeOut.writeUnsignedShort(code.getInsSize());
        this.codeOut.writeUnsignedShort(code.getOutsSize());
        final Code.Try[] tries = code.getTries();
        final Code.CatchHandler[] catchHandlers = code.getCatchHandlers();
        this.codeOut.writeUnsignedShort(tries.length);
        final int debugInfoOffset = code.getDebugInfoOffset();
        if (debugInfoOffset != 0) {
            this.codeOut.writeInt(this.debugInfoOut.getPosition());
            this.transformDebugInfoItem(in.open(debugInfoOffset), indexMap);
        }
        else {
            this.codeOut.writeInt(0);
        }
        final short[] instructions = code.getInstructions();
        final short[] newInstructions = this.instructionTransformer.transform(indexMap, instructions);
        this.codeOut.writeInt(newInstructions.length);
        this.codeOut.write(newInstructions);
        if (tries.length > 0) {
            if (newInstructions.length % 2 == 1) {
                this.codeOut.writeShort((short)0);
            }
            final Dex.Section triesSection = this.dexOut.open(this.codeOut.getPosition());
            this.codeOut.skip(tries.length * 8);
            final int[] offsets = this.transformCatchHandlers(indexMap, catchHandlers);
            this.transformTries(triesSection, tries, offsets);
        }
    }
    
    private int[] transformCatchHandlers(final IndexMap indexMap, final Code.CatchHandler[] catchHandlers) {
        final int baseOffset = this.codeOut.getPosition();
        this.codeOut.writeUleb128(catchHandlers.length);
        final int[] offsets = new int[catchHandlers.length];
        for (int i = 0; i < catchHandlers.length; ++i) {
            offsets[i] = this.codeOut.getPosition() - baseOffset;
            this.transformEncodedCatchHandler(catchHandlers[i], indexMap);
        }
        return offsets;
    }
    
    private void transformTries(final Dex.Section out, final Code.Try[] tries, final int[] catchHandlerOffsets) {
        for (final Code.Try tryItem : tries) {
            out.writeInt(tryItem.getStartAddress());
            out.writeUnsignedShort(tryItem.getInstructionCount());
            out.writeUnsignedShort(catchHandlerOffsets[tryItem.getCatchHandlerIndex()]);
        }
    }
    
    private void transformDebugInfoItem(final Dex.Section in, final IndexMap indexMap) {
        final TableOfContents.Section debugInfos = this.contentsOut.debugInfos;
        ++debugInfos.size;
        final int lineStart = in.readUleb128();
        this.debugInfoOut.writeUleb128(lineStart);
        final int parametersSize = in.readUleb128();
        this.debugInfoOut.writeUleb128(parametersSize);
        for (int p = 0; p < parametersSize; ++p) {
            final int parameterName = in.readUleb128p1();
            this.debugInfoOut.writeUleb128p1(indexMap.adjustString(parameterName));
        }
    Label_0152:
        while (true) {
            final int opcode = in.readByte();
            this.debugInfoOut.writeByte(opcode);
            switch (opcode) {
                case 0: {
                    break Label_0152;
                }
                case 1: {
                    final int addrDiff = in.readUleb128();
                    this.debugInfoOut.writeUleb128(addrDiff);
                    continue;
                }
                case 2: {
                    final int lineDiff = in.readSleb128();
                    this.debugInfoOut.writeSleb128(lineDiff);
                    continue;
                }
                case 3:
                case 4: {
                    final int registerNum = in.readUleb128();
                    this.debugInfoOut.writeUleb128(registerNum);
                    final int nameIndex = in.readUleb128p1();
                    this.debugInfoOut.writeUleb128p1(indexMap.adjustString(nameIndex));
                    final int typeIndex = in.readUleb128p1();
                    this.debugInfoOut.writeUleb128p1(indexMap.adjustType(typeIndex));
                    if (opcode == 4) {
                        final int sigIndex = in.readUleb128p1();
                        this.debugInfoOut.writeUleb128p1(indexMap.adjustString(sigIndex));
                        continue;
                    }
                    continue;
                }
                case 5:
                case 6: {
                    final int registerNum = in.readUleb128();
                    this.debugInfoOut.writeUleb128(registerNum);
                    continue;
                }
                case 9: {
                    final int nameIndex = in.readUleb128p1();
                    this.debugInfoOut.writeUleb128p1(indexMap.adjustString(nameIndex));
                    continue;
                }
            }
        }
    }
    
    private void transformEncodedCatchHandler(final Code.CatchHandler catchHandler, final IndexMap indexMap) {
        final int catchAllAddress = catchHandler.getCatchAllAddress();
        final int[] typeIndexes = catchHandler.getTypeIndexes();
        final int[] addresses = catchHandler.getAddresses();
        if (catchAllAddress != -1) {
            this.codeOut.writeSleb128(-typeIndexes.length);
        }
        else {
            this.codeOut.writeSleb128(typeIndexes.length);
        }
        for (int i = 0; i < typeIndexes.length; ++i) {
            this.codeOut.writeUleb128(indexMap.adjustType(typeIndexes[i]));
            this.codeOut.writeUleb128(addresses[i]);
        }
        if (catchAllAddress != -1) {
            this.codeOut.writeUleb128(catchAllAddress);
        }
    }
    
    private void transformStaticValues(final Dex.Section in, final IndexMap indexMap) {
        final TableOfContents.Section encodedArrays = this.contentsOut.encodedArrays;
        ++encodedArrays.size;
        indexMap.putEncodedArrayValueOffset(in.getPosition(), this.encodedArrayOut.getPosition());
        indexMap.adjustEncodedArray(in.readEncodedArray()).writeTo(this.encodedArrayOut);
    }
    
    public static void main(final String[] args) throws IOException {
        if (args.length < 2) {
            printUsage();
            return;
        }
        final Dex[] dexes = new Dex[args.length - 1];
        for (int i = 1; i < args.length; ++i) {
            dexes[i - 1] = new Dex(new File(args[i]));
        }
        final Dex merged = new DexMerger(dexes, CollisionPolicy.KEEP_FIRST, new DxContext()).merge();
        merged.writeTo(new File(args[0]));
    }
    
    private static void printUsage() {
        System.out.println("Usage: DexMerger <out.dex> <a.dex> <b.dex> ...");
        System.out.println();
        System.out.println("If a class is defined in several dex, the class found in the first dex will be used.");
    }
    
    abstract class IdMerger<T extends Comparable<T>>
    {
        private final Dex.Section out;
        
        protected IdMerger(final Dex.Section out) {
            this.out = out;
        }
        
        public final void mergeSorted() {
            final TableOfContents.Section[] sections = new TableOfContents.Section[DexMerger.this.dexes.length];
            final Dex.Section[] dexSections = new Dex.Section[DexMerger.this.dexes.length];
            final int[] offsets = new int[DexMerger.this.dexes.length];
            final int[] indexes = new int[DexMerger.this.dexes.length];
            final TreeMap<T, List<Integer>> values = new TreeMap<T, List<Integer>>();
            for (int i = 0; i < DexMerger.this.dexes.length; ++i) {
                sections[i] = this.getSection(DexMerger.this.dexes[i].getTableOfContents());
                dexSections[i] = (sections[i].exists() ? DexMerger.this.dexes[i].open(sections[i].off) : null);
                offsets[i] = this.readIntoMap(dexSections[i], sections[i], DexMerger.this.indexMaps[i], indexes[i], values, i);
            }
            if (values.isEmpty()) {
                this.getSection(DexMerger.this.contentsOut).off = 0;
                this.getSection(DexMerger.this.contentsOut).size = 0;
                return;
            }
            this.getSection(DexMerger.this.contentsOut).off = this.out.getPosition();
            int outCount = 0;
            while (!values.isEmpty()) {
                final Map.Entry<T, List<Integer>> first = values.pollFirstEntry();
                for (final Integer dex : first.getValue()) {
                    this.updateIndex(offsets[dex], DexMerger.this.indexMaps[dex], indexes[dex]++, outCount);
                    offsets[dex] = this.readIntoMap(dexSections[dex], sections[dex], DexMerger.this.indexMaps[dex], indexes[dex], values, dex);
                }
                this.write(first.getKey());
                ++outCount;
            }
            this.getSection(DexMerger.this.contentsOut).size = outCount;
        }
        
        private int readIntoMap(final Dex.Section in, final TableOfContents.Section section, final IndexMap indexMap, final int index, final TreeMap<T, List<Integer>> values, final int dex) {
            final int offset = (in != null) ? in.getPosition() : -1;
            if (index < section.size) {
                final T v = this.read(in, indexMap, index);
                List<Integer> l = values.get(v);
                if (l == null) {
                    l = new ArrayList<Integer>();
                    values.put(v, l);
                }
                l.add(dex);
            }
            return offset;
        }
        
        public final void mergeUnsorted() {
            this.getSection(DexMerger.this.contentsOut).off = this.out.getPosition();
            final List<UnsortedValue> all = new ArrayList<UnsortedValue>();
            for (int i = 0; i < DexMerger.this.dexes.length; ++i) {
                all.addAll(this.readUnsortedValues(DexMerger.this.dexes[i], DexMerger.this.indexMaps[i]));
            }
            if (all.isEmpty()) {
                this.getSection(DexMerger.this.contentsOut).off = 0;
                this.getSection(DexMerger.this.contentsOut).size = 0;
                return;
            }
            Collections.sort(all);
            int outCount = 0;
            int j = 0;
            while (j < all.size()) {
                final UnsortedValue e1 = all.get(j++);
                this.updateIndex(e1.offset, e1.indexMap, e1.index, outCount - 1);
                while (j < all.size() && e1.compareTo((UnsortedValue)all.get(j)) == 0) {
                    final UnsortedValue e2 = all.get(j++);
                    this.updateIndex(e2.offset, e2.indexMap, e2.index, outCount - 1);
                }
                this.write(e1.value);
                ++outCount;
            }
            this.getSection(DexMerger.this.contentsOut).size = outCount;
        }
        
        private List<UnsortedValue> readUnsortedValues(final Dex source, final IndexMap indexMap) {
            final TableOfContents.Section section = this.getSection(source.getTableOfContents());
            if (!section.exists()) {
                return Collections.emptyList();
            }
            final List<UnsortedValue> result = new ArrayList<UnsortedValue>();
            final Dex.Section in = source.open(section.off);
            for (int i = 0; i < section.size; ++i) {
                final int offset = in.getPosition();
                final T value = this.read(in, indexMap, 0);
                result.add(new UnsortedValue(source, indexMap, value, i, offset));
            }
            return result;
        }
        
        abstract TableOfContents.Section getSection(final TableOfContents p0);
        
        abstract T read(final Dex.Section p0, final IndexMap p1, final int p2);
        
        abstract void updateIndex(final int p0, final IndexMap p1, final int p2, final int p3);
        
        abstract void write(final T p0);
        
        class UnsortedValue implements Comparable<UnsortedValue>
        {
            final Dex source;
            final IndexMap indexMap;
            final T value;
            final int index;
            final int offset;
            
            UnsortedValue(final Dex source, final IndexMap indexMap, final T value, final int index, final int offset) {
                this.source = source;
                this.indexMap = indexMap;
                this.value = value;
                this.index = index;
                this.offset = offset;
            }
            
            @Override
            public int compareTo(final UnsortedValue unsortedValue) {
                return this.value.compareTo(unsortedValue.value);
            }
        }
    }
    
    private static class WriterSizes
    {
        private int header;
        private int idsDefs;
        private int mapList;
        private int typeList;
        private int classData;
        private int code;
        private int stringData;
        private int debugInfo;
        private int encodedArray;
        private int annotationsDirectory;
        private int annotationsSet;
        private int annotationsSetRefList;
        private int annotation;
        
        public WriterSizes(final Dex[] dexes) {
            this.header = 112;
            for (int i = 0; i < dexes.length; ++i) {
                this.plus(dexes[i].getTableOfContents(), false);
            }
            this.fourByteAlign();
        }
        
        public WriterSizes(final DexMerger dexMerger) {
            this.header = 112;
            this.header = dexMerger.headerOut.used();
            this.idsDefs = dexMerger.idsDefsOut.used();
            this.mapList = dexMerger.mapListOut.used();
            this.typeList = dexMerger.typeListOut.used();
            this.classData = dexMerger.classDataOut.used();
            this.code = dexMerger.codeOut.used();
            this.stringData = dexMerger.stringDataOut.used();
            this.debugInfo = dexMerger.debugInfoOut.used();
            this.encodedArray = dexMerger.encodedArrayOut.used();
            this.annotationsDirectory = dexMerger.annotationsDirectoryOut.used();
            this.annotationsSet = dexMerger.annotationSetOut.used();
            this.annotationsSetRefList = dexMerger.annotationSetRefListOut.used();
            this.annotation = dexMerger.annotationOut.used();
            this.fourByteAlign();
        }
        
        private void plus(final TableOfContents contents, final boolean exact) {
            this.idsDefs += contents.stringIds.size * 4 + contents.typeIds.size * 4 + contents.protoIds.size * 12 + contents.fieldIds.size * 8 + contents.methodIds.size * 8 + contents.classDefs.size * 32;
            this.mapList = 4 + contents.sections.length * 12;
            this.typeList += fourByteAlign(contents.typeLists.byteCount);
            this.stringData += contents.stringDatas.byteCount;
            this.annotationsDirectory += contents.annotationsDirectories.byteCount;
            this.annotationsSet += contents.annotationSets.byteCount;
            this.annotationsSetRefList += contents.annotationSetRefLists.byteCount;
            if (exact) {
                this.code += contents.codes.byteCount;
                this.classData += contents.classDatas.byteCount;
                this.encodedArray += contents.encodedArrays.byteCount;
                this.annotation += contents.annotations.byteCount;
                this.debugInfo += contents.debugInfos.byteCount;
            }
            else {
                this.code += (int)Math.ceil(contents.codes.byteCount * 1.25);
                this.classData += (int)Math.ceil(contents.classDatas.byteCount * 1.67);
                this.encodedArray += contents.encodedArrays.byteCount * 2;
                this.annotation += (int)Math.ceil(contents.annotations.byteCount * 2);
                this.debugInfo += contents.debugInfos.byteCount * 2;
            }
        }
        
        private void fourByteAlign() {
            this.header = fourByteAlign(this.header);
            this.idsDefs = fourByteAlign(this.idsDefs);
            this.mapList = fourByteAlign(this.mapList);
            this.typeList = fourByteAlign(this.typeList);
            this.classData = fourByteAlign(this.classData);
            this.code = fourByteAlign(this.code);
            this.stringData = fourByteAlign(this.stringData);
            this.debugInfo = fourByteAlign(this.debugInfo);
            this.encodedArray = fourByteAlign(this.encodedArray);
            this.annotationsDirectory = fourByteAlign(this.annotationsDirectory);
            this.annotationsSet = fourByteAlign(this.annotationsSet);
            this.annotationsSetRefList = fourByteAlign(this.annotationsSetRefList);
            this.annotation = fourByteAlign(this.annotation);
        }
        
        private static int fourByteAlign(final int position) {
            return position + 3 & 0xFFFFFFFC;
        }
        
        public int size() {
            return this.header + this.idsDefs + this.mapList + this.typeList + this.classData + this.code + this.stringData + this.debugInfo + this.encodedArray + this.annotationsDirectory + this.annotationsSet + this.annotationsSetRefList + this.annotation;
        }
    }
}

package embedded.com.android.dx.merge;

import java.util.*;
import embedded.com.android.dx.util.*;
import embedded.com.android.dex.util.*;
import embedded.com.android.dex.*;

public final class IndexMap
{
    private final Dex target;
    public final int[] stringIds;
    public final short[] typeIds;
    public final short[] protoIds;
    public final short[] fieldIds;
    public final short[] methodIds;
    public final int[] callSiteIds;
    public final HashMap<Integer, Integer> methodHandleIds;
    private final HashMap<Integer, Integer> typeListOffsets;
    private final HashMap<Integer, Integer> annotationOffsets;
    private final HashMap<Integer, Integer> annotationSetOffsets;
    private final HashMap<Integer, Integer> annotationSetRefListOffsets;
    private final HashMap<Integer, Integer> annotationDirectoryOffsets;
    private final HashMap<Integer, Integer> encodedArrayValueOffset;
    
    public IndexMap(final Dex target, final TableOfContents tableOfContents) {
        this.target = target;
        this.stringIds = new int[tableOfContents.stringIds.size];
        this.typeIds = new short[tableOfContents.typeIds.size];
        this.protoIds = new short[tableOfContents.protoIds.size];
        this.fieldIds = new short[tableOfContents.fieldIds.size];
        this.methodIds = new short[tableOfContents.methodIds.size];
        this.callSiteIds = new int[tableOfContents.callSiteIds.size];
        this.methodHandleIds = new HashMap<Integer, Integer>();
        this.typeListOffsets = new HashMap<Integer, Integer>();
        this.annotationOffsets = new HashMap<Integer, Integer>();
        this.annotationSetOffsets = new HashMap<Integer, Integer>();
        this.annotationSetRefListOffsets = new HashMap<Integer, Integer>();
        this.annotationDirectoryOffsets = new HashMap<Integer, Integer>();
        this.encodedArrayValueOffset = new HashMap<Integer, Integer>();
        this.typeListOffsets.put(0, 0);
        this.annotationSetOffsets.put(0, 0);
        this.annotationDirectoryOffsets.put(0, 0);
        this.encodedArrayValueOffset.put(0, 0);
    }
    
    public void putTypeListOffset(final int oldOffset, final int newOffset) {
        if (oldOffset <= 0 || newOffset <= 0) {
            throw new IllegalArgumentException();
        }
        this.typeListOffsets.put(oldOffset, newOffset);
    }
    
    public void putAnnotationOffset(final int oldOffset, final int newOffset) {
        if (oldOffset <= 0 || newOffset <= 0) {
            throw new IllegalArgumentException();
        }
        this.annotationOffsets.put(oldOffset, newOffset);
    }
    
    public void putAnnotationSetOffset(final int oldOffset, final int newOffset) {
        if (oldOffset <= 0 || newOffset <= 0) {
            throw new IllegalArgumentException();
        }
        this.annotationSetOffsets.put(oldOffset, newOffset);
    }
    
    public void putAnnotationSetRefListOffset(final int oldOffset, final int newOffset) {
        if (oldOffset <= 0 || newOffset <= 0) {
            throw new IllegalArgumentException();
        }
        this.annotationSetRefListOffsets.put(oldOffset, newOffset);
    }
    
    public void putAnnotationDirectoryOffset(final int oldOffset, final int newOffset) {
        if (oldOffset <= 0 || newOffset <= 0) {
            throw new IllegalArgumentException();
        }
        this.annotationDirectoryOffsets.put(oldOffset, newOffset);
    }
    
    public void putEncodedArrayValueOffset(final int oldOffset, final int newOffset) {
        if (oldOffset <= 0 || newOffset <= 0) {
            throw new IllegalArgumentException();
        }
        this.encodedArrayValueOffset.put(oldOffset, newOffset);
    }
    
    public int adjustString(final int stringIndex) {
        return (stringIndex == -1) ? -1 : this.stringIds[stringIndex];
    }
    
    public int adjustType(final int typeIndex) {
        return (typeIndex == -1) ? -1 : (this.typeIds[typeIndex] & 0xFFFF);
    }
    
    public TypeList adjustTypeList(final TypeList typeList) {
        if (typeList == TypeList.EMPTY) {
            return typeList;
        }
        final short[] types = typeList.getTypes().clone();
        for (int i = 0; i < types.length; ++i) {
            types[i] = (short)this.adjustType(types[i]);
        }
        return new TypeList(this.target, types);
    }
    
    public int adjustProto(final int protoIndex) {
        return this.protoIds[protoIndex] & 0xFFFF;
    }
    
    public int adjustField(final int fieldIndex) {
        return this.fieldIds[fieldIndex] & 0xFFFF;
    }
    
    public int adjustMethod(final int methodIndex) {
        return this.methodIds[methodIndex] & 0xFFFF;
    }
    
    public int adjustTypeListOffset(final int typeListOffset) {
        return this.typeListOffsets.get(typeListOffset);
    }
    
    public int adjustAnnotation(final int annotationOffset) {
        return this.annotationOffsets.get(annotationOffset);
    }
    
    public int adjustAnnotationSet(final int annotationSetOffset) {
        return this.annotationSetOffsets.get(annotationSetOffset);
    }
    
    public int adjustAnnotationSetRefList(final int annotationSetRefListOffset) {
        return this.annotationSetRefListOffsets.get(annotationSetRefListOffset);
    }
    
    public int adjustAnnotationDirectory(final int annotationDirectoryOffset) {
        return this.annotationDirectoryOffsets.get(annotationDirectoryOffset);
    }
    
    public int adjustEncodedArray(final int encodedArrayAttribute) {
        return this.encodedArrayValueOffset.get(encodedArrayAttribute);
    }
    
    public int adjustCallSite(final int callSiteIndex) {
        return this.callSiteIds[callSiteIndex];
    }
    
    public int adjustMethodHandle(final int methodHandleIndex) {
        return this.methodHandleIds.get(methodHandleIndex);
    }
    
    public MethodId adjust(final MethodId methodId) {
        return new MethodId(this.target, this.adjustType(methodId.getDeclaringClassIndex()), this.adjustProto(methodId.getProtoIndex()), this.adjustString(methodId.getNameIndex()));
    }
    
    public CallSiteId adjust(final CallSiteId callSiteId) {
        return new CallSiteId(this.target, this.adjustEncodedArray(callSiteId.getCallSiteOffset()));
    }
    
    public MethodHandle adjust(final MethodHandle methodHandle) {
        return new MethodHandle(this.target, methodHandle.getMethodHandleType(), methodHandle.getUnused1(), methodHandle.getMethodHandleType().isField() ? this.adjustField(methodHandle.getFieldOrMethodId()) : this.adjustMethod(methodHandle.getFieldOrMethodId()), methodHandle.getUnused2());
    }
    
    public FieldId adjust(final FieldId fieldId) {
        return new FieldId(this.target, this.adjustType(fieldId.getDeclaringClassIndex()), this.adjustType(fieldId.getTypeIndex()), this.adjustString(fieldId.getNameIndex()));
    }
    
    public ProtoId adjust(final ProtoId protoId) {
        return new ProtoId(this.target, this.adjustString(protoId.getShortyIndex()), this.adjustType(protoId.getReturnTypeIndex()), this.adjustTypeListOffset(protoId.getParametersOffset()));
    }
    
    public ClassDef adjust(final ClassDef classDef) {
        return new ClassDef(this.target, classDef.getOffset(), this.adjustType(classDef.getTypeIndex()), classDef.getAccessFlags(), this.adjustType(classDef.getSupertypeIndex()), this.adjustTypeListOffset(classDef.getInterfacesOffset()), classDef.getSourceFileIndex(), classDef.getAnnotationsOffset(), classDef.getClassDataOffset(), classDef.getStaticValuesOffset());
    }
    
    public SortableType adjust(final SortableType sortableType) {
        return new SortableType(sortableType.getDex(), sortableType.getIndexMap(), this.adjust(sortableType.getClassDef()));
    }
    
    public EncodedValue adjustEncodedValue(final EncodedValue encodedValue) {
        final ByteArrayAnnotatedOutput out = new ByteArrayAnnotatedOutput(32);
        new EncodedValueTransformer(out).transform(new EncodedValueReader(encodedValue));
        return new EncodedValue(out.toByteArray());
    }
    
    public EncodedValue adjustEncodedArray(final EncodedValue encodedArray) {
        final ByteArrayAnnotatedOutput out = new ByteArrayAnnotatedOutput(32);
        new EncodedValueTransformer(out).transformArray(new EncodedValueReader(encodedArray, 28));
        return new EncodedValue(out.toByteArray());
    }
    
    public Annotation adjust(final Annotation annotation) {
        final ByteArrayAnnotatedOutput out = new ByteArrayAnnotatedOutput(32);
        new EncodedValueTransformer(out).transformAnnotation(annotation.getReader());
        return new Annotation(this.target, annotation.getVisibility(), new EncodedValue(out.toByteArray()));
    }
    
    private final class EncodedValueTransformer
    {
        private final ByteOutput out;
        
        public EncodedValueTransformer(final ByteOutput out) {
            this.out = out;
        }
        
        public void transform(final EncodedValueReader reader) {
            switch (reader.peek()) {
                case 0: {
                    EncodedValueCodec.writeSignedIntegralValue(this.out, 0, reader.readByte());
                    break;
                }
                case 2: {
                    EncodedValueCodec.writeSignedIntegralValue(this.out, 2, reader.readShort());
                    break;
                }
                case 4: {
                    EncodedValueCodec.writeSignedIntegralValue(this.out, 4, reader.readInt());
                    break;
                }
                case 6: {
                    EncodedValueCodec.writeSignedIntegralValue(this.out, 6, reader.readLong());
                    break;
                }
                case 3: {
                    EncodedValueCodec.writeUnsignedIntegralValue(this.out, 3, reader.readChar());
                    break;
                }
                case 16: {
                    final long longBits = Float.floatToIntBits(reader.readFloat()) << 32;
                    EncodedValueCodec.writeRightZeroExtendedValue(this.out, 16, longBits);
                    break;
                }
                case 17: {
                    EncodedValueCodec.writeRightZeroExtendedValue(this.out, 17, Double.doubleToLongBits(reader.readDouble()));
                    break;
                }
                case 21: {
                    EncodedValueCodec.writeUnsignedIntegralValue(this.out, 21, IndexMap.this.adjustProto(reader.readMethodType()));
                    break;
                }
                case 22: {
                    EncodedValueCodec.writeUnsignedIntegralValue(this.out, 22, IndexMap.this.adjustMethodHandle(reader.readMethodHandle()));
                    break;
                }
                case 23: {
                    EncodedValueCodec.writeUnsignedIntegralValue(this.out, 23, IndexMap.this.adjustString(reader.readString()));
                    break;
                }
                case 24: {
                    EncodedValueCodec.writeUnsignedIntegralValue(this.out, 24, IndexMap.this.adjustType(reader.readType()));
                    break;
                }
                case 25: {
                    EncodedValueCodec.writeUnsignedIntegralValue(this.out, 25, IndexMap.this.adjustField(reader.readField()));
                    break;
                }
                case 27: {
                    EncodedValueCodec.writeUnsignedIntegralValue(this.out, 27, IndexMap.this.adjustField(reader.readEnum()));
                    break;
                }
                case 26: {
                    EncodedValueCodec.writeUnsignedIntegralValue(this.out, 26, IndexMap.this.adjustMethod(reader.readMethod()));
                    break;
                }
                case 28: {
                    this.writeTypeAndArg(28, 0);
                    this.transformArray(reader);
                    break;
                }
                case 29: {
                    this.writeTypeAndArg(29, 0);
                    this.transformAnnotation(reader);
                    break;
                }
                case 30: {
                    reader.readNull();
                    this.writeTypeAndArg(30, 0);
                    break;
                }
                case 31: {
                    final boolean value = reader.readBoolean();
                    this.writeTypeAndArg(31, value ? 1 : 0);
                    break;
                }
                default: {
                    throw new DexException("Unexpected type: " + Integer.toHexString(reader.peek()));
                }
            }
        }
        
        private void transformAnnotation(final EncodedValueReader reader) {
            final int fieldCount = reader.readAnnotation();
            Leb128.writeUnsignedLeb128(this.out, IndexMap.this.adjustType(reader.getAnnotationType()));
            Leb128.writeUnsignedLeb128(this.out, fieldCount);
            for (int i = 0; i < fieldCount; ++i) {
                Leb128.writeUnsignedLeb128(this.out, IndexMap.this.adjustString(reader.readAnnotationName()));
                this.transform(reader);
            }
        }
        
        private void transformArray(final EncodedValueReader reader) {
            final int size = reader.readArray();
            Leb128.writeUnsignedLeb128(this.out, size);
            for (int i = 0; i < size; ++i) {
                this.transform(reader);
            }
        }
        
        private void writeTypeAndArg(final int type, final int arg) {
            this.out.writeByte(arg << 5 | type);
        }
    }
}

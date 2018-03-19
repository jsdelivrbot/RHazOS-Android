package embedded.com.android.dex;

public final class ClassDef
{
    public static final int NO_INDEX = -1;
    private final Dex buffer;
    private final int offset;
    private final int typeIndex;
    private final int accessFlags;
    private final int supertypeIndex;
    private final int interfacesOffset;
    private final int sourceFileIndex;
    private final int annotationsOffset;
    private final int classDataOffset;
    private final int staticValuesOffset;
    
    public ClassDef(final Dex buffer, final int offset, final int typeIndex, final int accessFlags, final int supertypeIndex, final int interfacesOffset, final int sourceFileIndex, final int annotationsOffset, final int classDataOffset, final int staticValuesOffset) {
        this.buffer = buffer;
        this.offset = offset;
        this.typeIndex = typeIndex;
        this.accessFlags = accessFlags;
        this.supertypeIndex = supertypeIndex;
        this.interfacesOffset = interfacesOffset;
        this.sourceFileIndex = sourceFileIndex;
        this.annotationsOffset = annotationsOffset;
        this.classDataOffset = classDataOffset;
        this.staticValuesOffset = staticValuesOffset;
    }
    
    public int getOffset() {
        return this.offset;
    }
    
    public int getTypeIndex() {
        return this.typeIndex;
    }
    
    public int getSupertypeIndex() {
        return this.supertypeIndex;
    }
    
    public int getInterfacesOffset() {
        return this.interfacesOffset;
    }
    
    public short[] getInterfaces() {
        return this.buffer.readTypeList(this.interfacesOffset).getTypes();
    }
    
    public int getAccessFlags() {
        return this.accessFlags;
    }
    
    public int getSourceFileIndex() {
        return this.sourceFileIndex;
    }
    
    public int getAnnotationsOffset() {
        return this.annotationsOffset;
    }
    
    public int getClassDataOffset() {
        return this.classDataOffset;
    }
    
    public int getStaticValuesOffset() {
        return this.staticValuesOffset;
    }
    
    @Override
    public String toString() {
        if (this.buffer == null) {
            return this.typeIndex + " " + this.supertypeIndex;
        }
        final StringBuilder result = new StringBuilder();
        result.append(this.buffer.typeNames().get(this.typeIndex));
        if (this.supertypeIndex != -1) {
            result.append(" extends ").append(this.buffer.typeNames().get(this.supertypeIndex));
        }
        return result.toString();
    }
}

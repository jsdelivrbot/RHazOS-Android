package embedded.com.android.dex;

import embedded.com.android.dex.util.*;

public final class MethodId implements Comparable<MethodId>
{
    private final Dex dex;
    private final int declaringClassIndex;
    private final int protoIndex;
    private final int nameIndex;
    
    public MethodId(final Dex dex, final int declaringClassIndex, final int protoIndex, final int nameIndex) {
        this.dex = dex;
        this.declaringClassIndex = declaringClassIndex;
        this.protoIndex = protoIndex;
        this.nameIndex = nameIndex;
    }
    
    public int getDeclaringClassIndex() {
        return this.declaringClassIndex;
    }
    
    public int getProtoIndex() {
        return this.protoIndex;
    }
    
    public int getNameIndex() {
        return this.nameIndex;
    }
    
    @Override
    public int compareTo(final MethodId other) {
        if (this.declaringClassIndex != other.declaringClassIndex) {
            return Unsigned.compare(this.declaringClassIndex, other.declaringClassIndex);
        }
        if (this.nameIndex != other.nameIndex) {
            return Unsigned.compare(this.nameIndex, other.nameIndex);
        }
        return Unsigned.compare(this.protoIndex, other.protoIndex);
    }
    
    public void writeTo(final Dex.Section out) {
        out.writeUnsignedShort(this.declaringClassIndex);
        out.writeUnsignedShort(this.protoIndex);
        out.writeInt(this.nameIndex);
    }
    
    @Override
    public String toString() {
        if (this.dex == null) {
            return this.declaringClassIndex + " " + this.protoIndex + " " + this.nameIndex;
        }
        return this.dex.typeNames().get(this.declaringClassIndex) + "." + this.dex.strings().get(this.nameIndex) + this.dex.readTypeList(this.dex.protoIds().get(this.protoIndex).getParametersOffset());
    }
}

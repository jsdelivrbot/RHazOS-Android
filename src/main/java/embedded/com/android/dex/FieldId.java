package embedded.com.android.dex;

import embedded.com.android.dex.util.*;

public final class FieldId implements Comparable<FieldId>
{
    private final Dex dex;
    private final int declaringClassIndex;
    private final int typeIndex;
    private final int nameIndex;
    
    public FieldId(final Dex dex, final int declaringClassIndex, final int typeIndex, final int nameIndex) {
        this.dex = dex;
        this.declaringClassIndex = declaringClassIndex;
        this.typeIndex = typeIndex;
        this.nameIndex = nameIndex;
    }
    
    public int getDeclaringClassIndex() {
        return this.declaringClassIndex;
    }
    
    public int getTypeIndex() {
        return this.typeIndex;
    }
    
    public int getNameIndex() {
        return this.nameIndex;
    }
    
    @Override
    public int compareTo(final FieldId other) {
        if (this.declaringClassIndex != other.declaringClassIndex) {
            return Unsigned.compare(this.declaringClassIndex, other.declaringClassIndex);
        }
        if (this.nameIndex != other.nameIndex) {
            return Unsigned.compare(this.nameIndex, other.nameIndex);
        }
        return Unsigned.compare(this.typeIndex, other.typeIndex);
    }
    
    public void writeTo(final Dex.Section out) {
        out.writeUnsignedShort(this.declaringClassIndex);
        out.writeUnsignedShort(this.typeIndex);
        out.writeInt(this.nameIndex);
    }
    
    @Override
    public String toString() {
        if (this.dex == null) {
            return this.declaringClassIndex + " " + this.typeIndex + " " + this.nameIndex;
        }
        return this.dex.typeNames().get(this.typeIndex) + "." + this.dex.strings().get(this.nameIndex);
    }
}

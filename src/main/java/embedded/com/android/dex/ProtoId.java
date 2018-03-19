package embedded.com.android.dex;

import embedded.com.android.dex.util.*;

public final class ProtoId implements Comparable<ProtoId>
{
    private final Dex dex;
    private final int shortyIndex;
    private final int returnTypeIndex;
    private final int parametersOffset;
    
    public ProtoId(final Dex dex, final int shortyIndex, final int returnTypeIndex, final int parametersOffset) {
        this.dex = dex;
        this.shortyIndex = shortyIndex;
        this.returnTypeIndex = returnTypeIndex;
        this.parametersOffset = parametersOffset;
    }
    
    @Override
    public int compareTo(final ProtoId other) {
        if (this.returnTypeIndex != other.returnTypeIndex) {
            return Unsigned.compare(this.returnTypeIndex, other.returnTypeIndex);
        }
        return Unsigned.compare(this.parametersOffset, other.parametersOffset);
    }
    
    public int getShortyIndex() {
        return this.shortyIndex;
    }
    
    public int getReturnTypeIndex() {
        return this.returnTypeIndex;
    }
    
    public int getParametersOffset() {
        return this.parametersOffset;
    }
    
    public void writeTo(final Dex.Section out) {
        out.writeInt(this.shortyIndex);
        out.writeInt(this.returnTypeIndex);
        out.writeInt(this.parametersOffset);
    }
    
    @Override
    public String toString() {
        if (this.dex == null) {
            return this.shortyIndex + " " + this.returnTypeIndex + " " + this.parametersOffset;
        }
        return this.dex.strings().get(this.shortyIndex) + ": " + this.dex.typeNames().get(this.returnTypeIndex) + " " + this.dex.readTypeList(this.parametersOffset);
    }
}

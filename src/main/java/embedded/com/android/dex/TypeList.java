package embedded.com.android.dex;

import embedded.com.android.dex.util.*;

public final class TypeList implements Comparable<TypeList>
{
    public static final TypeList EMPTY;
    private final Dex dex;
    private final short[] types;
    
    public TypeList(final Dex dex, final short[] types) {
        this.dex = dex;
        this.types = types;
    }
    
    public short[] getTypes() {
        return this.types;
    }
    
    @Override
    public int compareTo(final TypeList other) {
        for (int i = 0; i < this.types.length && i < other.types.length; ++i) {
            if (this.types[i] != other.types[i]) {
                return Unsigned.compare(this.types[i], other.types[i]);
            }
        }
        return Unsigned.compare(this.types.length, other.types.length);
    }
    
    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder();
        result.append("(");
        for (int i = 0, typesLength = this.types.length; i < typesLength; ++i) {
            result.append((this.dex != null) ? ((String)this.dex.typeNames().get(this.types[i])) : this.types[i]);
        }
        result.append(")");
        return result.toString();
    }
    
    static {
        EMPTY = new TypeList(null, Dex.EMPTY_SHORT_ARRAY);
    }
}

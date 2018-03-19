package embedded.com.android.dx.merge;

import java.util.*;
import embedded.com.android.dex.*;

final class SortableType
{
    public static final Comparator<SortableType> NULLS_LAST_ORDER;
    private final Dex dex;
    private final IndexMap indexMap;
    private ClassDef classDef;
    private int depth;
    
    public SortableType(final Dex dex, final IndexMap indexMap, final ClassDef classDef) {
        this.depth = -1;
        this.dex = dex;
        this.indexMap = indexMap;
        this.classDef = classDef;
    }
    
    public Dex getDex() {
        return this.dex;
    }
    
    public IndexMap getIndexMap() {
        return this.indexMap;
    }
    
    public ClassDef getClassDef() {
        return this.classDef;
    }
    
    public int getTypeIndex() {
        return this.classDef.getTypeIndex();
    }
    
    public boolean tryAssignDepth(final SortableType[] types) {
        int max;
        if (this.classDef.getSupertypeIndex() == -1) {
            max = 0;
        }
        else {
            if (this.classDef.getSupertypeIndex() == this.classDef.getTypeIndex()) {
                throw new DexException("Class with type index " + this.classDef.getTypeIndex() + " extends itself");
            }
            final SortableType sortableSupertype = types[this.classDef.getSupertypeIndex()];
            if (sortableSupertype == null) {
                max = 1;
            }
            else {
                if (sortableSupertype.depth == -1) {
                    return false;
                }
                max = sortableSupertype.depth;
            }
        }
        for (final short interfaceIndex : this.classDef.getInterfaces()) {
            final SortableType implemented = types[interfaceIndex];
            if (implemented == null) {
                max = Math.max(max, 1);
            }
            else {
                if (implemented.depth == -1) {
                    return false;
                }
                max = Math.max(max, implemented.depth);
            }
        }
        this.depth = max + 1;
        return true;
    }
    
    public boolean isDepthAssigned() {
        return this.depth != -1;
    }
    
    static {
        NULLS_LAST_ORDER = new Comparator<SortableType>() {
            @Override
            public int compare(final SortableType a, final SortableType b) {
                if (a == b) {
                    return 0;
                }
                if (b == null) {
                    return -1;
                }
                if (a == null) {
                    return 1;
                }
                if (a.depth != b.depth) {
                    return a.depth - b.depth;
                }
                return a.getTypeIndex() - b.getTypeIndex();
            }
        };
    }
}

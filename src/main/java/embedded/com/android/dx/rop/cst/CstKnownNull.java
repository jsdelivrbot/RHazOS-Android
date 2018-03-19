package embedded.com.android.dx.rop.cst;

import embedded.com.android.dx.rop.type.*;

public final class CstKnownNull extends CstLiteralBits
{
    public static final CstKnownNull THE_ONE;
    
    @Override
    public boolean equals(final Object other) {
        return other instanceof CstKnownNull;
    }
    
    @Override
    public int hashCode() {
        return 1147565434;
    }
    
    @Override
    protected int compareTo0(final Constant other) {
        return 0;
    }
    
    @Override
    public String toString() {
        return "known-null";
    }
    
    @Override
    public Type getType() {
        return Type.KNOWN_NULL;
    }
    
    @Override
    public String typeName() {
        return "known-null";
    }
    
    @Override
    public boolean isCategory2() {
        return false;
    }
    
    @Override
    public String toHuman() {
        return "null";
    }
    
    @Override
    public boolean fitsInInt() {
        return true;
    }
    
    @Override
    public int getIntBits() {
        return 0;
    }
    
    @Override
    public long getLongBits() {
        return 0L;
    }
    
    static {
        THE_ONE = new CstKnownNull();
    }
}

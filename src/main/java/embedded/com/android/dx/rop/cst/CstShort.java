package embedded.com.android.dx.rop.cst;

import embedded.com.android.dx.util.*;
import embedded.com.android.dx.rop.type.*;

public final class CstShort extends CstLiteral32
{
    public static final CstShort VALUE_0;
    
    public static CstShort make(final short value) {
        return new CstShort(value);
    }
    
    public static CstShort make(final int value) {
        final short cast = (short)value;
        if (cast != value) {
            throw new IllegalArgumentException("bogus short value: " + value);
        }
        return make(cast);
    }
    
    private CstShort(final short value) {
        super(value);
    }
    
    @Override
    public String toString() {
        final int value = this.getIntBits();
        return "short{0x" + Hex.u2(value) + " / " + value + '}';
    }
    
    @Override
    public Type getType() {
        return Type.SHORT;
    }
    
    @Override
    public String typeName() {
        return "short";
    }
    
    @Override
    public String toHuman() {
        return Integer.toString(this.getIntBits());
    }
    
    public short getValue() {
        return (short)this.getIntBits();
    }
    
    static {
        VALUE_0 = make((short)0);
    }
}

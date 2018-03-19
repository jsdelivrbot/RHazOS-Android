package embedded.com.android.dx.rop.cst;

import embedded.com.android.dx.util.*;
import embedded.com.android.dx.rop.type.*;

public final class CstByte extends CstLiteral32
{
    public static final CstByte VALUE_0;
    
    public static CstByte make(final byte value) {
        return new CstByte(value);
    }
    
    public static CstByte make(final int value) {
        final byte cast = (byte)value;
        if (cast != value) {
            throw new IllegalArgumentException("bogus byte value: " + value);
        }
        return make(cast);
    }
    
    private CstByte(final byte value) {
        super(value);
    }
    
    @Override
    public String toString() {
        final int value = this.getIntBits();
        return "byte{0x" + Hex.u1(value) + " / " + value + '}';
    }
    
    @Override
    public Type getType() {
        return Type.BYTE;
    }
    
    @Override
    public String typeName() {
        return "byte";
    }
    
    @Override
    public String toHuman() {
        return Integer.toString(this.getIntBits());
    }
    
    public byte getValue() {
        return (byte)this.getIntBits();
    }
    
    static {
        VALUE_0 = make((byte)0);
    }
}

package embedded.com.android.dx.rop.cst;

import embedded.com.android.dx.util.*;
import embedded.com.android.dx.rop.type.*;

public final class CstFloat extends CstLiteral32
{
    public static final CstFloat VALUE_0;
    public static final CstFloat VALUE_1;
    public static final CstFloat VALUE_2;
    
    public static CstFloat make(final int bits) {
        return new CstFloat(bits);
    }
    
    private CstFloat(final int bits) {
        super(bits);
    }
    
    @Override
    public String toString() {
        final int bits = this.getIntBits();
        return "float{0x" + Hex.u4(bits) + " / " + Float.intBitsToFloat(bits) + '}';
    }
    
    @Override
    public Type getType() {
        return Type.FLOAT;
    }
    
    @Override
    public String typeName() {
        return "float";
    }
    
    @Override
    public String toHuman() {
        return Float.toString(Float.intBitsToFloat(this.getIntBits()));
    }
    
    public float getValue() {
        return Float.intBitsToFloat(this.getIntBits());
    }
    
    static {
        VALUE_0 = make(Float.floatToIntBits(0.0f));
        VALUE_1 = make(Float.floatToIntBits(1.0f));
        VALUE_2 = make(Float.floatToIntBits(2.0f));
    }
}

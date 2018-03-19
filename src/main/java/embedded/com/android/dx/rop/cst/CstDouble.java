package embedded.com.android.dx.rop.cst;

import embedded.com.android.dx.util.*;
import embedded.com.android.dx.rop.type.*;

public final class CstDouble extends CstLiteral64
{
    public static final CstDouble VALUE_0;
    public static final CstDouble VALUE_1;
    
    public static CstDouble make(final long bits) {
        return new CstDouble(bits);
    }
    
    private CstDouble(final long bits) {
        super(bits);
    }
    
    @Override
    public String toString() {
        final long bits = this.getLongBits();
        return "double{0x" + Hex.u8(bits) + " / " + Double.longBitsToDouble(bits) + '}';
    }
    
    @Override
    public Type getType() {
        return Type.DOUBLE;
    }
    
    @Override
    public String typeName() {
        return "double";
    }
    
    @Override
    public String toHuman() {
        return Double.toString(Double.longBitsToDouble(this.getLongBits()));
    }
    
    public double getValue() {
        return Double.longBitsToDouble(this.getLongBits());
    }
    
    static {
        VALUE_0 = new CstDouble(Double.doubleToLongBits(0.0));
        VALUE_1 = new CstDouble(Double.doubleToLongBits(1.0));
    }
}

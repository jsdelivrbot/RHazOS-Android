package embedded.com.android.dx.rop.cst;

import embedded.com.android.dx.util.*;
import embedded.com.android.dx.rop.type.*;

public final class CstInteger extends CstLiteral32
{
    private static final CstInteger[] cache;
    public static final CstInteger VALUE_M1;
    public static final CstInteger VALUE_0;
    public static final CstInteger VALUE_1;
    public static final CstInteger VALUE_2;
    public static final CstInteger VALUE_3;
    public static final CstInteger VALUE_4;
    public static final CstInteger VALUE_5;
    
    public static CstInteger make(final int value) {
        final int idx = (value & Integer.MAX_VALUE) % CstInteger.cache.length;
        CstInteger obj = CstInteger.cache[idx];
        if (obj != null && obj.getValue() == value) {
            return obj;
        }
        obj = new CstInteger(value);
        return CstInteger.cache[idx] = obj;
    }
    
    private CstInteger(final int value) {
        super(value);
    }
    
    @Override
    public String toString() {
        final int value = this.getIntBits();
        return "int{0x" + Hex.u4(value) + " / " + value + '}';
    }
    
    @Override
    public Type getType() {
        return Type.INT;
    }
    
    @Override
    public String typeName() {
        return "int";
    }
    
    @Override
    public String toHuman() {
        return Integer.toString(this.getIntBits());
    }
    
    public int getValue() {
        return this.getIntBits();
    }
    
    static {
        cache = new CstInteger[511];
        VALUE_M1 = make(-1);
        VALUE_0 = make(0);
        VALUE_1 = make(1);
        VALUE_2 = make(2);
        VALUE_3 = make(3);
        VALUE_4 = make(4);
        VALUE_5 = make(5);
    }
}

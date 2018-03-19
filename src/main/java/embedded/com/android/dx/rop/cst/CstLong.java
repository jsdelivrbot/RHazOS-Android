package embedded.com.android.dx.rop.cst;

import embedded.com.android.dx.util.*;
import embedded.com.android.dx.rop.type.*;

public final class CstLong extends CstLiteral64
{
    public static final CstLong VALUE_0;
    public static final CstLong VALUE_1;
    
    public static CstLong make(final long value) {
        return new CstLong(value);
    }
    
    private CstLong(final long value) {
        super(value);
    }
    
    @Override
    public String toString() {
        final long value = this.getLongBits();
        return "long{0x" + Hex.u8(value) + " / " + value + '}';
    }
    
    @Override
    public Type getType() {
        return Type.LONG;
    }
    
    @Override
    public String typeName() {
        return "long";
    }
    
    @Override
    public String toHuman() {
        return Long.toString(this.getLongBits());
    }
    
    public long getValue() {
        return this.getLongBits();
    }
    
    static {
        VALUE_0 = make(0L);
        VALUE_1 = make(1L);
    }
}

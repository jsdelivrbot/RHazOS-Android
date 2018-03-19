package embedded.com.android.dx.rop.cst;

import embedded.com.android.dx.util.*;
import embedded.com.android.dx.rop.type.*;

public final class CstChar extends CstLiteral32
{
    public static final CstChar VALUE_0;
    
    public static CstChar make(final char value) {
        return new CstChar(value);
    }
    
    public static CstChar make(final int value) {
        final char cast = (char)value;
        if (cast != value) {
            throw new IllegalArgumentException("bogus char value: " + value);
        }
        return make(cast);
    }
    
    private CstChar(final char value) {
        super(value);
    }
    
    @Override
    public String toString() {
        final int value = this.getIntBits();
        return "char{0x" + Hex.u2(value) + " / " + value + '}';
    }
    
    @Override
    public Type getType() {
        return Type.CHAR;
    }
    
    @Override
    public String typeName() {
        return "char";
    }
    
    @Override
    public String toHuman() {
        return Integer.toString(this.getIntBits());
    }
    
    public char getValue() {
        return (char)this.getIntBits();
    }
    
    static {
        VALUE_0 = make('\0');
    }
}

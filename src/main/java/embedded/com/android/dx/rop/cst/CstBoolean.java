package embedded.com.android.dx.rop.cst;

import embedded.com.android.dx.rop.type.*;

public final class CstBoolean extends CstLiteral32
{
    public static final CstBoolean VALUE_FALSE;
    public static final CstBoolean VALUE_TRUE;
    
    public static CstBoolean make(final boolean value) {
        return value ? CstBoolean.VALUE_TRUE : CstBoolean.VALUE_FALSE;
    }
    
    public static CstBoolean make(final int value) {
        if (value == 0) {
            return CstBoolean.VALUE_FALSE;
        }
        if (value == 1) {
            return CstBoolean.VALUE_TRUE;
        }
        throw new IllegalArgumentException("bogus value: " + value);
    }
    
    private CstBoolean(final boolean value) {
        super(value ? 1 : 0);
    }
    
    @Override
    public String toString() {
        return this.getValue() ? "boolean{true}" : "boolean{false}";
    }
    
    @Override
    public Type getType() {
        return Type.BOOLEAN;
    }
    
    @Override
    public String typeName() {
        return "boolean";
    }
    
    @Override
    public String toHuman() {
        return this.getValue() ? "true" : "false";
    }
    
    public boolean getValue() {
        return this.getIntBits() != 0;
    }
    
    static {
        VALUE_FALSE = new CstBoolean(false);
        VALUE_TRUE = new CstBoolean(true);
    }
}

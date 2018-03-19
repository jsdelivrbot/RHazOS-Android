package embedded.com.android.dx.cf.attrib;

import embedded.com.android.dx.rop.cst.*;

public final class AttConstantValue extends BaseAttribute
{
    public static final String ATTRIBUTE_NAME = "ConstantValue";
    private final TypedConstant constantValue;
    
    public AttConstantValue(final TypedConstant constantValue) {
        super("ConstantValue");
        if (constantValue instanceof CstString || constantValue instanceof CstInteger || constantValue instanceof CstLong || constantValue instanceof CstFloat || constantValue instanceof CstDouble) {
            this.constantValue = constantValue;
            return;
        }
        if (constantValue == null) {
            throw new NullPointerException("constantValue == null");
        }
        throw new IllegalArgumentException("bad type for constantValue");
    }
    
    @Override
    public int byteLength() {
        return 8;
    }
    
    public TypedConstant getConstantValue() {
        return this.constantValue;
    }
}

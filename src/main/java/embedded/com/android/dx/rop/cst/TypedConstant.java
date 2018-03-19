package embedded.com.android.dx.rop.cst;

import embedded.com.android.dx.rop.type.*;

public abstract class TypedConstant extends Constant implements TypeBearer
{
    @Override
    public final TypeBearer getFrameType() {
        return this;
    }
    
    @Override
    public final int getBasicType() {
        return this.getType().getBasicType();
    }
    
    @Override
    public final int getBasicFrameType() {
        return this.getType().getBasicFrameType();
    }
    
    @Override
    public final boolean isConstant() {
        return true;
    }
}

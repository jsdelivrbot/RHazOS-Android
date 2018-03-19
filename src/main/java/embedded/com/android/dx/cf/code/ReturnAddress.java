package embedded.com.android.dx.cf.code;

import embedded.com.android.dx.util.*;
import embedded.com.android.dx.rop.type.*;

public final class ReturnAddress implements TypeBearer
{
    private final int subroutineAddress;
    
    public ReturnAddress(final int subroutineAddress) {
        if (subroutineAddress < 0) {
            throw new IllegalArgumentException("subroutineAddress < 0");
        }
        this.subroutineAddress = subroutineAddress;
    }
    
    @Override
    public String toString() {
        return "<addr:" + Hex.u2(this.subroutineAddress) + ">";
    }
    
    @Override
    public String toHuman() {
        return this.toString();
    }
    
    @Override
    public Type getType() {
        return Type.RETURN_ADDRESS;
    }
    
    @Override
    public TypeBearer getFrameType() {
        return this;
    }
    
    @Override
    public int getBasicType() {
        return Type.RETURN_ADDRESS.getBasicType();
    }
    
    @Override
    public int getBasicFrameType() {
        return Type.RETURN_ADDRESS.getBasicFrameType();
    }
    
    @Override
    public boolean isConstant() {
        return false;
    }
    
    @Override
    public boolean equals(final Object other) {
        return other instanceof ReturnAddress && this.subroutineAddress == ((ReturnAddress)other).subroutineAddress;
    }
    
    @Override
    public int hashCode() {
        return this.subroutineAddress;
    }
    
    public int getSubroutineAddress() {
        return this.subroutineAddress;
    }
}

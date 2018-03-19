package embedded.com.android.dx.rop.type;

import embedded.com.android.dx.util.*;

public interface TypeBearer extends ToHuman
{
    Type getType();
    
    TypeBearer getFrameType();
    
    int getBasicType();
    
    int getBasicFrameType();
    
    boolean isConstant();
}

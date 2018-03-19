package embedded.com.android.dx.cf.iface;

import embedded.com.android.dx.rop.cst.*;

public interface Member extends HasAttribute
{
    CstType getDefiningClass();
    
    int getAccessFlags();
    
    CstString getName();
    
    CstString getDescriptor();
    
    CstNat getNat();
    
    AttributeList getAttributes();
}

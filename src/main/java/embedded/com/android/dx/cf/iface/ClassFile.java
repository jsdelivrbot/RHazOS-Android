package embedded.com.android.dx.cf.iface;

import embedded.com.android.dx.rop.type.*;
import embedded.com.android.dx.cf.code.*;
import embedded.com.android.dx.rop.cst.*;

public interface ClassFile extends HasAttribute
{
    int getMagic();
    
    int getMinorVersion();
    
    int getMajorVersion();
    
    int getAccessFlags();
    
    CstType getThisClass();
    
    CstType getSuperclass();
    
    ConstantPool getConstantPool();
    
    TypeList getInterfaces();
    
    FieldList getFields();
    
    MethodList getMethods();
    
    AttributeList getAttributes();
    
    BootstrapMethodsList getBootstrapMethods();
    
    CstString getSourceFile();
}

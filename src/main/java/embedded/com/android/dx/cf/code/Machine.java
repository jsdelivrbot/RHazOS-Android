package embedded.com.android.dx.cf.code;

import embedded.com.android.dx.rop.type.*;
import embedded.com.android.dx.rop.cst.*;
import java.util.*;
import embedded.com.android.dx.rop.code.*;

public interface Machine
{
    Prototype getPrototype();
    
    void clearArgs();
    
    void popArgs(final Frame p0, final int p1);
    
    void popArgs(final Frame p0, final Prototype p1);
    
    void popArgs(final Frame p0, final Type p1);
    
    void popArgs(final Frame p0, final Type p1, final Type p2);
    
    void popArgs(final Frame p0, final Type p1, final Type p2, final Type p3);
    
    void localArg(final Frame p0, final int p1);
    
    void localInfo(final boolean p0);
    
    void auxType(final Type p0);
    
    void auxIntArg(final int p0);
    
    void auxCstArg(final Constant p0);
    
    void auxTargetArg(final int p0);
    
    void auxSwitchArg(final SwitchList p0);
    
    void auxInitValues(final ArrayList<Constant> p0);
    
    void localTarget(final int p0, final Type p1, final LocalItem p2);
    
    void run(final Frame p0, final int p1, final int p2);
}

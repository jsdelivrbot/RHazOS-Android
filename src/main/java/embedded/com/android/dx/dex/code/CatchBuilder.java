package embedded.com.android.dx.dex.code;

import java.util.*;
import embedded.com.android.dx.rop.type.*;

public interface CatchBuilder
{
    CatchTable build();
    
    boolean hasAnyCatches();
    
    HashSet<Type> getCatchTypes();
}

package embedded.com.android.dx.cf.attrib;

import embedded.com.android.dx.cf.code.*;
import embedded.com.android.dx.util.*;

public abstract class BaseLocalVariables extends BaseAttribute
{
    private final LocalVariableList localVariables;
    
    public BaseLocalVariables(final String name, final LocalVariableList localVariables) {
        super(name);
        try {
            if (localVariables.isMutable()) {
                throw new MutabilityException("localVariables.isMutable()");
            }
        }
        catch (NullPointerException ex) {
            throw new NullPointerException("localVariables == null");
        }
        this.localVariables = localVariables;
    }
    
    @Override
    public final int byteLength() {
        return 8 + this.localVariables.size() * 10;
    }
    
    public final LocalVariableList getLocalVariables() {
        return this.localVariables;
    }
}

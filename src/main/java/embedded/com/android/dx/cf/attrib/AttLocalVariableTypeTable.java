package embedded.com.android.dx.cf.attrib;

import embedded.com.android.dx.cf.code.*;

public final class AttLocalVariableTypeTable extends BaseLocalVariables
{
    public static final String ATTRIBUTE_NAME = "LocalVariableTypeTable";
    
    public AttLocalVariableTypeTable(final LocalVariableList localVariables) {
        super("LocalVariableTypeTable", localVariables);
    }
}

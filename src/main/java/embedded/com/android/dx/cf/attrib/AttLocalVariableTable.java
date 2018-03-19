package embedded.com.android.dx.cf.attrib;

import embedded.com.android.dx.cf.code.*;

public final class AttLocalVariableTable extends BaseLocalVariables
{
    public static final String ATTRIBUTE_NAME = "LocalVariableTable";
    
    public AttLocalVariableTable(final LocalVariableList localVariables) {
        super("LocalVariableTable", localVariables);
    }
}

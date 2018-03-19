package embedded.com.android.dx.cf.attrib;

import embedded.com.android.dx.rop.cst.*;

public final class AttSourceFile extends BaseAttribute
{
    public static final String ATTRIBUTE_NAME = "SourceFile";
    private final CstString sourceFile;
    
    public AttSourceFile(final CstString sourceFile) {
        super("SourceFile");
        if (sourceFile == null) {
            throw new NullPointerException("sourceFile == null");
        }
        this.sourceFile = sourceFile;
    }
    
    @Override
    public int byteLength() {
        return 8;
    }
    
    public CstString getSourceFile() {
        return this.sourceFile;
    }
}

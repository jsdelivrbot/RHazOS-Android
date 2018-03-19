package embedded.com.android.dx.cf.attrib;

import embedded.com.android.dx.cf.code.*;
import embedded.com.android.dx.util.*;

public final class AttLineNumberTable extends BaseAttribute
{
    public static final String ATTRIBUTE_NAME = "LineNumberTable";
    private final LineNumberList lineNumbers;
    
    public AttLineNumberTable(final LineNumberList lineNumbers) {
        super("LineNumberTable");
        try {
            if (lineNumbers.isMutable()) {
                throw new MutabilityException("lineNumbers.isMutable()");
            }
        }
        catch (NullPointerException ex) {
            throw new NullPointerException("lineNumbers == null");
        }
        this.lineNumbers = lineNumbers;
    }
    
    @Override
    public int byteLength() {
        return 8 + 4 * this.lineNumbers.size();
    }
    
    public LineNumberList getLineNumbers() {
        return this.lineNumbers;
    }
}

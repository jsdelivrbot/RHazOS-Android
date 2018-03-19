package embedded.com.android.dx.cf.attrib;

public final class AttDeprecated extends BaseAttribute
{
    public static final String ATTRIBUTE_NAME = "Deprecated";
    
    public AttDeprecated() {
        super("Deprecated");
    }
    
    @Override
    public int byteLength() {
        return 6;
    }
}

package embedded.com.android.dx.cf.attrib;

public final class AttSynthetic extends BaseAttribute
{
    public static final String ATTRIBUTE_NAME = "Synthetic";
    
    public AttSynthetic() {
        super("Synthetic");
    }
    
    @Override
    public int byteLength() {
        return 6;
    }
}

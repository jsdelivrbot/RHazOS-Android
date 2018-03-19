package embedded.com.android.dx.cf.attrib;

import embedded.com.android.dx.cf.code.*;

public class AttBootstrapMethods extends BaseAttribute
{
    public static final String ATTRIBUTE_NAME = "BootstrapMethods";
    private static final int ATTRIBUTE_HEADER_BYTES = 8;
    private static final int BOOTSTRAP_METHOD_BYTES = 4;
    private static final int BOOTSTRAP_ARGUMENT_BYTES = 2;
    private final BootstrapMethodsList bootstrapMethods;
    private final int byteLength;
    
    public AttBootstrapMethods(final BootstrapMethodsList bootstrapMethods) {
        super("BootstrapMethods");
        this.bootstrapMethods = bootstrapMethods;
        int bytes = 8 + bootstrapMethods.size() * 4;
        for (int i = 0; i < bootstrapMethods.size(); ++i) {
            final int numberOfArguments = bootstrapMethods.get(i).getBootstrapMethodArguments().size();
            bytes += numberOfArguments * 2;
        }
        this.byteLength = bytes;
    }
    
    @Override
    public int byteLength() {
        return this.byteLength;
    }
    
    public BootstrapMethodsList getBootstrapMethods() {
        return this.bootstrapMethods;
    }
}

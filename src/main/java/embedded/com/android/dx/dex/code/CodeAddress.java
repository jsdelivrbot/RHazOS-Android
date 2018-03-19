package embedded.com.android.dx.dex.code;

import embedded.com.android.dx.rop.code.*;

public final class CodeAddress extends ZeroSizeInsn
{
    private final boolean bindsClosely;
    
    public CodeAddress(final SourcePosition position) {
        this(position, false);
    }
    
    public CodeAddress(final SourcePosition position, final boolean bindsClosely) {
        super(position);
        this.bindsClosely = bindsClosely;
    }
    
    @Override
    public final DalvInsn withRegisters(final RegisterSpecList registers) {
        return new CodeAddress(this.getPosition());
    }
    
    @Override
    protected String argString() {
        return null;
    }
    
    @Override
    protected String listingString0(final boolean noteIndices) {
        return "code-address";
    }
    
    public boolean getBindsClosely() {
        return this.bindsClosely;
    }
}

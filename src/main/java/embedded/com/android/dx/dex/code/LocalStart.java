package embedded.com.android.dx.dex.code;

import embedded.com.android.dx.rop.code.*;
import embedded.com.android.dx.ssa.*;

public final class LocalStart extends ZeroSizeInsn
{
    private final RegisterSpec local;
    
    public static String localString(final RegisterSpec spec) {
        return spec.regString() + ' ' + spec.getLocalItem().toString() + ": " + spec.getTypeBearer().toHuman();
    }
    
    public LocalStart(final SourcePosition position, final RegisterSpec local) {
        super(position);
        if (local == null) {
            throw new NullPointerException("local == null");
        }
        this.local = local;
    }
    
    @Override
    public DalvInsn withRegisterOffset(final int delta) {
        return new LocalStart(this.getPosition(), this.local.withOffset(delta));
    }
    
    @Override
    public DalvInsn withRegisters(final RegisterSpecList registers) {
        return new LocalStart(this.getPosition(), this.local);
    }
    
    public RegisterSpec getLocal() {
        return this.local;
    }
    
    @Override
    protected String argString() {
        return this.local.toString();
    }
    
    @Override
    protected String listingString0(final boolean noteIndices) {
        return "local-start " + localString(this.local);
    }
    
    @Override
    public DalvInsn withMapper(final RegisterMapper mapper) {
        return new LocalStart(this.getPosition(), mapper.map(this.local));
    }
}

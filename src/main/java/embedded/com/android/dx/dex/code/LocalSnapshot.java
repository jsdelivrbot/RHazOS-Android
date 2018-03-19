package embedded.com.android.dx.dex.code;

import embedded.com.android.dx.rop.code.*;
import embedded.com.android.dx.ssa.*;

public final class LocalSnapshot extends ZeroSizeInsn
{
    private final RegisterSpecSet locals;
    
    public LocalSnapshot(final SourcePosition position, final RegisterSpecSet locals) {
        super(position);
        if (locals == null) {
            throw new NullPointerException("locals == null");
        }
        this.locals = locals;
    }
    
    @Override
    public DalvInsn withRegisterOffset(final int delta) {
        return new LocalSnapshot(this.getPosition(), this.locals.withOffset(delta));
    }
    
    @Override
    public DalvInsn withRegisters(final RegisterSpecList registers) {
        return new LocalSnapshot(this.getPosition(), this.locals);
    }
    
    public RegisterSpecSet getLocals() {
        return this.locals;
    }
    
    @Override
    protected String argString() {
        return this.locals.toString();
    }
    
    @Override
    protected String listingString0(final boolean noteIndices) {
        final int sz = this.locals.size();
        final int max = this.locals.getMaxSize();
        final StringBuffer sb = new StringBuffer(100 + sz * 40);
        sb.append("local-snapshot");
        for (int i = 0; i < max; ++i) {
            final RegisterSpec spec = this.locals.get(i);
            if (spec != null) {
                sb.append("\n  ");
                sb.append(LocalStart.localString(spec));
            }
        }
        return sb.toString();
    }
    
    @Override
    public DalvInsn withMapper(final RegisterMapper mapper) {
        return new LocalSnapshot(this.getPosition(), mapper.map(this.locals));
    }
}

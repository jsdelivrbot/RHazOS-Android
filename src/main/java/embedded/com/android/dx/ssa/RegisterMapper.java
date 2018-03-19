package embedded.com.android.dx.ssa;

import embedded.com.android.dx.rop.code.*;

public abstract class RegisterMapper
{
    public abstract int getNewRegisterCount();
    
    public abstract RegisterSpec map(final RegisterSpec p0);
    
    public final RegisterSpecList map(final RegisterSpecList sources) {
        final int sz = sources.size();
        final RegisterSpecList newSources = new RegisterSpecList(sz);
        for (int i = 0; i < sz; ++i) {
            newSources.set(i, this.map(sources.get(i)));
        }
        newSources.setImmutable();
        return newSources.equals(sources) ? sources : newSources;
    }
    
    public final RegisterSpecSet map(final RegisterSpecSet sources) {
        final int sz = sources.getMaxSize();
        final RegisterSpecSet newSources = new RegisterSpecSet(this.getNewRegisterCount());
        for (int i = 0; i < sz; ++i) {
            final RegisterSpec registerSpec = sources.get(i);
            if (registerSpec != null) {
                newSources.put(this.map(registerSpec));
            }
        }
        newSources.setImmutable();
        return newSources.equals(sources) ? sources : newSources;
    }
}

package embedded.com.android.dx.cf.iface;

import embedded.com.android.dx.util.*;

public final class StdMethodList extends FixedSizeList implements MethodList
{
    public StdMethodList(final int size) {
        super(size);
    }
    
    @Override
    public Method get(final int n) {
        return (Method)this.get0(n);
    }
    
    public void set(final int n, final Method method) {
        this.set0(n, method);
    }
}

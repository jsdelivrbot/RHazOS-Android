package embedded.com.android.dx.cf.code;

import embedded.com.android.dx.util.*;
import embedded.com.android.dx.rop.cst.*;

public class BootstrapMethodArgumentsList extends FixedSizeList
{
    public BootstrapMethodArgumentsList(final int count) {
        super(count);
    }
    
    public Constant get(final int n) {
        return (Constant)this.get0(n);
    }
    
    public void set(final int n, final Constant cst) {
        if (cst instanceof CstString || cst instanceof CstType || cst instanceof CstInteger || cst instanceof CstLong || cst instanceof CstFloat || cst instanceof CstDouble || cst instanceof CstMethodHandle || cst instanceof CstProtoRef) {
            this.set0(n, cst);
            return;
        }
        final Class<?> klass = cst.getClass();
        throw new IllegalArgumentException("bad type for bootstrap argument: " + klass);
    }
}

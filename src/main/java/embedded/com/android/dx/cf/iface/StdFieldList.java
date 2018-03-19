package embedded.com.android.dx.cf.iface;

import embedded.com.android.dx.util.*;

public final class StdFieldList extends FixedSizeList implements FieldList
{
    public StdFieldList(final int size) {
        super(size);
    }
    
    @Override
    public Field get(final int n) {
        return (Field)this.get0(n);
    }
    
    public void set(final int n, final Field field) {
        this.set0(n, field);
    }
}

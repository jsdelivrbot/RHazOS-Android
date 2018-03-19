package embedded.com.android.dx.rop.cst;

import embedded.com.android.dx.util.*;

public abstract class Constant implements ToHuman, Comparable<Constant>
{
    public abstract boolean isCategory2();
    
    public abstract String typeName();
    
    @Override
    public final int compareTo(final Constant other) {
        final Class clazz = this.getClass();
        final Class otherClazz = other.getClass();
        if (clazz != otherClazz) {
            return clazz.getName().compareTo(otherClazz.getName());
        }
        return this.compareTo0(other);
    }
    
    protected abstract int compareTo0(final Constant p0);
}

package embedded.com.android.dx.rop.annotation;

import embedded.com.android.dx.util.*;

public final class AnnotationsList extends FixedSizeList
{
    public static final AnnotationsList EMPTY;
    
    public static AnnotationsList combine(final AnnotationsList list1, final AnnotationsList list2) {
        final int size = list1.size();
        if (size != list2.size()) {
            throw new IllegalArgumentException("list1.size() != list2.size()");
        }
        final AnnotationsList result = new AnnotationsList(size);
        for (int i = 0; i < size; ++i) {
            final Annotations a1 = list1.get(i);
            final Annotations a2 = list2.get(i);
            result.set(i, Annotations.combine(a1, a2));
        }
        result.setImmutable();
        return result;
    }
    
    public AnnotationsList(final int size) {
        super(size);
    }
    
    public Annotations get(final int n) {
        return (Annotations)this.get0(n);
    }
    
    public void set(final int n, final Annotations a) {
        a.throwIfMutable();
        this.set0(n, a);
    }
    
    static {
        EMPTY = new AnnotationsList(0);
    }
}

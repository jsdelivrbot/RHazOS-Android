package embedded.com.android.dx.rop.annotation;

import embedded.com.android.dx.util.*;
import embedded.com.android.dx.rop.cst.*;
import java.util.*;

public final class Annotations extends MutabilityControl implements Comparable<Annotations>
{
    public static final Annotations EMPTY;
    private final TreeMap<CstType, Annotation> annotations;
    
    public static Annotations combine(final Annotations a1, final Annotations a2) {
        final Annotations result = new Annotations();
        result.addAll(a1);
        result.addAll(a2);
        result.setImmutable();
        return result;
    }
    
    public static Annotations combine(final Annotations annotations, final Annotation annotation) {
        final Annotations result = new Annotations();
        result.addAll(annotations);
        result.add(annotation);
        result.setImmutable();
        return result;
    }
    
    public Annotations() {
        this.annotations = new TreeMap<CstType, Annotation>();
    }
    
    @Override
    public int hashCode() {
        return this.annotations.hashCode();
    }
    
    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof Annotations)) {
            return false;
        }
        final Annotations otherAnnotations = (Annotations)other;
        return this.annotations.equals(otherAnnotations.annotations);
    }
    
    @Override
    public int compareTo(final Annotations other) {
        final Iterator<Annotation> thisIter = this.annotations.values().iterator();
        final Iterator<Annotation> otherIter = other.annotations.values().iterator();
        while (thisIter.hasNext() && otherIter.hasNext()) {
            final Annotation thisOne = thisIter.next();
            final Annotation otherOne = otherIter.next();
            final int result = thisOne.compareTo(otherOne);
            if (result != 0) {
                return result;
            }
        }
        if (thisIter.hasNext()) {
            return 1;
        }
        if (otherIter.hasNext()) {
            return -1;
        }
        return 0;
    }
    
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        boolean first = true;
        sb.append("annotations{");
        for (final Annotation a : this.annotations.values()) {
            if (first) {
                first = false;
            }
            else {
                sb.append(", ");
            }
            sb.append(a.toHuman());
        }
        sb.append("}");
        return sb.toString();
    }
    
    public int size() {
        return this.annotations.size();
    }
    
    public void add(final Annotation annotation) {
        this.throwIfImmutable();
        if (annotation == null) {
            throw new NullPointerException("annotation == null");
        }
        final CstType type = annotation.getType();
        if (this.annotations.containsKey(type)) {
            throw new IllegalArgumentException("duplicate type: " + type.toHuman());
        }
        this.annotations.put(type, annotation);
    }
    
    public void addAll(final Annotations toAdd) {
        this.throwIfImmutable();
        if (toAdd == null) {
            throw new NullPointerException("toAdd == null");
        }
        for (final Annotation a : toAdd.annotations.values()) {
            this.add(a);
        }
    }
    
    public Collection<Annotation> getAnnotations() {
        return Collections.unmodifiableCollection((Collection<? extends Annotation>)this.annotations.values());
    }
    
    static {
        (EMPTY = new Annotations()).setImmutable();
    }
}

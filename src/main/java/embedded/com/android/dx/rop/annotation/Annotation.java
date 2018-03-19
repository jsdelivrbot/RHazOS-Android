package embedded.com.android.dx.rop.annotation;

import embedded.com.android.dx.util.*;
import embedded.com.android.dx.rop.cst.*;
import java.util.*;

public final class Annotation extends MutabilityControl implements Comparable<Annotation>, ToHuman
{
    private final CstType type;
    private final AnnotationVisibility visibility;
    private final TreeMap<CstString, NameValuePair> elements;
    
    public Annotation(final CstType type, final AnnotationVisibility visibility) {
        if (type == null) {
            throw new NullPointerException("type == null");
        }
        if (visibility == null) {
            throw new NullPointerException("visibility == null");
        }
        this.type = type;
        this.visibility = visibility;
        this.elements = new TreeMap<CstString, NameValuePair>();
    }
    
    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof Annotation)) {
            return false;
        }
        final Annotation otherAnnotation = (Annotation)other;
        return this.type.equals(otherAnnotation.type) && this.visibility == otherAnnotation.visibility && this.elements.equals(otherAnnotation.elements);
    }
    
    @Override
    public int hashCode() {
        int hash = this.type.hashCode();
        hash = hash * 31 + this.elements.hashCode();
        hash = hash * 31 + this.visibility.hashCode();
        return hash;
    }
    
    @Override
    public int compareTo(final Annotation other) {
        int result = this.type.compareTo((Constant)other.type);
        if (result != 0) {
            return result;
        }
        result = this.visibility.compareTo(other.visibility);
        if (result != 0) {
            return result;
        }
        final Iterator<NameValuePair> thisIter = this.elements.values().iterator();
        final Iterator<NameValuePair> otherIter = other.elements.values().iterator();
        while (thisIter.hasNext() && otherIter.hasNext()) {
            final NameValuePair thisOne = thisIter.next();
            final NameValuePair otherOne = otherIter.next();
            result = thisOne.compareTo(otherOne);
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
        return this.toHuman();
    }
    
    @Override
    public String toHuman() {
        final StringBuilder sb = new StringBuilder();
        sb.append(this.visibility.toHuman());
        sb.append("-annotation ");
        sb.append(this.type.toHuman());
        sb.append(" {");
        boolean first = true;
        for (final NameValuePair pair : this.elements.values()) {
            if (first) {
                first = false;
            }
            else {
                sb.append(", ");
            }
            sb.append(pair.getName().toHuman());
            sb.append(": ");
            sb.append(pair.getValue().toHuman());
        }
        sb.append("}");
        return sb.toString();
    }
    
    public CstType getType() {
        return this.type;
    }
    
    public AnnotationVisibility getVisibility() {
        return this.visibility;
    }
    
    public void put(final NameValuePair pair) {
        this.throwIfImmutable();
        if (pair == null) {
            throw new NullPointerException("pair == null");
        }
        this.elements.put(pair.getName(), pair);
    }
    
    public void add(final NameValuePair pair) {
        this.throwIfImmutable();
        if (pair == null) {
            throw new NullPointerException("pair == null");
        }
        final CstString name = pair.getName();
        if (this.elements.get(name) != null) {
            throw new IllegalArgumentException("name already added: " + name);
        }
        this.elements.put(name, pair);
    }
    
    public Collection<NameValuePair> getNameValuePairs() {
        return Collections.unmodifiableCollection((Collection<? extends NameValuePair>)this.elements.values());
    }
}

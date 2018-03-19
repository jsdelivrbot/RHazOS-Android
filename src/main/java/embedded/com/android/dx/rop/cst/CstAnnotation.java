package embedded.com.android.dx.rop.cst;

import embedded.com.android.dx.rop.annotation.*;

public final class CstAnnotation extends Constant
{
    private final Annotation annotation;
    
    public CstAnnotation(final Annotation annotation) {
        if (annotation == null) {
            throw new NullPointerException("annotation == null");
        }
        annotation.throwIfMutable();
        this.annotation = annotation;
    }
    
    @Override
    public boolean equals(final Object other) {
        return other instanceof CstAnnotation && this.annotation.equals(((CstAnnotation)other).annotation);
    }
    
    @Override
    public int hashCode() {
        return this.annotation.hashCode();
    }
    
    @Override
    protected int compareTo0(final Constant other) {
        return this.annotation.compareTo(((CstAnnotation)other).annotation);
    }
    
    @Override
    public String toString() {
        return this.annotation.toString();
    }
    
    @Override
    public String typeName() {
        return "annotation";
    }
    
    @Override
    public boolean isCategory2() {
        return false;
    }
    
    @Override
    public String toHuman() {
        return this.annotation.toString();
    }
    
    public Annotation getAnnotation() {
        return this.annotation;
    }
}

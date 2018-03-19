package embedded.com.android.dx.dex.file;

import embedded.com.android.dx.rop.cst.*;
import embedded.com.android.dx.util.*;
import embedded.com.android.dx.rop.annotation.*;

public final class MethodAnnotationStruct implements ToHuman, Comparable<MethodAnnotationStruct>
{
    private final CstMethodRef method;
    private AnnotationSetItem annotations;
    
    public MethodAnnotationStruct(final CstMethodRef method, final AnnotationSetItem annotations) {
        if (method == null) {
            throw new NullPointerException("method == null");
        }
        if (annotations == null) {
            throw new NullPointerException("annotations == null");
        }
        this.method = method;
        this.annotations = annotations;
    }
    
    @Override
    public int hashCode() {
        return this.method.hashCode();
    }
    
    @Override
    public boolean equals(final Object other) {
        return other instanceof MethodAnnotationStruct && this.method.equals(((MethodAnnotationStruct)other).method);
    }
    
    @Override
    public int compareTo(final MethodAnnotationStruct other) {
        return this.method.compareTo((Constant)other.method);
    }
    
    public void addContents(final DexFile file) {
        final MethodIdsSection methodIds = file.getMethodIds();
        final MixedItemSection wordData = file.getWordData();
        methodIds.intern(this.method);
        this.annotations = wordData.intern(this.annotations);
    }
    
    public void writeTo(final DexFile file, final AnnotatedOutput out) {
        final int methodIdx = file.getMethodIds().indexOf(this.method);
        final int annotationsOff = this.annotations.getAbsoluteOffset();
        if (out.annotates()) {
            out.annotate(0, "    " + this.method.toHuman());
            out.annotate(4, "      method_idx:      " + Hex.u4(methodIdx));
            out.annotate(4, "      annotations_off: " + Hex.u4(annotationsOff));
        }
        out.writeInt(methodIdx);
        out.writeInt(annotationsOff);
    }
    
    @Override
    public String toHuman() {
        return this.method.toHuman() + ": " + this.annotations;
    }
    
    public CstMethodRef getMethod() {
        return this.method;
    }
    
    public Annotations getAnnotations() {
        return this.annotations.getAnnotations();
    }
}

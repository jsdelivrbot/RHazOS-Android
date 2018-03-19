package embedded.com.android.dx.dex.file;

import embedded.com.android.dx.rop.cst.*;
import embedded.com.android.dx.util.*;
import embedded.com.android.dx.rop.annotation.*;

public final class FieldAnnotationStruct implements ToHuman, Comparable<FieldAnnotationStruct>
{
    private final CstFieldRef field;
    private AnnotationSetItem annotations;
    
    public FieldAnnotationStruct(final CstFieldRef field, final AnnotationSetItem annotations) {
        if (field == null) {
            throw new NullPointerException("field == null");
        }
        if (annotations == null) {
            throw new NullPointerException("annotations == null");
        }
        this.field = field;
        this.annotations = annotations;
    }
    
    @Override
    public int hashCode() {
        return this.field.hashCode();
    }
    
    @Override
    public boolean equals(final Object other) {
        return other instanceof FieldAnnotationStruct && this.field.equals(((FieldAnnotationStruct)other).field);
    }
    
    @Override
    public int compareTo(final FieldAnnotationStruct other) {
        return this.field.compareTo((Constant)other.field);
    }
    
    public void addContents(final DexFile file) {
        final FieldIdsSection fieldIds = file.getFieldIds();
        final MixedItemSection wordData = file.getWordData();
        fieldIds.intern(this.field);
        this.annotations = wordData.intern(this.annotations);
    }
    
    public void writeTo(final DexFile file, final AnnotatedOutput out) {
        final int fieldIdx = file.getFieldIds().indexOf(this.field);
        final int annotationsOff = this.annotations.getAbsoluteOffset();
        if (out.annotates()) {
            out.annotate(0, "    " + this.field.toHuman());
            out.annotate(4, "      field_idx:       " + Hex.u4(fieldIdx));
            out.annotate(4, "      annotations_off: " + Hex.u4(annotationsOff));
        }
        out.writeInt(fieldIdx);
        out.writeInt(annotationsOff);
    }
    
    @Override
    public String toHuman() {
        return this.field.toHuman() + ": " + this.annotations;
    }
    
    public CstFieldRef getField() {
        return this.field;
    }
    
    public Annotations getAnnotations() {
        return this.annotations.getAnnotations();
    }
}

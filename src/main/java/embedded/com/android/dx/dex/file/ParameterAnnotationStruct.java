package embedded.com.android.dx.dex.file;

import embedded.com.android.dx.rop.annotation.*;
import embedded.com.android.dx.rop.cst.*;
import embedded.com.android.dx.util.*;
import java.util.*;

public final class ParameterAnnotationStruct implements ToHuman, Comparable<ParameterAnnotationStruct>
{
    private final CstMethodRef method;
    private final AnnotationsList annotationsList;
    private final UniformListItem<AnnotationSetRefItem> annotationsItem;
    
    public ParameterAnnotationStruct(final CstMethodRef method, final AnnotationsList annotationsList, final DexFile dexFile) {
        if (method == null) {
            throw new NullPointerException("method == null");
        }
        if (annotationsList == null) {
            throw new NullPointerException("annotationsList == null");
        }
        this.method = method;
        this.annotationsList = annotationsList;
        final int size = annotationsList.size();
        final ArrayList<AnnotationSetRefItem> arrayList = new ArrayList<AnnotationSetRefItem>(size);
        for (int i = 0; i < size; ++i) {
            final Annotations annotations = annotationsList.get(i);
            final AnnotationSetItem item = new AnnotationSetItem(annotations, dexFile);
            arrayList.add(new AnnotationSetRefItem(item));
        }
        this.annotationsItem = new UniformListItem<AnnotationSetRefItem>(ItemType.TYPE_ANNOTATION_SET_REF_LIST, arrayList);
    }
    
    @Override
    public int hashCode() {
        return this.method.hashCode();
    }
    
    @Override
    public boolean equals(final Object other) {
        return other instanceof ParameterAnnotationStruct && this.method.equals(((ParameterAnnotationStruct)other).method);
    }
    
    @Override
    public int compareTo(final ParameterAnnotationStruct other) {
        return this.method.compareTo((Constant)other.method);
    }
    
    public void addContents(final DexFile file) {
        final MethodIdsSection methodIds = file.getMethodIds();
        final MixedItemSection wordData = file.getWordData();
        methodIds.intern(this.method);
        wordData.add(this.annotationsItem);
    }
    
    public void writeTo(final DexFile file, final AnnotatedOutput out) {
        final int methodIdx = file.getMethodIds().indexOf(this.method);
        final int annotationsOff = this.annotationsItem.getAbsoluteOffset();
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
        final StringBuilder sb = new StringBuilder();
        sb.append(this.method.toHuman());
        sb.append(": ");
        boolean first = true;
        for (final AnnotationSetRefItem item : this.annotationsItem.getItems()) {
            if (first) {
                first = false;
            }
            else {
                sb.append(", ");
            }
            sb.append(item.toHuman());
        }
        return sb.toString();
    }
    
    public CstMethodRef getMethod() {
        return this.method;
    }
    
    public AnnotationsList getAnnotationsList() {
        return this.annotationsList;
    }
}

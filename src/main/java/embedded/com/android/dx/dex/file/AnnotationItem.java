package embedded.com.android.dx.dex.file;

import embedded.com.android.dx.util.*;
import java.util.*;
import embedded.com.android.dx.rop.cst.*;
import embedded.com.android.dx.rop.annotation.*;

public final class AnnotationItem extends OffsettedItem
{
    private static final int VISIBILITY_BUILD = 0;
    private static final int VISIBILITY_RUNTIME = 1;
    private static final int VISIBILITY_SYSTEM = 2;
    private static final int ALIGNMENT = 1;
    private static final TypeIdSorter TYPE_ID_SORTER;
    private final Annotation annotation;
    private TypeIdItem type;
    private byte[] encodedForm;
    
    public static void sortByTypeIdIndex(final AnnotationItem[] array) {
        Arrays.sort(array, AnnotationItem.TYPE_ID_SORTER);
    }
    
    public AnnotationItem(final Annotation annotation, final DexFile dexFile) {
        super(1, -1);
        if (annotation == null) {
            throw new NullPointerException("annotation == null");
        }
        this.annotation = annotation;
        this.type = null;
        this.encodedForm = null;
        this.addContents(dexFile);
    }
    
    @Override
    public ItemType itemType() {
        return ItemType.TYPE_ANNOTATION_ITEM;
    }
    
    @Override
    public int hashCode() {
        return this.annotation.hashCode();
    }
    
    @Override
    protected int compareTo0(final OffsettedItem other) {
        final AnnotationItem otherAnnotation = (AnnotationItem)other;
        return this.annotation.compareTo(otherAnnotation.annotation);
    }
    
    @Override
    public String toHuman() {
        return this.annotation.toHuman();
    }
    
    @Override
    public void addContents(final DexFile file) {
        this.type = file.getTypeIds().intern(this.annotation.getType());
        ValueEncoder.addContents(file, this.annotation);
    }
    
    @Override
    protected void place0(final Section addedTo, final int offset) {
        final ByteArrayAnnotatedOutput out = new ByteArrayAnnotatedOutput();
        final ValueEncoder encoder = new ValueEncoder(addedTo.getFile(), out);
        encoder.writeAnnotation(this.annotation, false);
        this.encodedForm = out.toByteArray();
        this.setWriteSize(this.encodedForm.length + 1);
    }
    
    public void annotateTo(final AnnotatedOutput out, final String prefix) {
        out.annotate(0, prefix + "visibility: " + this.annotation.getVisibility().toHuman());
        out.annotate(0, prefix + "type: " + this.annotation.getType().toHuman());
        for (final NameValuePair pair : this.annotation.getNameValuePairs()) {
            final CstString name = pair.getName();
            final Constant value = pair.getValue();
            out.annotate(0, prefix + name.toHuman() + ": " + ValueEncoder.constantToHuman(value));
        }
    }
    
    @Override
    protected void writeTo0(final DexFile file, final AnnotatedOutput out) {
        final boolean annotates = out.annotates();
        final AnnotationVisibility visibility = this.annotation.getVisibility();
        if (annotates) {
            out.annotate(0, this.offsetString() + " annotation");
            out.annotate(1, "  visibility: VISBILITY_" + visibility);
        }
        switch (visibility) {
            case BUILD: {
                out.writeByte(0);
                break;
            }
            case RUNTIME: {
                out.writeByte(1);
                break;
            }
            case SYSTEM: {
                out.writeByte(2);
                break;
            }
            default: {
                throw new RuntimeException("shouldn't happen");
            }
        }
        if (annotates) {
            final ValueEncoder encoder = new ValueEncoder(file, out);
            encoder.writeAnnotation(this.annotation, true);
        }
        else {
            out.write(this.encodedForm);
        }
    }
    
    static {
        TYPE_ID_SORTER = new TypeIdSorter();
    }
    
    private static class TypeIdSorter implements Comparator<AnnotationItem>
    {
        @Override
        public int compare(final AnnotationItem item1, final AnnotationItem item2) {
            final int index1 = item1.type.getIndex();
            final int index2 = item2.type.getIndex();
            if (index1 < index2) {
                return -1;
            }
            if (index1 > index2) {
                return 1;
            }
            return 0;
        }
    }
}

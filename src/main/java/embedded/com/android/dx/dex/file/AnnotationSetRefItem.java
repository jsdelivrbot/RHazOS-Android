package embedded.com.android.dx.dex.file;

import embedded.com.android.dx.util.*;

public final class AnnotationSetRefItem extends OffsettedItem
{
    private static final int ALIGNMENT = 4;
    private static final int WRITE_SIZE = 4;
    private AnnotationSetItem annotations;
    
    public AnnotationSetRefItem(final AnnotationSetItem annotations) {
        super(4, 4);
        if (annotations == null) {
            throw new NullPointerException("annotations == null");
        }
        this.annotations = annotations;
    }
    
    @Override
    public ItemType itemType() {
        return ItemType.TYPE_ANNOTATION_SET_REF_ITEM;
    }
    
    @Override
    public void addContents(final DexFile file) {
        final MixedItemSection wordData = file.getWordData();
        this.annotations = wordData.intern(this.annotations);
    }
    
    @Override
    public String toHuman() {
        return this.annotations.toHuman();
    }
    
    @Override
    protected void writeTo0(final DexFile file, final AnnotatedOutput out) {
        final int annotationsOff = this.annotations.getAbsoluteOffset();
        if (out.annotates()) {
            out.annotate(4, "  annotations_off: " + Hex.u4(annotationsOff));
        }
        out.writeInt(annotationsOff);
    }
}

package embedded.com.android.dx.dex.file;

import embedded.com.android.dx.rop.annotation.*;
import java.util.*;
import embedded.com.android.dx.util.*;

public final class AnnotationSetItem extends OffsettedItem
{
    private static final int ALIGNMENT = 4;
    private static final int ENTRY_WRITE_SIZE = 4;
    private final Annotations annotations;
    private final AnnotationItem[] items;
    
    public AnnotationSetItem(final Annotations annotations, final DexFile dexFile) {
        super(4, writeSize(annotations));
        this.annotations = annotations;
        this.items = new AnnotationItem[annotations.size()];
        int at = 0;
        for (final Annotation a : annotations.getAnnotations()) {
            this.items[at] = new AnnotationItem(a, dexFile);
            ++at;
        }
    }
    
    private static int writeSize(final Annotations annotations) {
        try {
            return annotations.size() * 4 + 4;
        }
        catch (NullPointerException ex) {
            throw new NullPointerException("list == null");
        }
    }
    
    public Annotations getAnnotations() {
        return this.annotations;
    }
    
    @Override
    public int hashCode() {
        return this.annotations.hashCode();
    }
    
    @Override
    protected int compareTo0(final OffsettedItem other) {
        final AnnotationSetItem otherSet = (AnnotationSetItem)other;
        return this.annotations.compareTo(otherSet.annotations);
    }
    
    @Override
    public ItemType itemType() {
        return ItemType.TYPE_ANNOTATION_SET_ITEM;
    }
    
    @Override
    public String toHuman() {
        return this.annotations.toString();
    }
    
    @Override
    public void addContents(final DexFile file) {
        final MixedItemSection byteData = file.getByteData();
        for (int size = this.items.length, i = 0; i < size; ++i) {
            this.items[i] = byteData.intern(this.items[i]);
        }
    }
    
    @Override
    protected void place0(final Section addedTo, final int offset) {
        AnnotationItem.sortByTypeIdIndex(this.items);
    }
    
    @Override
    protected void writeTo0(final DexFile file, final AnnotatedOutput out) {
        final boolean annotates = out.annotates();
        final int size = this.items.length;
        if (annotates) {
            out.annotate(0, this.offsetString() + " annotation set");
            out.annotate(4, "  size: " + Hex.u4(size));
        }
        out.writeInt(size);
        for (int i = 0; i < size; ++i) {
            final AnnotationItem item = this.items[i];
            final int offset = item.getAbsoluteOffset();
            if (annotates) {
                out.annotate(4, "  entries[" + Integer.toHexString(i) + "]: " + Hex.u4(offset));
                this.items[i].annotateTo(out, "    ");
            }
            out.writeInt(offset);
        }
    }
}

package embedded.com.android.dx.dex.file;

import embedded.com.android.dx.rop.cst.*;
import embedded.com.android.dx.util.*;

public final class EncodedArrayItem extends OffsettedItem
{
    private static final int ALIGNMENT = 1;
    private final CstArray array;
    private byte[] encodedForm;
    
    public EncodedArrayItem(final CstArray array) {
        super(1, -1);
        if (array == null) {
            throw new NullPointerException("array == null");
        }
        this.array = array;
        this.encodedForm = null;
    }
    
    @Override
    public ItemType itemType() {
        return ItemType.TYPE_ENCODED_ARRAY_ITEM;
    }
    
    @Override
    public int hashCode() {
        return this.array.hashCode();
    }
    
    @Override
    protected int compareTo0(final OffsettedItem other) {
        final EncodedArrayItem otherArray = (EncodedArrayItem)other;
        return this.array.compareTo((Constant)otherArray.array);
    }
    
    @Override
    public String toHuman() {
        return this.array.toHuman();
    }
    
    @Override
    public void addContents(final DexFile file) {
        ValueEncoder.addContents(file, this.array);
    }
    
    @Override
    protected void place0(final Section addedTo, final int offset) {
        final ByteArrayAnnotatedOutput out = new ByteArrayAnnotatedOutput();
        final ValueEncoder encoder = new ValueEncoder(addedTo.getFile(), out);
        encoder.writeArray(this.array, false);
        this.encodedForm = out.toByteArray();
        this.setWriteSize(this.encodedForm.length);
    }
    
    @Override
    protected void writeTo0(final DexFile file, final AnnotatedOutput out) {
        final boolean annotates = out.annotates();
        if (annotates) {
            out.annotate(0, this.offsetString() + " encoded array");
            final ValueEncoder encoder = new ValueEncoder(file, out);
            encoder.writeArray(this.array, true);
        }
        else {
            out.write(this.encodedForm);
        }
    }
}

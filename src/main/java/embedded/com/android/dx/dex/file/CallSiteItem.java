package embedded.com.android.dx.dex.file;

import embedded.com.android.dx.util.*;
import embedded.com.android.dx.rop.cst.*;

public final class CallSiteItem extends OffsettedItem
{
    private final CstCallSite value;
    private byte[] encodedForm;
    
    public CallSiteItem(final CstCallSite value) {
        super(1, writeSize(value));
        this.value = value;
    }
    
    private static int writeSize(final CstCallSite value) {
        return -1;
    }
    
    @Override
    protected void place0(final Section addedTo, final int offset) {
        final ByteArrayAnnotatedOutput out = new ByteArrayAnnotatedOutput();
        final ValueEncoder encoder = new ValueEncoder(addedTo.getFile(), out);
        encoder.writeArray(this.value, true);
        this.encodedForm = out.toByteArray();
        this.setWriteSize(this.encodedForm.length);
    }
    
    @Override
    public String toHuman() {
        return this.value.toHuman();
    }
    
    @Override
    public String toString() {
        return this.value.toString();
    }
    
    @Override
    protected void writeTo0(final DexFile file, final AnnotatedOutput out) {
        if (out.annotates()) {
            out.annotate(0, this.offsetString() + " call site");
            final ValueEncoder encoder = new ValueEncoder(file, out);
            encoder.writeArray(this.value, true);
        }
        else {
            out.write(this.encodedForm);
        }
    }
    
    @Override
    public ItemType itemType() {
        return ItemType.TYPE_ENCODED_ARRAY_ITEM;
    }
    
    @Override
    public void addContents(final DexFile file) {
        ValueEncoder.addContents(file, this.value);
    }
}

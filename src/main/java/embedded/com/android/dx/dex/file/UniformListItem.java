package embedded.com.android.dx.dex.file;

import java.util.*;
import embedded.com.android.dx.util.*;

public final class UniformListItem<T extends OffsettedItem> extends OffsettedItem
{
    private static final int HEADER_SIZE = 4;
    private final ItemType itemType;
    private final List<T> items;
    
    public UniformListItem(final ItemType itemType, final List<T> items) {
        super(getAlignment(items), writeSize(items));
        if (itemType == null) {
            throw new NullPointerException("itemType == null");
        }
        this.items = items;
        this.itemType = itemType;
    }
    
    private static int getAlignment(final List<? extends OffsettedItem> items) {
        try {
            return Math.max(4, ((OffsettedItem)items.get(0)).getAlignment());
        }
        catch (IndexOutOfBoundsException ex) {
            throw new IllegalArgumentException("items.size() == 0");
        }
        catch (NullPointerException ex2) {
            throw new NullPointerException("items == null");
        }
    }
    
    private static int writeSize(final List<? extends OffsettedItem> items) {
        final OffsettedItem first = (OffsettedItem)items.get(0);
        return items.size() * first.writeSize() + getAlignment(items);
    }
    
    @Override
    public ItemType itemType() {
        return this.itemType;
    }
    
    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer(100);
        sb.append(this.getClass().getName());
        sb.append(this.items);
        return sb.toString();
    }
    
    @Override
    public void addContents(final DexFile file) {
        for (final OffsettedItem i : this.items) {
            i.addContents(file);
        }
    }
    
    @Override
    public final String toHuman() {
        final StringBuffer sb = new StringBuffer(100);
        boolean first = true;
        sb.append("{");
        for (final OffsettedItem i : this.items) {
            if (first) {
                first = false;
            }
            else {
                sb.append(", ");
            }
            sb.append(i.toHuman());
        }
        sb.append("}");
        return sb.toString();
    }
    
    public final List<T> getItems() {
        return this.items;
    }
    
    @Override
    protected void place0(final Section addedTo, int offset) {
        offset += this.headerSize();
        boolean first = true;
        int theSize = -1;
        int theAlignment = -1;
        for (final OffsettedItem i : this.items) {
            final int size = i.writeSize();
            if (first) {
                theSize = size;
                theAlignment = i.getAlignment();
                first = false;
            }
            else {
                if (size != theSize) {
                    throw new UnsupportedOperationException("item size mismatch");
                }
                if (i.getAlignment() != theAlignment) {
                    throw new UnsupportedOperationException("item alignment mismatch");
                }
            }
            offset = i.place(addedTo, offset) + size;
        }
    }
    
    @Override
    protected void writeTo0(final DexFile file, final AnnotatedOutput out) {
        final int size = this.items.size();
        if (out.annotates()) {
            out.annotate(0, this.offsetString() + " " + this.typeName());
            out.annotate(4, "  size: " + Hex.u4(size));
        }
        out.writeInt(size);
        for (final OffsettedItem i : this.items) {
            i.writeTo(file, out);
        }
    }
    
    private int headerSize() {
        return this.getAlignment();
    }
}

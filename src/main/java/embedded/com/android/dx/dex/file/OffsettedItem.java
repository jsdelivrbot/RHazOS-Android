package embedded.com.android.dx.dex.file;

import embedded.com.android.dx.util.*;
import embedded.com.android.dex.util.*;

public abstract class OffsettedItem extends Item implements Comparable<OffsettedItem>
{
    private final int alignment;
    private int writeSize;
    private Section addedTo;
    private int offset;
    
    public static int getAbsoluteOffsetOr0(final OffsettedItem item) {
        if (item == null) {
            return 0;
        }
        return item.getAbsoluteOffset();
    }
    
    public OffsettedItem(final int alignment, final int writeSize) {
        Section.validateAlignment(alignment);
        if (writeSize < -1) {
            throw new IllegalArgumentException("writeSize < -1");
        }
        this.alignment = alignment;
        this.writeSize = writeSize;
        this.addedTo = null;
        this.offset = -1;
    }
    
    @Override
    public final boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        final OffsettedItem otherItem = (OffsettedItem)other;
        final ItemType thisType = this.itemType();
        final ItemType otherType = otherItem.itemType();
        return thisType == otherType && this.compareTo0(otherItem) == 0;
    }
    
    @Override
    public final int compareTo(final OffsettedItem other) {
        if (this == other) {
            return 0;
        }
        final ItemType thisType = this.itemType();
        final ItemType otherType = other.itemType();
        if (thisType != otherType) {
            return thisType.compareTo(otherType);
        }
        return this.compareTo0(other);
    }
    
    public final void setWriteSize(final int writeSize) {
        if (writeSize < 0) {
            throw new IllegalArgumentException("writeSize < 0");
        }
        if (this.writeSize >= 0) {
            throw new UnsupportedOperationException("writeSize already set");
        }
        this.writeSize = writeSize;
    }
    
    @Override
    public final int writeSize() {
        if (this.writeSize < 0) {
            throw new UnsupportedOperationException("writeSize is unknown");
        }
        return this.writeSize;
    }
    
    @Override
    public final void writeTo(final DexFile file, final AnnotatedOutput out) {
        out.alignTo(this.alignment);
        try {
            if (this.writeSize < 0) {
                throw new UnsupportedOperationException("writeSize is unknown");
            }
            out.assertCursor(this.getAbsoluteOffset());
        }
        catch (RuntimeException ex) {
            throw ExceptionWithContext.withContext(ex, "...while writing " + this);
        }
        this.writeTo0(file, out);
    }
    
    public final int getRelativeOffset() {
        if (this.offset < 0) {
            throw new RuntimeException("offset not yet known");
        }
        return this.offset;
    }
    
    public final int getAbsoluteOffset() {
        if (this.offset < 0) {
            throw new RuntimeException("offset not yet known");
        }
        return this.addedTo.getAbsoluteOffset(this.offset);
    }
    
    public final int place(final Section addedTo, int offset) {
        if (addedTo == null) {
            throw new NullPointerException("addedTo == null");
        }
        if (offset < 0) {
            throw new IllegalArgumentException("offset < 0");
        }
        if (this.addedTo != null) {
            throw new RuntimeException("already written");
        }
        final int mask = this.alignment - 1;
        offset = (offset + mask & ~mask);
        this.place0(this.addedTo = addedTo, this.offset = offset);
        return offset;
    }
    
    public final int getAlignment() {
        return this.alignment;
    }
    
    public final String offsetString() {
        return '[' + Integer.toHexString(this.getAbsoluteOffset()) + ']';
    }
    
    public abstract String toHuman();
    
    protected int compareTo0(final OffsettedItem other) {
        throw new UnsupportedOperationException("unsupported");
    }
    
    protected void place0(final Section addedTo, final int offset) {
    }
    
    protected abstract void writeTo0(final DexFile p0, final AnnotatedOutput p1);
}

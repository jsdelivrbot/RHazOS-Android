package embedded.com.android.dx.dex.file;

import embedded.com.android.dx.util.*;
import java.util.*;

public abstract class Section
{
    private final String name;
    private final DexFile file;
    private final int alignment;
    private int fileOffset;
    private boolean prepared;
    
    public static void validateAlignment(final int alignment) {
        if (alignment <= 0 || (alignment & alignment - 1) != 0x0) {
            throw new IllegalArgumentException("invalid alignment");
        }
    }
    
    public Section(final String name, final DexFile file, final int alignment) {
        if (file == null) {
            throw new NullPointerException("file == null");
        }
        validateAlignment(alignment);
        this.name = name;
        this.file = file;
        this.alignment = alignment;
        this.fileOffset = -1;
        this.prepared = false;
    }
    
    public final DexFile getFile() {
        return this.file;
    }
    
    public final int getAlignment() {
        return this.alignment;
    }
    
    public final int getFileOffset() {
        if (this.fileOffset < 0) {
            throw new RuntimeException("fileOffset not set");
        }
        return this.fileOffset;
    }
    
    public final int setFileOffset(int fileOffset) {
        if (fileOffset < 0) {
            throw new IllegalArgumentException("fileOffset < 0");
        }
        if (this.fileOffset >= 0) {
            throw new RuntimeException("fileOffset already set");
        }
        final int mask = this.alignment - 1;
        fileOffset = (fileOffset + mask & ~mask);
        return this.fileOffset = fileOffset;
    }
    
    public final void writeTo(final AnnotatedOutput out) {
        this.throwIfNotPrepared();
        this.align(out);
        final int cursor = out.getCursor();
        if (this.fileOffset < 0) {
            this.fileOffset = cursor;
        }
        else if (this.fileOffset != cursor) {
            throw new RuntimeException("alignment mismatch: for " + this + ", at " + cursor + ", but expected " + this.fileOffset);
        }
        if (out.annotates()) {
            if (this.name != null) {
                out.annotate(0, "\n" + this.name + ":");
            }
            else if (cursor != 0) {
                out.annotate(0, "\n");
            }
        }
        this.writeTo0(out);
    }
    
    public final int getAbsoluteOffset(final int relative) {
        if (relative < 0) {
            throw new IllegalArgumentException("relative < 0");
        }
        if (this.fileOffset < 0) {
            throw new RuntimeException("fileOffset not yet set");
        }
        return this.fileOffset + relative;
    }
    
    public abstract int getAbsoluteItemOffset(final Item p0);
    
    public final void prepare() {
        this.throwIfPrepared();
        this.prepare0();
        this.prepared = true;
    }
    
    public abstract Collection<? extends Item> items();
    
    protected abstract void prepare0();
    
    public abstract int writeSize();
    
    protected final void throwIfNotPrepared() {
        if (!this.prepared) {
            throw new RuntimeException("not prepared");
        }
    }
    
    protected final void throwIfPrepared() {
        if (this.prepared) {
            throw new RuntimeException("already prepared");
        }
    }
    
    protected final void align(final AnnotatedOutput out) {
        out.alignTo(this.alignment);
    }
    
    protected abstract void writeTo0(final AnnotatedOutput p0);
    
    protected final String getName() {
        return this.name;
    }
}

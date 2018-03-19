package embedded.com.android.dx.dex.file;

import embedded.com.android.dx.util.*;
import java.util.*;
import embedded.com.android.dex.util.*;

public final class MixedItemSection extends Section
{
    private static final Comparator<OffsettedItem> TYPE_SORTER;
    private final ArrayList<OffsettedItem> items;
    private final HashMap<OffsettedItem, OffsettedItem> interns;
    private final SortType sort;
    private int writeSize;
    
    public MixedItemSection(final String name, final DexFile file, final int alignment, final SortType sort) {
        super(name, file, alignment);
        this.items = new ArrayList<OffsettedItem>(100);
        this.interns = new HashMap<OffsettedItem, OffsettedItem>(100);
        this.sort = sort;
        this.writeSize = -1;
    }
    
    @Override
    public Collection<? extends Item> items() {
        return this.items;
    }
    
    @Override
    public int writeSize() {
        this.throwIfNotPrepared();
        return this.writeSize;
    }
    
    @Override
    public int getAbsoluteItemOffset(final Item item) {
        final OffsettedItem oi = (OffsettedItem)item;
        return oi.getAbsoluteOffset();
    }
    
    public int size() {
        return this.items.size();
    }
    
    public void writeHeaderPart(final AnnotatedOutput out) {
        this.throwIfNotPrepared();
        if (this.writeSize == -1) {
            throw new RuntimeException("write size not yet set");
        }
        final int sz = this.writeSize;
        final int offset = (sz == 0) ? 0 : this.getFileOffset();
        String name = this.getName();
        if (name == null) {
            name = "<unnamed>";
        }
        final int spaceCount = 15 - name.length();
        final char[] spaceArr = new char[spaceCount];
        Arrays.fill(spaceArr, ' ');
        final String spaces = new String(spaceArr);
        if (out.annotates()) {
            out.annotate(4, name + "_size:" + spaces + Hex.u4(sz));
            out.annotate(4, name + "_off: " + spaces + Hex.u4(offset));
        }
        out.writeInt(sz);
        out.writeInt(offset);
    }
    
    public void add(final OffsettedItem item) {
        this.throwIfPrepared();
        try {
            if (item.getAlignment() > this.getAlignment()) {
                throw new IllegalArgumentException("incompatible item alignment");
            }
        }
        catch (NullPointerException ex) {
            throw new NullPointerException("item == null");
        }
        this.items.add(item);
    }
    
    public synchronized <T extends OffsettedItem> T intern(final T item) {
        this.throwIfPrepared();
        final OffsettedItem result = this.interns.get(item);
        if (result != null) {
            return (T)result;
        }
        this.add(item);
        this.interns.put(item, item);
        return item;
    }
    
    public <T extends OffsettedItem> T get(final T item) {
        this.throwIfNotPrepared();
        final OffsettedItem result = this.interns.get(item);
        if (result != null) {
            return (T)result;
        }
        throw new NoSuchElementException(item.toString());
    }
    
    public void writeIndexAnnotation(final AnnotatedOutput out, final ItemType itemType, final String intro) {
        this.throwIfNotPrepared();
        final TreeMap<String, OffsettedItem> index = new TreeMap<String, OffsettedItem>();
        for (final OffsettedItem item : this.items) {
            if (item.itemType() == itemType) {
                final String label = item.toHuman();
                index.put(label, item);
            }
        }
        if (index.size() == 0) {
            return;
        }
        out.annotate(0, intro);
        for (final Map.Entry<String, OffsettedItem> entry : index.entrySet()) {
            final String label = entry.getKey();
            final OffsettedItem item2 = entry.getValue();
            out.annotate(0, item2.offsetString() + ' ' + label + '\n');
        }
    }
    
    @Override
    protected void prepare0() {
        final DexFile file = this.getFile();
        int i = 0;
        while (true) {
            final int sz = this.items.size();
            if (i >= sz) {
                break;
            }
            while (i < sz) {
                final OffsettedItem one = this.items.get(i);
                one.addContents(file);
                ++i;
            }
        }
    }
    
    public void placeItems() {
        this.throwIfNotPrepared();
        switch (this.sort) {
            case INSTANCE: {
                Collections.sort(this.items);
                break;
            }
            case TYPE: {
                Collections.sort(this.items, MixedItemSection.TYPE_SORTER);
                break;
            }
        }
        final int sz = this.items.size();
        int outAt = 0;
        for (int i = 0; i < sz; ++i) {
            final OffsettedItem one = this.items.get(i);
            try {
                final int placedAt = one.place(this, outAt);
                if (placedAt < outAt) {
                    throw new RuntimeException("bogus place() result for " + one);
                }
                outAt = placedAt + one.writeSize();
            }
            catch (RuntimeException ex) {
                throw ExceptionWithContext.withContext(ex, "...while placing " + one);
            }
        }
        this.writeSize = outAt;
    }
    
    @Override
    protected void writeTo0(final AnnotatedOutput out) {
        final boolean annotates = out.annotates();
        boolean first = true;
        final DexFile file = this.getFile();
        int at = 0;
        for (final OffsettedItem one : this.items) {
            if (annotates) {
                if (first) {
                    first = false;
                }
                else {
                    out.annotate(0, "\n");
                }
            }
            final int alignMask = one.getAlignment() - 1;
            final int writeAt = at + alignMask & ~alignMask;
            if (at != writeAt) {
                out.writeZeroes(writeAt - at);
                at = writeAt;
            }
            one.writeTo(file, out);
            at += one.writeSize();
        }
        if (at != this.writeSize) {
            throw new RuntimeException("output size mismatch");
        }
    }
    
    static {
        TYPE_SORTER = new Comparator<OffsettedItem>() {
            @Override
            public int compare(final OffsettedItem item1, final OffsettedItem item2) {
                final ItemType type1 = item1.itemType();
                final ItemType type2 = item2.itemType();
                return type1.compareTo(type2);
            }
        };
    }
    
    enum SortType
    {
        NONE, 
        TYPE, 
        INSTANCE;
    }
}

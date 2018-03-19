package embedded.com.android.dx.dex.file;

import java.util.*;
import embedded.com.android.dx.util.*;

public final class MapItem extends OffsettedItem
{
    private static final int ALIGNMENT = 4;
    private static final int WRITE_SIZE = 12;
    private final ItemType type;
    private final Section section;
    private final Item firstItem;
    private final Item lastItem;
    private final int itemCount;
    
    public static void addMap(final Section[] sections, final MixedItemSection mapSection) {
        if (sections == null) {
            throw new NullPointerException("sections == null");
        }
        if (mapSection.items().size() != 0) {
            throw new IllegalArgumentException("mapSection.items().size() != 0");
        }
        final ArrayList<MapItem> items = new ArrayList<MapItem>(50);
        for (final Section section : sections) {
            ItemType currentType = null;
            Item firstItem = null;
            Item lastItem = null;
            int count = 0;
            for (final Item item : section.items()) {
                final ItemType type = item.itemType();
                if (type != currentType) {
                    if (count != 0) {
                        items.add(new MapItem(currentType, section, firstItem, lastItem, count));
                    }
                    currentType = type;
                    firstItem = item;
                    count = 0;
                }
                lastItem = item;
                ++count;
            }
            if (count != 0) {
                items.add(new MapItem(currentType, section, firstItem, lastItem, count));
            }
            else if (section == mapSection) {
                items.add(new MapItem(mapSection));
            }
        }
        mapSection.add(new UniformListItem<MapItem>(ItemType.TYPE_MAP_LIST, items));
    }
    
    private MapItem(final ItemType type, final Section section, final Item firstItem, final Item lastItem, final int itemCount) {
        super(4, 12);
        if (type == null) {
            throw new NullPointerException("type == null");
        }
        if (section == null) {
            throw new NullPointerException("section == null");
        }
        if (firstItem == null) {
            throw new NullPointerException("firstItem == null");
        }
        if (lastItem == null) {
            throw new NullPointerException("lastItem == null");
        }
        if (itemCount <= 0) {
            throw new IllegalArgumentException("itemCount <= 0");
        }
        this.type = type;
        this.section = section;
        this.firstItem = firstItem;
        this.lastItem = lastItem;
        this.itemCount = itemCount;
    }
    
    private MapItem(final Section section) {
        super(4, 12);
        if (section == null) {
            throw new NullPointerException("section == null");
        }
        this.type = ItemType.TYPE_MAP_LIST;
        this.section = section;
        this.firstItem = null;
        this.lastItem = null;
        this.itemCount = 1;
    }
    
    @Override
    public ItemType itemType() {
        return ItemType.TYPE_MAP_ITEM;
    }
    
    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer(100);
        sb.append(this.getClass().getName());
        sb.append('{');
        sb.append(this.section.toString());
        sb.append(' ');
        sb.append(this.type.toHuman());
        sb.append('}');
        return sb.toString();
    }
    
    @Override
    public void addContents(final DexFile file) {
    }
    
    @Override
    public final String toHuman() {
        return this.toString();
    }
    
    @Override
    protected void writeTo0(final DexFile file, final AnnotatedOutput out) {
        final int value = this.type.getMapValue();
        int offset;
        if (this.firstItem == null) {
            offset = this.section.getFileOffset();
        }
        else {
            offset = this.section.getAbsoluteItemOffset(this.firstItem);
        }
        if (out.annotates()) {
            out.annotate(0, this.offsetString() + ' ' + this.type.getTypeName() + " map");
            out.annotate(2, "  type:   " + Hex.u2(value) + " // " + this.type.toString());
            out.annotate(2, "  unused: 0");
            out.annotate(4, "  size:   " + Hex.u4(this.itemCount));
            out.annotate(4, "  offset: " + Hex.u4(offset));
        }
        out.writeShort(value);
        out.writeShort(0);
        out.writeInt(this.itemCount);
        out.writeInt(offset);
    }
}

package embedded.com.android.dx.dex.file;

import embedded.com.android.dx.rop.cst.*;
import java.util.*;

public final class HeaderSection extends UniformItemSection
{
    private final List<HeaderItem> list;
    
    public HeaderSection(final DexFile file) {
        super(null, file, 4);
        final HeaderItem item = new HeaderItem();
        item.setIndex(0);
        this.list = Collections.singletonList(item);
    }
    
    @Override
    public IndexedItem get(final Constant cst) {
        return null;
    }
    
    @Override
    public Collection<? extends Item> items() {
        return this.list;
    }
    
    @Override
    protected void orderItems() {
    }
}

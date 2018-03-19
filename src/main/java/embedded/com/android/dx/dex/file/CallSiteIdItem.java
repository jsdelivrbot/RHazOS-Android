package embedded.com.android.dx.dex.file;

import embedded.com.android.dx.util.*;
import embedded.com.android.dx.rop.cst.*;

public final class CallSiteIdItem extends IndexedItem implements Comparable
{
    private static final int ITEM_SIZE = 4;
    final CstCallSiteRef invokeDynamicRef;
    CallSiteItem data;
    
    public CallSiteIdItem(final CstCallSiteRef invokeDynamicRef) {
        this.invokeDynamicRef = invokeDynamicRef;
        this.data = null;
    }
    
    @Override
    public ItemType itemType() {
        return ItemType.TYPE_CALL_SITE_ID_ITEM;
    }
    
    @Override
    public int writeSize() {
        return 4;
    }
    
    @Override
    public void addContents(final DexFile file) {
        final CstCallSite callSite = this.invokeDynamicRef.getCallSite();
        final CallSiteIdsSection callSiteIds = file.getCallSiteIds();
        CallSiteItem callSiteItem = callSiteIds.getCallSiteItem(callSite);
        if (callSiteItem == null) {
            final MixedItemSection byteData = file.getByteData();
            callSiteItem = new CallSiteItem(callSite);
            byteData.add(callSiteItem);
            callSiteIds.addCallSiteItem(callSite, callSiteItem);
        }
        this.data = callSiteItem;
    }
    
    @Override
    public void writeTo(final DexFile file, final AnnotatedOutput out) {
        final int offset = this.data.getAbsoluteOffset();
        if (out.annotates()) {
            out.annotate(0, this.indexString() + ' ' + this.invokeDynamicRef.toString());
            out.annotate(4, "call_site_off: " + Hex.u4(offset));
        }
        out.writeInt(offset);
    }
    
    @Override
    public int compareTo(final Object o) {
        final CallSiteIdItem other = (CallSiteIdItem)o;
        return this.invokeDynamicRef.compareTo((Constant)other.invokeDynamicRef);
    }
}

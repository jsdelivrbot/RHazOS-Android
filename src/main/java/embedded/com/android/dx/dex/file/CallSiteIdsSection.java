package embedded.com.android.dx.dex.file;

import embedded.com.android.dx.rop.cst.*;
import java.util.*;

public final class CallSiteIdsSection extends UniformItemSection
{
    private final TreeMap<CstCallSiteRef, CallSiteIdItem> callSiteIds;
    private final TreeMap<CstCallSite, CallSiteItem> callSites;
    
    public CallSiteIdsSection(final DexFile dexFile) {
        super("call_site_ids", dexFile, 4);
        this.callSiteIds = new TreeMap<CstCallSiteRef, CallSiteIdItem>();
        this.callSites = new TreeMap<CstCallSite, CallSiteItem>();
    }
    
    @Override
    public IndexedItem get(final Constant cst) {
        if (cst == null) {
            throw new NullPointerException("cst == null");
        }
        this.throwIfNotPrepared();
        final IndexedItem result = this.callSiteIds.get(cst);
        if (result == null) {
            throw new IllegalArgumentException("not found");
        }
        return result;
    }
    
    @Override
    protected void orderItems() {
        int index = 0;
        for (final CallSiteIdItem callSiteId : this.callSiteIds.values()) {
            callSiteId.setIndex(index++);
        }
    }
    
    @Override
    public Collection<? extends Item> items() {
        return this.callSiteIds.values();
    }
    
    public synchronized void intern(final CstCallSiteRef cstRef) {
        if (cstRef == null) {
            throw new NullPointerException("cstRef");
        }
        this.throwIfPrepared();
        CallSiteIdItem result = this.callSiteIds.get(cstRef);
        if (result == null) {
            result = new CallSiteIdItem(cstRef);
            this.callSiteIds.put(cstRef, result);
        }
    }
    
    void addCallSiteItem(final CstCallSite callSite, final CallSiteItem callSiteItem) {
        if (callSite == null) {
            throw new NullPointerException("callSite == null");
        }
        if (callSiteItem == null) {
            throw new NullPointerException("callSiteItem == null");
        }
        this.callSites.put(callSite, callSiteItem);
    }
    
    CallSiteItem getCallSiteItem(final CstCallSite callSite) {
        if (callSite == null) {
            throw new NullPointerException("callSite == null");
        }
        return this.callSites.get(callSite);
    }
}

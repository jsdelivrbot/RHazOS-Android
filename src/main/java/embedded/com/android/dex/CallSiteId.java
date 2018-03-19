package embedded.com.android.dex;

import embedded.com.android.dex.util.*;

public class CallSiteId implements Comparable<CallSiteId>
{
    private final Dex dex;
    private final int offset;
    
    public CallSiteId(final Dex dex, final int offset) {
        this.dex = dex;
        this.offset = offset;
    }
    
    @Override
    public int compareTo(final CallSiteId o) {
        return Unsigned.compare(this.offset, o.offset);
    }
    
    public int getCallSiteOffset() {
        return this.offset;
    }
    
    public void writeTo(final Dex.Section out) {
        out.writeInt(this.offset);
    }
    
    @Override
    public String toString() {
        if (this.dex == null) {
            return String.valueOf(this.offset);
        }
        return this.dex.protoIds().get(this.offset).toString();
    }
}

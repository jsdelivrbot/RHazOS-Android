package embedded.com.android.dx.dex.file;

import embedded.com.android.dex.*;
import java.util.concurrent.atomic.*;
import java.util.*;

public abstract class MemberIdsSection extends UniformItemSection
{
    public MemberIdsSection(final String name, final DexFile file) {
        super(name, file, 4);
    }
    
    @Override
    protected void orderItems() {
        int idx = 0;
        if (this.items().size() > 65536) {
            throw new DexIndexOverflowException(this.getTooManyMembersMessage());
        }
        for (final Object i : this.items()) {
            ((MemberIdItem)i).setIndex(idx);
            ++idx;
        }
    }
    
    private String getTooManyMembersMessage() {
        final Map<String, AtomicInteger> membersByPackage = new TreeMap<String, AtomicInteger>();
        for (final Object member : this.items()) {
            final String packageName = ((MemberIdItem)member).getDefiningClass().getPackageName();
            AtomicInteger count = membersByPackage.get(packageName);
            if (count == null) {
                count = new AtomicInteger();
                membersByPackage.put(packageName, count);
            }
            count.incrementAndGet();
        }
        final Formatter formatter = new Formatter();
        try {
            final String memberType = (this instanceof MethodIdsSection) ? "method" : "field";
            formatter.format("Too many %1$s references to fit in one dex file: %2$d; max is %3$d.%nYou may try using multi-dex. If multi-dex is enabled then the list of classes for the main dex list is too large.%nReferences by package:", memberType, this.items().size(), 65536);
            for (final Map.Entry<String, AtomicInteger> entry : membersByPackage.entrySet()) {
                formatter.format("%n%6d %s", entry.getValue().get(), entry.getKey());
            }
            return formatter.toString();
        }
        finally {
            formatter.close();
        }
    }
}

package embedded.com.android.dx.cf.iface;

import embedded.com.android.dx.util.*;

public final class StdAttributeList extends FixedSizeList implements AttributeList
{
    public StdAttributeList(final int size) {
        super(size);
    }
    
    @Override
    public Attribute get(final int n) {
        return (Attribute)this.get0(n);
    }
    
    @Override
    public int byteLength() {
        final int sz = this.size();
        int result = 2;
        for (int i = 0; i < sz; ++i) {
            result += this.get(i).byteLength();
        }
        return result;
    }
    
    @Override
    public Attribute findFirst(final String name) {
        for (int sz = this.size(), i = 0; i < sz; ++i) {
            final Attribute att = this.get(i);
            if (att.getName().equals(name)) {
                return att;
            }
        }
        return null;
    }
    
    @Override
    public Attribute findNext(final Attribute attrib) {
        for (int sz = this.size(), at = 0; at < sz; ++at) {
            final Attribute att = this.get(at);
            if (att == attrib) {
                final String name = attrib.getName();
                ++at;
                while (at < sz) {
                    final Attribute att2 = this.get(at);
                    if (att2.getName().equals(name)) {
                        return att2;
                    }
                    ++at;
                }
                return null;
            }
        }
        return null;
    }
    
    public void set(final int n, final Attribute attribute) {
        this.set0(n, attribute);
    }
}

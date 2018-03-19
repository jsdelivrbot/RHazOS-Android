package embedded.com.android.dx.cf.attrib;

import embedded.com.android.dx.cf.iface.*;

public abstract class BaseAttribute implements Attribute
{
    private final String name;
    
    public BaseAttribute(final String name) {
        if (name == null) {
            throw new NullPointerException("name == null");
        }
        this.name = name;
    }
    
    @Override
    public String getName() {
        return this.name;
    }
}

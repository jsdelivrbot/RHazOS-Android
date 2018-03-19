package embedded.com.android.dx.cf.attrib;

import embedded.com.android.dx.util.*;
import embedded.com.android.dx.rop.cst.*;

public final class RawAttribute extends BaseAttribute
{
    private final ByteArray data;
    private final ConstantPool pool;
    
    public RawAttribute(final String name, final ByteArray data, final ConstantPool pool) {
        super(name);
        if (data == null) {
            throw new NullPointerException("data == null");
        }
        this.data = data;
        this.pool = pool;
    }
    
    public RawAttribute(final String name, final ByteArray data, final int offset, final int length, final ConstantPool pool) {
        this(name, data.slice(offset, offset + length), pool);
    }
    
    public ByteArray getData() {
        return this.data;
    }
    
    @Override
    public int byteLength() {
        return this.data.size() + 6;
    }
    
    public ConstantPool getPool() {
        return this.pool;
    }
}

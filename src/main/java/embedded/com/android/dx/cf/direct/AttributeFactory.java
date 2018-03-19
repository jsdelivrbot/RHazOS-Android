package embedded.com.android.dx.cf.direct;

import embedded.com.android.dx.cf.iface.*;
import embedded.com.android.dx.util.*;
import embedded.com.android.dx.rop.cst.*;
import embedded.com.android.dx.cf.attrib.*;

public class AttributeFactory
{
    public static final int CTX_CLASS = 0;
    public static final int CTX_FIELD = 1;
    public static final int CTX_METHOD = 2;
    public static final int CTX_CODE = 3;
    public static final int CTX_COUNT = 4;
    
    public final Attribute parse(final DirectClassFile cf, final int context, final int offset, final ParseObserver observer) {
        if (cf == null) {
            throw new NullPointerException("cf == null");
        }
        if (context < 0 || context >= 4) {
            throw new IllegalArgumentException("bad context");
        }
        CstString name = null;
        try {
            final ByteArray bytes = cf.getBytes();
            final ConstantPool pool = cf.getConstantPool();
            final int nameIdx = bytes.getUnsignedShort(offset);
            final int length = bytes.getInt(offset + 2);
            name = (CstString)pool.get(nameIdx);
            if (observer != null) {
                observer.parsed(bytes, offset, 2, "name: " + name.toHuman());
                observer.parsed(bytes, offset + 2, 4, "length: " + Hex.u4(length));
            }
            return this.parse0(cf, context, name.getString(), offset + 6, length, observer);
        }
        catch (ParseException ex) {
            ex.addContext("...while parsing " + ((name != null) ? (name.toHuman() + " ") : "") + "attribute at offset " + Hex.u4(offset));
            throw ex;
        }
    }
    
    protected Attribute parse0(final DirectClassFile cf, final int context, final String name, final int offset, final int length, final ParseObserver observer) {
        final ByteArray bytes = cf.getBytes();
        final ConstantPool pool = cf.getConstantPool();
        final Attribute result = new RawAttribute(name, bytes, offset, length, pool);
        if (observer != null) {
            observer.parsed(bytes, offset, length, "attribute data");
        }
        return result;
    }
}

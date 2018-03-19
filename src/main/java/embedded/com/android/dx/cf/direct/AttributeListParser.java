package embedded.com.android.dx.cf.direct;

import embedded.com.android.dx.util.*;
import embedded.com.android.dx.cf.iface.*;

final class AttributeListParser
{
    private final DirectClassFile cf;
    private final int context;
    private final int offset;
    private final AttributeFactory attributeFactory;
    private final StdAttributeList list;
    private int endOffset;
    private ParseObserver observer;
    
    public AttributeListParser(final DirectClassFile cf, final int context, final int offset, final AttributeFactory attributeFactory) {
        if (cf == null) {
            throw new NullPointerException("cf == null");
        }
        if (attributeFactory == null) {
            throw new NullPointerException("attributeFactory == null");
        }
        final int size = cf.getBytes().getUnsignedShort(offset);
        this.cf = cf;
        this.context = context;
        this.offset = offset;
        this.attributeFactory = attributeFactory;
        this.list = new StdAttributeList(size);
        this.endOffset = -1;
    }
    
    public void setObserver(final ParseObserver observer) {
        this.observer = observer;
    }
    
    public int getEndOffset() {
        this.parseIfNecessary();
        return this.endOffset;
    }
    
    public StdAttributeList getList() {
        this.parseIfNecessary();
        return this.list;
    }
    
    private void parseIfNecessary() {
        if (this.endOffset < 0) {
            this.parse();
        }
    }
    
    private void parse() {
        final int sz = this.list.size();
        int at = this.offset + 2;
        final ByteArray bytes = this.cf.getBytes();
        if (this.observer != null) {
            this.observer.parsed(bytes, this.offset, 2, "attributes_count: " + Hex.u2(sz));
        }
        for (int i = 0; i < sz; ++i) {
            try {
                if (this.observer != null) {
                    this.observer.parsed(bytes, at, 0, "\nattributes[" + i + "]:\n");
                    this.observer.changeIndent(1);
                }
                final Attribute attrib = this.attributeFactory.parse(this.cf, this.context, at, this.observer);
                at += attrib.byteLength();
                this.list.set(i, attrib);
                if (this.observer != null) {
                    this.observer.changeIndent(-1);
                    this.observer.parsed(bytes, at, 0, "end attributes[" + i + "]\n");
                }
            }
            catch (ParseException ex) {
                ex.addContext("...while parsing attributes[" + i + "]");
                throw ex;
            }
            catch (RuntimeException ex2) {
                final ParseException pe = new ParseException(ex2);
                pe.addContext("...while parsing attributes[" + i + "]");
                throw pe;
            }
        }
        this.endOffset = at;
    }
}

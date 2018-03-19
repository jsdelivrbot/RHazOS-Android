package embedded.com.android.dx.cf.direct;

import embedded.com.android.dx.util.*;
import embedded.com.android.dx.rop.cst.*;
import embedded.com.android.dx.cf.iface.*;

abstract class MemberListParser
{
    private final DirectClassFile cf;
    private final CstType definer;
    private final int offset;
    private final AttributeFactory attributeFactory;
    private int endOffset;
    private ParseObserver observer;
    
    public MemberListParser(final DirectClassFile cf, final CstType definer, final int offset, final AttributeFactory attributeFactory) {
        if (cf == null) {
            throw new NullPointerException("cf == null");
        }
        if (offset < 0) {
            throw new IllegalArgumentException("offset < 0");
        }
        if (attributeFactory == null) {
            throw new NullPointerException("attributeFactory == null");
        }
        this.cf = cf;
        this.definer = definer;
        this.offset = offset;
        this.attributeFactory = attributeFactory;
        this.endOffset = -1;
    }
    
    public int getEndOffset() {
        this.parseIfNecessary();
        return this.endOffset;
    }
    
    public final void setObserver(final ParseObserver observer) {
        this.observer = observer;
    }
    
    protected final void parseIfNecessary() {
        if (this.endOffset < 0) {
            this.parse();
        }
    }
    
    protected final int getCount() {
        final ByteArray bytes = this.cf.getBytes();
        return bytes.getUnsignedShort(this.offset);
    }
    
    protected final CstType getDefiner() {
        return this.definer;
    }
    
    protected abstract String humanName();
    
    protected abstract String humanAccessFlags(final int p0);
    
    protected abstract int getAttributeContext();
    
    protected abstract Member set(final int p0, final int p1, final CstNat p2, final AttributeList p3);
    
    private void parse() {
        final int attributeContext = this.getAttributeContext();
        final int count = this.getCount();
        int at = this.offset + 2;
        final ByteArray bytes = this.cf.getBytes();
        final ConstantPool pool = this.cf.getConstantPool();
        if (this.observer != null) {
            this.observer.parsed(bytes, this.offset, 2, this.humanName() + "s_count: " + Hex.u2(count));
        }
        for (int i = 0; i < count; ++i) {
            try {
                final int accessFlags = bytes.getUnsignedShort(at);
                final int nameIdx = bytes.getUnsignedShort(at + 2);
                final int descIdx = bytes.getUnsignedShort(at + 4);
                final CstString name = (CstString)pool.get(nameIdx);
                final CstString desc = (CstString)pool.get(descIdx);
                if (this.observer != null) {
                    this.observer.startParsingMember(bytes, at, name.getString(), desc.getString());
                    this.observer.parsed(bytes, at, 0, "\n" + this.humanName() + "s[" + i + "]:\n");
                    this.observer.changeIndent(1);
                    this.observer.parsed(bytes, at, 2, "access_flags: " + this.humanAccessFlags(accessFlags));
                    this.observer.parsed(bytes, at + 2, 2, "name: " + name.toHuman());
                    this.observer.parsed(bytes, at + 4, 2, "descriptor: " + desc.toHuman());
                }
                at += 6;
                final AttributeListParser parser = new AttributeListParser(this.cf, attributeContext, at, this.attributeFactory);
                parser.setObserver(this.observer);
                at = parser.getEndOffset();
                final StdAttributeList attributes = parser.getList();
                attributes.setImmutable();
                final CstNat nat = new CstNat(name, desc);
                final Member member = this.set(i, accessFlags, nat, attributes);
                if (this.observer != null) {
                    this.observer.changeIndent(-1);
                    this.observer.parsed(bytes, at, 0, "end " + this.humanName() + "s[" + i + "]\n");
                    this.observer.endParsingMember(bytes, at, name.getString(), desc.getString(), member);
                }
            }
            catch (ParseException ex) {
                ex.addContext("...while parsing " + this.humanName() + "s[" + i + "]");
                throw ex;
            }
            catch (RuntimeException ex2) {
                final ParseException pe = new ParseException(ex2);
                pe.addContext("...while parsing " + this.humanName() + "s[" + i + "]");
                throw pe;
            }
        }
        this.endOffset = at;
    }
}

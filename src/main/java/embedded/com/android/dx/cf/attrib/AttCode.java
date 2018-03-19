package embedded.com.android.dx.cf.attrib;

import embedded.com.android.dx.cf.code.*;
import embedded.com.android.dx.cf.iface.*;
import embedded.com.android.dx.util.*;

public final class AttCode extends BaseAttribute
{
    public static final String ATTRIBUTE_NAME = "Code";
    private final int maxStack;
    private final int maxLocals;
    private final BytecodeArray code;
    private final ByteCatchList catches;
    private final AttributeList attributes;
    
    public AttCode(final int maxStack, final int maxLocals, final BytecodeArray code, final ByteCatchList catches, final AttributeList attributes) {
        super("Code");
        if (maxStack < 0) {
            throw new IllegalArgumentException("maxStack < 0");
        }
        if (maxLocals < 0) {
            throw new IllegalArgumentException("maxLocals < 0");
        }
        if (code == null) {
            throw new NullPointerException("code == null");
        }
        try {
            if (catches.isMutable()) {
                throw new MutabilityException("catches.isMutable()");
            }
        }
        catch (NullPointerException ex) {
            throw new NullPointerException("catches == null");
        }
        try {
            if (attributes.isMutable()) {
                throw new MutabilityException("attributes.isMutable()");
            }
        }
        catch (NullPointerException ex) {
            throw new NullPointerException("attributes == null");
        }
        this.maxStack = maxStack;
        this.maxLocals = maxLocals;
        this.code = code;
        this.catches = catches;
        this.attributes = attributes;
    }
    
    @Override
    public int byteLength() {
        return 10 + this.code.byteLength() + this.catches.byteLength() + this.attributes.byteLength();
    }
    
    public int getMaxStack() {
        return this.maxStack;
    }
    
    public int getMaxLocals() {
        return this.maxLocals;
    }
    
    public BytecodeArray getCode() {
        return this.code;
    }
    
    public ByteCatchList getCatches() {
        return this.catches;
    }
    
    public AttributeList getAttributes() {
        return this.attributes;
    }
}

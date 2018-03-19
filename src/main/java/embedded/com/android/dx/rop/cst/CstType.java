package embedded.com.android.dx.rop.cst;

import embedded.com.android.dx.rop.type.*;
import java.util.concurrent.*;

public final class CstType extends TypedConstant
{
    private static final ConcurrentMap<Type, CstType> interns;
    public static final CstType OBJECT;
    public static final CstType BOOLEAN;
    public static final CstType BYTE;
    public static final CstType CHARACTER;
    public static final CstType DOUBLE;
    public static final CstType FLOAT;
    public static final CstType LONG;
    public static final CstType INTEGER;
    public static final CstType SHORT;
    public static final CstType VOID;
    public static final CstType BOOLEAN_ARRAY;
    public static final CstType BYTE_ARRAY;
    public static final CstType CHAR_ARRAY;
    public static final CstType DOUBLE_ARRAY;
    public static final CstType FLOAT_ARRAY;
    public static final CstType LONG_ARRAY;
    public static final CstType INT_ARRAY;
    public static final CstType SHORT_ARRAY;
    public static final CstType METHOD_HANDLE;
    private final Type type;
    private CstString descriptor;
    
    private static void initInterns() {
        internInitial(CstType.OBJECT);
        internInitial(CstType.BOOLEAN);
        internInitial(CstType.BYTE);
        internInitial(CstType.CHARACTER);
        internInitial(CstType.DOUBLE);
        internInitial(CstType.FLOAT);
        internInitial(CstType.LONG);
        internInitial(CstType.INTEGER);
        internInitial(CstType.SHORT);
        internInitial(CstType.VOID);
        internInitial(CstType.BOOLEAN_ARRAY);
        internInitial(CstType.BYTE_ARRAY);
        internInitial(CstType.CHAR_ARRAY);
        internInitial(CstType.DOUBLE_ARRAY);
        internInitial(CstType.FLOAT_ARRAY);
        internInitial(CstType.LONG_ARRAY);
        internInitial(CstType.INT_ARRAY);
        internInitial(CstType.SHORT_ARRAY);
        internInitial(CstType.METHOD_HANDLE);
    }
    
    private static void internInitial(final CstType cst) {
        if (CstType.interns.putIfAbsent(cst.getClassType(), cst) != null) {
            throw new IllegalStateException("Attempted re-init of " + cst);
        }
    }
    
    public static CstType forBoxedPrimitiveType(final Type primitiveType) {
        switch (primitiveType.getBasicType()) {
            case 1: {
                return CstType.BOOLEAN;
            }
            case 2: {
                return CstType.BYTE;
            }
            case 3: {
                return CstType.CHARACTER;
            }
            case 4: {
                return CstType.DOUBLE;
            }
            case 5: {
                return CstType.FLOAT;
            }
            case 6: {
                return CstType.INTEGER;
            }
            case 7: {
                return CstType.LONG;
            }
            case 8: {
                return CstType.SHORT;
            }
            case 0: {
                return CstType.VOID;
            }
            default: {
                throw new IllegalArgumentException("not primitive: " + primitiveType);
            }
        }
    }
    
    public static CstType intern(final Type type) {
        final CstType cst = new CstType(type);
        final CstType result = CstType.interns.putIfAbsent(type, cst);
        return (result != null) ? result : cst;
    }
    
    public CstType(final Type type) {
        if (type == null) {
            throw new NullPointerException("type == null");
        }
        if (type == Type.KNOWN_NULL) {
            throw new UnsupportedOperationException("KNOWN_NULL is not representable");
        }
        this.type = type;
        this.descriptor = null;
    }
    
    @Override
    public boolean equals(final Object other) {
        return other instanceof CstType && this.type == ((CstType)other).type;
    }
    
    @Override
    public int hashCode() {
        return this.type.hashCode();
    }
    
    @Override
    protected int compareTo0(final Constant other) {
        final String thisDescriptor = this.type.getDescriptor();
        final String otherDescriptor = ((CstType)other).type.getDescriptor();
        return thisDescriptor.compareTo(otherDescriptor);
    }
    
    @Override
    public String toString() {
        return "type{" + this.toHuman() + '}';
    }
    
    @Override
    public Type getType() {
        return Type.CLASS;
    }
    
    @Override
    public String typeName() {
        return "type";
    }
    
    @Override
    public boolean isCategory2() {
        return false;
    }
    
    @Override
    public String toHuman() {
        return this.type.toHuman();
    }
    
    public Type getClassType() {
        return this.type;
    }
    
    public CstString getDescriptor() {
        if (this.descriptor == null) {
            this.descriptor = new CstString(this.type.getDescriptor());
        }
        return this.descriptor;
    }
    
    public String getPackageName() {
        final String descriptor = this.getDescriptor().getString();
        final int lastSlash = descriptor.lastIndexOf(47);
        final int lastLeftSquare = descriptor.lastIndexOf(91);
        if (lastSlash == -1) {
            return "default";
        }
        return descriptor.substring(lastLeftSquare + 2, lastSlash).replace('/', '.');
    }
    
    public static void clearInternTable() {
        CstType.interns.clear();
        initInterns();
    }
    
    static {
        interns = new ConcurrentHashMap<Type, CstType>(1000, 0.75f, 4);
        OBJECT = new CstType(Type.OBJECT);
        BOOLEAN = new CstType(Type.BOOLEAN_CLASS);
        BYTE = new CstType(Type.BYTE_CLASS);
        CHARACTER = new CstType(Type.CHARACTER_CLASS);
        DOUBLE = new CstType(Type.DOUBLE_CLASS);
        FLOAT = new CstType(Type.FLOAT_CLASS);
        LONG = new CstType(Type.LONG_CLASS);
        INTEGER = new CstType(Type.INTEGER_CLASS);
        SHORT = new CstType(Type.SHORT_CLASS);
        VOID = new CstType(Type.VOID_CLASS);
        BOOLEAN_ARRAY = new CstType(Type.BOOLEAN_ARRAY);
        BYTE_ARRAY = new CstType(Type.BYTE_ARRAY);
        CHAR_ARRAY = new CstType(Type.CHAR_ARRAY);
        DOUBLE_ARRAY = new CstType(Type.DOUBLE_ARRAY);
        FLOAT_ARRAY = new CstType(Type.FLOAT_ARRAY);
        LONG_ARRAY = new CstType(Type.LONG_ARRAY);
        INT_ARRAY = new CstType(Type.INT_ARRAY);
        SHORT_ARRAY = new CstType(Type.SHORT_ARRAY);
        METHOD_HANDLE = new CstType(Type.METHOD_HANDLE);
        initInterns();
    }
}

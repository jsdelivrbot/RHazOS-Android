package embedded.com.android.dx.rop.type;

import java.util.concurrent.*;

public final class Prototype implements Comparable<Prototype>
{
    private static final ConcurrentMap<String, Prototype> internTable;
    private final String descriptor;
    private final Type returnType;
    private final StdTypeList parameterTypes;
    private StdTypeList parameterFrameTypes;
    
    public static Prototype intern(final String descriptor) {
        if (descriptor == null) {
            throw new NullPointerException("descriptor == null");
        }
        Prototype result = Prototype.internTable.get(descriptor);
        if (result != null) {
            return result;
        }
        result = fromDescriptor(descriptor);
        return putIntern(result);
    }
    
    public static Prototype fromDescriptor(final String descriptor) {
        final Prototype result = Prototype.internTable.get(descriptor);
        if (result != null) {
            return result;
        }
        final Type[] params = makeParameterArray(descriptor);
        int paramCount = 0;
        int at = 1;
        while (true) {
            final int startAt = at;
            char c = descriptor.charAt(at);
            if (c == ')') {
                ++at;
                final Type returnType = Type.internReturnType(descriptor.substring(at));
                final StdTypeList parameterTypes = new StdTypeList(paramCount);
                for (int i = 0; i < paramCount; ++i) {
                    parameterTypes.set(i, params[i]);
                }
                return new Prototype(descriptor, returnType, parameterTypes);
            }
            while (c == '[') {
                ++at;
                c = descriptor.charAt(at);
            }
            if (c == 'L') {
                final int endAt = descriptor.indexOf(59, at);
                if (endAt == -1) {
                    throw new IllegalArgumentException("bad descriptor");
                }
                at = endAt + 1;
            }
            else {
                ++at;
            }
            params[paramCount] = Type.intern(descriptor.substring(startAt, at));
            ++paramCount;
        }
    }
    
    public static void clearInternTable() {
        Prototype.internTable.clear();
    }
    
    private static Type[] makeParameterArray(final String descriptor) {
        final int length = descriptor.length();
        if (descriptor.charAt(0) != '(') {
            throw new IllegalArgumentException("bad descriptor");
        }
        int closeAt = 0;
        int maxParams = 0;
        for (int i = 1; i < length; ++i) {
            final char c = descriptor.charAt(i);
            if (c == ')') {
                closeAt = i;
                break;
            }
            if (c >= 'A' && c <= 'Z') {
                ++maxParams;
            }
        }
        if (closeAt == 0 || closeAt == length - 1) {
            throw new IllegalArgumentException("bad descriptor");
        }
        if (descriptor.indexOf(41, closeAt + 1) != -1) {
            throw new IllegalArgumentException("bad descriptor");
        }
        return new Type[maxParams];
    }
    
    public static Prototype intern(final String descriptor, Type definer, final boolean isStatic, final boolean isInit) {
        final Prototype base = intern(descriptor);
        if (isStatic) {
            return base;
        }
        if (isInit) {
            definer = definer.asUninitialized(Integer.MAX_VALUE);
        }
        return base.withFirstParameter(definer);
    }
    
    public static Prototype internInts(final Type returnType, final int count) {
        final StringBuffer sb = new StringBuffer(100);
        sb.append('(');
        for (int i = 0; i < count; ++i) {
            sb.append('I');
        }
        sb.append(')');
        sb.append(returnType.getDescriptor());
        return intern(sb.toString());
    }
    
    private Prototype(final String descriptor, final Type returnType, final StdTypeList parameterTypes) {
        if (descriptor == null) {
            throw new NullPointerException("descriptor == null");
        }
        if (returnType == null) {
            throw new NullPointerException("returnType == null");
        }
        if (parameterTypes == null) {
            throw new NullPointerException("parameterTypes == null");
        }
        this.descriptor = descriptor;
        this.returnType = returnType;
        this.parameterTypes = parameterTypes;
        this.parameterFrameTypes = null;
    }
    
    @Override
    public boolean equals(final Object other) {
        return this == other || (other instanceof Prototype && this.descriptor.equals(((Prototype)other).descriptor));
    }
    
    @Override
    public int hashCode() {
        return this.descriptor.hashCode();
    }
    
    @Override
    public int compareTo(final Prototype other) {
        if (this == other) {
            return 0;
        }
        int result = this.returnType.compareTo(other.returnType);
        if (result != 0) {
            return result;
        }
        final int thisSize = this.parameterTypes.size();
        final int otherSize = other.parameterTypes.size();
        for (int size = Math.min(thisSize, otherSize), i = 0; i < size; ++i) {
            final Type thisType = this.parameterTypes.get(i);
            final Type otherType = other.parameterTypes.get(i);
            result = thisType.compareTo(otherType);
            if (result != 0) {
                return result;
            }
        }
        if (thisSize < otherSize) {
            return -1;
        }
        if (thisSize > otherSize) {
            return 1;
        }
        return 0;
    }
    
    @Override
    public String toString() {
        return this.descriptor;
    }
    
    public String getDescriptor() {
        return this.descriptor;
    }
    
    public Type getReturnType() {
        return this.returnType;
    }
    
    public StdTypeList getParameterTypes() {
        return this.parameterTypes;
    }
    
    public StdTypeList getParameterFrameTypes() {
        if (this.parameterFrameTypes == null) {
            final int sz = this.parameterTypes.size();
            final StdTypeList list = new StdTypeList(sz);
            boolean any = false;
            for (int i = 0; i < sz; ++i) {
                Type one = this.parameterTypes.get(i);
                if (one.isIntlike()) {
                    any = true;
                    one = Type.INT;
                }
                list.set(i, one);
            }
            this.parameterFrameTypes = (any ? list : this.parameterTypes);
        }
        return this.parameterFrameTypes;
    }
    
    public Prototype withFirstParameter(final Type param) {
        final String newDesc = "(" + param.getDescriptor() + this.descriptor.substring(1);
        final StdTypeList newParams = this.parameterTypes.withFirst(param);
        newParams.setImmutable();
        final Prototype result = new Prototype(newDesc, this.returnType, newParams);
        return putIntern(result);
    }
    
    private static Prototype putIntern(final Prototype desc) {
        final Prototype result = Prototype.internTable.putIfAbsent(desc.getDescriptor(), desc);
        return (result != null) ? result : desc;
    }
    
    static {
        internTable = new ConcurrentHashMap<String, Prototype>(10000, 0.75f, 4);
    }
}

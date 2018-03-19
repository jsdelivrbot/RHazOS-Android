package embedded.com.android.dx.rop.type;

import embedded.com.android.dx.util.*;
import java.util.concurrent.*;

public final class Type implements TypeBearer, Comparable<Type>
{
    private static final ConcurrentMap<String, Type> internTable;
    public static final int BT_VOID = 0;
    public static final int BT_BOOLEAN = 1;
    public static final int BT_BYTE = 2;
    public static final int BT_CHAR = 3;
    public static final int BT_DOUBLE = 4;
    public static final int BT_FLOAT = 5;
    public static final int BT_INT = 6;
    public static final int BT_LONG = 7;
    public static final int BT_SHORT = 8;
    public static final int BT_OBJECT = 9;
    public static final int BT_ADDR = 10;
    public static final int BT_COUNT = 11;
    public static final Type BOOLEAN;
    public static final Type BYTE;
    public static final Type CHAR;
    public static final Type DOUBLE;
    public static final Type FLOAT;
    public static final Type INT;
    public static final Type LONG;
    public static final Type SHORT;
    public static final Type VOID;
    public static final Type KNOWN_NULL;
    public static final Type RETURN_ADDRESS;
    public static final Type ANNOTATION;
    public static final Type CLASS;
    public static final Type CLONEABLE;
    public static final Type METHOD_HANDLE;
    public static final Type OBJECT;
    public static final Type SERIALIZABLE;
    public static final Type STRING;
    public static final Type THROWABLE;
    public static final Type BOOLEAN_CLASS;
    public static final Type BYTE_CLASS;
    public static final Type CHARACTER_CLASS;
    public static final Type DOUBLE_CLASS;
    public static final Type FLOAT_CLASS;
    public static final Type INTEGER_CLASS;
    public static final Type LONG_CLASS;
    public static final Type SHORT_CLASS;
    public static final Type VOID_CLASS;
    public static final Type BOOLEAN_ARRAY;
    public static final Type BYTE_ARRAY;
    public static final Type CHAR_ARRAY;
    public static final Type DOUBLE_ARRAY;
    public static final Type FLOAT_ARRAY;
    public static final Type INT_ARRAY;
    public static final Type LONG_ARRAY;
    public static final Type OBJECT_ARRAY;
    public static final Type SHORT_ARRAY;
    private final String descriptor;
    private final int basicType;
    private final int newAt;
    private String className;
    private Type arrayType;
    private Type componentType;
    private Type initializedType;
    
    private static void initInterns() {
        putIntern(Type.BOOLEAN);
        putIntern(Type.BYTE);
        putIntern(Type.CHAR);
        putIntern(Type.DOUBLE);
        putIntern(Type.FLOAT);
        putIntern(Type.INT);
        putIntern(Type.LONG);
        putIntern(Type.SHORT);
        putIntern(Type.ANNOTATION);
        putIntern(Type.CLASS);
        putIntern(Type.CLONEABLE);
        putIntern(Type.METHOD_HANDLE);
        putIntern(Type.OBJECT);
        putIntern(Type.SERIALIZABLE);
        putIntern(Type.STRING);
        putIntern(Type.THROWABLE);
        putIntern(Type.BOOLEAN_CLASS);
        putIntern(Type.BYTE_CLASS);
        putIntern(Type.CHARACTER_CLASS);
        putIntern(Type.DOUBLE_CLASS);
        putIntern(Type.FLOAT_CLASS);
        putIntern(Type.INTEGER_CLASS);
        putIntern(Type.LONG_CLASS);
        putIntern(Type.SHORT_CLASS);
        putIntern(Type.VOID_CLASS);
        putIntern(Type.BOOLEAN_ARRAY);
        putIntern(Type.BYTE_ARRAY);
        putIntern(Type.CHAR_ARRAY);
        putIntern(Type.DOUBLE_ARRAY);
        putIntern(Type.FLOAT_ARRAY);
        putIntern(Type.INT_ARRAY);
        putIntern(Type.LONG_ARRAY);
        putIntern(Type.OBJECT_ARRAY);
        putIntern(Type.SHORT_ARRAY);
    }
    
    public static Type intern(final String descriptor) {
        Type result = Type.internTable.get(descriptor);
        if (result != null) {
            return result;
        }
        char firstChar;
        try {
            firstChar = descriptor.charAt(0);
        }
        catch (IndexOutOfBoundsException ex) {
            throw new IllegalArgumentException("descriptor is empty");
        }
        catch (NullPointerException ex2) {
            throw new NullPointerException("descriptor == null");
        }
        if (firstChar == '[') {
            result = intern(descriptor.substring(1));
            return result.getArrayType();
        }
        final int length = descriptor.length();
        if (firstChar != 'L' || descriptor.charAt(length - 1) != ';') {
            throw new IllegalArgumentException("bad descriptor: " + descriptor);
        }
        for (int limit = length - 1, i = 1; i < limit; ++i) {
            final char c = descriptor.charAt(i);
            switch (c) {
                case '(':
                case ')':
                case '.':
                case ';':
                case '[': {
                    throw new IllegalArgumentException("bad descriptor: " + descriptor);
                }
                case '/': {
                    if (i == 1 || i == length - 1 || descriptor.charAt(i - 1) == '/') {
                        throw new IllegalArgumentException("bad descriptor: " + descriptor);
                    }
                    break;
                }
            }
        }
        result = new Type(descriptor, 9);
        return putIntern(result);
    }
    
    public static Type internReturnType(final String descriptor) {
        try {
            if (descriptor.equals("V")) {
                return Type.VOID;
            }
        }
        catch (NullPointerException ex) {
            throw new NullPointerException("descriptor == null");
        }
        return intern(descriptor);
    }
    
    public static Type internClassName(final String name) {
        if (name == null) {
            throw new NullPointerException("name == null");
        }
        if (name.startsWith("[")) {
            return intern(name);
        }
        return intern('L' + name + ';');
    }
    
    private Type(final String descriptor, final int basicType, final int newAt) {
        if (descriptor == null) {
            throw new NullPointerException("descriptor == null");
        }
        if (basicType < 0 || basicType >= 11) {
            throw new IllegalArgumentException("bad basicType");
        }
        if (newAt < -1) {
            throw new IllegalArgumentException("newAt < -1");
        }
        this.descriptor = descriptor;
        this.basicType = basicType;
        this.newAt = newAt;
        this.arrayType = null;
        this.componentType = null;
        this.initializedType = null;
    }
    
    private Type(final String descriptor, final int basicType) {
        this(descriptor, basicType, -1);
    }
    
    @Override
    public boolean equals(final Object other) {
        return this == other || (other instanceof Type && this.descriptor.equals(((Type)other).descriptor));
    }
    
    @Override
    public int hashCode() {
        return this.descriptor.hashCode();
    }
    
    @Override
    public int compareTo(final Type other) {
        return this.descriptor.compareTo(other.descriptor);
    }
    
    @Override
    public String toString() {
        return this.descriptor;
    }
    
    @Override
    public String toHuman() {
        switch (this.basicType) {
            case 0: {
                return "void";
            }
            case 1: {
                return "boolean";
            }
            case 2: {
                return "byte";
            }
            case 3: {
                return "char";
            }
            case 4: {
                return "double";
            }
            case 5: {
                return "float";
            }
            case 6: {
                return "int";
            }
            case 7: {
                return "long";
            }
            case 8: {
                return "short";
            }
            case 9: {
                if (this.isArray()) {
                    return this.getComponentType().toHuman() + "[]";
                }
                return this.getClassName().replace("/", ".");
            }
            default: {
                return this.descriptor;
            }
        }
    }
    
    @Override
    public Type getType() {
        return this;
    }
    
    @Override
    public Type getFrameType() {
        switch (this.basicType) {
            case 1:
            case 2:
            case 3:
            case 6:
            case 8: {
                return Type.INT;
            }
            default: {
                return this;
            }
        }
    }
    
    @Override
    public int getBasicType() {
        return this.basicType;
    }
    
    @Override
    public int getBasicFrameType() {
        switch (this.basicType) {
            case 1:
            case 2:
            case 3:
            case 6:
            case 8: {
                return 6;
            }
            default: {
                return this.basicType;
            }
        }
    }
    
    @Override
    public boolean isConstant() {
        return false;
    }
    
    public String getDescriptor() {
        return this.descriptor;
    }
    
    public String getClassName() {
        if (this.className == null) {
            if (!this.isReference()) {
                throw new IllegalArgumentException("not an object type: " + this.descriptor);
            }
            if (this.descriptor.charAt(0) == '[') {
                this.className = this.descriptor;
            }
            else {
                this.className = this.descriptor.substring(1, this.descriptor.length() - 1);
            }
        }
        return this.className;
    }
    
    public int getCategory() {
        switch (this.basicType) {
            case 4:
            case 7: {
                return 2;
            }
            default: {
                return 1;
            }
        }
    }
    
    public boolean isCategory1() {
        switch (this.basicType) {
            case 4:
            case 7: {
                return false;
            }
            default: {
                return true;
            }
        }
    }
    
    public boolean isCategory2() {
        switch (this.basicType) {
            case 4:
            case 7: {
                return true;
            }
            default: {
                return false;
            }
        }
    }
    
    public boolean isIntlike() {
        switch (this.basicType) {
            case 1:
            case 2:
            case 3:
            case 6:
            case 8: {
                return true;
            }
            default: {
                return false;
            }
        }
    }
    
    public boolean isPrimitive() {
        switch (this.basicType) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
            case 8: {
                return true;
            }
            default: {
                return false;
            }
        }
    }
    
    public boolean isReference() {
        return this.basicType == 9;
    }
    
    public boolean isArray() {
        return this.descriptor.charAt(0) == '[';
    }
    
    public boolean isArrayOrKnownNull() {
        return this.isArray() || this.equals(Type.KNOWN_NULL);
    }
    
    public boolean isUninitialized() {
        return this.newAt >= 0;
    }
    
    public int getNewAt() {
        return this.newAt;
    }
    
    public Type getInitializedType() {
        if (this.initializedType == null) {
            throw new IllegalArgumentException("initialized type: " + this.descriptor);
        }
        return this.initializedType;
    }
    
    public Type getArrayType() {
        if (this.arrayType == null) {
            this.arrayType = putIntern(new Type('[' + this.descriptor, 9));
        }
        return this.arrayType;
    }
    
    public Type getComponentType() {
        if (this.componentType == null) {
            if (this.descriptor.charAt(0) != '[') {
                throw new IllegalArgumentException("not an array type: " + this.descriptor);
            }
            this.componentType = intern(this.descriptor.substring(1));
        }
        return this.componentType;
    }
    
    public Type asUninitialized(final int newAt) {
        if (newAt < 0) {
            throw new IllegalArgumentException("newAt < 0");
        }
        if (!this.isReference()) {
            throw new IllegalArgumentException("not a reference type: " + this.descriptor);
        }
        if (this.isUninitialized()) {
            throw new IllegalArgumentException("already uninitialized: " + this.descriptor);
        }
        final String newDesc = 'N' + Hex.u2(newAt) + this.descriptor;
        final Type result = new Type(newDesc, 9, newAt);
        result.initializedType = this;
        return putIntern(result);
    }
    
    private static Type putIntern(final Type type) {
        final Type result = Type.internTable.putIfAbsent(type.getDescriptor(), type);
        return (result != null) ? result : type;
    }
    
    public static void clearInternTable() {
        Type.internTable.clear();
        initInterns();
    }
    
    static {
        internTable = new ConcurrentHashMap<String, Type>(10000, 0.75f, 4);
        BOOLEAN = new Type("Z", 1);
        BYTE = new Type("B", 2);
        CHAR = new Type("C", 3);
        DOUBLE = new Type("D", 4);
        FLOAT = new Type("F", 5);
        INT = new Type("I", 6);
        LONG = new Type("J", 7);
        SHORT = new Type("S", 8);
        VOID = new Type("V", 0);
        KNOWN_NULL = new Type("<null>", 9);
        RETURN_ADDRESS = new Type("<addr>", 10);
        ANNOTATION = new Type("Ljava/lang/annotation/Annotation;", 9);
        CLASS = new Type("Ljava/lang/Class;", 9);
        CLONEABLE = new Type("Ljava/lang/Cloneable;", 9);
        METHOD_HANDLE = new Type("Ljava/lang/invoke/MethodHandle;", 9);
        OBJECT = new Type("Ljava/lang/Object;", 9);
        SERIALIZABLE = new Type("Ljava/io/Serializable;", 9);
        STRING = new Type("Ljava/lang/String;", 9);
        THROWABLE = new Type("Ljava/lang/Throwable;", 9);
        BOOLEAN_CLASS = new Type("Ljava/lang/Boolean;", 9);
        BYTE_CLASS = new Type("Ljava/lang/Byte;", 9);
        CHARACTER_CLASS = new Type("Ljava/lang/Character;", 9);
        DOUBLE_CLASS = new Type("Ljava/lang/Double;", 9);
        FLOAT_CLASS = new Type("Ljava/lang/Float;", 9);
        INTEGER_CLASS = new Type("Ljava/lang/Integer;", 9);
        LONG_CLASS = new Type("Ljava/lang/Long;", 9);
        SHORT_CLASS = new Type("Ljava/lang/Short;", 9);
        VOID_CLASS = new Type("Ljava/lang/Void;", 9);
        BOOLEAN_ARRAY = new Type("[" + Type.BOOLEAN.descriptor, 9);
        BYTE_ARRAY = new Type("[" + Type.BYTE.descriptor, 9);
        CHAR_ARRAY = new Type("[" + Type.CHAR.descriptor, 9);
        DOUBLE_ARRAY = new Type("[" + Type.DOUBLE.descriptor, 9);
        FLOAT_ARRAY = new Type("[" + Type.FLOAT.descriptor, 9);
        INT_ARRAY = new Type("[" + Type.INT.descriptor, 9);
        LONG_ARRAY = new Type("[" + Type.LONG.descriptor, 9);
        OBJECT_ARRAY = new Type("[" + Type.OBJECT.descriptor, 9);
        SHORT_ARRAY = new Type("[" + Type.SHORT.descriptor, 9);
        initInterns();
    }
}

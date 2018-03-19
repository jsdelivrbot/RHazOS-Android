package embedded.com.android.dx.cf.direct;

import embedded.com.android.dx.util.*;
import embedded.com.android.dx.rop.type.*;
import embedded.com.android.dx.rop.code.*;
import java.io.*;
import embedded.com.android.dx.rop.annotation.*;
import embedded.com.android.dx.cf.attrib.*;
import embedded.com.android.dx.cf.iface.*;
import embedded.com.android.dx.cf.code.*;
import embedded.com.android.dx.rop.cst.*;

public class StdAttributeFactory extends AttributeFactory
{
    public static final StdAttributeFactory THE_ONE;
    
    @Override
    protected Attribute parse0(final DirectClassFile cf, final int context, final String name, final int offset, final int length, final ParseObserver observer) {
        switch (context) {
            case 0: {
                if (name == "BootstrapMethods") {
                    return this.bootstrapMethods(cf, offset, length, observer);
                }
                if (name == "Deprecated") {
                    return this.deprecated(cf, offset, length, observer);
                }
                if (name == "EnclosingMethod") {
                    return this.enclosingMethod(cf, offset, length, observer);
                }
                if (name == "InnerClasses") {
                    return this.innerClasses(cf, offset, length, observer);
                }
                if (name == "RuntimeInvisibleAnnotations") {
                    return this.runtimeInvisibleAnnotations(cf, offset, length, observer);
                }
                if (name == "RuntimeVisibleAnnotations") {
                    return this.runtimeVisibleAnnotations(cf, offset, length, observer);
                }
                if (name == "Synthetic") {
                    return this.synthetic(cf, offset, length, observer);
                }
                if (name == "Signature") {
                    return this.signature(cf, offset, length, observer);
                }
                if (name == "SourceDebugExtension") {
                    return this.sourceDebugExtension(cf, offset, length, observer);
                }
                if (name == "SourceFile") {
                    return this.sourceFile(cf, offset, length, observer);
                }
                break;
            }
            case 1: {
                if (name == "ConstantValue") {
                    return this.constantValue(cf, offset, length, observer);
                }
                if (name == "Deprecated") {
                    return this.deprecated(cf, offset, length, observer);
                }
                if (name == "RuntimeInvisibleAnnotations") {
                    return this.runtimeInvisibleAnnotations(cf, offset, length, observer);
                }
                if (name == "RuntimeVisibleAnnotations") {
                    return this.runtimeVisibleAnnotations(cf, offset, length, observer);
                }
                if (name == "Signature") {
                    return this.signature(cf, offset, length, observer);
                }
                if (name == "Synthetic") {
                    return this.synthetic(cf, offset, length, observer);
                }
                break;
            }
            case 2: {
                if (name == "AnnotationDefault") {
                    return this.annotationDefault(cf, offset, length, observer);
                }
                if (name == "Code") {
                    return this.code(cf, offset, length, observer);
                }
                if (name == "Deprecated") {
                    return this.deprecated(cf, offset, length, observer);
                }
                if (name == "Exceptions") {
                    return this.exceptions(cf, offset, length, observer);
                }
                if (name == "RuntimeInvisibleAnnotations") {
                    return this.runtimeInvisibleAnnotations(cf, offset, length, observer);
                }
                if (name == "RuntimeVisibleAnnotations") {
                    return this.runtimeVisibleAnnotations(cf, offset, length, observer);
                }
                if (name == "RuntimeInvisibleParameterAnnotations") {
                    return this.runtimeInvisibleParameterAnnotations(cf, offset, length, observer);
                }
                if (name == "RuntimeVisibleParameterAnnotations") {
                    return this.runtimeVisibleParameterAnnotations(cf, offset, length, observer);
                }
                if (name == "Signature") {
                    return this.signature(cf, offset, length, observer);
                }
                if (name == "Synthetic") {
                    return this.synthetic(cf, offset, length, observer);
                }
                break;
            }
            case 3: {
                if (name == "LineNumberTable") {
                    return this.lineNumberTable(cf, offset, length, observer);
                }
                if (name == "LocalVariableTable") {
                    return this.localVariableTable(cf, offset, length, observer);
                }
                if (name == "LocalVariableTypeTable") {
                    return this.localVariableTypeTable(cf, offset, length, observer);
                }
                break;
            }
        }
        return super.parse0(cf, context, name, offset, length, observer);
    }
    
    private Attribute annotationDefault(final DirectClassFile cf, final int offset, final int length, final ParseObserver observer) {
        if (length < 2) {
            throwSeverelyTruncated();
        }
        final AnnotationParser ap = new AnnotationParser(cf, offset, length, observer);
        final Constant cst = ap.parseValueAttribute();
        return new AttAnnotationDefault(cst, length);
    }
    
    private Attribute bootstrapMethods(final DirectClassFile cf, int offset, int length, final ParseObserver observer) {
        if (length < 2) {
            return throwSeverelyTruncated();
        }
        final ByteArray bytes = cf.getBytes();
        final int numMethods = bytes.getUnsignedShort(offset);
        if (observer != null) {
            observer.parsed(bytes, offset, 2, "num_boostrap_methods: " + Hex.u2(numMethods));
        }
        offset += 2;
        length -= 2;
        final BootstrapMethodsList methods = this.parseBootstrapMethods(bytes, cf.getConstantPool(), cf.getThisClass(), numMethods, offset, length, observer);
        return new AttBootstrapMethods(methods);
    }
    
    private Attribute code(final DirectClassFile cf, int offset, int length, final ParseObserver observer) {
        if (length < 12) {
            return throwSeverelyTruncated();
        }
        final ByteArray bytes = cf.getBytes();
        final ConstantPool pool = cf.getConstantPool();
        final int maxStack = bytes.getUnsignedShort(offset);
        final int maxLocals = bytes.getUnsignedShort(offset + 2);
        final int codeLength = bytes.getInt(offset + 4);
        final int origOffset = offset;
        if (observer != null) {
            observer.parsed(bytes, offset, 2, "max_stack: " + Hex.u2(maxStack));
            observer.parsed(bytes, offset + 2, 2, "max_locals: " + Hex.u2(maxLocals));
            observer.parsed(bytes, offset + 4, 4, "code_length: " + Hex.u4(codeLength));
        }
        offset += 8;
        length -= 8;
        if (length < codeLength + 4) {
            return throwTruncated();
        }
        final int codeOffset = offset;
        offset += codeLength;
        length -= codeLength;
        final BytecodeArray code = new BytecodeArray(bytes.slice(codeOffset, codeOffset + codeLength), pool);
        if (observer != null) {
            code.forEach(new CodeObserver(code.getBytes(), observer));
        }
        final int exceptionTableLength = bytes.getUnsignedShort(offset);
        final ByteCatchList catches = (exceptionTableLength == 0) ? ByteCatchList.EMPTY : new ByteCatchList(exceptionTableLength);
        if (observer != null) {
            observer.parsed(bytes, offset, 2, "exception_table_length: " + Hex.u2(exceptionTableLength));
        }
        offset += 2;
        length -= 2;
        if (length < exceptionTableLength * 8 + 2) {
            return throwTruncated();
        }
        for (int i = 0; i < exceptionTableLength; ++i) {
            if (observer != null) {
                observer.changeIndent(1);
            }
            final int startPc = bytes.getUnsignedShort(offset);
            final int endPc = bytes.getUnsignedShort(offset + 2);
            final int handlerPc = bytes.getUnsignedShort(offset + 4);
            final int catchTypeIdx = bytes.getUnsignedShort(offset + 6);
            final CstType catchType = (CstType)pool.get0Ok(catchTypeIdx);
            catches.set(i, startPc, endPc, handlerPc, catchType);
            if (observer != null) {
                observer.parsed(bytes, offset, 8, Hex.u2(startPc) + ".." + Hex.u2(endPc) + " -> " + Hex.u2(handlerPc) + " " + ((catchType == null) ? "<any>" : catchType.toHuman()));
            }
            offset += 8;
            length -= 8;
            if (observer != null) {
                observer.changeIndent(-1);
            }
        }
        catches.setImmutable();
        final AttributeListParser parser = new AttributeListParser(cf, 3, offset, this);
        parser.setObserver(observer);
        final StdAttributeList attributes = parser.getList();
        attributes.setImmutable();
        final int attributeByteCount = parser.getEndOffset() - offset;
        if (attributeByteCount != length) {
            return throwBadLength(attributeByteCount + (offset - origOffset));
        }
        return new AttCode(maxStack, maxLocals, code, catches, attributes);
    }
    
    private Attribute constantValue(final DirectClassFile cf, final int offset, final int length, final ParseObserver observer) {
        if (length != 2) {
            return throwBadLength(2);
        }
        final ByteArray bytes = cf.getBytes();
        final ConstantPool pool = cf.getConstantPool();
        final int idx = bytes.getUnsignedShort(offset);
        final TypedConstant cst = (TypedConstant)pool.get(idx);
        final Attribute result = new AttConstantValue(cst);
        if (observer != null) {
            observer.parsed(bytes, offset, 2, "value: " + cst);
        }
        return result;
    }
    
    private Attribute deprecated(final DirectClassFile cf, final int offset, final int length, final ParseObserver observer) {
        if (length != 0) {
            return throwBadLength(0);
        }
        return new AttDeprecated();
    }
    
    private Attribute enclosingMethod(final DirectClassFile cf, final int offset, final int length, final ParseObserver observer) {
        if (length != 4) {
            throwBadLength(4);
        }
        final ByteArray bytes = cf.getBytes();
        final ConstantPool pool = cf.getConstantPool();
        int idx = bytes.getUnsignedShort(offset);
        final CstType type = (CstType)pool.get(idx);
        idx = bytes.getUnsignedShort(offset + 2);
        final CstNat method = (CstNat)pool.get0Ok(idx);
        final Attribute result = new AttEnclosingMethod(type, method);
        if (observer != null) {
            observer.parsed(bytes, offset, 2, "class: " + type);
            observer.parsed(bytes, offset + 2, 2, "method: " + DirectClassFile.stringOrNone(method));
        }
        return result;
    }
    
    private Attribute exceptions(final DirectClassFile cf, int offset, int length, final ParseObserver observer) {
        if (length < 2) {
            return throwSeverelyTruncated();
        }
        final ByteArray bytes = cf.getBytes();
        final int count = bytes.getUnsignedShort(offset);
        if (observer != null) {
            observer.parsed(bytes, offset, 2, "number_of_exceptions: " + Hex.u2(count));
        }
        offset += 2;
        length -= 2;
        if (length != count * 2) {
            throwBadLength(count * 2 + 2);
        }
        final TypeList list = cf.makeTypeList(offset, count);
        return new AttExceptions(list);
    }
    
    private Attribute innerClasses(final DirectClassFile cf, int offset, int length, final ParseObserver observer) {
        if (length < 2) {
            return throwSeverelyTruncated();
        }
        final ByteArray bytes = cf.getBytes();
        final ConstantPool pool = cf.getConstantPool();
        final int count = bytes.getUnsignedShort(offset);
        if (observer != null) {
            observer.parsed(bytes, offset, 2, "number_of_classes: " + Hex.u2(count));
        }
        offset += 2;
        length -= 2;
        if (length != count * 8) {
            throwBadLength(count * 8 + 2);
        }
        final InnerClassList list = new InnerClassList(count);
        for (int i = 0; i < count; ++i) {
            final int innerClassIdx = bytes.getUnsignedShort(offset);
            final int outerClassIdx = bytes.getUnsignedShort(offset + 2);
            final int nameIdx = bytes.getUnsignedShort(offset + 4);
            final int accessFlags = bytes.getUnsignedShort(offset + 6);
            final CstType innerClass = (CstType)pool.get(innerClassIdx);
            final CstType outerClass = (CstType)pool.get0Ok(outerClassIdx);
            final CstString name = (CstString)pool.get0Ok(nameIdx);
            list.set(i, innerClass, outerClass, name, accessFlags);
            if (observer != null) {
                observer.parsed(bytes, offset, 2, "inner_class: " + DirectClassFile.stringOrNone(innerClass));
                observer.parsed(bytes, offset + 2, 2, "  outer_class: " + DirectClassFile.stringOrNone(outerClass));
                observer.parsed(bytes, offset + 4, 2, "  name: " + DirectClassFile.stringOrNone(name));
                observer.parsed(bytes, offset + 6, 2, "  access_flags: " + AccessFlags.innerClassString(accessFlags));
            }
            offset += 8;
        }
        list.setImmutable();
        return new AttInnerClasses(list);
    }
    
    private Attribute lineNumberTable(final DirectClassFile cf, int offset, int length, final ParseObserver observer) {
        if (length < 2) {
            return throwSeverelyTruncated();
        }
        final ByteArray bytes = cf.getBytes();
        final int count = bytes.getUnsignedShort(offset);
        if (observer != null) {
            observer.parsed(bytes, offset, 2, "line_number_table_length: " + Hex.u2(count));
        }
        offset += 2;
        length -= 2;
        if (length != count * 4) {
            throwBadLength(count * 4 + 2);
        }
        final LineNumberList list = new LineNumberList(count);
        for (int i = 0; i < count; ++i) {
            final int startPc = bytes.getUnsignedShort(offset);
            final int lineNumber = bytes.getUnsignedShort(offset + 2);
            list.set(i, startPc, lineNumber);
            if (observer != null) {
                observer.parsed(bytes, offset, 4, Hex.u2(startPc) + " " + lineNumber);
            }
            offset += 4;
        }
        list.setImmutable();
        return new AttLineNumberTable(list);
    }
    
    private Attribute localVariableTable(final DirectClassFile cf, final int offset, final int length, final ParseObserver observer) {
        if (length < 2) {
            return throwSeverelyTruncated();
        }
        final ByteArray bytes = cf.getBytes();
        final int count = bytes.getUnsignedShort(offset);
        if (observer != null) {
            observer.parsed(bytes, offset, 2, "local_variable_table_length: " + Hex.u2(count));
        }
        final LocalVariableList list = this.parseLocalVariables(bytes.slice(offset + 2, offset + length), cf.getConstantPool(), observer, count, false);
        return new AttLocalVariableTable(list);
    }
    
    private Attribute localVariableTypeTable(final DirectClassFile cf, final int offset, final int length, final ParseObserver observer) {
        if (length < 2) {
            return throwSeverelyTruncated();
        }
        final ByteArray bytes = cf.getBytes();
        final int count = bytes.getUnsignedShort(offset);
        if (observer != null) {
            observer.parsed(bytes, offset, 2, "local_variable_type_table_length: " + Hex.u2(count));
        }
        final LocalVariableList list = this.parseLocalVariables(bytes.slice(offset + 2, offset + length), cf.getConstantPool(), observer, count, true);
        return new AttLocalVariableTypeTable(list);
    }
    
    private LocalVariableList parseLocalVariables(final ByteArray bytes, final ConstantPool pool, final ParseObserver observer, final int count, final boolean typeTable) {
        if (bytes.size() != count * 10) {
            throwBadLength(count * 10 + 2);
        }
        final ByteArray.MyDataInputStream in = bytes.makeDataInputStream();
        final LocalVariableList list = new LocalVariableList(count);
        try {
            for (int i = 0; i < count; ++i) {
                final int startPc = in.readUnsignedShort();
                final int length = in.readUnsignedShort();
                final int nameIdx = in.readUnsignedShort();
                final int typeIdx = in.readUnsignedShort();
                final int index = in.readUnsignedShort();
                final CstString name = (CstString)pool.get(nameIdx);
                final CstString type = (CstString)pool.get(typeIdx);
                CstString descriptor = null;
                CstString signature = null;
                if (typeTable) {
                    signature = type;
                }
                else {
                    descriptor = type;
                }
                list.set(i, startPc, length, name, descriptor, signature, index);
                if (observer != null) {
                    observer.parsed(bytes, i * 10, 10, Hex.u2(startPc) + ".." + Hex.u2(startPc + length) + " " + Hex.u2(index) + " " + name.toHuman() + " " + type.toHuman());
                }
            }
        }
        catch (IOException ex) {
            throw new RuntimeException("shouldn't happen", ex);
        }
        list.setImmutable();
        return list;
    }
    
    private Attribute runtimeInvisibleAnnotations(final DirectClassFile cf, final int offset, final int length, final ParseObserver observer) {
        if (length < 2) {
            throwSeverelyTruncated();
        }
        final AnnotationParser ap = new AnnotationParser(cf, offset, length, observer);
        final Annotations annotations = ap.parseAnnotationAttribute(AnnotationVisibility.BUILD);
        return new AttRuntimeInvisibleAnnotations(annotations, length);
    }
    
    private Attribute runtimeVisibleAnnotations(final DirectClassFile cf, final int offset, final int length, final ParseObserver observer) {
        if (length < 2) {
            throwSeverelyTruncated();
        }
        final AnnotationParser ap = new AnnotationParser(cf, offset, length, observer);
        final Annotations annotations = ap.parseAnnotationAttribute(AnnotationVisibility.RUNTIME);
        return new AttRuntimeVisibleAnnotations(annotations, length);
    }
    
    private Attribute runtimeInvisibleParameterAnnotations(final DirectClassFile cf, final int offset, final int length, final ParseObserver observer) {
        if (length < 2) {
            throwSeverelyTruncated();
        }
        final AnnotationParser ap = new AnnotationParser(cf, offset, length, observer);
        final AnnotationsList list = ap.parseParameterAttribute(AnnotationVisibility.BUILD);
        return new AttRuntimeInvisibleParameterAnnotations(list, length);
    }
    
    private Attribute runtimeVisibleParameterAnnotations(final DirectClassFile cf, final int offset, final int length, final ParseObserver observer) {
        if (length < 2) {
            throwSeverelyTruncated();
        }
        final AnnotationParser ap = new AnnotationParser(cf, offset, length, observer);
        final AnnotationsList list = ap.parseParameterAttribute(AnnotationVisibility.RUNTIME);
        return new AttRuntimeVisibleParameterAnnotations(list, length);
    }
    
    private Attribute signature(final DirectClassFile cf, final int offset, final int length, final ParseObserver observer) {
        if (length != 2) {
            throwBadLength(2);
        }
        final ByteArray bytes = cf.getBytes();
        final ConstantPool pool = cf.getConstantPool();
        final int idx = bytes.getUnsignedShort(offset);
        final CstString cst = (CstString)pool.get(idx);
        final Attribute result = new AttSignature(cst);
        if (observer != null) {
            observer.parsed(bytes, offset, 2, "signature: " + cst);
        }
        return result;
    }
    
    private Attribute sourceDebugExtension(final DirectClassFile cf, final int offset, final int length, final ParseObserver observer) {
        final ByteArray bytes = cf.getBytes().slice(offset, offset + length);
        final CstString smapString = new CstString(bytes);
        final Attribute result = new AttSourceDebugExtension(smapString);
        if (observer != null) {
            final String decoded = smapString.getString();
            observer.parsed(bytes, offset, length, "sourceDebugExtension: " + decoded);
        }
        return result;
    }
    
    private Attribute sourceFile(final DirectClassFile cf, final int offset, final int length, final ParseObserver observer) {
        if (length != 2) {
            throwBadLength(2);
        }
        final ByteArray bytes = cf.getBytes();
        final ConstantPool pool = cf.getConstantPool();
        final int idx = bytes.getUnsignedShort(offset);
        final CstString cst = (CstString)pool.get(idx);
        final Attribute result = new AttSourceFile(cst);
        if (observer != null) {
            observer.parsed(bytes, offset, 2, "source: " + cst);
        }
        return result;
    }
    
    private Attribute synthetic(final DirectClassFile cf, final int offset, final int length, final ParseObserver observer) {
        if (length != 0) {
            return throwBadLength(0);
        }
        return new AttSynthetic();
    }
    
    private static Attribute throwSeverelyTruncated() {
        throw new ParseException("severely truncated attribute");
    }
    
    private static Attribute throwTruncated() {
        throw new ParseException("truncated attribute");
    }
    
    private static Attribute throwBadLength(final int expected) {
        throw new ParseException("bad attribute length; expected length " + Hex.u4(expected));
    }
    
    private BootstrapMethodsList parseBootstrapMethods(final ByteArray bytes, final ConstantPool constantPool, final CstType declaringClass, final int numMethods, int offset, int length, final ParseObserver observer) throws ParseException {
        final BootstrapMethodsList methods = new BootstrapMethodsList(numMethods);
        for (int methodIndex = 0; methodIndex < numMethods; ++methodIndex) {
            if (length < 4) {
                throwTruncated();
            }
            final int methodRef = bytes.getUnsignedShort(offset);
            final int numArguments = bytes.getUnsignedShort(offset + 2);
            if (observer != null) {
                observer.parsed(bytes, offset, 2, "bootstrap_method_ref: " + Hex.u2(methodRef));
                observer.parsed(bytes, offset + 2, 2, "num_bootstrap_arguments: " + Hex.u2(numArguments));
            }
            offset += 4;
            length -= 4;
            if (length < numArguments * 2) {
                throwTruncated();
            }
            final BootstrapMethodArgumentsList arguments = new BootstrapMethodArgumentsList(numArguments);
            for (int argIndex = 0; argIndex < numArguments; ++argIndex, offset += 2, length -= 2) {
                final int argumentRef = bytes.getUnsignedShort(offset);
                if (observer != null) {
                    observer.parsed(bytes, offset, 2, "bootstrap_arguments[" + argIndex + "]" + Hex.u2(argumentRef));
                }
                arguments.set(argIndex, constantPool.get(argumentRef));
            }
            arguments.setImmutable();
            final Constant cstMethodRef = constantPool.get(methodRef);
            methods.set(methodIndex, declaringClass, (CstMethodHandle)cstMethodRef, arguments);
        }
        methods.setImmutable();
        if (length != 0) {
            throwBadLength(length);
        }
        return methods;
    }
    
    static {
        THE_ONE = new StdAttributeFactory();
    }
}

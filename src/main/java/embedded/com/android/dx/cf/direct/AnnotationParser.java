package embedded.com.android.dx.cf.direct;

import embedded.com.android.dx.cf.iface.*;
import java.io.*;
import embedded.com.android.dx.util.*;
import embedded.com.android.dx.rop.type.*;
import embedded.com.android.dx.rop.annotation.*;
import embedded.com.android.dx.rop.cst.*;

public final class AnnotationParser
{
    private final DirectClassFile cf;
    private final ConstantPool pool;
    private final ByteArray bytes;
    private final ParseObserver observer;
    private final ByteArray.MyDataInputStream input;
    private int parseCursor;
    
    public AnnotationParser(final DirectClassFile cf, final int offset, final int length, final ParseObserver observer) {
        if (cf == null) {
            throw new NullPointerException("cf == null");
        }
        this.cf = cf;
        this.pool = cf.getConstantPool();
        this.observer = observer;
        this.bytes = cf.getBytes().slice(offset, offset + length);
        this.input = this.bytes.makeDataInputStream();
        this.parseCursor = 0;
    }
    
    public Constant parseValueAttribute() {
        Constant result;
        try {
            result = this.parseValue();
            if (this.input.available() != 0) {
                throw new ParseException("extra data in attribute");
            }
        }
        catch (IOException ex) {
            throw new RuntimeException("shouldn't happen", ex);
        }
        return result;
    }
    
    public AnnotationsList parseParameterAttribute(final AnnotationVisibility visibility) {
        AnnotationsList result;
        try {
            result = this.parseAnnotationsList(visibility);
            if (this.input.available() != 0) {
                throw new ParseException("extra data in attribute");
            }
        }
        catch (IOException ex) {
            throw new RuntimeException("shouldn't happen", ex);
        }
        return result;
    }
    
    public Annotations parseAnnotationAttribute(final AnnotationVisibility visibility) {
        Annotations result;
        try {
            result = this.parseAnnotations(visibility);
            if (this.input.available() != 0) {
                throw new ParseException("extra data in attribute");
            }
        }
        catch (IOException ex) {
            throw new RuntimeException("shouldn't happen", ex);
        }
        return result;
    }
    
    private AnnotationsList parseAnnotationsList(final AnnotationVisibility visibility) throws IOException {
        final int count = this.input.readUnsignedByte();
        if (this.observer != null) {
            this.parsed(1, "num_parameters: " + Hex.u1(count));
        }
        final AnnotationsList outerList = new AnnotationsList(count);
        for (int i = 0; i < count; ++i) {
            if (this.observer != null) {
                this.parsed(0, "parameter_annotations[" + i + "]:");
                this.changeIndent(1);
            }
            final Annotations annotations = this.parseAnnotations(visibility);
            outerList.set(i, annotations);
            if (this.observer != null) {
                this.observer.changeIndent(-1);
            }
        }
        outerList.setImmutable();
        return outerList;
    }
    
    private Annotations parseAnnotations(final AnnotationVisibility visibility) throws IOException {
        final int count = this.input.readUnsignedShort();
        if (this.observer != null) {
            this.parsed(2, "num_annotations: " + Hex.u2(count));
        }
        final Annotations annotations = new Annotations();
        for (int i = 0; i < count; ++i) {
            if (this.observer != null) {
                this.parsed(0, "annotations[" + i + "]:");
                this.changeIndent(1);
            }
            final Annotation annotation = this.parseAnnotation(visibility);
            annotations.add(annotation);
            if (this.observer != null) {
                this.observer.changeIndent(-1);
            }
        }
        annotations.setImmutable();
        return annotations;
    }
    
    private Annotation parseAnnotation(final AnnotationVisibility visibility) throws IOException {
        this.requireLength(4);
        final int typeIndex = this.input.readUnsignedShort();
        final int numElements = this.input.readUnsignedShort();
        final CstString typeString = (CstString)this.pool.get(typeIndex);
        final CstType type = new CstType(Type.intern(typeString.getString()));
        if (this.observer != null) {
            this.parsed(2, "type: " + type.toHuman());
            this.parsed(2, "num_elements: " + numElements);
        }
        final Annotation annotation = new Annotation(type, visibility);
        for (int i = 0; i < numElements; ++i) {
            if (this.observer != null) {
                this.parsed(0, "elements[" + i + "]:");
                this.changeIndent(1);
            }
            final NameValuePair element = this.parseElement();
            annotation.add(element);
            if (this.observer != null) {
                this.changeIndent(-1);
            }
        }
        annotation.setImmutable();
        return annotation;
    }
    
    private NameValuePair parseElement() throws IOException {
        this.requireLength(5);
        final int elementNameIndex = this.input.readUnsignedShort();
        final CstString elementName = (CstString)this.pool.get(elementNameIndex);
        if (this.observer != null) {
            this.parsed(2, "element_name: " + elementName.toHuman());
            this.parsed(0, "value: ");
            this.changeIndent(1);
        }
        final Constant value = this.parseValue();
        if (this.observer != null) {
            this.changeIndent(-1);
        }
        return new NameValuePair(elementName, value);
    }
    
    private Constant parseValue() throws IOException {
        final int tag = this.input.readUnsignedByte();
        if (this.observer != null) {
            final CstString humanTag = new CstString(Character.toString((char)tag));
            this.parsed(1, "tag: " + humanTag.toQuoted());
        }
        switch (tag) {
            case 66: {
                final CstInteger value = (CstInteger)this.parseConstant();
                return CstByte.make(value.getValue());
            }
            case 67: {
                final CstInteger value = (CstInteger)this.parseConstant();
                final int intValue = value.getValue();
                return CstChar.make(value.getValue());
            }
            case 68: {
                final CstDouble value2 = (CstDouble)this.parseConstant();
                return value2;
            }
            case 70: {
                final CstFloat value3 = (CstFloat)this.parseConstant();
                return value3;
            }
            case 73: {
                final CstInteger value = (CstInteger)this.parseConstant();
                return value;
            }
            case 74: {
                final CstLong value4 = (CstLong)this.parseConstant();
                return value4;
            }
            case 83: {
                final CstInteger value = (CstInteger)this.parseConstant();
                return CstShort.make(value.getValue());
            }
            case 90: {
                final CstInteger value = (CstInteger)this.parseConstant();
                return CstBoolean.make(value.getValue());
            }
            case 99: {
                final int classInfoIndex = this.input.readUnsignedShort();
                final CstString value5 = (CstString)this.pool.get(classInfoIndex);
                final Type type = Type.internReturnType(value5.getString());
                if (this.observer != null) {
                    this.parsed(2, "class_info: " + type.toHuman());
                }
                return new CstType(type);
            }
            case 115: {
                return this.parseConstant();
            }
            case 101: {
                this.requireLength(4);
                final int typeNameIndex = this.input.readUnsignedShort();
                final int constNameIndex = this.input.readUnsignedShort();
                final CstString typeName = (CstString)this.pool.get(typeNameIndex);
                final CstString constName = (CstString)this.pool.get(constNameIndex);
                if (this.observer != null) {
                    this.parsed(2, "type_name: " + typeName.toHuman());
                    this.parsed(2, "const_name: " + constName.toHuman());
                }
                return new CstEnumRef(new CstNat(constName, typeName));
            }
            case 64: {
                final Annotation annotation = this.parseAnnotation(AnnotationVisibility.EMBEDDED);
                return new CstAnnotation(annotation);
            }
            case 91: {
                this.requireLength(2);
                final int numValues = this.input.readUnsignedShort();
                final CstArray.List list = new CstArray.List(numValues);
                if (this.observer != null) {
                    this.parsed(2, "num_values: " + numValues);
                    this.changeIndent(1);
                }
                for (int i = 0; i < numValues; ++i) {
                    if (this.observer != null) {
                        this.changeIndent(-1);
                        this.parsed(0, "element_value[" + i + "]:");
                        this.changeIndent(1);
                    }
                    list.set(i, this.parseValue());
                }
                if (this.observer != null) {
                    this.changeIndent(-1);
                }
                list.setImmutable();
                return new CstArray(list);
            }
            default: {
                throw new ParseException("unknown annotation tag: " + Hex.u1(tag));
            }
        }
    }
    
    private Constant parseConstant() throws IOException {
        final int constValueIndex = this.input.readUnsignedShort();
        final Constant value = this.pool.get(constValueIndex);
        if (this.observer != null) {
            final String human = (value instanceof CstString) ? ((CstString)value).toQuoted() : value.toHuman();
            this.parsed(2, "constant_value: " + human);
        }
        return value;
    }
    
    private void requireLength(final int requiredLength) throws IOException {
        if (this.input.available() < requiredLength) {
            throw new ParseException("truncated annotation attribute");
        }
    }
    
    private void parsed(final int length, final String message) {
        this.observer.parsed(this.bytes, this.parseCursor, length, message);
        this.parseCursor += length;
    }
    
    private void changeIndent(final int indent) {
        this.observer.changeIndent(indent);
    }
}

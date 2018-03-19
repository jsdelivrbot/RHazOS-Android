package embedded.com.android.dx.cf.direct;

import embedded.com.android.dx.cf.code.*;
import embedded.com.android.dx.rop.cst.*;
import embedded.com.android.dx.cf.attrib.*;
import embedded.com.android.dx.cf.iface.*;
import embedded.com.android.dx.util.*;
import embedded.com.android.dx.cf.cst.*;
import embedded.com.android.dx.rop.code.*;
import embedded.com.android.dx.rop.type.*;

public class DirectClassFile implements ClassFile
{
    private static final int CLASS_FILE_MAGIC = -889275714;
    private static final int CLASS_FILE_MIN_MAJOR_VERSION = 45;
    private static final int CLASS_FILE_MAX_MAJOR_VERSION = 52;
    private static final int CLASS_FILE_MAX_MINOR_VERSION = 0;
    private final String filePath;
    private final ByteArray bytes;
    private final boolean strictParse;
    private StdConstantPool pool;
    private int accessFlags;
    private CstType thisClass;
    private CstType superClass;
    private TypeList interfaces;
    private FieldList fields;
    private MethodList methods;
    private StdAttributeList attributes;
    private AttributeFactory attributeFactory;
    private ParseObserver observer;
    
    public static String stringOrNone(final Object obj) {
        if (obj == null) {
            return "(none)";
        }
        return obj.toString();
    }
    
    public DirectClassFile(final ByteArray bytes, final String filePath, final boolean strictParse) {
        if (bytes == null) {
            throw new NullPointerException("bytes == null");
        }
        if (filePath == null) {
            throw new NullPointerException("filePath == null");
        }
        this.filePath = filePath;
        this.bytes = bytes;
        this.strictParse = strictParse;
        this.accessFlags = -1;
    }
    
    public DirectClassFile(final byte[] bytes, final String filePath, final boolean strictParse) {
        this(new ByteArray(bytes), filePath, strictParse);
    }
    
    public void setObserver(final ParseObserver observer) {
        this.observer = observer;
    }
    
    public void setAttributeFactory(final AttributeFactory attributeFactory) {
        if (attributeFactory == null) {
            throw new NullPointerException("attributeFactory == null");
        }
        this.attributeFactory = attributeFactory;
    }
    
    public String getFilePath() {
        return this.filePath;
    }
    
    public ByteArray getBytes() {
        return this.bytes;
    }
    
    @Override
    public int getMagic() {
        this.parseToInterfacesIfNecessary();
        return this.getMagic0();
    }
    
    @Override
    public int getMinorVersion() {
        this.parseToInterfacesIfNecessary();
        return this.getMinorVersion0();
    }
    
    @Override
    public int getMajorVersion() {
        this.parseToInterfacesIfNecessary();
        return this.getMajorVersion0();
    }
    
    @Override
    public int getAccessFlags() {
        this.parseToInterfacesIfNecessary();
        return this.accessFlags;
    }
    
    @Override
    public CstType getThisClass() {
        this.parseToInterfacesIfNecessary();
        return this.thisClass;
    }
    
    @Override
    public CstType getSuperclass() {
        this.parseToInterfacesIfNecessary();
        return this.superClass;
    }
    
    @Override
    public ConstantPool getConstantPool() {
        this.parseToInterfacesIfNecessary();
        return this.pool;
    }
    
    @Override
    public TypeList getInterfaces() {
        this.parseToInterfacesIfNecessary();
        return this.interfaces;
    }
    
    @Override
    public FieldList getFields() {
        this.parseToEndIfNecessary();
        return this.fields;
    }
    
    @Override
    public MethodList getMethods() {
        this.parseToEndIfNecessary();
        return this.methods;
    }
    
    @Override
    public AttributeList getAttributes() {
        this.parseToEndIfNecessary();
        return this.attributes;
    }
    
    @Override
    public BootstrapMethodsList getBootstrapMethods() {
        final AttBootstrapMethods bootstrapMethodsAttribute = (AttBootstrapMethods)this.getAttributes().findFirst("BootstrapMethods");
        if (bootstrapMethodsAttribute != null) {
            return bootstrapMethodsAttribute.getBootstrapMethods();
        }
        return BootstrapMethodsList.EMPTY;
    }
    
    @Override
    public CstString getSourceFile() {
        final AttributeList attribs = this.getAttributes();
        final Attribute attSf = attribs.findFirst("SourceFile");
        if (attSf instanceof AttSourceFile) {
            return ((AttSourceFile)attSf).getSourceFile();
        }
        return null;
    }
    
    public TypeList makeTypeList(final int offset, final int size) {
        if (size == 0) {
            return StdTypeList.EMPTY;
        }
        if (this.pool == null) {
            throw new IllegalStateException("pool not yet initialized");
        }
        return new DcfTypeList(this.bytes, offset, size, this.pool, this.observer);
    }
    
    public int getMagic0() {
        return this.bytes.getInt(0);
    }
    
    public int getMinorVersion0() {
        return this.bytes.getUnsignedShort(4);
    }
    
    public int getMajorVersion0() {
        return this.bytes.getUnsignedShort(6);
    }
    
    private void parseToInterfacesIfNecessary() {
        if (this.accessFlags == -1) {
            this.parse();
        }
    }
    
    private void parseToEndIfNecessary() {
        if (this.attributes == null) {
            this.parse();
        }
    }
    
    private void parse() {
        try {
            this.parse0();
        }
        catch (ParseException ex) {
            ex.addContext("...while parsing " + this.filePath);
            throw ex;
        }
        catch (RuntimeException ex2) {
            final ParseException pe = new ParseException(ex2);
            pe.addContext("...while parsing " + this.filePath);
            throw pe;
        }
    }
    
    private boolean isGoodMagic(final int magic) {
        return magic == -889275714;
    }
    
    private boolean isGoodVersion(final int minorVersion, final int majorVersion) {
        if (minorVersion >= 0) {
            if (majorVersion == 52) {
                if (minorVersion <= 0) {
                    return true;
                }
            }
            else if (majorVersion < 52 && majorVersion >= 45) {
                return true;
            }
        }
        return false;
    }
    
    private void parse0() {
        if (this.bytes.size() < 10) {
            throw new ParseException("severely truncated class file");
        }
        if (this.observer != null) {
            this.observer.parsed(this.bytes, 0, 0, "begin classfile");
            this.observer.parsed(this.bytes, 0, 4, "magic: " + Hex.u4(this.getMagic0()));
            this.observer.parsed(this.bytes, 4, 2, "minor_version: " + Hex.u2(this.getMinorVersion0()));
            this.observer.parsed(this.bytes, 6, 2, "major_version: " + Hex.u2(this.getMajorVersion0()));
        }
        if (this.strictParse) {
            if (!this.isGoodMagic(this.getMagic0())) {
                throw new ParseException("bad class file magic (" + Hex.u4(this.getMagic0()) + ")");
            }
            if (!this.isGoodVersion(this.getMinorVersion0(), this.getMajorVersion0())) {
                throw new ParseException("unsupported class file version " + this.getMajorVersion0() + "." + this.getMinorVersion0());
            }
        }
        final ConstantPoolParser cpParser = new ConstantPoolParser(this.bytes);
        cpParser.setObserver(this.observer);
        (this.pool = cpParser.getPool()).setImmutable();
        int at = cpParser.getEndOffset();
        final int accessFlags = this.bytes.getUnsignedShort(at);
        int cpi = this.bytes.getUnsignedShort(at + 2);
        this.thisClass = (CstType)this.pool.get(cpi);
        cpi = this.bytes.getUnsignedShort(at + 4);
        this.superClass = (CstType)this.pool.get0Ok(cpi);
        final int count = this.bytes.getUnsignedShort(at + 6);
        if (this.observer != null) {
            this.observer.parsed(this.bytes, at, 2, "access_flags: " + AccessFlags.classString(accessFlags));
            this.observer.parsed(this.bytes, at + 2, 2, "this_class: " + this.thisClass);
            this.observer.parsed(this.bytes, at + 4, 2, "super_class: " + stringOrNone(this.superClass));
            this.observer.parsed(this.bytes, at + 6, 2, "interfaces_count: " + Hex.u2(count));
            if (count != 0) {
                this.observer.parsed(this.bytes, at + 8, 0, "interfaces:");
            }
        }
        at += 8;
        this.interfaces = this.makeTypeList(at, count);
        at += count * 2;
        if (this.strictParse) {
            final String thisClassName = this.thisClass.getClassType().getClassName();
            if (!this.filePath.endsWith(".class") || !this.filePath.startsWith(thisClassName) || this.filePath.length() != thisClassName.length() + 6) {
                throw new ParseException("class name (" + thisClassName + ") does not match path (" + this.filePath + ")");
            }
        }
        this.accessFlags = accessFlags;
        final FieldListParser flParser = new FieldListParser(this, this.thisClass, at, this.attributeFactory);
        flParser.setObserver(this.observer);
        this.fields = flParser.getList();
        at = flParser.getEndOffset();
        final MethodListParser mlParser = new MethodListParser(this, this.thisClass, at, this.attributeFactory);
        mlParser.setObserver(this.observer);
        this.methods = mlParser.getList();
        at = mlParser.getEndOffset();
        final AttributeListParser alParser = new AttributeListParser(this, 0, at, this.attributeFactory);
        alParser.setObserver(this.observer);
        (this.attributes = alParser.getList()).setImmutable();
        at = alParser.getEndOffset();
        if (at != this.bytes.size()) {
            throw new ParseException("extra bytes at end of class file, at offset " + Hex.u4(at));
        }
        if (this.observer != null) {
            this.observer.parsed(this.bytes, at, 0, "end classfile");
        }
    }
    
    private static class DcfTypeList implements TypeList
    {
        private final ByteArray bytes;
        private final int size;
        private final StdConstantPool pool;
        
        public DcfTypeList(ByteArray bytes, int offset, final int size, final StdConstantPool pool, final ParseObserver observer) {
            if (size < 0) {
                throw new IllegalArgumentException("size < 0");
            }
            bytes = bytes.slice(offset, offset + size * 2);
            this.bytes = bytes;
            this.size = size;
            this.pool = pool;
            for (int i = 0; i < size; ++i) {
                offset = i * 2;
                final int idx = bytes.getUnsignedShort(offset);
                CstType type;
                try {
                    type = (CstType)pool.get(idx);
                }
                catch (ClassCastException ex) {
                    throw new RuntimeException("bogus class cpi", ex);
                }
                if (observer != null) {
                    observer.parsed(bytes, offset, 2, "  " + type);
                }
            }
        }
        
        @Override
        public boolean isMutable() {
            return false;
        }
        
        @Override
        public int size() {
            return this.size;
        }
        
        @Override
        public int getWordCount() {
            return this.size;
        }
        
        @Override
        public Type getType(final int n) {
            final int idx = this.bytes.getUnsignedShort(n * 2);
            return ((CstType)this.pool.get(idx)).getClassType();
        }
        
        @Override
        public TypeList withAddedType(final Type type) {
            throw new UnsupportedOperationException("unsupported");
        }
    }
}

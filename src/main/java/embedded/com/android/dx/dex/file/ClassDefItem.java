package embedded.com.android.dx.dex.file;

import embedded.com.android.dx.rop.code.*;
import embedded.com.android.dx.rop.type.*;
import java.util.*;
import embedded.com.android.dx.rop.cst.*;
import embedded.com.android.dx.rop.annotation.*;
import embedded.com.android.dx.util.*;
import java.io.*;

public final class ClassDefItem extends IndexedItem
{
    private final CstType thisClass;
    private final int accessFlags;
    private final CstType superclass;
    private TypeListItem interfaces;
    private final CstString sourceFile;
    private final ClassDataItem classData;
    private EncodedArrayItem staticValuesItem;
    private AnnotationsDirectoryItem annotationsDirectory;
    
    public ClassDefItem(final CstType thisClass, final int accessFlags, final CstType superclass, final TypeList interfaces, final CstString sourceFile) {
        if (thisClass == null) {
            throw new NullPointerException("thisClass == null");
        }
        if (interfaces == null) {
            throw new NullPointerException("interfaces == null");
        }
        this.thisClass = thisClass;
        this.accessFlags = accessFlags;
        this.superclass = superclass;
        this.interfaces = ((interfaces.size() == 0) ? null : new TypeListItem(interfaces));
        this.sourceFile = sourceFile;
        this.classData = new ClassDataItem(thisClass);
        this.staticValuesItem = null;
        this.annotationsDirectory = new AnnotationsDirectoryItem();
    }
    
    @Override
    public ItemType itemType() {
        return ItemType.TYPE_CLASS_DEF_ITEM;
    }
    
    @Override
    public int writeSize() {
        return 32;
    }
    
    @Override
    public void addContents(final DexFile file) {
        final TypeIdsSection typeIds = file.getTypeIds();
        final MixedItemSection byteData = file.getByteData();
        final MixedItemSection wordData = file.getWordData();
        final MixedItemSection typeLists = file.getTypeLists();
        final StringIdsSection stringIds = file.getStringIds();
        typeIds.intern(this.thisClass);
        if (!this.classData.isEmpty()) {
            final MixedItemSection classDataSection = file.getClassData();
            classDataSection.add(this.classData);
            final CstArray staticValues = this.classData.getStaticValuesConstant();
            if (staticValues != null) {
                this.staticValuesItem = byteData.intern(new EncodedArrayItem(staticValues));
            }
        }
        if (this.superclass != null) {
            typeIds.intern(this.superclass);
        }
        if (this.interfaces != null) {
            this.interfaces = typeLists.intern(this.interfaces);
        }
        if (this.sourceFile != null) {
            stringIds.intern(this.sourceFile);
        }
        if (!this.annotationsDirectory.isEmpty()) {
            if (this.annotationsDirectory.isInternable()) {
                this.annotationsDirectory = wordData.intern(this.annotationsDirectory);
            }
            else {
                wordData.add(this.annotationsDirectory);
            }
        }
    }
    
    @Override
    public void writeTo(final DexFile file, final AnnotatedOutput out) {
        final boolean annotates = out.annotates();
        final TypeIdsSection typeIds = file.getTypeIds();
        final int classIdx = typeIds.indexOf(this.thisClass);
        final int superIdx = (this.superclass == null) ? -1 : typeIds.indexOf(this.superclass);
        final int interOff = OffsettedItem.getAbsoluteOffsetOr0(this.interfaces);
        final int annoOff = this.annotationsDirectory.isEmpty() ? 0 : this.annotationsDirectory.getAbsoluteOffset();
        final int sourceFileIdx = (this.sourceFile == null) ? -1 : file.getStringIds().indexOf(this.sourceFile);
        final int dataOff = this.classData.isEmpty() ? 0 : this.classData.getAbsoluteOffset();
        final int staticValuesOff = OffsettedItem.getAbsoluteOffsetOr0(this.staticValuesItem);
        if (annotates) {
            out.annotate(0, this.indexString() + ' ' + this.thisClass.toHuman());
            out.annotate(4, "  class_idx:           " + Hex.u4(classIdx));
            out.annotate(4, "  access_flags:        " + AccessFlags.classString(this.accessFlags));
            out.annotate(4, "  superclass_idx:      " + Hex.u4(superIdx) + " // " + ((this.superclass == null) ? "<none>" : this.superclass.toHuman()));
            out.annotate(4, "  interfaces_off:      " + Hex.u4(interOff));
            if (interOff != 0) {
                final TypeList list = this.interfaces.getList();
                for (int sz = list.size(), i = 0; i < sz; ++i) {
                    out.annotate(0, "    " + list.getType(i).toHuman());
                }
            }
            out.annotate(4, "  source_file_idx:     " + Hex.u4(sourceFileIdx) + " // " + ((this.sourceFile == null) ? "<none>" : this.sourceFile.toHuman()));
            out.annotate(4, "  annotations_off:     " + Hex.u4(annoOff));
            out.annotate(4, "  class_data_off:      " + Hex.u4(dataOff));
            out.annotate(4, "  static_values_off:   " + Hex.u4(staticValuesOff));
        }
        out.writeInt(classIdx);
        out.writeInt(this.accessFlags);
        out.writeInt(superIdx);
        out.writeInt(interOff);
        out.writeInt(sourceFileIdx);
        out.writeInt(annoOff);
        out.writeInt(dataOff);
        out.writeInt(staticValuesOff);
    }
    
    public CstType getThisClass() {
        return this.thisClass;
    }
    
    public int getAccessFlags() {
        return this.accessFlags;
    }
    
    public CstType getSuperclass() {
        return this.superclass;
    }
    
    public TypeList getInterfaces() {
        if (this.interfaces == null) {
            return StdTypeList.EMPTY;
        }
        return this.interfaces.getList();
    }
    
    public CstString getSourceFile() {
        return this.sourceFile;
    }
    
    public void addStaticField(final EncodedField field, final Constant value) {
        this.classData.addStaticField(field, value);
    }
    
    public void addInstanceField(final EncodedField field) {
        this.classData.addInstanceField(field);
    }
    
    public void addDirectMethod(final EncodedMethod method) {
        this.classData.addDirectMethod(method);
    }
    
    public void addVirtualMethod(final EncodedMethod method) {
        this.classData.addVirtualMethod(method);
    }
    
    public ArrayList<EncodedMethod> getMethods() {
        return this.classData.getMethods();
    }
    
    public void setClassAnnotations(final Annotations annotations, final DexFile dexFile) {
        this.annotationsDirectory.setClassAnnotations(annotations, dexFile);
    }
    
    public void addFieldAnnotations(final CstFieldRef field, final Annotations annotations, final DexFile dexFile) {
        this.annotationsDirectory.addFieldAnnotations(field, annotations, dexFile);
    }
    
    public void addMethodAnnotations(final CstMethodRef method, final Annotations annotations, final DexFile dexFile) {
        this.annotationsDirectory.addMethodAnnotations(method, annotations, dexFile);
    }
    
    public void addParameterAnnotations(final CstMethodRef method, final AnnotationsList list, final DexFile dexFile) {
        this.annotationsDirectory.addParameterAnnotations(method, list, dexFile);
    }
    
    public Annotations getMethodAnnotations(final CstMethodRef method) {
        return this.annotationsDirectory.getMethodAnnotations(method);
    }
    
    public AnnotationsList getParameterAnnotations(final CstMethodRef method) {
        return this.annotationsDirectory.getParameterAnnotations(method);
    }
    
    public void debugPrint(final Writer out, final boolean verbose) {
        final PrintWriter pw = Writers.printWriterFor(out);
        pw.println(this.getClass().getName() + " {");
        pw.println("  accessFlags: " + Hex.u2(this.accessFlags));
        pw.println("  superclass: " + this.superclass);
        pw.println("  interfaces: " + ((this.interfaces == null) ? "<none>" : this.interfaces));
        pw.println("  sourceFile: " + ((this.sourceFile == null) ? "<none>" : this.sourceFile.toQuoted()));
        this.classData.debugPrint(out, verbose);
        this.annotationsDirectory.debugPrint(pw);
        pw.println("}");
    }
}

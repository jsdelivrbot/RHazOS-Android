package embedded.com.android.dx.dex.file;

import embedded.com.android.dx.rop.cst.*;
import embedded.com.android.dx.rop.annotation.*;
import embedded.com.android.dx.util.*;
import java.util.*;
import java.io.*;

public final class AnnotationsDirectoryItem extends OffsettedItem
{
    private static final int ALIGNMENT = 4;
    private static final int HEADER_SIZE = 16;
    private static final int ELEMENT_SIZE = 8;
    private AnnotationSetItem classAnnotations;
    private ArrayList<FieldAnnotationStruct> fieldAnnotations;
    private ArrayList<MethodAnnotationStruct> methodAnnotations;
    private ArrayList<ParameterAnnotationStruct> parameterAnnotations;
    
    public AnnotationsDirectoryItem() {
        super(4, -1);
        this.classAnnotations = null;
        this.fieldAnnotations = null;
        this.methodAnnotations = null;
        this.parameterAnnotations = null;
    }
    
    @Override
    public ItemType itemType() {
        return ItemType.TYPE_ANNOTATIONS_DIRECTORY_ITEM;
    }
    
    public boolean isEmpty() {
        return this.classAnnotations == null && this.fieldAnnotations == null && this.methodAnnotations == null && this.parameterAnnotations == null;
    }
    
    public boolean isInternable() {
        return this.classAnnotations != null && this.fieldAnnotations == null && this.methodAnnotations == null && this.parameterAnnotations == null;
    }
    
    @Override
    public int hashCode() {
        if (this.classAnnotations == null) {
            return 0;
        }
        return this.classAnnotations.hashCode();
    }
    
    public int compareTo0(final OffsettedItem other) {
        if (!this.isInternable()) {
            throw new UnsupportedOperationException("uninternable instance");
        }
        final AnnotationsDirectoryItem otherDirectory = (AnnotationsDirectoryItem)other;
        return this.classAnnotations.compareTo(otherDirectory.classAnnotations);
    }
    
    public void setClassAnnotations(final Annotations annotations, final DexFile dexFile) {
        if (annotations == null) {
            throw new NullPointerException("annotations == null");
        }
        if (this.classAnnotations != null) {
            throw new UnsupportedOperationException("class annotations already set");
        }
        this.classAnnotations = new AnnotationSetItem(annotations, dexFile);
    }
    
    public void addFieldAnnotations(final CstFieldRef field, final Annotations annotations, final DexFile dexFile) {
        if (this.fieldAnnotations == null) {
            this.fieldAnnotations = new ArrayList<FieldAnnotationStruct>();
        }
        this.fieldAnnotations.add(new FieldAnnotationStruct(field, new AnnotationSetItem(annotations, dexFile)));
    }
    
    public void addMethodAnnotations(final CstMethodRef method, final Annotations annotations, final DexFile dexFile) {
        if (this.methodAnnotations == null) {
            this.methodAnnotations = new ArrayList<MethodAnnotationStruct>();
        }
        this.methodAnnotations.add(new MethodAnnotationStruct(method, new AnnotationSetItem(annotations, dexFile)));
    }
    
    public void addParameterAnnotations(final CstMethodRef method, final AnnotationsList list, final DexFile dexFile) {
        if (this.parameterAnnotations == null) {
            this.parameterAnnotations = new ArrayList<ParameterAnnotationStruct>();
        }
        this.parameterAnnotations.add(new ParameterAnnotationStruct(method, list, dexFile));
    }
    
    public Annotations getMethodAnnotations(final CstMethodRef method) {
        if (this.methodAnnotations == null) {
            return null;
        }
        for (final MethodAnnotationStruct item : this.methodAnnotations) {
            if (item.getMethod().equals(method)) {
                return item.getAnnotations();
            }
        }
        return null;
    }
    
    public AnnotationsList getParameterAnnotations(final CstMethodRef method) {
        if (this.parameterAnnotations == null) {
            return null;
        }
        for (final ParameterAnnotationStruct item : this.parameterAnnotations) {
            if (item.getMethod().equals(method)) {
                return item.getAnnotationsList();
            }
        }
        return null;
    }
    
    @Override
    public void addContents(final DexFile file) {
        final MixedItemSection wordData = file.getWordData();
        if (this.classAnnotations != null) {
            this.classAnnotations = wordData.intern(this.classAnnotations);
        }
        if (this.fieldAnnotations != null) {
            for (final FieldAnnotationStruct item : this.fieldAnnotations) {
                item.addContents(file);
            }
        }
        if (this.methodAnnotations != null) {
            for (final MethodAnnotationStruct item2 : this.methodAnnotations) {
                item2.addContents(file);
            }
        }
        if (this.parameterAnnotations != null) {
            for (final ParameterAnnotationStruct item3 : this.parameterAnnotations) {
                item3.addContents(file);
            }
        }
    }
    
    @Override
    public String toHuman() {
        throw new RuntimeException("unsupported");
    }
    
    @Override
    protected void place0(final Section addedTo, final int offset) {
        final int elementCount = listSize(this.fieldAnnotations) + listSize(this.methodAnnotations) + listSize(this.parameterAnnotations);
        this.setWriteSize(16 + elementCount * 8);
    }
    
    @Override
    protected void writeTo0(final DexFile file, final AnnotatedOutput out) {
        final boolean annotates = out.annotates();
        final int classOff = OffsettedItem.getAbsoluteOffsetOr0(this.classAnnotations);
        final int fieldsSize = listSize(this.fieldAnnotations);
        final int methodsSize = listSize(this.methodAnnotations);
        final int parametersSize = listSize(this.parameterAnnotations);
        if (annotates) {
            out.annotate(0, this.offsetString() + " annotations directory");
            out.annotate(4, "  class_annotations_off: " + Hex.u4(classOff));
            out.annotate(4, "  fields_size:           " + Hex.u4(fieldsSize));
            out.annotate(4, "  methods_size:          " + Hex.u4(methodsSize));
            out.annotate(4, "  parameters_size:       " + Hex.u4(parametersSize));
        }
        out.writeInt(classOff);
        out.writeInt(fieldsSize);
        out.writeInt(methodsSize);
        out.writeInt(parametersSize);
        if (fieldsSize != 0) {
            Collections.sort(this.fieldAnnotations);
            if (annotates) {
                out.annotate(0, "  fields:");
            }
            for (final FieldAnnotationStruct item : this.fieldAnnotations) {
                item.writeTo(file, out);
            }
        }
        if (methodsSize != 0) {
            Collections.sort(this.methodAnnotations);
            if (annotates) {
                out.annotate(0, "  methods:");
            }
            for (final MethodAnnotationStruct item2 : this.methodAnnotations) {
                item2.writeTo(file, out);
            }
        }
        if (parametersSize != 0) {
            Collections.sort(this.parameterAnnotations);
            if (annotates) {
                out.annotate(0, "  parameters:");
            }
            for (final ParameterAnnotationStruct item3 : this.parameterAnnotations) {
                item3.writeTo(file, out);
            }
        }
    }
    
    private static int listSize(final ArrayList<?> list) {
        if (list == null) {
            return 0;
        }
        return list.size();
    }
    
    void debugPrint(final PrintWriter out) {
        if (this.classAnnotations != null) {
            out.println("  class annotations: " + this.classAnnotations);
        }
        if (this.fieldAnnotations != null) {
            out.println("  field annotations:");
            for (final FieldAnnotationStruct item : this.fieldAnnotations) {
                out.println("    " + item.toHuman());
            }
        }
        if (this.methodAnnotations != null) {
            out.println("  method annotations:");
            for (final MethodAnnotationStruct item2 : this.methodAnnotations) {
                out.println("    " + item2.toHuman());
            }
        }
        if (this.parameterAnnotations != null) {
            out.println("  parameter annotations:");
            for (final ParameterAnnotationStruct item3 : this.parameterAnnotations) {
                out.println("    " + item3.toHuman());
            }
        }
    }
}

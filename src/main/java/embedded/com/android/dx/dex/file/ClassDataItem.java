package embedded.com.android.dx.dex.file;

import java.io.*;
import java.util.*;
import embedded.com.android.dx.rop.cst.*;
import embedded.com.android.dx.util.*;

public final class ClassDataItem extends OffsettedItem
{
    private final CstType thisClass;
    private final ArrayList<EncodedField> staticFields;
    private final HashMap<EncodedField, Constant> staticValues;
    private final ArrayList<EncodedField> instanceFields;
    private final ArrayList<EncodedMethod> directMethods;
    private final ArrayList<EncodedMethod> virtualMethods;
    private CstArray staticValuesConstant;
    private byte[] encodedForm;
    
    public ClassDataItem(final CstType thisClass) {
        super(1, -1);
        if (thisClass == null) {
            throw new NullPointerException("thisClass == null");
        }
        this.thisClass = thisClass;
        this.staticFields = new ArrayList<EncodedField>(20);
        this.staticValues = new HashMap<EncodedField, Constant>(40);
        this.instanceFields = new ArrayList<EncodedField>(20);
        this.directMethods = new ArrayList<EncodedMethod>(20);
        this.virtualMethods = new ArrayList<EncodedMethod>(20);
        this.staticValuesConstant = null;
    }
    
    @Override
    public ItemType itemType() {
        return ItemType.TYPE_CLASS_DATA_ITEM;
    }
    
    @Override
    public String toHuman() {
        return this.toString();
    }
    
    public boolean isEmpty() {
        return this.staticFields.isEmpty() && this.instanceFields.isEmpty() && this.directMethods.isEmpty() && this.virtualMethods.isEmpty();
    }
    
    public void addStaticField(final EncodedField field, final Constant value) {
        if (field == null) {
            throw new NullPointerException("field == null");
        }
        if (this.staticValuesConstant != null) {
            throw new UnsupportedOperationException("static fields already sorted");
        }
        this.staticFields.add(field);
        this.staticValues.put(field, value);
    }
    
    public void addInstanceField(final EncodedField field) {
        if (field == null) {
            throw new NullPointerException("field == null");
        }
        this.instanceFields.add(field);
    }
    
    public void addDirectMethod(final EncodedMethod method) {
        if (method == null) {
            throw new NullPointerException("method == null");
        }
        this.directMethods.add(method);
    }
    
    public void addVirtualMethod(final EncodedMethod method) {
        if (method == null) {
            throw new NullPointerException("method == null");
        }
        this.virtualMethods.add(method);
    }
    
    public ArrayList<EncodedMethod> getMethods() {
        final int sz = this.directMethods.size() + this.virtualMethods.size();
        final ArrayList<EncodedMethod> result = new ArrayList<EncodedMethod>(sz);
        result.addAll(this.directMethods);
        result.addAll(this.virtualMethods);
        return result;
    }
    
    public void debugPrint(final Writer out, final boolean verbose) {
        final PrintWriter pw = Writers.printWriterFor(out);
        for (int sz = this.staticFields.size(), i = 0; i < sz; ++i) {
            pw.println("  sfields[" + i + "]: " + this.staticFields.get(i));
        }
        for (int sz = this.instanceFields.size(), i = 0; i < sz; ++i) {
            pw.println("  ifields[" + i + "]: " + this.instanceFields.get(i));
        }
        for (int sz = this.directMethods.size(), i = 0; i < sz; ++i) {
            pw.println("  dmeths[" + i + "]:");
            this.directMethods.get(i).debugPrint(pw, verbose);
        }
        for (int sz = this.virtualMethods.size(), i = 0; i < sz; ++i) {
            pw.println("  vmeths[" + i + "]:");
            this.virtualMethods.get(i).debugPrint(pw, verbose);
        }
    }
    
    @Override
    public void addContents(final DexFile file) {
        if (!this.staticFields.isEmpty()) {
            this.getStaticValuesConstant();
            for (final EncodedField field : this.staticFields) {
                field.addContents(file);
            }
        }
        if (!this.instanceFields.isEmpty()) {
            Collections.sort(this.instanceFields);
            for (final EncodedField field : this.instanceFields) {
                field.addContents(file);
            }
        }
        if (!this.directMethods.isEmpty()) {
            Collections.sort(this.directMethods);
            for (final EncodedMethod method : this.directMethods) {
                method.addContents(file);
            }
        }
        if (!this.virtualMethods.isEmpty()) {
            Collections.sort(this.virtualMethods);
            for (final EncodedMethod method : this.virtualMethods) {
                method.addContents(file);
            }
        }
    }
    
    public CstArray getStaticValuesConstant() {
        if (this.staticValuesConstant == null && this.staticFields.size() != 0) {
            this.staticValuesConstant = this.makeStaticValuesConstant();
        }
        return this.staticValuesConstant;
    }
    
    private CstArray makeStaticValuesConstant() {
        Collections.sort(this.staticFields);
        int size;
        for (size = this.staticFields.size(); size > 0; --size) {
            final EncodedField field = this.staticFields.get(size - 1);
            final Constant cst = this.staticValues.get(field);
            if (cst instanceof CstLiteralBits) {
                if (((CstLiteralBits)cst).getLongBits() != 0L) {
                    break;
                }
            }
            else if (cst != null) {
                break;
            }
        }
        if (size == 0) {
            return null;
        }
        final CstArray.List list = new CstArray.List(size);
        for (int i = 0; i < size; ++i) {
            final EncodedField field2 = this.staticFields.get(i);
            Constant cst2 = this.staticValues.get(field2);
            if (cst2 == null) {
                cst2 = Zeroes.zeroFor(field2.getRef().getType());
            }
            list.set(i, cst2);
        }
        list.setImmutable();
        return new CstArray(list);
    }
    
    @Override
    protected void place0(final Section addedTo, final int offset) {
        final ByteArrayAnnotatedOutput out = new ByteArrayAnnotatedOutput();
        this.encodeOutput(addedTo.getFile(), out);
        this.encodedForm = out.toByteArray();
        this.setWriteSize(this.encodedForm.length);
    }
    
    private void encodeOutput(final DexFile file, final AnnotatedOutput out) {
        final boolean annotates = out.annotates();
        if (annotates) {
            out.annotate(0, this.offsetString() + " class data for " + this.thisClass.toHuman());
        }
        encodeSize(file, out, "static_fields", this.staticFields.size());
        encodeSize(file, out, "instance_fields", this.instanceFields.size());
        encodeSize(file, out, "direct_methods", this.directMethods.size());
        encodeSize(file, out, "virtual_methods", this.virtualMethods.size());
        encodeList(file, out, "static_fields", this.staticFields);
        encodeList(file, out, "instance_fields", this.instanceFields);
        encodeList(file, out, "direct_methods", this.directMethods);
        encodeList(file, out, "virtual_methods", this.virtualMethods);
        if (annotates) {
            out.endAnnotation();
        }
    }
    
    private static void encodeSize(final DexFile file, final AnnotatedOutput out, final String label, final int size) {
        if (out.annotates()) {
            out.annotate(String.format("  %-21s %08x", label + "_size:", size));
        }
        out.writeUleb128(size);
    }
    
    private static void encodeList(final DexFile file, final AnnotatedOutput out, final String label, final ArrayList<? extends EncodedMember> list) {
        final int size = list.size();
        int lastIndex = 0;
        if (size == 0) {
            return;
        }
        if (out.annotates()) {
            out.annotate(0, "  " + label + ":");
        }
        for (int i = 0; i < size; ++i) {
            lastIndex = ((EncodedMember)list.get(i)).encode(file, out, lastIndex, i);
        }
    }
    
    public void writeTo0(final DexFile file, final AnnotatedOutput out) {
        final boolean annotates = out.annotates();
        if (annotates) {
            this.encodeOutput(file, out);
        }
        else {
            out.write(this.encodedForm);
        }
    }
}

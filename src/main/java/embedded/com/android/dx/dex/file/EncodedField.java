package embedded.com.android.dx.dex.file;

import embedded.com.android.dx.rop.cst.*;
import java.io.*;
import embedded.com.android.dx.util.*;
import embedded.com.android.dex.*;
import embedded.com.android.dx.rop.code.*;

public final class EncodedField extends EncodedMember implements Comparable<EncodedField>
{
    private final CstFieldRef field;
    
    public EncodedField(final CstFieldRef field, final int accessFlags) {
        super(accessFlags);
        if (field == null) {
            throw new NullPointerException("field == null");
        }
        this.field = field;
    }
    
    @Override
    public int hashCode() {
        return this.field.hashCode();
    }
    
    @Override
    public boolean equals(final Object other) {
        return other instanceof EncodedField && this.compareTo((EncodedField)other) == 0;
    }
    
    @Override
    public int compareTo(final EncodedField other) {
        return this.field.compareTo((Constant)other.field);
    }
    
    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer(100);
        sb.append(this.getClass().getName());
        sb.append('{');
        sb.append(Hex.u2(this.getAccessFlags()));
        sb.append(' ');
        sb.append(this.field);
        sb.append('}');
        return sb.toString();
    }
    
    @Override
    public void addContents(final DexFile file) {
        final FieldIdsSection fieldIds = file.getFieldIds();
        fieldIds.intern(this.field);
    }
    
    @Override
    public CstString getName() {
        return this.field.getNat().getName();
    }
    
    @Override
    public String toHuman() {
        return this.field.toHuman();
    }
    
    @Override
    public void debugPrint(final PrintWriter out, final boolean verbose) {
        out.println(this.toString());
    }
    
    public CstFieldRef getRef() {
        return this.field;
    }
    
    @Override
    public int encode(final DexFile file, final AnnotatedOutput out, final int lastIndex, final int dumpSeq) {
        final int fieldIdx = file.getFieldIds().indexOf(this.field);
        final int diff = fieldIdx - lastIndex;
        final int accessFlags = this.getAccessFlags();
        if (out.annotates()) {
            out.annotate(0, String.format("  [%x] %s", dumpSeq, this.field.toHuman()));
            out.annotate(Leb128.unsignedLeb128Size(diff), "    field_idx:    " + Hex.u4(fieldIdx));
            out.annotate(Leb128.unsignedLeb128Size(accessFlags), "    access_flags: " + AccessFlags.fieldString(accessFlags));
        }
        out.writeUleb128(diff);
        out.writeUleb128(accessFlags);
        return fieldIdx;
    }
}

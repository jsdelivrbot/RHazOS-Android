package embedded.com.android.dx.dex.file;

import java.io.PrintWriter;

import embedded.com.android.dex.Leb128;
import embedded.com.android.dx.dex.code.DalvCode;
import embedded.com.android.dx.rop.code.AccessFlags;
import embedded.com.android.dx.rop.cst.Constant;
import embedded.com.android.dx.rop.cst.CstMethodRef;
import embedded.com.android.dx.rop.cst.CstString;
import embedded.com.android.dx.rop.type.TypeList;
import embedded.com.android.dx.util.AnnotatedOutput;
import embedded.com.android.dx.util.Hex;

public final class EncodedMethod extends EncodedMember implements Comparable<EncodedMethod>
{
    private final CstMethodRef method;
    private final CodeItem code;
    
    public EncodedMethod(final CstMethodRef method, final int accessFlags, final DalvCode code, final TypeList throwsList) {
        super(accessFlags);
        if (method == null) {
            throw new NullPointerException("method == null");
        }
        this.method = method;
        if (code == null) {
            this.code = null;
        }
        else {
            final boolean isStatic = (accessFlags & 0x8) != 0x0;
            this.code = new CodeItem(method, code, isStatic, throwsList);
        }
    }
    
    @Override
    public boolean equals(final Object other) {
        return other instanceof EncodedMethod && this.compareTo((EncodedMethod)other) == 0;
    }
    
    @Override
    public int compareTo(final EncodedMethod other) {
        return this.method.compareTo((Constant)other.method);
    }
    
    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer(100);
        sb.append(this.getClass().getName());
        sb.append('{');
        sb.append(Hex.u2(this.getAccessFlags()));
        sb.append(' ');
        sb.append(this.method);
        if (this.code != null) {
            sb.append(' ');
            sb.append(this.code);
        }
        sb.append('}');
        return sb.toString();
    }
    
    @Override
    public void addContents(final DexFile file) {
        final MethodIdsSection methodIds = file.getMethodIds();
        final MixedItemSection wordData = file.getWordData();
        methodIds.intern(this.method);
        if (this.code != null) {
            wordData.add(this.code);
        }
    }
    
    @Override
    public final String toHuman() {
        return this.method.toHuman();
    }
    
    @Override
    public final CstString getName() {
        return this.method.getNat().getName();
    }
    
    @Override
    public void debugPrint(final PrintWriter out, final boolean verbose) {
        if (this.code == null) {
            out.println(this.getRef().toHuman() + ": abstract or native");
        }
        else {
            this.code.debugPrint(out, "  ", verbose);
        }
    }
    
    public final CstMethodRef getRef() {
        return this.method;
    }
    
    @Override
    public int encode(final DexFile file, final AnnotatedOutput out, final int lastIndex, final int dumpSeq) {
        final int methodIdx = file.getMethodIds().indexOf(this.method);
        final int diff = methodIdx - lastIndex;
        final int accessFlags = this.getAccessFlags();
        final int codeOff = OffsettedItem.getAbsoluteOffsetOr0(this.code);
        final boolean hasCode = codeOff != 0;
        final boolean shouldHaveCode = (accessFlags & 0x500) == 0x0;
        if (hasCode != shouldHaveCode) {
            throw new UnsupportedOperationException("code vs. access_flags mismatch");
        }
        if (out.annotates()) {
            out.annotate(0, String.format("  [%x] %s", dumpSeq, this.method.toHuman()));
            out.annotate(Leb128.unsignedLeb128Size(diff), "    method_idx:   " + Hex.u4(methodIdx));
            out.annotate(Leb128.unsignedLeb128Size(accessFlags), "    access_flags: " + AccessFlags.methodString(accessFlags));
            out.annotate(Leb128.unsignedLeb128Size(codeOff), "    code_off:     " + Hex.u4(codeOff));
        }
        out.writeUleb128(diff);
        out.writeUleb128(accessFlags);
        out.writeUleb128(codeOff);
        return methodIdx;
    }
}

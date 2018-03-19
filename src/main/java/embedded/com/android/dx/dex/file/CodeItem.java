package embedded.com.android.dx.dex.file;

import embedded.com.android.dx.rop.cst.*;
import java.util.*;
import java.io.*;
import embedded.com.android.dx.dex.code.*;
import embedded.com.android.dx.util.*;
import embedded.com.android.dx.rop.type.*;
import embedded.com.android.dex.util.*;

public final class CodeItem extends OffsettedItem
{
    private static final int ALIGNMENT = 4;
    private static final int HEADER_SIZE = 16;
    private final CstMethodRef ref;
    private final DalvCode code;
    private CatchStructs catches;
    private final boolean isStatic;
    private final TypeList throwsList;
    private DebugInfoItem debugInfo;
    
    public CodeItem(final CstMethodRef ref, final DalvCode code, final boolean isStatic, final TypeList throwsList) {
        super(4, -1);
        if (ref == null) {
            throw new NullPointerException("ref == null");
        }
        if (code == null) {
            throw new NullPointerException("code == null");
        }
        if (throwsList == null) {
            throw new NullPointerException("throwsList == null");
        }
        this.ref = ref;
        this.code = code;
        this.isStatic = isStatic;
        this.throwsList = throwsList;
        this.catches = null;
        this.debugInfo = null;
    }
    
    @Override
    public ItemType itemType() {
        return ItemType.TYPE_CODE_ITEM;
    }
    
    @Override
    public void addContents(final DexFile file) {
        final MixedItemSection byteData = file.getByteData();
        final TypeIdsSection typeIds = file.getTypeIds();
        if (this.code.hasPositions() || this.code.hasLocals()) {
            byteData.add(this.debugInfo = new DebugInfoItem(this.code, this.isStatic, this.ref));
        }
        if (this.code.hasAnyCatches()) {
            for (final Type type : this.code.getCatchTypes()) {
                typeIds.intern(type);
            }
            this.catches = new CatchStructs(this.code);
        }
        for (final Constant c : this.code.getInsnConstants()) {
            file.internIfAppropriate(c);
        }
    }
    
    @Override
    public String toString() {
        return "CodeItem{" + this.toHuman() + "}";
    }
    
    @Override
    public String toHuman() {
        return this.ref.toHuman();
    }
    
    public CstMethodRef getRef() {
        return this.ref;
    }
    
    public void debugPrint(final PrintWriter out, final String prefix, final boolean verbose) {
        out.println(this.ref.toHuman() + ":");
        final DalvInsnList insns = this.code.getInsns();
        out.println("regs: " + Hex.u2(this.getRegistersSize()) + "; ins: " + Hex.u2(this.getInsSize()) + "; outs: " + Hex.u2(this.getOutsSize()));
        insns.debugPrint(out, prefix, verbose);
        final String prefix2 = prefix + "  ";
        if (this.catches != null) {
            out.print(prefix);
            out.println("catches");
            this.catches.debugPrint(out, prefix2);
        }
        if (this.debugInfo != null) {
            out.print(prefix);
            out.println("debug info");
            this.debugInfo.debugPrint(out, prefix2);
        }
    }
    
    @Override
    protected void place0(final Section addedTo, final int offset) {
        final DexFile file = addedTo.getFile();
        this.code.assignIndices(new DalvCode.AssignIndicesCallback() {
            @Override
            public int getIndex(final Constant cst) {
                final IndexedItem item = file.findItemOrNull(cst);
                if (item == null) {
                    return -1;
                }
                return item.getIndex();
            }
        });
        int catchesSize;
        if (this.catches != null) {
            this.catches.encode(file);
            catchesSize = this.catches.writeSize();
        }
        else {
            catchesSize = 0;
        }
        int insnsSize = this.code.getInsns().codeSize();
        if ((insnsSize & 0x1) != 0x0) {
            ++insnsSize;
        }
        this.setWriteSize(16 + insnsSize * 2 + catchesSize);
    }
    
    @Override
    protected void writeTo0(final DexFile file, final AnnotatedOutput out) {
        final boolean annotates = out.annotates();
        final int regSz = this.getRegistersSize();
        final int outsSz = this.getOutsSize();
        final int insSz = this.getInsSize();
        final int insnsSz = this.code.getInsns().codeSize();
        final boolean needPadding = (insnsSz & 0x1) != 0x0;
        final int triesSz = (this.catches == null) ? 0 : this.catches.triesSize();
        final int debugOff = (this.debugInfo == null) ? 0 : this.debugInfo.getAbsoluteOffset();
        if (annotates) {
            out.annotate(0, this.offsetString() + ' ' + this.ref.toHuman());
            out.annotate(2, "  registers_size: " + Hex.u2(regSz));
            out.annotate(2, "  ins_size:       " + Hex.u2(insSz));
            out.annotate(2, "  outs_size:      " + Hex.u2(outsSz));
            out.annotate(2, "  tries_size:     " + Hex.u2(triesSz));
            out.annotate(4, "  debug_off:      " + Hex.u4(debugOff));
            out.annotate(4, "  insns_size:     " + Hex.u4(insnsSz));
            final int size = this.throwsList.size();
            if (size != 0) {
                out.annotate(0, "  throws " + StdTypeList.toHuman(this.throwsList));
            }
        }
        out.writeShort(regSz);
        out.writeShort(insSz);
        out.writeShort(outsSz);
        out.writeShort(triesSz);
        out.writeInt(debugOff);
        out.writeInt(insnsSz);
        this.writeCodes(file, out);
        if (this.catches != null) {
            if (needPadding) {
                if (annotates) {
                    out.annotate(2, "  padding: 0");
                }
                out.writeShort(0);
            }
            this.catches.writeTo(file, out);
        }
        if (annotates && this.debugInfo != null) {
            out.annotate(0, "  debug info");
            this.debugInfo.annotateTo(file, out, "    ");
        }
    }
    
    private void writeCodes(final DexFile file, final AnnotatedOutput out) {
        final DalvInsnList insns = this.code.getInsns();
        try {
            insns.writeTo(out);
        }
        catch (RuntimeException ex) {
            throw ExceptionWithContext.withContext(ex, "...while writing instructions for " + this.ref.toHuman());
        }
    }
    
    private int getInsSize() {
        return this.ref.getParameterWordCount(this.isStatic);
    }
    
    private int getOutsSize() {
        return this.code.getInsns().getOutsSize();
    }
    
    private int getRegistersSize() {
        return this.code.getInsns().getRegistersSize();
    }
}

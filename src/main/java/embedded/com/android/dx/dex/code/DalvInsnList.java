package embedded.com.android.dx.dex.code;

import java.util.*;
import embedded.com.android.dex.util.*;
import embedded.com.android.dx.rop.cst.*;
import embedded.com.android.dx.util.*;
import java.io.*;

public final class DalvInsnList extends FixedSizeList
{
    private final int regCount;
    
    public static DalvInsnList makeImmutable(final ArrayList<DalvInsn> list, final int regCount) {
        final int size = list.size();
        final DalvInsnList result = new DalvInsnList(size, regCount);
        for (int i = 0; i < size; ++i) {
            result.set(i, list.get(i));
        }
        result.setImmutable();
        return result;
    }
    
    public DalvInsnList(final int size, final int regCount) {
        super(size);
        this.regCount = regCount;
    }
    
    public DalvInsn get(final int n) {
        return (DalvInsn)this.get0(n);
    }
    
    public void set(final int n, final DalvInsn insn) {
        this.set0(n, insn);
    }
    
    public int codeSize() {
        final int sz = this.size();
        if (sz == 0) {
            return 0;
        }
        final DalvInsn last = this.get(sz - 1);
        return last.getNextAddress();
    }
    
    public void writeTo(final AnnotatedOutput out) {
        final int startCursor = out.getCursor();
        final int sz = this.size();
        if (out.annotates()) {
            final boolean verbose = out.isVerbose();
            for (int i = 0; i < sz; ++i) {
                final DalvInsn insn = (DalvInsn)this.get0(i);
                final int codeBytes = insn.codeSize() * 2;
                String s;
                if (codeBytes != 0 || verbose) {
                    s = insn.listingString("  ", out.getAnnotationWidth(), true);
                }
                else {
                    s = null;
                }
                if (s != null) {
                    out.annotate(codeBytes, s);
                }
                else if (codeBytes != 0) {
                    out.annotate(codeBytes, "");
                }
            }
        }
        for (int j = 0; j < sz; ++j) {
            final DalvInsn insn2 = (DalvInsn)this.get0(j);
            try {
                insn2.writeTo(out);
            }
            catch (RuntimeException ex) {
                throw ExceptionWithContext.withContext(ex, "...while writing " + insn2);
            }
        }
        final int written = (out.getCursor() - startCursor) / 2;
        if (written != this.codeSize()) {
            throw new RuntimeException("write length mismatch; expected " + this.codeSize() + " but actually wrote " + written);
        }
    }
    
    public int getRegistersSize() {
        return this.regCount;
    }
    
    public int getOutsSize() {
        final int sz = this.size();
        int result = 0;
        for (int i = 0; i < sz; ++i) {
            final DalvInsn insn = (DalvInsn)this.get0(i);
            int count = 0;
            if (insn instanceof CstInsn) {
                final Constant cst = ((CstInsn)insn).getConstant();
                if (cst instanceof CstBaseMethodRef) {
                    final CstBaseMethodRef methodRef = (CstBaseMethodRef)cst;
                    final boolean isStatic = insn.getOpcode().getFamily() == 113;
                    count = methodRef.getParameterWordCount(isStatic);
                }
                else if (cst instanceof CstCallSiteRef) {
                    final CstCallSiteRef invokeDynamicRef = (CstCallSiteRef)cst;
                    count = invokeDynamicRef.getPrototype().getParameterTypes().getWordCount();
                }
            }
            else {
                if (!(insn instanceof MultiCstInsn)) {
                    continue;
                }
                if (insn.getOpcode().getFamily() != 250) {
                    throw new RuntimeException("Expecting invoke-polymorphic");
                }
                final MultiCstInsn mci = (MultiCstInsn)insn;
                final CstProtoRef proto = (CstProtoRef)mci.getConstant(1);
                count = proto.getPrototype().getParameterTypes().getWordCount();
                ++count;
            }
            if (count > result) {
                result = count;
            }
        }
        return result;
    }
    
    public void debugPrint(final Writer out, final String prefix, final boolean verbose) {
        final IndentingWriter iw = new IndentingWriter(out, 0, prefix);
        final int sz = this.size();
        try {
            for (int i = 0; i < sz; ++i) {
                final DalvInsn insn = (DalvInsn)this.get0(i);
                String s;
                if (insn.codeSize() != 0 || verbose) {
                    s = insn.listingString("", 0, verbose);
                }
                else {
                    s = null;
                }
                if (s != null) {
                    iw.write(s);
                }
            }
            iw.flush();
        }
        catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public void debugPrint(final OutputStream out, final String prefix, final boolean verbose) {
        final Writer w = new OutputStreamWriter(out);
        this.debugPrint(w, prefix, verbose);
        try {
            w.flush();
        }
        catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}

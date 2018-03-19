package embedded.com.android.dx.dex.file;

import embedded.com.android.dx.rop.cst.*;
import java.io.*;
import embedded.com.android.dx.util.*;
import embedded.com.android.dex.util.*;
import embedded.com.android.dx.dex.code.*;

public class DebugInfoItem extends OffsettedItem
{
    private static final int ALIGNMENT = 1;
    private static final boolean ENABLE_ENCODER_SELF_CHECK = false;
    private final DalvCode code;
    private byte[] encoded;
    private final boolean isStatic;
    private final CstMethodRef ref;
    
    public DebugInfoItem(final DalvCode code, final boolean isStatic, final CstMethodRef ref) {
        super(1, -1);
        if (code == null) {
            throw new NullPointerException("code == null");
        }
        this.code = code;
        this.isStatic = isStatic;
        this.ref = ref;
    }
    
    @Override
    public ItemType itemType() {
        return ItemType.TYPE_DEBUG_INFO_ITEM;
    }
    
    @Override
    public void addContents(final DexFile file) {
    }
    
    @Override
    protected void place0(final Section addedTo, final int offset) {
        try {
            this.encoded = this.encode(addedTo.getFile(), null, null, null, false);
            this.setWriteSize(this.encoded.length);
        }
        catch (RuntimeException ex) {
            throw ExceptionWithContext.withContext(ex, "...while placing debug info for " + this.ref.toHuman());
        }
    }
    
    @Override
    public String toHuman() {
        throw new RuntimeException("unsupported");
    }
    
    public void annotateTo(final DexFile file, final AnnotatedOutput out, final String prefix) {
        this.encode(file, prefix, null, out, false);
    }
    
    public void debugPrint(final PrintWriter out, final String prefix) {
        this.encode(null, prefix, out, null, false);
    }
    
    @Override
    protected void writeTo0(final DexFile file, final AnnotatedOutput out) {
        if (out.annotates()) {
            out.annotate(this.offsetString() + " debug info");
            this.encode(file, null, null, out, true);
        }
        out.write(this.encoded);
    }
    
    private byte[] encode(final DexFile file, final String prefix, final PrintWriter debugPrint, final AnnotatedOutput out, final boolean consume) {
        final byte[] result = this.encode0(file, prefix, debugPrint, out, consume);
        return result;
    }
    
    private byte[] encode0(final DexFile file, final String prefix, final PrintWriter debugPrint, final AnnotatedOutput out, final boolean consume) {
        final PositionList positions = this.code.getPositions();
        final LocalList locals = this.code.getLocals();
        final DalvInsnList insns = this.code.getInsns();
        final int codeSize = insns.codeSize();
        final int regSize = insns.getRegistersSize();
        final DebugInfoEncoder encoder = new DebugInfoEncoder(positions, locals, file, codeSize, regSize, this.isStatic, this.ref);
        byte[] result;
        if (debugPrint == null && out == null) {
            result = encoder.convert();
        }
        else {
            result = encoder.convertAndAnnotate(prefix, debugPrint, out, consume);
        }
        return result;
    }
}

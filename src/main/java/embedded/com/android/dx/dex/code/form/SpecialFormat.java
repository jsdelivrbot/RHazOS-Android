package embedded.com.android.dx.dex.code.form;

import embedded.com.android.dx.dex.code.*;
import embedded.com.android.dx.util.*;

public final class SpecialFormat extends InsnFormat
{
    public static final InsnFormat THE_ONE;
    
    @Override
    public String insnArgString(final DalvInsn insn) {
        throw new RuntimeException("unsupported");
    }
    
    @Override
    public String insnCommentString(final DalvInsn insn, final boolean noteIndices) {
        throw new RuntimeException("unsupported");
    }
    
    @Override
    public int codeSize() {
        throw new RuntimeException("unsupported");
    }
    
    @Override
    public boolean isCompatible(final DalvInsn insn) {
        return true;
    }
    
    @Override
    public void writeTo(final AnnotatedOutput out, final DalvInsn insn) {
        throw new RuntimeException("unsupported");
    }
    
    static {
        THE_ONE = new SpecialFormat();
    }
}

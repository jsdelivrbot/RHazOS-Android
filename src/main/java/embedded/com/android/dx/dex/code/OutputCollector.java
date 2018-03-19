package embedded.com.android.dx.dex.code;

import java.util.*;
import embedded.com.android.dx.dex.*;

public final class OutputCollector
{
    private final OutputFinisher finisher;
    private ArrayList<DalvInsn> suffix;
    
    public OutputCollector(final DexOptions dexOptions, final int initialCapacity, final int suffixInitialCapacity, final int regCount, final int paramSize) {
        this.finisher = new OutputFinisher(dexOptions, initialCapacity, regCount, paramSize);
        this.suffix = new ArrayList<DalvInsn>(suffixInitialCapacity);
    }
    
    public void add(final DalvInsn insn) {
        this.finisher.add(insn);
    }
    
    public void reverseBranch(final int which, final CodeAddress newTarget) {
        this.finisher.reverseBranch(which, newTarget);
    }
    
    public void addSuffix(final DalvInsn insn) {
        this.suffix.add(insn);
    }
    
    public OutputFinisher getFinisher() {
        if (this.suffix == null) {
            throw new UnsupportedOperationException("already processed");
        }
        this.appendSuffixToOutput();
        return this.finisher;
    }
    
    private void appendSuffixToOutput() {
        for (int size = this.suffix.size(), i = 0; i < size; ++i) {
            this.finisher.add(this.suffix.get(i));
        }
        this.suffix = null;
    }
}

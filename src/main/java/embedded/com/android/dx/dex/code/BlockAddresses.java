package embedded.com.android.dx.dex.code;

import embedded.com.android.dx.rop.code.*;

public final class BlockAddresses
{
    private final CodeAddress[] starts;
    private final CodeAddress[] lasts;
    private final CodeAddress[] ends;
    
    public BlockAddresses(final RopMethod method) {
        final BasicBlockList blocks = method.getBlocks();
        final int maxLabel = blocks.getMaxLabel();
        this.starts = new CodeAddress[maxLabel];
        this.lasts = new CodeAddress[maxLabel];
        this.ends = new CodeAddress[maxLabel];
        this.setupArrays(method);
    }
    
    public CodeAddress getStart(final BasicBlock block) {
        return this.starts[block.getLabel()];
    }
    
    public CodeAddress getStart(final int label) {
        return this.starts[label];
    }
    
    public CodeAddress getLast(final BasicBlock block) {
        return this.lasts[block.getLabel()];
    }
    
    public CodeAddress getLast(final int label) {
        return this.lasts[label];
    }
    
    public CodeAddress getEnd(final BasicBlock block) {
        return this.ends[block.getLabel()];
    }
    
    public CodeAddress getEnd(final int label) {
        return this.ends[label];
    }
    
    private void setupArrays(final RopMethod method) {
        final BasicBlockList blocks = method.getBlocks();
        for (int sz = blocks.size(), i = 0; i < sz; ++i) {
            final BasicBlock one = blocks.get(i);
            final int label = one.getLabel();
            final Insn insn = one.getInsns().get(0);
            this.starts[label] = new CodeAddress(insn.getPosition());
            final SourcePosition pos = one.getLastInsn().getPosition();
            this.lasts[label] = new CodeAddress(pos);
            this.ends[label] = new CodeAddress(pos);
        }
    }
}

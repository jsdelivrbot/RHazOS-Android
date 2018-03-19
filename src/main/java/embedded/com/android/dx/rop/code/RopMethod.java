package embedded.com.android.dx.rop.code;

import embedded.com.android.dx.util.*;

public final class RopMethod
{
    private final BasicBlockList blocks;
    private final int firstLabel;
    private IntList[] predecessors;
    private IntList exitPredecessors;
    
    public RopMethod(final BasicBlockList blocks, final int firstLabel) {
        if (blocks == null) {
            throw new NullPointerException("blocks == null");
        }
        if (firstLabel < 0) {
            throw new IllegalArgumentException("firstLabel < 0");
        }
        this.blocks = blocks;
        this.firstLabel = firstLabel;
        this.predecessors = null;
        this.exitPredecessors = null;
    }
    
    public BasicBlockList getBlocks() {
        return this.blocks;
    }
    
    public int getFirstLabel() {
        return this.firstLabel;
    }
    
    public IntList labelToPredecessors(final int label) {
        if (this.exitPredecessors == null) {
            this.calcPredecessors();
        }
        final IntList result = this.predecessors[label];
        if (result == null) {
            throw new RuntimeException("no such block: " + Hex.u2(label));
        }
        return result;
    }
    
    public IntList getExitPredecessors() {
        if (this.exitPredecessors == null) {
            this.calcPredecessors();
        }
        return this.exitPredecessors;
    }
    
    public RopMethod withRegisterOffset(final int delta) {
        final RopMethod result = new RopMethod(this.blocks.withRegisterOffset(delta), this.firstLabel);
        if (this.exitPredecessors != null) {
            result.exitPredecessors = this.exitPredecessors;
            result.predecessors = this.predecessors;
        }
        return result;
    }
    
    private void calcPredecessors() {
        final int maxLabel = this.blocks.getMaxLabel();
        final IntList[] predecessors = new IntList[maxLabel];
        final IntList exitPredecessors = new IntList(10);
        for (int sz = this.blocks.size(), i = 0; i < sz; ++i) {
            final BasicBlock one = this.blocks.get(i);
            final int label = one.getLabel();
            final IntList successors = one.getSuccessors();
            final int ssz = successors.size();
            if (ssz == 0) {
                exitPredecessors.add(label);
            }
            else {
                for (int j = 0; j < ssz; ++j) {
                    final int succLabel = successors.get(j);
                    IntList succPreds = predecessors[succLabel];
                    if (succPreds == null) {
                        succPreds = new IntList(10);
                        predecessors[succLabel] = succPreds;
                    }
                    succPreds.add(label);
                }
            }
        }
        for (final IntList preds : predecessors) {
            if (preds != null) {
                preds.sort();
                preds.setImmutable();
            }
        }
        exitPredecessors.sort();
        exitPredecessors.setImmutable();
        if (predecessors[this.firstLabel] == null) {
            predecessors[this.firstLabel] = IntList.EMPTY;
        }
        this.predecessors = predecessors;
        this.exitPredecessors = exitPredecessors;
    }
}

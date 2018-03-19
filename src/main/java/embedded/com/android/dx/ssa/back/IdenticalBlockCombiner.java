package embedded.com.android.dx.ssa.back;

import java.util.*;
import embedded.com.android.dx.util.*;
import embedded.com.android.dx.rop.code.*;

public class IdenticalBlockCombiner
{
    private final RopMethod ropMethod;
    private final BasicBlockList blocks;
    private final BasicBlockList newBlocks;
    
    public IdenticalBlockCombiner(final RopMethod rm) {
        this.ropMethod = rm;
        this.blocks = this.ropMethod.getBlocks();
        this.newBlocks = this.blocks.getMutableCopy();
    }
    
    public RopMethod process() {
        final int szBlocks = this.blocks.size();
        final BitSet toDelete = new BitSet(this.blocks.getMaxLabel());
        for (int bindex = 0; bindex < szBlocks; ++bindex) {
            final BasicBlock b = this.blocks.get(bindex);
            if (!toDelete.get(b.getLabel())) {
                final IntList preds = this.ropMethod.labelToPredecessors(b.getLabel());
                for (int szPreds = preds.size(), i = 0; i < szPreds; ++i) {
                    final int iLabel = preds.get(i);
                    final BasicBlock iBlock = this.blocks.labelToBlock(iLabel);
                    if (!toDelete.get(iLabel) && iBlock.getSuccessors().size() <= 1) {
                        if (iBlock.getFirstInsn().getOpcode().getOpcode() != 55) {
                            final IntList toCombine = new IntList();
                            for (int j = i + 1; j < szPreds; ++j) {
                                final int jLabel = preds.get(j);
                                final BasicBlock jBlock = this.blocks.labelToBlock(jLabel);
                                if (jBlock.getSuccessors().size() == 1 && compareInsns(iBlock, jBlock)) {
                                    toCombine.add(jLabel);
                                    toDelete.set(jLabel);
                                }
                            }
                            this.combineBlocks(iLabel, toCombine);
                        }
                    }
                }
            }
        }
        for (int k = szBlocks - 1; k >= 0; --k) {
            if (toDelete.get(this.newBlocks.get(k).getLabel())) {
                this.newBlocks.set(k, null);
            }
        }
        this.newBlocks.shrinkToFit();
        this.newBlocks.setImmutable();
        return new RopMethod(this.newBlocks, this.ropMethod.getFirstLabel());
    }
    
    private static boolean compareInsns(final BasicBlock a, final BasicBlock b) {
        return a.getInsns().contentEquals(b.getInsns());
    }
    
    private void combineBlocks(final int alphaLabel, final IntList betaLabels) {
        for (int szBetas = betaLabels.size(), i = 0; i < szBetas; ++i) {
            final int betaLabel = betaLabels.get(i);
            final BasicBlock bb = this.blocks.labelToBlock(betaLabel);
            final IntList preds = this.ropMethod.labelToPredecessors(bb.getLabel());
            for (int szPreds = preds.size(), j = 0; j < szPreds; ++j) {
                final BasicBlock predBlock = this.newBlocks.labelToBlock(preds.get(j));
                this.replaceSucc(predBlock, betaLabel, alphaLabel);
            }
        }
    }
    
    private void replaceSucc(final BasicBlock block, final int oldLabel, final int newLabel) {
        final IntList newSuccessors = block.getSuccessors().mutableCopy();
        newSuccessors.set(newSuccessors.indexOf(oldLabel), newLabel);
        int newPrimarySuccessor = block.getPrimarySuccessor();
        if (newPrimarySuccessor == oldLabel) {
            newPrimarySuccessor = newLabel;
        }
        newSuccessors.setImmutable();
        final BasicBlock newBB = new BasicBlock(block.getLabel(), block.getInsns(), newSuccessors, newPrimarySuccessor);
        this.newBlocks.set(this.newBlocks.indexOfLabel(block.getLabel()), newBB);
    }
}

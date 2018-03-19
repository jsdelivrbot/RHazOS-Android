package embedded.com.android.dx.rop.code;

import embedded.com.android.dx.util.*;

public final class LocalVariableExtractor
{
    private final RopMethod method;
    private final BasicBlockList blocks;
    private final LocalVariableInfo resultInfo;
    private final int[] workSet;
    
    public static LocalVariableInfo extract(final RopMethod method) {
        final LocalVariableExtractor lve = new LocalVariableExtractor(method);
        return lve.doit();
    }
    
    private LocalVariableExtractor(final RopMethod method) {
        if (method == null) {
            throw new NullPointerException("method == null");
        }
        final BasicBlockList blocks = method.getBlocks();
        final int maxLabel = blocks.getMaxLabel();
        this.method = method;
        this.blocks = blocks;
        this.resultInfo = new LocalVariableInfo(method);
        this.workSet = Bits.makeBitSet(maxLabel);
    }
    
    private LocalVariableInfo doit() {
        for (int label = this.method.getFirstLabel(); label >= 0; label = Bits.findFirst(this.workSet, 0)) {
            Bits.clear(this.workSet, label);
            this.processBlock(label);
        }
        this.resultInfo.setImmutable();
        return this.resultInfo;
    }
    
    private void processBlock(final int label) {
        RegisterSpecSet primaryState = this.resultInfo.mutableCopyOfStarts(label);
        final BasicBlock block = this.blocks.labelToBlock(label);
        final InsnList insns = block.getInsns();
        final int insnSz = insns.size();
        final boolean canThrowDuringLastInsn = block.hasExceptionHandlers() && insns.getLast().getResult() != null;
        final int freezeSecondaryStateAt = insnSz - 1;
        final RegisterSpecSet secondaryState = primaryState;
        for (int i = 0; i < insnSz; ++i) {
            if (canThrowDuringLastInsn && i == freezeSecondaryStateAt) {
                primaryState.setImmutable();
                primaryState = primaryState.mutableCopy();
            }
            final Insn insn = insns.get(i);
            RegisterSpec result = insn.getLocalAssignment();
            if (result == null) {
                result = insn.getResult();
                if (result != null && primaryState.get(result.getReg()) != null) {
                    primaryState.remove(primaryState.get(result.getReg()));
                }
            }
            else {
                result = result.withSimpleType();
                final RegisterSpec already = primaryState.get(result);
                if (!result.equals(already)) {
                    final RegisterSpec previous = primaryState.localItemToSpec(result.getLocalItem());
                    if (previous != null && previous.getReg() != result.getReg()) {
                        primaryState.remove(previous);
                    }
                    this.resultInfo.addAssignment(insn, result);
                    primaryState.put(result);
                }
            }
        }
        primaryState.setImmutable();
        final IntList successors = block.getSuccessors();
        final int succSz = successors.size();
        final int primarySuccessor = block.getPrimarySuccessor();
        for (int j = 0; j < succSz; ++j) {
            final int succ = successors.get(j);
            final RegisterSpecSet state = (succ == primarySuccessor) ? primaryState : secondaryState;
            if (this.resultInfo.mergeStarts(succ, state)) {
                Bits.set(this.workSet, succ);
            }
        }
    }
}

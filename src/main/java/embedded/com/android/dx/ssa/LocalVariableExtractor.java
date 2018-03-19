package embedded.com.android.dx.ssa;

import java.util.*;
import embedded.com.android.dx.rop.code.*;
import embedded.com.android.dx.util.*;

public class LocalVariableExtractor
{
    private final SsaMethod method;
    private final ArrayList<SsaBasicBlock> blocks;
    private final LocalVariableInfo resultInfo;
    private final BitSet workSet;
    
    public static LocalVariableInfo extract(final SsaMethod method) {
        final LocalVariableExtractor lve = new LocalVariableExtractor(method);
        return lve.doit();
    }
    
    private LocalVariableExtractor(final SsaMethod method) {
        if (method == null) {
            throw new NullPointerException("method == null");
        }
        final ArrayList<SsaBasicBlock> blocks = method.getBlocks();
        this.method = method;
        this.blocks = blocks;
        this.resultInfo = new LocalVariableInfo(method);
        this.workSet = new BitSet(blocks.size());
    }
    
    private LocalVariableInfo doit() {
        if (this.method.getRegCount() > 0) {
            for (int bi = this.method.getEntryBlockIndex(); bi >= 0; bi = this.workSet.nextSetBit(0)) {
                this.workSet.clear(bi);
                this.processBlock(bi);
            }
        }
        this.resultInfo.setImmutable();
        return this.resultInfo;
    }
    
    private void processBlock(final int blockIndex) {
        RegisterSpecSet primaryState = this.resultInfo.mutableCopyOfStarts(blockIndex);
        final SsaBasicBlock block = this.blocks.get(blockIndex);
        final List<SsaInsn> insns = block.getInsns();
        final int insnSz = insns.size();
        if (blockIndex == this.method.getExitBlockIndex()) {
            return;
        }
        final SsaInsn lastInsn = insns.get(insnSz - 1);
        final boolean hasExceptionHandlers = lastInsn.getOriginalRopInsn().getCatches().size() != 0;
        final boolean canThrowDuringLastInsn = hasExceptionHandlers && lastInsn.getResult() != null;
        final int freezeSecondaryStateAt = insnSz - 1;
        final RegisterSpecSet secondaryState = primaryState;
        for (int i = 0; i < insnSz; ++i) {
            if (canThrowDuringLastInsn && i == freezeSecondaryStateAt) {
                primaryState.setImmutable();
                primaryState = primaryState.mutableCopy();
            }
            final SsaInsn insn = insns.get(i);
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
        final IntList successors = block.getSuccessorList();
        final int succSz = successors.size();
        final int primarySuccessor = block.getPrimarySuccessorIndex();
        for (int j = 0; j < succSz; ++j) {
            final int succ = successors.get(j);
            final RegisterSpecSet state = (succ == primarySuccessor) ? primaryState : secondaryState;
            if (this.resultInfo.mergeStarts(succ, state)) {
                this.workSet.set(succ);
            }
        }
    }
}

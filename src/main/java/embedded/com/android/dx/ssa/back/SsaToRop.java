package embedded.com.android.dx.ssa.back;

import embedded.com.android.dx.ssa.*;
import embedded.com.android.dx.util.*;
import java.util.*;
import embedded.com.android.dx.rop.code.*;

public class SsaToRop
{
    private static final boolean DEBUG = false;
    private final SsaMethod ssaMeth;
    private final boolean minimizeRegisters;
    private final InterferenceGraph interference;
    
    public static RopMethod convertToRopMethod(final SsaMethod ssaMeth, final boolean minimizeRegisters) {
        return new SsaToRop(ssaMeth, minimizeRegisters).convert();
    }
    
    private SsaToRop(final SsaMethod ssaMethod, final boolean minimizeRegisters) {
        this.minimizeRegisters = minimizeRegisters;
        this.ssaMeth = ssaMethod;
        this.interference = LivenessAnalyzer.constructInterferenceGraph(ssaMethod);
    }
    
    private RopMethod convert() {
        final RegisterAllocator allocator = new FirstFitLocalCombiningAllocator(this.ssaMeth, this.interference, this.minimizeRegisters);
        final RegisterMapper mapper = allocator.allocateRegisters();
        this.ssaMeth.setBackMode();
        this.ssaMeth.mapRegisters(mapper);
        this.removePhiFunctions();
        if (allocator.wantsParamsMovedHigh()) {
            this.moveParametersToHighRegisters();
        }
        this.removeEmptyGotos();
        RopMethod ropMethod = new RopMethod(this.convertBasicBlocks(), this.ssaMeth.blockIndexToRopLabel(this.ssaMeth.getEntryBlockIndex()));
        ropMethod = new IdenticalBlockCombiner(ropMethod).process();
        return ropMethod;
    }
    
    private void removeEmptyGotos() {
        final ArrayList<SsaBasicBlock> blocks = this.ssaMeth.getBlocks();
        this.ssaMeth.forEachBlockDepthFirst(false, new SsaBasicBlock.Visitor() {
            @Override
            public void visitBlock(final SsaBasicBlock b, final SsaBasicBlock parent) {
                final ArrayList<SsaInsn> insns = b.getInsns();
                if (insns.size() == 1 && insns.get(0).getOpcode() == Rops.GOTO) {
                    final BitSet preds = (BitSet)b.getPredecessors().clone();
                    for (int i = preds.nextSetBit(0); i >= 0; i = preds.nextSetBit(i + 1)) {
                        final SsaBasicBlock pb = blocks.get(i);
                        pb.replaceSuccessor(b.getIndex(), b.getPrimarySuccessorIndex());
                    }
                }
            }
        });
    }
    
    private void removePhiFunctions() {
        final ArrayList<SsaBasicBlock> blocks = this.ssaMeth.getBlocks();
        for (final SsaBasicBlock block : blocks) {
            block.forEachPhiInsn(new PhiVisitor(blocks));
            block.removeAllPhiInsns();
        }
        for (final SsaBasicBlock block : blocks) {
            block.scheduleMovesFromPhis();
        }
    }
    
    private void moveParametersToHighRegisters() {
        final int paramWidth = this.ssaMeth.getParamWidth();
        final BasicRegisterMapper mapper = new BasicRegisterMapper(this.ssaMeth.getRegCount());
        for (int regCount = this.ssaMeth.getRegCount(), i = 0; i < regCount; ++i) {
            if (i < paramWidth) {
                mapper.addMapping(i, regCount - paramWidth + i, 1);
            }
            else {
                mapper.addMapping(i, i - paramWidth, 1);
            }
        }
        this.ssaMeth.mapRegisters(mapper);
    }
    
    private BasicBlockList convertBasicBlocks() {
        final ArrayList<SsaBasicBlock> blocks = this.ssaMeth.getBlocks();
        final SsaBasicBlock exitBlock = this.ssaMeth.getExitBlock();
        this.ssaMeth.computeReachability();
        int ropBlockCount = this.ssaMeth.getCountReachableBlocks();
        ropBlockCount -= ((exitBlock != null && exitBlock.isReachable()) ? 1 : 0);
        final BasicBlockList result = new BasicBlockList(ropBlockCount);
        int ropBlockIndex = 0;
        for (final SsaBasicBlock b : blocks) {
            if (b.isReachable() && b != exitBlock) {
                result.set(ropBlockIndex++, this.convertBasicBlock(b));
            }
        }
        if (exitBlock != null && exitBlock.getInsns().size() != 0) {
            throw new RuntimeException("Exit block must have no insns when leaving SSA form");
        }
        return result;
    }
    
    private void verifyValidExitPredecessor(final SsaBasicBlock b) {
        final ArrayList<SsaInsn> insns = b.getInsns();
        final SsaInsn lastInsn = insns.get(insns.size() - 1);
        final Rop opcode = lastInsn.getOpcode();
        if (opcode.getBranchingness() != 2 && opcode != Rops.THROW) {
            throw new RuntimeException("Exit predecessor must end in valid exit statement.");
        }
    }
    
    private BasicBlock convertBasicBlock(final SsaBasicBlock block) {
        IntList successorList = block.getRopLabelSuccessorList();
        int primarySuccessorLabel = block.getPrimarySuccessorRopLabel();
        final SsaBasicBlock exitBlock = this.ssaMeth.getExitBlock();
        final int exitRopLabel = (exitBlock == null) ? -1 : exitBlock.getRopLabel();
        if (successorList.contains(exitRopLabel)) {
            if (successorList.size() > 1) {
                throw new RuntimeException("Exit predecessor must have no other successors" + Hex.u2(block.getRopLabel()));
            }
            successorList = IntList.EMPTY;
            primarySuccessorLabel = -1;
            this.verifyValidExitPredecessor(block);
        }
        successorList.setImmutable();
        final BasicBlock result = new BasicBlock(block.getRopLabel(), this.convertInsns(block.getInsns()), successorList, primarySuccessorLabel);
        return result;
    }
    
    private InsnList convertInsns(final ArrayList<SsaInsn> ssaInsns) {
        final int insnCount = ssaInsns.size();
        final InsnList result = new InsnList(insnCount);
        for (int i = 0; i < insnCount; ++i) {
            result.set(i, ssaInsns.get(i).toRopInsn());
        }
        result.setImmutable();
        return result;
    }
    
    public int[] getRegistersByFrequency() {
        final int regCount = this.ssaMeth.getRegCount();
        final Integer[] ret = new Integer[regCount];
        for (int i = 0; i < regCount; ++i) {
            ret[i] = i;
        }
        Arrays.sort(ret, new Comparator<Integer>() {
            @Override
            public int compare(final Integer o1, final Integer o2) {
                return SsaToRop.this.ssaMeth.getUseListForRegister(o2).size() - SsaToRop.this.ssaMeth.getUseListForRegister(o1).size();
            }
        });
        final int[] result = new int[regCount];
        for (int j = 0; j < regCount; ++j) {
            result[j] = ret[j];
        }
        return result;
    }
    
    private static class PhiVisitor implements PhiInsn.Visitor
    {
        private final ArrayList<SsaBasicBlock> blocks;
        
        public PhiVisitor(final ArrayList<SsaBasicBlock> blocks) {
            this.blocks = blocks;
        }
        
        @Override
        public void visitPhiInsn(final PhiInsn insn) {
            final RegisterSpecList sources = insn.getSources();
            final RegisterSpec result = insn.getResult();
            for (int sz = sources.size(), i = 0; i < sz; ++i) {
                final RegisterSpec source = sources.get(i);
                final SsaBasicBlock predBlock = this.blocks.get(insn.predBlockIndexForSourcesIndex(i));
                predBlock.addMoveToEnd(result, source);
            }
        }
    }
}

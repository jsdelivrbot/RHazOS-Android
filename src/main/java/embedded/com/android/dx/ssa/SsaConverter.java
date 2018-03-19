package embedded.com.android.dx.ssa;

import java.util.*;
import embedded.com.android.dx.rop.code.*;
import embedded.com.android.dx.util.*;

public class SsaConverter
{
    public static final boolean DEBUG = false;
    
    public static SsaMethod convertToSsaMethod(final RopMethod rmeth, final int paramWidth, final boolean isStatic) {
        final SsaMethod result = SsaMethod.newFromRopMethod(rmeth, paramWidth, isStatic);
        edgeSplit(result);
        final LocalVariableInfo localInfo = LocalVariableExtractor.extract(result);
        placePhiFunctions(result, localInfo, 0);
        new SsaRenamer(result).run();
        result.makeExitBlock();
        return result;
    }
    
    public static void updateSsaMethod(final SsaMethod ssaMeth, final int threshold) {
        final LocalVariableInfo localInfo = LocalVariableExtractor.extract(ssaMeth);
        placePhiFunctions(ssaMeth, localInfo, threshold);
        new SsaRenamer(ssaMeth, threshold).run();
    }
    
    public static SsaMethod testEdgeSplit(final RopMethod rmeth, final int paramWidth, final boolean isStatic) {
        final SsaMethod result = SsaMethod.newFromRopMethod(rmeth, paramWidth, isStatic);
        edgeSplit(result);
        return result;
    }
    
    public static SsaMethod testPhiPlacement(final RopMethod rmeth, final int paramWidth, final boolean isStatic) {
        final SsaMethod result = SsaMethod.newFromRopMethod(rmeth, paramWidth, isStatic);
        edgeSplit(result);
        final LocalVariableInfo localInfo = LocalVariableExtractor.extract(result);
        placePhiFunctions(result, localInfo, 0);
        return result;
    }
    
    private static void edgeSplit(final SsaMethod result) {
        edgeSplitPredecessors(result);
        edgeSplitMoveExceptionsAndResults(result);
        edgeSplitSuccessors(result);
    }
    
    private static void edgeSplitPredecessors(final SsaMethod result) {
        final ArrayList<SsaBasicBlock> blocks = result.getBlocks();
        for (int i = blocks.size() - 1; i >= 0; --i) {
            final SsaBasicBlock block = blocks.get(i);
            if (nodeNeedsUniquePredecessor(block)) {
                block.insertNewPredecessor();
            }
        }
    }
    
    private static boolean nodeNeedsUniquePredecessor(final SsaBasicBlock block) {
        final int countPredecessors = block.getPredecessors().cardinality();
        final int countSuccessors = block.getSuccessors().cardinality();
        return countPredecessors > 1 && countSuccessors > 1;
    }
    
    private static void edgeSplitMoveExceptionsAndResults(final SsaMethod ssaMeth) {
        final ArrayList<SsaBasicBlock> blocks = ssaMeth.getBlocks();
        for (int i = blocks.size() - 1; i >= 0; --i) {
            final SsaBasicBlock block = blocks.get(i);
            if (!block.isExitBlock() && block.getPredecessors().cardinality() > 1 && block.getInsns().get(0).isMoveException()) {
                final BitSet preds = (BitSet)block.getPredecessors().clone();
                for (int j = preds.nextSetBit(0); j >= 0; j = preds.nextSetBit(j + 1)) {
                    final SsaBasicBlock predecessor = blocks.get(j);
                    final SsaBasicBlock zNode = predecessor.insertNewSuccessor(block);
                    zNode.getInsns().add(0, block.getInsns().get(0).clone());
                }
                block.getInsns().remove(0);
            }
        }
    }
    
    private static void edgeSplitSuccessors(final SsaMethod result) {
        final ArrayList<SsaBasicBlock> blocks = result.getBlocks();
        for (int i = blocks.size() - 1; i >= 0; --i) {
            final SsaBasicBlock block = blocks.get(i);
            final BitSet successors = (BitSet)block.getSuccessors().clone();
            for (int j = successors.nextSetBit(0); j >= 0; j = successors.nextSetBit(j + 1)) {
                final SsaBasicBlock succ = blocks.get(j);
                if (needsNewSuccessor(block, succ)) {
                    block.insertNewSuccessor(succ);
                }
            }
        }
    }
    
    private static boolean needsNewSuccessor(final SsaBasicBlock block, final SsaBasicBlock succ) {
        final ArrayList<SsaInsn> insns = block.getInsns();
        final SsaInsn lastInsn = insns.get(insns.size() - 1);
        return (lastInsn.getResult() != null || lastInsn.getSources().size() > 0) && succ.getPredecessors().cardinality() > 1;
    }
    
    private static void placePhiFunctions(final SsaMethod ssaMeth, final LocalVariableInfo localInfo, final int threshold) {
        final ArrayList<SsaBasicBlock> ssaBlocks = ssaMeth.getBlocks();
        final int blockCount = ssaBlocks.size();
        final int regCount = ssaMeth.getRegCount() - threshold;
        final DomFront df = new DomFront(ssaMeth);
        final DomFront.DomInfo[] domInfos = df.run();
        final BitSet[] defsites = new BitSet[regCount];
        final BitSet[] phisites = new BitSet[regCount];
        for (int i = 0; i < regCount; ++i) {
            defsites[i] = new BitSet(blockCount);
            phisites[i] = new BitSet(blockCount);
        }
        for (int bi = 0, s = ssaBlocks.size(); bi < s; ++bi) {
            final SsaBasicBlock b = ssaBlocks.get(bi);
            for (final SsaInsn insn : b.getInsns()) {
                final RegisterSpec rs = insn.getResult();
                if (rs != null && rs.getReg() - threshold >= 0) {
                    defsites[rs.getReg() - threshold].set(bi);
                }
            }
        }
        for (int reg = 0, s2 = regCount; reg < s2; ++reg) {
            final BitSet worklist = (BitSet)defsites[reg].clone();
            int workBlockIndex;
            while (0 <= (workBlockIndex = worklist.nextSetBit(0))) {
                worklist.clear(workBlockIndex);
                IntIterator iter;
                for (iter = domInfos[workBlockIndex].dominanceFrontiers.iterator(); iter.hasNext();) {
                    final int dfBlockIndex = iter.next();
                    if (!phisites[reg].get(dfBlockIndex)) {
                        phisites[reg].set(dfBlockIndex);
                        final int tReg = reg + threshold;
                        final RegisterSpec rs2 = localInfo.getStarts(dfBlockIndex).get(tReg);
                        if (rs2 == null) {
                            ssaBlocks.get(dfBlockIndex).addPhiInsnForReg(tReg);
                        }
                        else {
                            ssaBlocks.get(dfBlockIndex).addPhiInsnForReg(rs2);
                        }
                        if (defsites[reg].get(dfBlockIndex)) {
                            continue;
                        }
                        worklist.set(dfBlockIndex);
                    }
                }
            }
        }
    }
}

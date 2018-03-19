package embedded.com.android.dx.ssa.back;

import embedded.com.android.dx.ssa.*;
import java.util.*;
import embedded.com.android.dx.rop.code.*;

public class LivenessAnalyzer
{
    private final BitSet visitedBlocks;
    private final BitSet liveOutBlocks;
    private final int regV;
    private final SsaMethod ssaMeth;
    private final InterferenceGraph interference;
    private SsaBasicBlock blockN;
    private int statementIndex;
    private NextFunction nextFunction;
    
    public static InterferenceGraph constructInterferenceGraph(final SsaMethod ssaMeth) {
        final int szRegs = ssaMeth.getRegCount();
        final InterferenceGraph interference = new InterferenceGraph(szRegs);
        for (int i = 0; i < szRegs; ++i) {
            new LivenessAnalyzer(ssaMeth, i, interference).run();
        }
        coInterferePhis(ssaMeth, interference);
        return interference;
    }
    
    private LivenessAnalyzer(final SsaMethod ssaMeth, final int reg, final InterferenceGraph interference) {
        final int blocksSz = ssaMeth.getBlocks().size();
        this.ssaMeth = ssaMeth;
        this.regV = reg;
        this.visitedBlocks = new BitSet(blocksSz);
        this.liveOutBlocks = new BitSet(blocksSz);
        this.interference = interference;
    }
    
    private void handleTailRecursion() {
        while (this.nextFunction != NextFunction.DONE) {
            switch (this.nextFunction) {
                case LIVE_IN_AT_STATEMENT: {
                    this.nextFunction = NextFunction.DONE;
                    this.liveInAtStatement();
                    continue;
                }
                case LIVE_OUT_AT_STATEMENT: {
                    this.nextFunction = NextFunction.DONE;
                    this.liveOutAtStatement();
                    continue;
                }
                case LIVE_OUT_AT_BLOCK: {
                    this.nextFunction = NextFunction.DONE;
                    this.liveOutAtBlock();
                    continue;
                }
                default: {
                    continue;
                }
            }
        }
    }
    
    public void run() {
        final List<SsaInsn> useList = this.ssaMeth.getUseListForRegister(this.regV);
        for (final SsaInsn insn : useList) {
            this.nextFunction = NextFunction.DONE;
            if (insn instanceof PhiInsn) {
                final PhiInsn phi = (PhiInsn)insn;
                for (final SsaBasicBlock pred : phi.predBlocksForReg(this.regV, this.ssaMeth)) {
                    this.blockN = pred;
                    this.nextFunction = NextFunction.LIVE_OUT_AT_BLOCK;
                    this.handleTailRecursion();
                }
            }
            else {
                this.blockN = insn.getBlock();
                this.statementIndex = this.blockN.getInsns().indexOf(insn);
                if (this.statementIndex < 0) {
                    throw new RuntimeException("insn not found in it's own block");
                }
                this.nextFunction = NextFunction.LIVE_IN_AT_STATEMENT;
                this.handleTailRecursion();
            }
        }
        int nextLiveOutBlock;
        while ((nextLiveOutBlock = this.liveOutBlocks.nextSetBit(0)) >= 0) {
            this.blockN = this.ssaMeth.getBlocks().get(nextLiveOutBlock);
            this.liveOutBlocks.clear(nextLiveOutBlock);
            this.nextFunction = NextFunction.LIVE_OUT_AT_BLOCK;
            this.handleTailRecursion();
        }
    }
    
    private void liveOutAtBlock() {
        if (!this.visitedBlocks.get(this.blockN.getIndex())) {
            this.visitedBlocks.set(this.blockN.getIndex());
            this.blockN.addLiveOut(this.regV);
            final ArrayList<SsaInsn> insns = this.blockN.getInsns();
            this.statementIndex = insns.size() - 1;
            this.nextFunction = NextFunction.LIVE_OUT_AT_STATEMENT;
        }
    }
    
    private void liveInAtStatement() {
        if (this.statementIndex == 0) {
            this.blockN.addLiveIn(this.regV);
            final BitSet preds = this.blockN.getPredecessors();
            this.liveOutBlocks.or(preds);
        }
        else {
            --this.statementIndex;
            this.nextFunction = NextFunction.LIVE_OUT_AT_STATEMENT;
        }
    }
    
    private void liveOutAtStatement() {
        final SsaInsn statement = this.blockN.getInsns().get(this.statementIndex);
        final RegisterSpec rs = statement.getResult();
        if (!statement.isResultReg(this.regV)) {
            if (rs != null) {
                this.interference.add(this.regV, rs.getReg());
            }
            this.nextFunction = NextFunction.LIVE_IN_AT_STATEMENT;
        }
    }
    
    private static void coInterferePhis(final SsaMethod ssaMeth, final InterferenceGraph interference) {
        for (final SsaBasicBlock b : ssaMeth.getBlocks()) {
            final List<SsaInsn> phis = b.getPhiInsns();
            for (int szPhis = phis.size(), i = 0; i < szPhis; ++i) {
                for (int j = 0; j < szPhis; ++j) {
                    if (i != j) {
                        interference.add(phis.get(i).getResult().getReg(), phis.get(j).getResult().getReg());
                    }
                }
            }
        }
    }
    
    private enum NextFunction
    {
        LIVE_IN_AT_STATEMENT, 
        LIVE_OUT_AT_STATEMENT, 
        LIVE_OUT_AT_BLOCK, 
        DONE;
    }
}

package embedded.com.android.dx.ssa;

import embedded.com.android.dx.rop.code.*;
import java.util.*;

public class DeadCodeRemover
{
    private final SsaMethod ssaMeth;
    private final int regCount;
    private final BitSet worklist;
    private final ArrayList<SsaInsn>[] useList;
    
    public static void process(final SsaMethod ssaMethod) {
        final DeadCodeRemover dc = new DeadCodeRemover(ssaMethod);
        dc.run();
    }
    
    private DeadCodeRemover(final SsaMethod ssaMethod) {
        this.ssaMeth = ssaMethod;
        this.regCount = ssaMethod.getRegCount();
        this.worklist = new BitSet(this.regCount);
        this.useList = this.ssaMeth.getUseListCopy();
    }
    
    private void run() {
        this.pruneDeadInstructions();
        final HashSet<SsaInsn> deletedInsns = new HashSet<SsaInsn>();
        this.ssaMeth.forEachInsn(new NoSideEffectVisitor(this.worklist));
        int regV;
        while (0 <= (regV = this.worklist.nextSetBit(0))) {
            this.worklist.clear(regV);
            if (this.useList[regV].size() == 0 || this.isCircularNoSideEffect(regV, null)) {
                final SsaInsn insnS = this.ssaMeth.getDefinitionForRegister(regV);
                if (deletedInsns.contains(insnS)) {
                    continue;
                }
                final RegisterSpecList sources = insnS.getSources();
                for (int sz = sources.size(), i = 0; i < sz; ++i) {
                    final RegisterSpec source = sources.get(i);
                    this.useList[source.getReg()].remove(insnS);
                    if (!hasSideEffect(this.ssaMeth.getDefinitionForRegister(source.getReg()))) {
                        this.worklist.set(source.getReg());
                    }
                }
                deletedInsns.add(insnS);
            }
        }
        this.ssaMeth.deleteInsns(deletedInsns);
    }
    
    private void pruneDeadInstructions() {
        final HashSet<SsaInsn> deletedInsns = new HashSet<SsaInsn>();
        this.ssaMeth.computeReachability();
        for (final SsaBasicBlock block : this.ssaMeth.getBlocks()) {
            if (block.isReachable()) {
                continue;
            }
            for (int i = 0; i < block.getInsns().size(); ++i) {
                final SsaInsn insn = block.getInsns().get(i);
                final RegisterSpecList sources = insn.getSources();
                final int sourcesSize = sources.size();
                if (sourcesSize != 0) {
                    deletedInsns.add(insn);
                }
                for (int j = 0; j < sourcesSize; ++j) {
                    final RegisterSpec source = sources.get(j);
                    this.useList[source.getReg()].remove(insn);
                }
                final RegisterSpec result = insn.getResult();
                if (result != null) {
                    for (final SsaInsn use : this.useList[result.getReg()]) {
                        if (use instanceof PhiInsn) {
                            final PhiInsn phiUse = (PhiInsn)use;
                            phiUse.removePhiRegister(result);
                        }
                    }
                }
            }
        }
        this.ssaMeth.deleteInsns(deletedInsns);
    }
    
    private boolean isCircularNoSideEffect(final int regV, BitSet set) {
        if (set != null && set.get(regV)) {
            return true;
        }
        for (final SsaInsn use : this.useList[regV]) {
            if (hasSideEffect(use)) {
                return false;
            }
        }
        if (set == null) {
            set = new BitSet(this.regCount);
        }
        set.set(regV);
        for (final SsaInsn use : this.useList[regV]) {
            final RegisterSpec result = use.getResult();
            if (result == null || !this.isCircularNoSideEffect(result.getReg(), set)) {
                return false;
            }
        }
        return true;
    }
    
    private static boolean hasSideEffect(final SsaInsn insn) {
        return insn == null || insn.hasSideEffect();
    }
    
    private static class NoSideEffectVisitor implements SsaInsn.Visitor
    {
        BitSet noSideEffectRegs;
        
        public NoSideEffectVisitor(final BitSet noSideEffectRegs) {
            this.noSideEffectRegs = noSideEffectRegs;
        }
        
        @Override
        public void visitMoveInsn(final NormalSsaInsn insn) {
            if (!hasSideEffect(insn)) {
                this.noSideEffectRegs.set(insn.getResult().getReg());
            }
        }
        
        @Override
        public void visitPhiInsn(final PhiInsn phi) {
            if (!hasSideEffect(phi)) {
                this.noSideEffectRegs.set(phi.getResult().getReg());
            }
        }
        
        @Override
        public void visitNonMoveInsn(final NormalSsaInsn insn) {
            final RegisterSpec result = insn.getResult();
            if (!hasSideEffect(insn) && result != null) {
                this.noSideEffectRegs.set(result.getReg());
            }
        }
    }
}

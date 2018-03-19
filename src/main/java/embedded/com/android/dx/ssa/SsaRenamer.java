package embedded.com.android.dx.ssa;

import embedded.com.android.dx.util.*;
import embedded.com.android.dx.rop.type.*;
import embedded.com.android.dx.rop.code.*;
import java.util.*;

public class SsaRenamer implements Runnable
{
    private static final boolean DEBUG = false;
    private final SsaMethod ssaMeth;
    private int nextSsaReg;
    private final int ropRegCount;
    private int threshold;
    private final RegisterSpec[][] startsForBlocks;
    private final ArrayList<LocalItem> ssaRegToLocalItems;
    private IntList ssaRegToRopReg;
    
    public SsaRenamer(final SsaMethod ssaMeth) {
        this.ropRegCount = ssaMeth.getRegCount();
        this.ssaMeth = ssaMeth;
        this.nextSsaReg = this.ropRegCount;
        this.threshold = 0;
        this.startsForBlocks = new RegisterSpec[ssaMeth.getBlocks().size()][];
        this.ssaRegToLocalItems = new ArrayList<LocalItem>();
        final RegisterSpec[] initialRegMapping = new RegisterSpec[this.ropRegCount];
        for (int i = 0; i < this.ropRegCount; ++i) {
            initialRegMapping[i] = RegisterSpec.make(i, Type.VOID);
        }
        this.startsForBlocks[ssaMeth.getEntryBlockIndex()] = initialRegMapping;
    }
    
    public SsaRenamer(final SsaMethod ssaMeth, final int thresh) {
        this(ssaMeth);
        this.threshold = thresh;
    }
    
    @Override
    public void run() {
        this.ssaMeth.forEachBlockDepthFirstDom(new SsaBasicBlock.Visitor() {
            @Override
            public void visitBlock(final SsaBasicBlock block, final SsaBasicBlock unused) {
                new BlockRenamer(block).process();
            }
        });
        this.ssaMeth.setNewRegCount(this.nextSsaReg);
        this.ssaMeth.onInsnsChanged();
    }
    
    private static RegisterSpec[] dupArray(final RegisterSpec[] orig) {
        final RegisterSpec[] copy = new RegisterSpec[orig.length];
        System.arraycopy(orig, 0, copy, 0, orig.length);
        return copy;
    }
    
    private LocalItem getLocalForNewReg(final int ssaReg) {
        if (ssaReg < this.ssaRegToLocalItems.size()) {
            return this.ssaRegToLocalItems.get(ssaReg);
        }
        return null;
    }
    
    private void setNameForSsaReg(final RegisterSpec ssaReg) {
        final int reg = ssaReg.getReg();
        final LocalItem local = ssaReg.getLocalItem();
        this.ssaRegToLocalItems.ensureCapacity(reg + 1);
        while (this.ssaRegToLocalItems.size() <= reg) {
            this.ssaRegToLocalItems.add(null);
        }
        this.ssaRegToLocalItems.set(reg, local);
    }
    
    private boolean isBelowThresholdRegister(final int ssaReg) {
        return ssaReg < this.threshold;
    }
    
    private boolean isVersionZeroRegister(final int ssaReg) {
        return ssaReg < this.ropRegCount;
    }
    
    private static boolean equalsHandlesNulls(final Object a, final Object b) {
        return a == b || (a != null && a.equals(b));
    }
    
    private class BlockRenamer implements SsaInsn.Visitor
    {
        private final SsaBasicBlock block;
        private final RegisterSpec[] currentMapping;
        private final HashSet<SsaInsn> movesToKeep;
        private final HashMap<SsaInsn, SsaInsn> insnsToReplace;
        private final RenamingMapper mapper;
        
        BlockRenamer(final SsaBasicBlock block) {
            this.block = block;
            this.currentMapping = SsaRenamer.this.startsForBlocks[block.getIndex()];
            this.movesToKeep = new HashSet<SsaInsn>();
            this.insnsToReplace = new HashMap<SsaInsn, SsaInsn>();
            this.mapper = new RenamingMapper();
            SsaRenamer.this.startsForBlocks[block.getIndex()] = null;
        }
        
        public void process() {
            this.block.forEachInsn(this);
            this.updateSuccessorPhis();
            final ArrayList<SsaInsn> insns = this.block.getInsns();
            final int szInsns = insns.size();
            for (int i = szInsns - 1; i >= 0; --i) {
                final SsaInsn insn = insns.get(i);
                final SsaInsn replaceInsn = this.insnsToReplace.get(insn);
                if (replaceInsn != null) {
                    insns.set(i, replaceInsn);
                }
                else if (insn.isNormalMoveInsn() && !this.movesToKeep.contains(insn)) {
                    insns.remove(i);
                }
            }
            boolean first = true;
            for (final SsaBasicBlock child : this.block.getDomChildren()) {
                if (child != this.block) {
                    final RegisterSpec[] childStart = first ? this.currentMapping : dupArray(this.currentMapping);
                    SsaRenamer.this.startsForBlocks[child.getIndex()] = childStart;
                    first = false;
                }
            }
        }
        
        private void addMapping(final int ropReg, final RegisterSpec ssaReg) {
            final int ssaRegNum = ssaReg.getReg();
            final LocalItem ssaRegLocal = ssaReg.getLocalItem();
            this.currentMapping[ropReg] = ssaReg;
            for (int i = this.currentMapping.length - 1; i >= 0; --i) {
                final RegisterSpec cur = this.currentMapping[i];
                if (ssaRegNum == cur.getReg()) {
                    this.currentMapping[i] = ssaReg;
                }
            }
            if (ssaRegLocal == null) {
                return;
            }
            SsaRenamer.this.setNameForSsaReg(ssaReg);
            for (int i = this.currentMapping.length - 1; i >= 0; --i) {
                final RegisterSpec cur = this.currentMapping[i];
                if (ssaRegNum != cur.getReg() && ssaRegLocal.equals(cur.getLocalItem())) {
                    this.currentMapping[i] = cur.withLocalItem(null);
                }
            }
        }
        
        @Override
        public void visitPhiInsn(final PhiInsn phi) {
            this.processResultReg(phi);
        }
        
        @Override
        public void visitMoveInsn(final NormalSsaInsn insn) {
            final RegisterSpec ropResult = insn.getResult();
            final int ropResultReg = ropResult.getReg();
            final int ropSourceReg = insn.getSources().get(0).getReg();
            insn.mapSourceRegisters(this.mapper);
            final int ssaSourceReg = insn.getSources().get(0).getReg();
            final LocalItem sourceLocal = this.currentMapping[ropSourceReg].getLocalItem();
            final LocalItem resultLocal = ropResult.getLocalItem();
            final LocalItem newLocal = (resultLocal == null) ? sourceLocal : resultLocal;
            final LocalItem associatedLocal = SsaRenamer.this.getLocalForNewReg(ssaSourceReg);
            final boolean onlyOneAssociatedLocal = associatedLocal == null || newLocal == null || newLocal.equals(associatedLocal);
            final RegisterSpec ssaReg = RegisterSpec.makeLocalOptional(ssaSourceReg, ropResult.getType(), newLocal);
            if (!Optimizer.getPreserveLocals() || (onlyOneAssociatedLocal && equalsHandlesNulls(newLocal, sourceLocal) && SsaRenamer.this.threshold == 0)) {
                this.addMapping(ropResultReg, ssaReg);
            }
            else if (onlyOneAssociatedLocal && sourceLocal == null && SsaRenamer.this.threshold == 0) {
                final RegisterSpecList ssaSources = RegisterSpecList.make(RegisterSpec.make(ssaReg.getReg(), ssaReg.getType(), newLocal));
                final SsaInsn newInsn = SsaInsn.makeFromRop(new PlainInsn(Rops.opMarkLocal(ssaReg), SourcePosition.NO_INFO, null, ssaSources), this.block);
                this.insnsToReplace.put(insn, newInsn);
                this.addMapping(ropResultReg, ssaReg);
            }
            else {
                this.processResultReg(insn);
                this.movesToKeep.add(insn);
            }
        }
        
        @Override
        public void visitNonMoveInsn(final NormalSsaInsn insn) {
            insn.mapSourceRegisters(this.mapper);
            this.processResultReg(insn);
        }
        
        void processResultReg(final SsaInsn insn) {
            final RegisterSpec ropResult = insn.getResult();
            if (ropResult == null) {
                return;
            }
            final int ropReg = ropResult.getReg();
            if (SsaRenamer.this.isBelowThresholdRegister(ropReg)) {
                return;
            }
            insn.changeResultReg(SsaRenamer.this.nextSsaReg);
            this.addMapping(ropReg, insn.getResult());
            SsaRenamer.this.nextSsaReg++;
        }
        
        private void updateSuccessorPhis() {
            final PhiInsn.Visitor visitor = new PhiInsn.Visitor() {
                @Override
                public void visitPhiInsn(final PhiInsn insn) {
                    final int ropReg = insn.getRopResultReg();
                    if (SsaRenamer.this.isBelowThresholdRegister(ropReg)) {
                        return;
                    }
                    final RegisterSpec stackTop = BlockRenamer.this.currentMapping[ropReg];
                    if (!SsaRenamer.this.isVersionZeroRegister(stackTop.getReg())) {
                        insn.addPhiOperand(stackTop, BlockRenamer.this.block);
                    }
                }
            };
            final BitSet successors = this.block.getSuccessors();
            for (int i = successors.nextSetBit(0); i >= 0; i = successors.nextSetBit(i + 1)) {
                final SsaBasicBlock successor = SsaRenamer.this.ssaMeth.getBlocks().get(i);
                successor.forEachPhiInsn(visitor);
            }
        }
        
        private class RenamingMapper extends RegisterMapper
        {
            @Override
            public int getNewRegisterCount() {
                return SsaRenamer.this.nextSsaReg;
            }
            
            @Override
            public RegisterSpec map(final RegisterSpec registerSpec) {
                if (registerSpec == null) {
                    return null;
                }
                final int reg = registerSpec.getReg();
                return registerSpec.withReg(BlockRenamer.this.currentMapping[reg].getReg());
            }
        }
    }
}

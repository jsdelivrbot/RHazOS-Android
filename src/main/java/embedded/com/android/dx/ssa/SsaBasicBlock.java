package embedded.com.android.dx.ssa;

import embedded.com.android.dx.util.*;
import embedded.com.android.dx.rop.type.*;
import embedded.com.android.dx.rop.code.*;
import java.util.*;

public final class SsaBasicBlock
{
    public static final Comparator<SsaBasicBlock> LABEL_COMPARATOR;
    private ArrayList<SsaInsn> insns;
    private BitSet predecessors;
    private BitSet successors;
    private IntList successorList;
    private int primarySuccessor;
    private int ropLabel;
    private SsaMethod parent;
    private int index;
    private final ArrayList<SsaBasicBlock> domChildren;
    private int movesFromPhisAtEnd;
    private int movesFromPhisAtBeginning;
    private int reachable;
    private IntSet liveIn;
    private IntSet liveOut;
    
    public SsaBasicBlock(final int basicBlockIndex, final int ropLabel, final SsaMethod parent) {
        this.primarySuccessor = -1;
        this.movesFromPhisAtEnd = 0;
        this.movesFromPhisAtBeginning = 0;
        this.reachable = -1;
        this.parent = parent;
        this.index = basicBlockIndex;
        this.insns = new ArrayList<SsaInsn>();
        this.ropLabel = ropLabel;
        this.predecessors = new BitSet(parent.getBlocks().size());
        this.successors = new BitSet(parent.getBlocks().size());
        this.successorList = new IntList();
        this.domChildren = new ArrayList<SsaBasicBlock>();
    }
    
    public static SsaBasicBlock newFromRop(final RopMethod rmeth, final int basicBlockIndex, final SsaMethod parent) {
        final BasicBlockList ropBlocks = rmeth.getBlocks();
        final BasicBlock bb = ropBlocks.get(basicBlockIndex);
        final SsaBasicBlock result = new SsaBasicBlock(basicBlockIndex, bb.getLabel(), parent);
        final InsnList ropInsns = bb.getInsns();
        result.insns.ensureCapacity(ropInsns.size());
        for (int i = 0, sz = ropInsns.size(); i < sz; ++i) {
            result.insns.add(new NormalSsaInsn(ropInsns.get(i), result));
        }
        result.predecessors = SsaMethod.bitSetFromLabelList(ropBlocks, rmeth.labelToPredecessors(bb.getLabel()));
        result.successors = SsaMethod.bitSetFromLabelList(ropBlocks, bb.getSuccessors());
        result.successorList = SsaMethod.indexListFromLabelList(ropBlocks, bb.getSuccessors());
        if (result.successorList.size() != 0) {
            final int primarySuccessor = bb.getPrimarySuccessor();
            result.primarySuccessor = ((primarySuccessor < 0) ? -1 : ropBlocks.indexOfLabel(primarySuccessor));
        }
        return result;
    }
    
    public void addDomChild(final SsaBasicBlock child) {
        this.domChildren.add(child);
    }
    
    public ArrayList<SsaBasicBlock> getDomChildren() {
        return this.domChildren;
    }
    
    public void addPhiInsnForReg(final int reg) {
        this.insns.add(0, new PhiInsn(reg, this));
    }
    
    public void addPhiInsnForReg(final RegisterSpec resultSpec) {
        this.insns.add(0, new PhiInsn(resultSpec, this));
    }
    
    public void addInsnToHead(final Insn insn) {
        final SsaInsn newInsn = SsaInsn.makeFromRop(insn, this);
        this.insns.add(this.getCountPhiInsns(), newInsn);
        this.parent.onInsnAdded(newInsn);
    }
    
    public void replaceLastInsn(final Insn insn) {
        if (insn.getOpcode().getBranchingness() == 1) {
            throw new IllegalArgumentException("last insn must branch");
        }
        final SsaInsn oldInsn = this.insns.get(this.insns.size() - 1);
        final SsaInsn newInsn = SsaInsn.makeFromRop(insn, this);
        this.insns.set(this.insns.size() - 1, newInsn);
        this.parent.onInsnRemoved(oldInsn);
        this.parent.onInsnAdded(newInsn);
    }
    
    public void forEachPhiInsn(final PhiInsn.Visitor v) {
        for (int sz = this.insns.size(), i = 0; i < sz; ++i) {
            final SsaInsn insn = this.insns.get(i);
            if (!(insn instanceof PhiInsn)) {
                break;
            }
            v.visitPhiInsn((PhiInsn)insn);
        }
    }
    
    public void removeAllPhiInsns() {
        this.insns.subList(0, this.getCountPhiInsns()).clear();
    }
    
    private int getCountPhiInsns() {
        int sz;
        int countPhiInsns;
        for (sz = this.insns.size(), countPhiInsns = 0; countPhiInsns < sz; ++countPhiInsns) {
            final SsaInsn insn = this.insns.get(countPhiInsns);
            if (!(insn instanceof PhiInsn)) {
                break;
            }
        }
        return countPhiInsns;
    }
    
    public ArrayList<SsaInsn> getInsns() {
        return this.insns;
    }
    
    public List<SsaInsn> getPhiInsns() {
        return this.insns.subList(0, this.getCountPhiInsns());
    }
    
    public int getIndex() {
        return this.index;
    }
    
    public int getRopLabel() {
        return this.ropLabel;
    }
    
    public String getRopLabelString() {
        return Hex.u2(this.ropLabel);
    }
    
    public BitSet getPredecessors() {
        return this.predecessors;
    }
    
    public BitSet getSuccessors() {
        return this.successors;
    }
    
    public IntList getSuccessorList() {
        return this.successorList;
    }
    
    public int getPrimarySuccessorIndex() {
        return this.primarySuccessor;
    }
    
    public int getPrimarySuccessorRopLabel() {
        return this.parent.blockIndexToRopLabel(this.primarySuccessor);
    }
    
    public SsaBasicBlock getPrimarySuccessor() {
        if (this.primarySuccessor < 0) {
            return null;
        }
        return this.parent.getBlocks().get(this.primarySuccessor);
    }
    
    public IntList getRopLabelSuccessorList() {
        final IntList result = new IntList(this.successorList.size());
        for (int sz = this.successorList.size(), i = 0; i < sz; ++i) {
            result.add(this.parent.blockIndexToRopLabel(this.successorList.get(i)));
        }
        return result;
    }
    
    public SsaMethod getParent() {
        return this.parent;
    }
    
    public SsaBasicBlock insertNewPredecessor() {
        final SsaBasicBlock newPred = this.parent.makeNewGotoBlock();
        newPred.predecessors = this.predecessors;
        newPred.successors.set(this.index);
        newPred.successorList.add(this.index);
        newPred.primarySuccessor = this.index;
        (this.predecessors = new BitSet(this.parent.getBlocks().size())).set(newPred.index);
        for (int i = newPred.predecessors.nextSetBit(0); i >= 0; i = newPred.predecessors.nextSetBit(i + 1)) {
            final SsaBasicBlock predBlock = this.parent.getBlocks().get(i);
            predBlock.replaceSuccessor(this.index, newPred.index);
        }
        return newPred;
    }
    
    public SsaBasicBlock insertNewSuccessor(final SsaBasicBlock other) {
        final SsaBasicBlock newSucc = this.parent.makeNewGotoBlock();
        if (!this.successors.get(other.index)) {
            throw new RuntimeException("Block " + other.getRopLabelString() + " not successor of " + this.getRopLabelString());
        }
        newSucc.predecessors.set(this.index);
        newSucc.successors.set(other.index);
        newSucc.successorList.add(other.index);
        newSucc.primarySuccessor = other.index;
        for (int i = this.successorList.size() - 1; i >= 0; --i) {
            if (this.successorList.get(i) == other.index) {
                this.successorList.set(i, newSucc.index);
            }
        }
        if (this.primarySuccessor == other.index) {
            this.primarySuccessor = newSucc.index;
        }
        this.successors.clear(other.index);
        this.successors.set(newSucc.index);
        other.predecessors.set(newSucc.index);
        other.predecessors.set(this.index, this.successors.get(other.index));
        return newSucc;
    }
    
    public void replaceSuccessor(final int oldIndex, final int newIndex) {
        if (oldIndex == newIndex) {
            return;
        }
        this.successors.set(newIndex);
        if (this.primarySuccessor == oldIndex) {
            this.primarySuccessor = newIndex;
        }
        for (int i = this.successorList.size() - 1; i >= 0; --i) {
            if (this.successorList.get(i) == oldIndex) {
                this.successorList.set(i, newIndex);
            }
        }
        this.successors.clear(oldIndex);
        this.parent.getBlocks().get(newIndex).predecessors.set(this.index);
        this.parent.getBlocks().get(oldIndex).predecessors.clear(this.index);
    }
    
    public void removeSuccessor(final int oldIndex) {
        int removeIndex = 0;
        for (int i = this.successorList.size() - 1; i >= 0; --i) {
            if (this.successorList.get(i) == oldIndex) {
                removeIndex = i;
            }
            else {
                this.primarySuccessor = this.successorList.get(i);
            }
        }
        this.successorList.removeIndex(removeIndex);
        this.successors.clear(oldIndex);
        this.parent.getBlocks().get(oldIndex).predecessors.clear(this.index);
    }
    
    public void exitBlockFixup(final SsaBasicBlock exitBlock) {
        if (this == exitBlock) {
            return;
        }
        if (this.successorList.size() == 0) {
            this.successors.set(exitBlock.index);
            this.successorList.add(exitBlock.index);
            this.primarySuccessor = exitBlock.index;
            exitBlock.predecessors.set(this.index);
        }
    }
    
    public void addMoveToEnd(final RegisterSpec result, final RegisterSpec source) {
        if (result.getReg() == source.getReg()) {
            return;
        }
        final NormalSsaInsn lastInsn = (NormalSsaInsn) this.insns.get(this.insns.size() - 1);
        if (lastInsn.getResult() != null || lastInsn.getSources().size() > 0) {
            for (int i = this.successors.nextSetBit(0); i >= 0; i = this.successors.nextSetBit(i + 1)) {
                final SsaBasicBlock succ = this.parent.getBlocks().get(i);
                succ.addMoveToBeginning(result, source);
            }
        }
        else {
            final RegisterSpecList sources = RegisterSpecList.make(source);
            final NormalSsaInsn toAdd = new NormalSsaInsn(new PlainInsn(Rops.opMove(result.getType()), SourcePosition.NO_INFO, result, sources), this);
            this.insns.add(this.insns.size() - 1, toAdd);
            ++this.movesFromPhisAtEnd;
        }
    }
    
    public void addMoveToBeginning(final RegisterSpec result, final RegisterSpec source) {
        if (result.getReg() == source.getReg()) {
            return;
        }
        final RegisterSpecList sources = RegisterSpecList.make(source);
        final NormalSsaInsn toAdd = new NormalSsaInsn(new PlainInsn(Rops.opMove(result.getType()), SourcePosition.NO_INFO, result, sources), this);
        this.insns.add(this.getCountPhiInsns(), toAdd);
        ++this.movesFromPhisAtBeginning;
    }
    
    private static void setRegsUsed(final BitSet regsUsed, final RegisterSpec rs) {
        regsUsed.set(rs.getReg());
        if (rs.getCategory() > 1) {
            regsUsed.set(rs.getReg() + 1);
        }
    }
    
    private static boolean checkRegUsed(final BitSet regsUsed, final RegisterSpec rs) {
        final int reg = rs.getReg();
        final int category = rs.getCategory();
        return regsUsed.get(reg) || (category == 2 && regsUsed.get(reg + 1));
    }
    
    private void scheduleUseBeforeAssigned(final List<SsaInsn> toSchedule) {
        final BitSet regsUsedAsSources = new BitSet(this.parent.getRegCount());
        final BitSet regsUsedAsResults = new BitSet(this.parent.getRegCount());
        int sz = toSchedule.size();
        int insertPlace = 0;
        while (insertPlace < sz) {
            final int oldInsertPlace = insertPlace;
            for (int i = insertPlace; i < sz; ++i) {
                setRegsUsed(regsUsedAsSources, toSchedule.get(i).getSources().get(0));
                setRegsUsed(regsUsedAsResults, toSchedule.get(i).getResult());
            }
            for (int i = insertPlace; i < sz; ++i) {
                final SsaInsn insn = toSchedule.get(i);
                if (!checkRegUsed(regsUsedAsSources, insn.getResult())) {
                    Collections.swap(toSchedule, i, insertPlace++);
                }
            }
            if (oldInsertPlace == insertPlace) {
                SsaInsn insnToSplit = null;
                for (int j = insertPlace; j < sz; ++j) {
                    final SsaInsn insn2 = toSchedule.get(j);
                    if (checkRegUsed(regsUsedAsSources, insn2.getResult()) && checkRegUsed(regsUsedAsResults, insn2.getSources().get(0))) {
                        insnToSplit = insn2;
                        Collections.swap(toSchedule, insertPlace, j);
                        break;
                    }
                }
                final RegisterSpec result = insnToSplit.getResult();
                final RegisterSpec tempSpec = result.withReg(this.parent.borrowSpareRegister(result.getCategory()));
                final NormalSsaInsn toAdd = new NormalSsaInsn(new PlainInsn(Rops.opMove(result.getType()), SourcePosition.NO_INFO, tempSpec, insnToSplit.getSources()), this);
                toSchedule.add(insertPlace++, toAdd);
                final RegisterSpecList newSources = RegisterSpecList.make(tempSpec);
                final NormalSsaInsn toReplace = new NormalSsaInsn(new PlainInsn(Rops.opMove(result.getType()), SourcePosition.NO_INFO, result, newSources), this);
                toSchedule.set(insertPlace, toReplace);
                sz = toSchedule.size();
            }
            regsUsedAsSources.clear();
            regsUsedAsResults.clear();
        }
    }
    
    public void addLiveOut(final int regV) {
        if (this.liveOut == null) {
            this.liveOut = SetFactory.makeLivenessSet(this.parent.getRegCount());
        }
        this.liveOut.add(regV);
    }
    
    public void addLiveIn(final int regV) {
        if (this.liveIn == null) {
            this.liveIn = SetFactory.makeLivenessSet(this.parent.getRegCount());
        }
        this.liveIn.add(regV);
    }
    
    public IntSet getLiveInRegs() {
        if (this.liveIn == null) {
            this.liveIn = SetFactory.makeLivenessSet(this.parent.getRegCount());
        }
        return this.liveIn;
    }
    
    public IntSet getLiveOutRegs() {
        if (this.liveOut == null) {
            this.liveOut = SetFactory.makeLivenessSet(this.parent.getRegCount());
        }
        return this.liveOut;
    }
    
    public boolean isExitBlock() {
        return this.index == this.parent.getExitBlockIndex();
    }
    
    public boolean isReachable() {
        if (this.reachable == -1) {
            this.parent.computeReachability();
        }
        return this.reachable == 1;
    }
    
    public void setReachable(final int reach) {
        this.reachable = reach;
    }
    
    public void scheduleMovesFromPhis() {
        if (this.movesFromPhisAtBeginning > 1) {
            final List<SsaInsn> toSchedule = this.insns.subList(0, this.movesFromPhisAtBeginning);
            this.scheduleUseBeforeAssigned(toSchedule);
            final SsaInsn firstNonPhiMoveInsn = this.insns.get(this.movesFromPhisAtBeginning);
            if (firstNonPhiMoveInsn.isMoveException()) {
                throw new RuntimeException("Unexpected: moves from phis before move-exception");
            }
        }
        if (this.movesFromPhisAtEnd > 1) {
            this.scheduleUseBeforeAssigned(this.insns.subList(this.insns.size() - this.movesFromPhisAtEnd - 1, this.insns.size() - 1));
        }
        this.parent.returnSpareRegisters();
    }
    
    public void forEachInsn(final SsaInsn.Visitor visitor) {
        for (int len = this.insns.size(), i = 0; i < len; ++i) {
            this.insns.get(i).accept(visitor);
        }
    }
    
    @Override
    public String toString() {
        return "{" + this.index + ":" + Hex.u2(this.ropLabel) + '}';
    }
    
    static {
        LABEL_COMPARATOR = new LabelComparator();
    }
    
    public static final class LabelComparator implements Comparator<SsaBasicBlock>
    {
        @Override
        public int compare(final SsaBasicBlock b1, final SsaBasicBlock b2) {
            final int label1 = b1.ropLabel;
            final int label2 = b2.ropLabel;
            if (label1 < label2) {
                return -1;
            }
            if (label1 > label2) {
                return 1;
            }
            return 0;
        }
    }
    
    public interface Visitor
    {
        void visitBlock(final SsaBasicBlock p0, final SsaBasicBlock p1);
    }
}

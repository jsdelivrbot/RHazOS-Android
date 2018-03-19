package embedded.com.android.dx.ssa;

import embedded.com.android.dx.util.*;
import embedded.com.android.dx.rop.code.*;
import java.util.*;

public final class SsaMethod
{
    private ArrayList<SsaBasicBlock> blocks;
    private int entryBlockIndex;
    private int exitBlockIndex;
    private int registerCount;
    private int spareRegisterBase;
    private int borrowedSpareRegisters;
    private int maxLabel;
    private final int paramWidth;
    private final boolean isStatic;
    private SsaInsn[] definitionList;
    private ArrayList<SsaInsn>[] useList;
    private List<SsaInsn>[] unmodifiableUseList;
    private boolean backMode;
    
    public static SsaMethod newFromRopMethod(final RopMethod ropMethod, final int paramWidth, final boolean isStatic) {
        final SsaMethod result = new SsaMethod(ropMethod, paramWidth, isStatic);
        result.convertRopToSsaBlocks(ropMethod);
        return result;
    }
    
    private SsaMethod(final RopMethod ropMethod, final int paramWidth, final boolean isStatic) {
        this.paramWidth = paramWidth;
        this.isStatic = isStatic;
        this.backMode = false;
        this.maxLabel = ropMethod.getBlocks().getMaxLabel();
        this.registerCount = ropMethod.getBlocks().getRegCount();
        this.spareRegisterBase = this.registerCount;
    }
    
    static BitSet bitSetFromLabelList(final BasicBlockList blocks, final IntList labelList) {
        final BitSet result = new BitSet(blocks.size());
        for (int i = 0, sz = labelList.size(); i < sz; ++i) {
            result.set(blocks.indexOfLabel(labelList.get(i)));
        }
        return result;
    }
    
    public static IntList indexListFromLabelList(final BasicBlockList ropBlocks, final IntList labelList) {
        final IntList result = new IntList(labelList.size());
        for (int i = 0, sz = labelList.size(); i < sz; ++i) {
            result.add(ropBlocks.indexOfLabel(labelList.get(i)));
        }
        return result;
    }
    
    private void convertRopToSsaBlocks(final RopMethod rmeth) {
        final BasicBlockList ropBlocks = rmeth.getBlocks();
        final int sz = ropBlocks.size();
        this.blocks = new ArrayList<SsaBasicBlock>(sz + 2);
        for (int i = 0; i < sz; ++i) {
            final SsaBasicBlock sbb = SsaBasicBlock.newFromRop(rmeth, i, this);
            this.blocks.add(sbb);
        }
        final int origEntryBlockIndex = rmeth.getBlocks().indexOfLabel(rmeth.getFirstLabel());
        final SsaBasicBlock entryBlock = this.blocks.get(origEntryBlockIndex).insertNewPredecessor();
        this.entryBlockIndex = entryBlock.getIndex();
        this.exitBlockIndex = -1;
    }
    
    void makeExitBlock() {
        if (this.exitBlockIndex >= 0) {
            throw new RuntimeException("must be called at most once");
        }
        this.exitBlockIndex = this.blocks.size();
        final SsaBasicBlock exitBlock = new SsaBasicBlock(this.exitBlockIndex, this.maxLabel++, this);
        this.blocks.add(exitBlock);
        for (final SsaBasicBlock block : this.blocks) {
            block.exitBlockFixup(exitBlock);
        }
        if (exitBlock.getPredecessors().cardinality() == 0) {
            this.blocks.remove(this.exitBlockIndex);
            this.exitBlockIndex = -1;
            --this.maxLabel;
        }
    }
    
    private static SsaInsn getGoto(final SsaBasicBlock block) {
        return new NormalSsaInsn(new PlainInsn(Rops.GOTO, SourcePosition.NO_INFO, null, RegisterSpecList.EMPTY), block);
    }
    
    public SsaBasicBlock makeNewGotoBlock() {
        final int newIndex = this.blocks.size();
        final SsaBasicBlock newBlock = new SsaBasicBlock(newIndex, this.maxLabel++, this);
        newBlock.getInsns().add(getGoto(newBlock));
        this.blocks.add(newBlock);
        return newBlock;
    }
    
    public int getEntryBlockIndex() {
        return this.entryBlockIndex;
    }
    
    public SsaBasicBlock getEntryBlock() {
        return this.blocks.get(this.entryBlockIndex);
    }
    
    public int getExitBlockIndex() {
        return this.exitBlockIndex;
    }
    
    public SsaBasicBlock getExitBlock() {
        return (this.exitBlockIndex < 0) ? null : this.blocks.get(this.exitBlockIndex);
    }
    
    public int blockIndexToRopLabel(final int bi) {
        if (bi < 0) {
            return -1;
        }
        return this.blocks.get(bi).getRopLabel();
    }
    
    public int getRegCount() {
        return this.registerCount;
    }
    
    public int getParamWidth() {
        return this.paramWidth;
    }
    
    public boolean isStatic() {
        return this.isStatic;
    }
    
    public int borrowSpareRegister(final int category) {
        final int result = this.spareRegisterBase + this.borrowedSpareRegisters;
        this.borrowedSpareRegisters += category;
        this.registerCount = Math.max(this.registerCount, result + category);
        return result;
    }
    
    public void returnSpareRegisters() {
        this.borrowedSpareRegisters = 0;
    }
    
    public ArrayList<SsaBasicBlock> getBlocks() {
        return this.blocks;
    }
    
    public int getCountReachableBlocks() {
        int ret = 0;
        for (final SsaBasicBlock b : this.blocks) {
            if (b.isReachable()) {
                ++ret;
            }
        }
        return ret;
    }
    
    public void computeReachability() {
        for (final SsaBasicBlock block : this.blocks) {
            block.setReachable(0);
        }
        final ArrayList<SsaBasicBlock> blockList = new ArrayList<SsaBasicBlock>();
        blockList.add(this.getEntryBlock());
        while (!blockList.isEmpty()) {
            final SsaBasicBlock block = blockList.remove(0);
            if (block.isReachable()) {
                continue;
            }
            block.setReachable(1);
            final BitSet succs = block.getSuccessors();
            for (int i = succs.nextSetBit(0); i >= 0; i = succs.nextSetBit(i + 1)) {
                blockList.add(this.blocks.get(i));
            }
        }
    }
    
    public void mapRegisters(final RegisterMapper mapper) {
        for (final SsaBasicBlock block : this.getBlocks()) {
            for (final SsaInsn insn : block.getInsns()) {
                insn.mapRegisters(mapper);
            }
        }
        this.registerCount = mapper.getNewRegisterCount();
        this.spareRegisterBase = this.registerCount;
    }
    
    public SsaInsn getDefinitionForRegister(final int reg) {
        if (this.backMode) {
            throw new RuntimeException("No def list in back mode");
        }
        if (this.definitionList != null) {
            return this.definitionList[reg];
        }
        this.definitionList = new SsaInsn[this.getRegCount()];
        this.forEachInsn(new SsaInsn.Visitor() {
            @Override
            public void visitMoveInsn(final NormalSsaInsn insn) {
                SsaMethod.this.definitionList[insn.getResult().getReg()] = insn;
            }
            
            @Override
            public void visitPhiInsn(final PhiInsn phi) {
                SsaMethod.this.definitionList[phi.getResult().getReg()] = phi;
            }
            
            @Override
            public void visitNonMoveInsn(final NormalSsaInsn insn) {
                final RegisterSpec result = insn.getResult();
                if (result != null) {
                    SsaMethod.this.definitionList[insn.getResult().getReg()] = insn;
                }
            }
        });
        return this.definitionList[reg];
    }
    
    private void buildUseList() {
        if (this.backMode) {
            throw new RuntimeException("No use list in back mode");
        }
        this.useList = (ArrayList<SsaInsn>[])new ArrayList[this.registerCount];
        for (int i = 0; i < this.registerCount; ++i) {
            this.useList[i] = new ArrayList<SsaInsn>();
        }
        this.forEachInsn(new SsaInsn.Visitor() {
            @Override
            public void visitMoveInsn(final NormalSsaInsn insn) {
                this.addToUses(insn);
            }
            
            @Override
            public void visitPhiInsn(final PhiInsn phi) {
                this.addToUses(phi);
            }
            
            @Override
            public void visitNonMoveInsn(final NormalSsaInsn insn) {
                this.addToUses(insn);
            }
            
            private void addToUses(final SsaInsn insn) {
                final RegisterSpecList rl = insn.getSources();
                for (int sz = rl.size(), i = 0; i < sz; ++i) {
                    SsaMethod.this.useList[rl.get(i).getReg()].add(insn);
                }
            }
        });
        this.unmodifiableUseList = (List<SsaInsn>[])new List[this.registerCount];
        for (int i = 0; i < this.registerCount; ++i) {
            this.unmodifiableUseList[i] = Collections.unmodifiableList((List<? extends SsaInsn>)this.useList[i]);
        }
    }
    
    void onSourceChanged(final SsaInsn insn, final RegisterSpec oldSource, final RegisterSpec newSource) {
        if (this.useList == null) {
            return;
        }
        if (oldSource != null) {
            final int reg = oldSource.getReg();
            this.useList[reg].remove(insn);
        }
        final int reg = newSource.getReg();
        if (this.useList.length <= reg) {
            this.useList = null;
            return;
        }
        this.useList[reg].add(insn);
    }
    
    void onSourcesChanged(final SsaInsn insn, final RegisterSpecList oldSources) {
        if (this.useList == null) {
            return;
        }
        if (oldSources != null) {
            this.removeFromUseList(insn, oldSources);
        }
        final RegisterSpecList sources = insn.getSources();
        for (int szNew = sources.size(), i = 0; i < szNew; ++i) {
            final int reg = sources.get(i).getReg();
            this.useList[reg].add(insn);
        }
    }
    
    private void removeFromUseList(final SsaInsn insn, final RegisterSpecList oldSources) {
        if (oldSources == null) {
            return;
        }
        for (int szNew = oldSources.size(), i = 0; i < szNew; ++i) {
            if (!this.useList[oldSources.get(i).getReg()].remove(insn)) {
                throw new RuntimeException("use not found");
            }
        }
    }
    
    void onInsnAdded(final SsaInsn insn) {
        this.onSourcesChanged(insn, null);
        this.updateOneDefinition(insn, null);
    }
    
    void onInsnRemoved(final SsaInsn insn) {
        if (this.useList != null) {
            this.removeFromUseList(insn, insn.getSources());
        }
        final RegisterSpec resultReg = insn.getResult();
        if (this.definitionList != null && resultReg != null) {
            this.definitionList[resultReg.getReg()] = null;
        }
    }
    
    public void onInsnsChanged() {
        this.definitionList = null;
        this.useList = null;
        this.unmodifiableUseList = null;
    }
    
    void updateOneDefinition(final SsaInsn insn, final RegisterSpec oldResult) {
        if (this.definitionList == null) {
            return;
        }
        if (oldResult != null) {
            final int reg = oldResult.getReg();
            this.definitionList[reg] = null;
        }
        final RegisterSpec resultReg = insn.getResult();
        if (resultReg != null) {
            final int reg2 = resultReg.getReg();
            if (this.definitionList[reg2] != null) {
                throw new RuntimeException("Duplicate add of insn");
            }
            this.definitionList[resultReg.getReg()] = insn;
        }
    }
    
    public List<SsaInsn> getUseListForRegister(final int reg) {
        if (this.unmodifiableUseList == null) {
            this.buildUseList();
        }
        return this.unmodifiableUseList[reg];
    }
    
    public ArrayList<SsaInsn>[] getUseListCopy() {
        if (this.useList == null) {
            this.buildUseList();
        }
        final ArrayList<SsaInsn>[] useListCopy = (ArrayList<SsaInsn>[])new ArrayList[this.registerCount];
        for (int i = 0; i < this.registerCount; ++i) {
            useListCopy[i] = new ArrayList<SsaInsn>(this.useList[i]);
        }
        return useListCopy;
    }
    
    public boolean isRegALocal(final RegisterSpec spec) {
        final SsaInsn defn = this.getDefinitionForRegister(spec.getReg());
        if (defn == null) {
            return false;
        }
        if (defn.getLocalAssignment() != null) {
            return true;
        }
        for (final SsaInsn use : this.getUseListForRegister(spec.getReg())) {
            final Insn insn = use.getOriginalRopInsn();
            if (insn != null && insn.getOpcode().getOpcode() == 54) {
                return true;
            }
        }
        return false;
    }
    
    void setNewRegCount(final int newRegCount) {
        this.registerCount = newRegCount;
        this.spareRegisterBase = this.registerCount;
        this.onInsnsChanged();
    }
    
    public int makeNewSsaReg() {
        final int reg = this.registerCount++;
        this.spareRegisterBase = this.registerCount;
        this.onInsnsChanged();
        return reg;
    }
    
    public void forEachInsn(final SsaInsn.Visitor visitor) {
        for (final SsaBasicBlock block : this.blocks) {
            block.forEachInsn(visitor);
        }
    }
    
    public void forEachPhiInsn(final PhiInsn.Visitor v) {
        for (final SsaBasicBlock block : this.blocks) {
            block.forEachPhiInsn(v);
        }
    }
    
    public void forEachBlockDepthFirst(final boolean reverse, final SsaBasicBlock.Visitor v) {
        final BitSet visited = new BitSet(this.blocks.size());
        final Stack<SsaBasicBlock> stack = new Stack<SsaBasicBlock>();
        final SsaBasicBlock rootBlock = reverse ? this.getExitBlock() : this.getEntryBlock();
        if (rootBlock == null) {
            return;
        }
        stack.add(null);
        stack.add(rootBlock);
        while (stack.size() > 0) {
            final SsaBasicBlock cur = stack.pop();
            final SsaBasicBlock parent = stack.pop();
            if (!visited.get(cur.getIndex())) {
                final BitSet children = reverse ? cur.getPredecessors() : cur.getSuccessors();
                for (int i = children.nextSetBit(0); i >= 0; i = children.nextSetBit(i + 1)) {
                    stack.add(cur);
                    stack.add(this.blocks.get(i));
                }
                visited.set(cur.getIndex());
                v.visitBlock(cur, parent);
            }
        }
    }
    
    public void forEachBlockDepthFirstDom(final SsaBasicBlock.Visitor v) {
        final BitSet visited = new BitSet(this.getBlocks().size());
        final Stack<SsaBasicBlock> stack = new Stack<SsaBasicBlock>();
        stack.add(this.getEntryBlock());
        while (stack.size() > 0) {
            final SsaBasicBlock cur = stack.pop();
            final ArrayList<SsaBasicBlock> curDomChildren = cur.getDomChildren();
            if (!visited.get(cur.getIndex())) {
                for (int i = curDomChildren.size() - 1; i >= 0; --i) {
                    final SsaBasicBlock child = curDomChildren.get(i);
                    stack.add(child);
                }
                visited.set(cur.getIndex());
                v.visitBlock(cur, null);
            }
        }
    }
    
    public void deleteInsns(final Set<SsaInsn> deletedInsns) {
        for (final SsaBasicBlock block : this.getBlocks()) {
            final ArrayList<SsaInsn> insns = block.getInsns();
            for (int i = insns.size() - 1; i >= 0; --i) {
                final SsaInsn insn = insns.get(i);
                if (deletedInsns.contains(insn)) {
                    this.onInsnRemoved(insn);
                    insns.remove(i);
                }
            }
            final int insnsSz = insns.size();
            final SsaInsn lastInsn = (insnsSz == 0) ? null : insns.get(insnsSz - 1);
            if (block != this.getExitBlock() && (insnsSz == 0 || lastInsn.getOriginalRopInsn() == null || lastInsn.getOriginalRopInsn().getOpcode().getBranchingness() == 1)) {
                final Insn gotoInsn = new PlainInsn(Rops.GOTO, SourcePosition.NO_INFO, null, RegisterSpecList.EMPTY);
                insns.add(SsaInsn.makeFromRop(gotoInsn, block));
                final BitSet succs = block.getSuccessors();
                for (int j = succs.nextSetBit(0); j >= 0; j = succs.nextSetBit(j + 1)) {
                    if (j != block.getPrimarySuccessorIndex()) {
                        block.removeSuccessor(j);
                    }
                }
            }
        }
    }
    
    public void setBackMode() {
        this.backMode = true;
        this.useList = null;
        this.definitionList = null;
    }
}

package embedded.com.android.dx.ssa;

import embedded.com.android.dx.rop.cst.*;
import embedded.com.android.dx.rop.type.*;
import embedded.com.android.dx.rop.code.*;
import java.util.*;

public class EscapeAnalysis
{
    private SsaMethod ssaMeth;
    private int regCount;
    private ArrayList<EscapeSet> latticeValues;
    
    private EscapeAnalysis(final SsaMethod ssaMeth) {
        this.ssaMeth = ssaMeth;
        this.regCount = ssaMeth.getRegCount();
        this.latticeValues = new ArrayList<EscapeSet>();
    }
    
    private int findSetIndex(final RegisterSpec reg) {
        int i;
        for (i = 0; i < this.latticeValues.size(); ++i) {
            final EscapeSet e = this.latticeValues.get(i);
            if (e.regSet.get(reg.getReg())) {
                return i;
            }
        }
        return i;
    }
    
    private SsaInsn getInsnForMove(final SsaInsn moveInsn) {
        final int pred = moveInsn.getBlock().getPredecessors().nextSetBit(0);
        final ArrayList<SsaInsn> predInsns = this.ssaMeth.getBlocks().get(pred).getInsns();
        return predInsns.get(predInsns.size() - 1);
    }
    
    private SsaInsn getMoveForInsn(final SsaInsn insn) {
        final int succ = insn.getBlock().getSuccessors().nextSetBit(0);
        final ArrayList<SsaInsn> succInsns = this.ssaMeth.getBlocks().get(succ).getInsns();
        return succInsns.get(0);
    }
    
    private void addEdge(final EscapeSet parentSet, final EscapeSet childSet) {
        if (!childSet.parentSets.contains(parentSet)) {
            childSet.parentSets.add(parentSet);
        }
        if (!parentSet.childSets.contains(childSet)) {
            parentSet.childSets.add(childSet);
        }
    }
    
    private void replaceNode(final EscapeSet newNode, final EscapeSet oldNode) {
        for (final EscapeSet e : oldNode.parentSets) {
            e.childSets.remove(oldNode);
            e.childSets.add(newNode);
            newNode.parentSets.add(e);
        }
        for (final EscapeSet e : oldNode.childSets) {
            e.parentSets.remove(oldNode);
            e.parentSets.add(newNode);
            newNode.childSets.add(e);
        }
    }
    
    public static void process(final SsaMethod ssaMethod) {
        new EscapeAnalysis(ssaMethod).run();
    }
    
    private void processInsn(final SsaInsn insn) {
        final int op = insn.getOpcode().getOpcode();
        final RegisterSpec result = insn.getResult();
        if (op == 56 && result.getTypeBearer().getBasicType() == 9) {
            final EscapeSet escSet = this.processMoveResultPseudoInsn(insn);
            this.processRegister(result, escSet);
        }
        else if (op == 3 && result.getTypeBearer().getBasicType() == 9) {
            final EscapeSet escSet = new EscapeSet(result.getReg(), this.regCount, EscapeState.NONE);
            this.latticeValues.add(escSet);
            this.processRegister(result, escSet);
        }
        else if (op == 55 && result.getTypeBearer().getBasicType() == 9) {
            final EscapeSet escSet = new EscapeSet(result.getReg(), this.regCount, EscapeState.NONE);
            this.latticeValues.add(escSet);
            this.processRegister(result, escSet);
        }
    }
    
    private EscapeSet processMoveResultPseudoInsn(final SsaInsn insn) {
        final RegisterSpec result = insn.getResult();
        final SsaInsn prevSsaInsn = this.getInsnForMove(insn);
        final int prevOpcode = prevSsaInsn.getOpcode().getOpcode();
        EscapeSet escSet = null;
        switch (prevOpcode) {
            case 5:
            case 40: {
                escSet = new EscapeSet(result.getReg(), this.regCount, EscapeState.NONE);
                break;
            }
            case 41:
            case 42: {
                final RegisterSpec prevSource = prevSsaInsn.getSources().get(0);
                if (prevSource.getTypeBearer().isConstant()) {
                    escSet = new EscapeSet(result.getReg(), this.regCount, EscapeState.NONE);
                    escSet.replaceableArray = true;
                    break;
                }
                escSet = new EscapeSet(result.getReg(), this.regCount, EscapeState.GLOBAL);
                break;
            }
            case 46: {
                escSet = new EscapeSet(result.getReg(), this.regCount, EscapeState.GLOBAL);
                break;
            }
            case 38:
            case 43:
            case 45: {
                final RegisterSpec prevSource = prevSsaInsn.getSources().get(0);
                final int setIndex = this.findSetIndex(prevSource);
                if (setIndex != this.latticeValues.size()) {
                    escSet = this.latticeValues.get(setIndex);
                    escSet.regSet.set(result.getReg());
                    return escSet;
                }
                if (prevSource.getType() == Type.KNOWN_NULL) {
                    escSet = new EscapeSet(result.getReg(), this.regCount, EscapeState.NONE);
                    break;
                }
                escSet = new EscapeSet(result.getReg(), this.regCount, EscapeState.GLOBAL);
                break;
            }
            default: {
                return null;
            }
        }
        this.latticeValues.add(escSet);
        return escSet;
    }
    
    private void processRegister(final RegisterSpec result, final EscapeSet escSet) {
        final ArrayList<RegisterSpec> regWorklist = new ArrayList<RegisterSpec>();
        regWorklist.add(result);
        while (!regWorklist.isEmpty()) {
            final int listSize = regWorklist.size() - 1;
            final RegisterSpec def = regWorklist.remove(listSize);
            final List<SsaInsn> useList = this.ssaMeth.getUseListForRegister(def.getReg());
            for (final SsaInsn use : useList) {
                final Rop useOpcode = use.getOpcode();
                if (useOpcode == null) {
                    this.processPhiUse(use, escSet, regWorklist);
                }
                else {
                    this.processUse(def, use, escSet, regWorklist);
                }
            }
        }
    }
    
    private void processPhiUse(final SsaInsn use, final EscapeSet escSet, final ArrayList<RegisterSpec> regWorklist) {
        final int setIndex = this.findSetIndex(use.getResult());
        if (setIndex != this.latticeValues.size()) {
            final EscapeSet mergeSet = this.latticeValues.get(setIndex);
            if (mergeSet != escSet) {
                escSet.replaceableArray = false;
                escSet.regSet.or(mergeSet.regSet);
                if (escSet.escape.compareTo(mergeSet.escape) < 0) {
                    escSet.escape = mergeSet.escape;
                }
                this.replaceNode(escSet, mergeSet);
                this.latticeValues.remove(setIndex);
            }
        }
        else {
            escSet.regSet.set(use.getResult().getReg());
            regWorklist.add(use.getResult());
        }
    }
    
    private void processUse(final RegisterSpec def, final SsaInsn use, final EscapeSet escSet, final ArrayList<RegisterSpec> regWorklist) {
        final int useOpcode = use.getOpcode().getOpcode();
        switch (useOpcode) {
            case 2: {
                escSet.regSet.set(use.getResult().getReg());
                regWorklist.add(use.getResult());
                break;
            }
            case 7:
            case 8:
            case 43: {
                if (escSet.escape.compareTo(EscapeState.METHOD) < 0) {
                    escSet.escape = EscapeState.METHOD;
                    break;
                }
                break;
            }
            case 39: {
                final RegisterSpec putIndex = use.getSources().get(2);
                if (!putIndex.getTypeBearer().isConstant()) {
                    escSet.replaceableArray = false;
                }
            }
            case 47: {
                final RegisterSpec putValue = use.getSources().get(0);
                if (putValue.getTypeBearer().getBasicType() != 9) {
                    break;
                }
                escSet.replaceableArray = false;
                final RegisterSpecList sources = use.getSources();
                if (sources.get(0).getReg() == def.getReg()) {
                    final int setIndex = this.findSetIndex(sources.get(1));
                    if (setIndex != this.latticeValues.size()) {
                        final EscapeSet parentSet = this.latticeValues.get(setIndex);
                        this.addEdge(parentSet, escSet);
                        if (escSet.escape.compareTo(parentSet.escape) < 0) {
                            escSet.escape = parentSet.escape;
                        }
                    }
                    break;
                }
                final int setIndex = this.findSetIndex(sources.get(0));
                if (setIndex != this.latticeValues.size()) {
                    final EscapeSet childSet = this.latticeValues.get(setIndex);
                    this.addEdge(escSet, childSet);
                    if (childSet.escape.compareTo(escSet.escape) < 0) {
                        childSet.escape = escSet.escape;
                    }
                }
                break;
            }
            case 38: {
                final RegisterSpec getIndex = use.getSources().get(1);
                if (!getIndex.getTypeBearer().isConstant()) {
                    escSet.replaceableArray = false;
                    break;
                }
                break;
            }
            case 48: {
                escSet.escape = EscapeState.GLOBAL;
                break;
            }
            case 33:
            case 35:
            case 49:
            case 50:
            case 51:
            case 52:
            case 53: {
                escSet.escape = EscapeState.INTER;
                break;
            }
        }
    }
    
    private void scalarReplacement() {
        for (final EscapeSet escSet : this.latticeValues) {
            if (escSet.replaceableArray) {
                if (escSet.escape != EscapeState.NONE) {
                    continue;
                }
                final int e = escSet.regSet.nextSetBit(0);
                final SsaInsn def = this.ssaMeth.getDefinitionForRegister(e);
                final SsaInsn prev = this.getInsnForMove(def);
                final TypeBearer lengthReg = prev.getSources().get(0).getTypeBearer();
                final int length = ((CstLiteralBits)lengthReg).getIntBits();
                final ArrayList<RegisterSpec> newRegs = new ArrayList<RegisterSpec>(length);
                final HashSet<SsaInsn> deletedInsns = new HashSet<SsaInsn>();
                this.replaceDef(def, prev, length, newRegs);
                deletedInsns.add(prev);
                deletedInsns.add(def);
                final List<SsaInsn> useList = this.ssaMeth.getUseListForRegister(e);
                for (final SsaInsn use : useList) {
                    this.replaceUse(use, prev, newRegs, deletedInsns);
                    deletedInsns.add(use);
                }
                this.ssaMeth.deleteInsns(deletedInsns);
                this.ssaMeth.onInsnsChanged();
                SsaConverter.updateSsaMethod(this.ssaMeth, this.regCount);
                this.movePropagate();
            }
        }
    }
    
    private void replaceDef(final SsaInsn def, final SsaInsn prev, final int length, final ArrayList<RegisterSpec> newRegs) {
        final Type resultType = def.getResult().getType();
        for (int i = 0; i < length; ++i) {
            final Constant newZero = Zeroes.zeroFor(resultType.getComponentType());
            final TypedConstant typedZero = (TypedConstant)newZero;
            final RegisterSpec newReg = RegisterSpec.make(this.ssaMeth.makeNewSsaReg(), typedZero);
            newRegs.add(newReg);
            this.insertPlainInsnBefore(def, RegisterSpecList.EMPTY, newReg, 5, newZero);
        }
    }
    
    private void replaceUse(final SsaInsn use, final SsaInsn prev, final ArrayList<RegisterSpec> newRegs, final HashSet<SsaInsn> deletedInsns) {
        final int length = newRegs.size();
        switch (use.getOpcode().getOpcode()) {
            case 38: {
                final SsaInsn next = this.getMoveForInsn(use);
                final RegisterSpecList sources = use.getSources();
                final CstLiteralBits indexReg = (CstLiteralBits)sources.get(1).getTypeBearer();
                final int index = indexReg.getIntBits();
                if (index < length) {
                    final RegisterSpec source = newRegs.get(index);
                    final RegisterSpec result = source.withReg(next.getResult().getReg());
                    this.insertPlainInsnBefore(next, RegisterSpecList.make(source), result, 2, null);
                }
                else {
                    this.insertExceptionThrow(next, sources.get(1), deletedInsns);
                    deletedInsns.add(next.getBlock().getInsns().get(2));
                }
                deletedInsns.add(next);
                break;
            }
            case 39: {
                final RegisterSpecList sources = use.getSources();
                final CstLiteralBits indexReg = (CstLiteralBits)sources.get(2).getTypeBearer();
                final int index = indexReg.getIntBits();
                if (index < length) {
                    final RegisterSpec source = sources.get(0);
                    final RegisterSpec result = source.withReg(newRegs.get(index).getReg());
                    this.insertPlainInsnBefore(use, RegisterSpecList.make(source), result, 2, null);
                    newRegs.set(index, result.withSimpleType());
                    break;
                }
                this.insertExceptionThrow(use, sources.get(2), deletedInsns);
                break;
            }
            case 34: {
                final TypeBearer lengthReg = prev.getSources().get(0).getTypeBearer();
                final SsaInsn next = this.getMoveForInsn(use);
                this.insertPlainInsnBefore(next, RegisterSpecList.EMPTY, next.getResult(), 5, (Constant)lengthReg);
                deletedInsns.add(next);
            }
            case 57: {
                final Insn ropUse = use.getOriginalRopInsn();
                final FillArrayDataInsn fill = (FillArrayDataInsn)ropUse;
                final ArrayList<Constant> constList = fill.getInitValues();
                for (int i = 0; i < length; ++i) {
                    final RegisterSpec newFill = RegisterSpec.make(newRegs.get(i).getReg(), (TypeBearer)constList.get(i));
                    this.insertPlainInsnBefore(use, RegisterSpecList.EMPTY, newFill, 5, constList.get(i));
                    newRegs.set(i, newFill);
                }
                break;
            }
        }
    }
    
    private void movePropagate() {
        for (int i = 0; i < this.ssaMeth.getRegCount(); ++i) {
            final SsaInsn insn = this.ssaMeth.getDefinitionForRegister(i);
            if (insn != null && insn.getOpcode() != null) {
                if (insn.getOpcode().getOpcode() == 2) {
                    final ArrayList<SsaInsn>[] useList = this.ssaMeth.getUseListCopy();
                    final RegisterSpec source = insn.getSources().get(0);
                    final RegisterSpec result = insn.getResult();
                    if (source.getReg() >= this.regCount || result.getReg() >= this.regCount) {
                        final RegisterMapper mapper = new RegisterMapper() {
                            @Override
                            public int getNewRegisterCount() {
                                return EscapeAnalysis.this.ssaMeth.getRegCount();
                            }
                            
                            @Override
                            public RegisterSpec map(final RegisterSpec registerSpec) {
                                if (registerSpec.getReg() == result.getReg()) {
                                    return source;
                                }
                                return registerSpec;
                            }
                        };
                        for (final SsaInsn use : useList[result.getReg()]) {
                            use.mapSourceRegisters(mapper);
                        }
                    }
                }
            }
        }
    }
    
    private void run() {
        this.ssaMeth.forEachBlockDepthFirstDom(new SsaBasicBlock.Visitor() {
            @Override
            public void visitBlock(final SsaBasicBlock block, final SsaBasicBlock unused) {
                block.forEachInsn(new SsaInsn.Visitor() {
                    @Override
                    public void visitMoveInsn(final NormalSsaInsn insn) {
                    }
                    
                    @Override
                    public void visitPhiInsn(final PhiInsn insn) {
                    }
                    
                    @Override
                    public void visitNonMoveInsn(final NormalSsaInsn insn) {
                        EscapeAnalysis.this.processInsn(insn);
                    }
                });
            }
        });
        for (final EscapeSet e : this.latticeValues) {
            if (e.escape != EscapeState.NONE) {
                for (final EscapeSet field : e.childSets) {
                    if (e.escape.compareTo(field.escape) > 0) {
                        field.escape = e.escape;
                    }
                }
            }
        }
        this.scalarReplacement();
    }
    
    private void insertExceptionThrow(final SsaInsn insn, final RegisterSpec index, final HashSet<SsaInsn> deletedInsns) {
        final CstType exception = new CstType(Exceptions.TYPE_ArrayIndexOutOfBoundsException);
        this.insertThrowingInsnBefore(insn, RegisterSpecList.EMPTY, null, 40, exception);
        final SsaBasicBlock currBlock = insn.getBlock();
        final SsaBasicBlock newBlock = currBlock.insertNewSuccessor(currBlock.getPrimarySuccessor());
        final SsaInsn newInsn = newBlock.getInsns().get(0);
        final RegisterSpec newReg = RegisterSpec.make(this.ssaMeth.makeNewSsaReg(), exception);
        this.insertPlainInsnBefore(newInsn, RegisterSpecList.EMPTY, newReg, 56, null);
        final SsaBasicBlock newBlock2 = newBlock.insertNewSuccessor(newBlock.getPrimarySuccessor());
        final SsaInsn newInsn2 = newBlock2.getInsns().get(0);
        final CstNat newNat = new CstNat(new CstString("<init>"), new CstString("(I)V"));
        final CstMethodRef newRef = new CstMethodRef(exception, newNat);
        this.insertThrowingInsnBefore(newInsn2, RegisterSpecList.make(newReg, index), null, 52, newRef);
        deletedInsns.add(newInsn2);
        final SsaBasicBlock newBlock3 = newBlock2.insertNewSuccessor(newBlock2.getPrimarySuccessor());
        final SsaInsn newInsn3 = newBlock3.getInsns().get(0);
        this.insertThrowingInsnBefore(newInsn3, RegisterSpecList.make(newReg), null, 35, null);
        newBlock3.replaceSuccessor(newBlock3.getPrimarySuccessorIndex(), this.ssaMeth.getExitBlock().getIndex());
        deletedInsns.add(newInsn3);
    }
    
    private void insertPlainInsnBefore(final SsaInsn insn, final RegisterSpecList newSources, final RegisterSpec newResult, final int newOpcode, final Constant cst) {
        final Insn originalRopInsn = insn.getOriginalRopInsn();
        Rop newRop;
        if (newOpcode == 56) {
            newRop = Rops.opMoveResultPseudo(newResult.getType());
        }
        else {
            newRop = Rops.ropFor(newOpcode, newResult, newSources, cst);
        }
        Insn newRopInsn;
        if (cst == null) {
            newRopInsn = new PlainInsn(newRop, originalRopInsn.getPosition(), newResult, newSources);
        }
        else {
            newRopInsn = new PlainCstInsn(newRop, originalRopInsn.getPosition(), newResult, newSources, cst);
        }
        final NormalSsaInsn newInsn = new NormalSsaInsn(newRopInsn, insn.getBlock());
        final List<SsaInsn> insns = insn.getBlock().getInsns();
        insns.add(insns.lastIndexOf(insn), newInsn);
        this.ssaMeth.onInsnAdded(newInsn);
    }
    
    private void insertThrowingInsnBefore(final SsaInsn insn, final RegisterSpecList newSources, final RegisterSpec newResult, final int newOpcode, final Constant cst) {
        final Insn origRopInsn = insn.getOriginalRopInsn();
        final Rop newRop = Rops.ropFor(newOpcode, newResult, newSources, cst);
        Insn newRopInsn;
        if (cst == null) {
            newRopInsn = new ThrowingInsn(newRop, origRopInsn.getPosition(), newSources, StdTypeList.EMPTY);
        }
        else {
            newRopInsn = new ThrowingCstInsn(newRop, origRopInsn.getPosition(), newSources, StdTypeList.EMPTY, cst);
        }
        final NormalSsaInsn newInsn = new NormalSsaInsn(newRopInsn, insn.getBlock());
        final List<SsaInsn> insns = insn.getBlock().getInsns();
        insns.add(insns.lastIndexOf(insn), newInsn);
        this.ssaMeth.onInsnAdded(newInsn);
    }
    
    static class EscapeSet
    {
        BitSet regSet;
        EscapeState escape;
        ArrayList<EscapeSet> childSets;
        ArrayList<EscapeSet> parentSets;
        boolean replaceableArray;
        
        EscapeSet(final int reg, final int size, final EscapeState escState) {
            (this.regSet = new BitSet(size)).set(reg);
            this.escape = escState;
            this.childSets = new ArrayList<EscapeSet>();
            this.parentSets = new ArrayList<EscapeSet>();
            this.replaceableArray = false;
        }
    }
    
    public enum EscapeState
    {
        TOP, 
        NONE, 
        METHOD, 
        INTER, 
        GLOBAL;
    }
}

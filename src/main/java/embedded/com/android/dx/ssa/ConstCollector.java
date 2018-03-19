package embedded.com.android.dx.ssa;

import embedded.com.android.dx.rop.type.*;
import embedded.com.android.dx.rop.cst.*;
import java.util.*;
import embedded.com.android.dx.rop.code.*;

public class ConstCollector
{
    private static final int MAX_COLLECTED_CONSTANTS = 5;
    private static final boolean COLLECT_STRINGS = false;
    private static final boolean COLLECT_ONE_LOCAL = false;
    private final SsaMethod ssaMeth;
    
    public static void process(final SsaMethod ssaMethod) {
        final ConstCollector cc = new ConstCollector(ssaMethod);
        cc.run();
    }
    
    private ConstCollector(final SsaMethod ssaMethod) {
        this.ssaMeth = ssaMethod;
    }
    
    private void run() {
        final int regSz = this.ssaMeth.getRegCount();
        final ArrayList<TypedConstant> constantList = this.getConstsSortedByCountUse();
        final int toCollect = Math.min(constantList.size(), 5);
        final SsaBasicBlock start = this.ssaMeth.getEntryBlock();
        final HashMap<TypedConstant, RegisterSpec> newRegs = new HashMap<TypedConstant, RegisterSpec>(toCollect);
        for (int i = 0; i < toCollect; ++i) {
            final TypedConstant cst = constantList.get(i);
            final RegisterSpec result = RegisterSpec.make(this.ssaMeth.makeNewSsaReg(), cst);
            final Rop constRop = Rops.opConst(cst);
            if (constRop.getBranchingness() == 1) {
                start.addInsnToHead(new PlainCstInsn(Rops.opConst(cst), SourcePosition.NO_INFO, result, RegisterSpecList.EMPTY, cst));
            }
            else {
                final SsaBasicBlock entryBlock = this.ssaMeth.getEntryBlock();
                final SsaBasicBlock successorBlock = entryBlock.getPrimarySuccessor();
                final SsaBasicBlock constBlock = entryBlock.insertNewSuccessor(successorBlock);
                constBlock.replaceLastInsn(new ThrowingCstInsn(constRop, SourcePosition.NO_INFO, RegisterSpecList.EMPTY, StdTypeList.EMPTY, cst));
                final SsaBasicBlock resultBlock = constBlock.insertNewSuccessor(successorBlock);
                final PlainInsn insn = new PlainInsn(Rops.opMoveResultPseudo(result.getTypeBearer()), SourcePosition.NO_INFO, result, RegisterSpecList.EMPTY);
                resultBlock.addInsnToHead(insn);
            }
            newRegs.put(cst, result);
        }
        this.updateConstUses(newRegs, regSz);
    }
    
    private ArrayList<TypedConstant> getConstsSortedByCountUse() {
        final int regSz = this.ssaMeth.getRegCount();
        final HashMap<TypedConstant, Integer> countUses = new HashMap<TypedConstant, Integer>();
        final HashSet<TypedConstant> usedByLocal = new HashSet<TypedConstant>();
        for (int i = 0; i < regSz; ++i) {
            SsaInsn insn = this.ssaMeth.getDefinitionForRegister(i);
            if (insn != null) {
                if (insn.getOpcode() != null) {
                    final RegisterSpec result = insn.getResult();
                    final TypeBearer typeBearer = result.getTypeBearer();
                    if (typeBearer.isConstant()) {
                        final TypedConstant cst = (TypedConstant)typeBearer;
                        if (insn.getOpcode().getOpcode() == 56) {
                            final int pred = insn.getBlock().getPredecessors().nextSetBit(0);
                            final ArrayList<SsaInsn> predInsns = this.ssaMeth.getBlocks().get(pred).getInsns();
                            insn = predInsns.get(predInsns.size() - 1);
                        }
                        if (insn.canThrow()) {
                            if (cst instanceof CstString) {}
                        }
                        else if (!this.ssaMeth.isRegALocal(result)) {
                            final Integer has = countUses.get(cst);
                            if (has == null) {
                                countUses.put(cst, 1);
                            }
                            else {
                                countUses.put(cst, has + 1);
                            }
                        }
                    }
                }
            }
        }
        final ArrayList<TypedConstant> constantList = new ArrayList<TypedConstant>();
        for (final Map.Entry<TypedConstant, Integer> entry : countUses.entrySet()) {
            if (entry.getValue() > 1) {
                constantList.add(entry.getKey());
            }
        }
        Collections.sort(constantList, new Comparator<Constant>() {
            @Override
            public int compare(final Constant a, final Constant b) {
                int ret = countUses.get(b) - countUses.get(a);
                if (ret == 0) {
                    ret = a.compareTo(b);
                }
                return ret;
            }
            
            @Override
            public boolean equals(final Object obj) {
                return obj == this;
            }
        });
        return constantList;
    }
    
    private void fixLocalAssignment(final RegisterSpec origReg, RegisterSpec newReg) {
        for (final SsaInsn use : this.ssaMeth.getUseListForRegister(origReg.getReg())) {
            final RegisterSpec localAssignment = use.getLocalAssignment();
            if (localAssignment == null) {
                continue;
            }
            if (use.getResult() == null) {
                continue;
            }
            final LocalItem local = localAssignment.getLocalItem();
            use.setResultLocal(null);
            newReg = newReg.withLocalItem(local);
            final SsaInsn newInsn = SsaInsn.makeFromRop(new PlainInsn(Rops.opMarkLocal(newReg), SourcePosition.NO_INFO, null, RegisterSpecList.make(newReg)), use.getBlock());
            final ArrayList<SsaInsn> insns = use.getBlock().getInsns();
            insns.add(insns.indexOf(use) + 1, newInsn);
        }
    }
    
    private void updateConstUses(final HashMap<TypedConstant, RegisterSpec> newRegs, final int origRegCount) {
        final HashSet<TypedConstant> usedByLocal = new HashSet<TypedConstant>();
        final ArrayList<SsaInsn>[] useList = this.ssaMeth.getUseListCopy();
        for (int i = 0; i < origRegCount; ++i) {
            final SsaInsn insn = this.ssaMeth.getDefinitionForRegister(i);
            if (insn != null) {
                final RegisterSpec origReg = insn.getResult();
                final TypeBearer typeBearer = insn.getResult().getTypeBearer();
                if (typeBearer.isConstant()) {
                    final TypedConstant cst = (TypedConstant)typeBearer;
                    final RegisterSpec newReg = newRegs.get(cst);
                    if (newReg != null) {
                        if (!this.ssaMeth.isRegALocal(origReg)) {
                            final RegisterMapper mapper = new RegisterMapper() {
                                @Override
                                public int getNewRegisterCount() {
                                    return ConstCollector.this.ssaMeth.getRegCount();
                                }
                                
                                @Override
                                public RegisterSpec map(final RegisterSpec registerSpec) {
                                    if (registerSpec.getReg() == origReg.getReg()) {
                                        return newReg.withLocalItem(registerSpec.getLocalItem());
                                    }
                                    return registerSpec;
                                }
                            };
                            for (final SsaInsn use : useList[origReg.getReg()]) {
                                if (use.canThrow() && use.getBlock().getSuccessors().cardinality() > 1) {
                                    continue;
                                }
                                use.mapSourceRegisters(mapper);
                            }
                        }
                    }
                }
            }
        }
    }
}

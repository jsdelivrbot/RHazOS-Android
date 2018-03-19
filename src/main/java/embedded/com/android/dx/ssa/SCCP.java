package embedded.com.android.dx.ssa;

import java.util.*;
import embedded.com.android.dx.rop.cst.*;
import embedded.com.android.dx.rop.type.*;
import embedded.com.android.dx.rop.code.*;

public class SCCP
{
    private static final int TOP = 0;
    private static final int CONSTANT = 1;
    private static final int VARYING = 2;
    private SsaMethod ssaMeth;
    private int regCount;
    private int[] latticeValues;
    private Constant[] latticeConstants;
    private ArrayList<SsaBasicBlock> cfgWorklist;
    private ArrayList<SsaBasicBlock> cfgPhiWorklist;
    private BitSet executableBlocks;
    private ArrayList<SsaInsn> ssaWorklist;
    private ArrayList<SsaInsn> varyingWorklist;
    private ArrayList<SsaInsn> branchWorklist;
    
    private SCCP(final SsaMethod ssaMeth) {
        this.ssaMeth = ssaMeth;
        this.regCount = ssaMeth.getRegCount();
        this.latticeValues = new int[this.regCount];
        this.latticeConstants = new Constant[this.regCount];
        this.cfgWorklist = new ArrayList<SsaBasicBlock>();
        this.cfgPhiWorklist = new ArrayList<SsaBasicBlock>();
        this.executableBlocks = new BitSet(ssaMeth.getBlocks().size());
        this.ssaWorklist = new ArrayList<SsaInsn>();
        this.varyingWorklist = new ArrayList<SsaInsn>();
        this.branchWorklist = new ArrayList<SsaInsn>();
        for (int i = 0; i < this.regCount; ++i) {
            this.latticeValues[i] = 0;
            this.latticeConstants[i] = null;
        }
    }
    
    public static void process(final SsaMethod ssaMethod) {
        new SCCP(ssaMethod).run();
    }
    
    private void addBlockToWorklist(final SsaBasicBlock ssaBlock) {
        if (!this.executableBlocks.get(ssaBlock.getIndex())) {
            this.cfgWorklist.add(ssaBlock);
            this.executableBlocks.set(ssaBlock.getIndex());
        }
        else {
            this.cfgPhiWorklist.add(ssaBlock);
        }
    }
    
    private void addUsersToWorklist(final int reg, final int latticeValue) {
        if (latticeValue == 2) {
            for (final SsaInsn insn : this.ssaMeth.getUseListForRegister(reg)) {
                this.varyingWorklist.add(insn);
            }
        }
        else {
            for (final SsaInsn insn : this.ssaMeth.getUseListForRegister(reg)) {
                this.ssaWorklist.add(insn);
            }
        }
    }
    
    private boolean setLatticeValueTo(final int reg, final int value, final Constant cst) {
        if (value != 1) {
            if (this.latticeValues[reg] != value) {
                this.latticeValues[reg] = value;
                return true;
            }
            return false;
        }
        else {
            if (this.latticeValues[reg] != value || !this.latticeConstants[reg].equals(cst)) {
                this.latticeValues[reg] = value;
                this.latticeConstants[reg] = cst;
                return true;
            }
            return false;
        }
    }
    
    private void simulatePhi(final PhiInsn insn) {
        final int phiResultReg = insn.getResult().getReg();
        if (this.latticeValues[phiResultReg] == 2) {
            return;
        }
        final RegisterSpecList sources = insn.getSources();
        int phiResultValue = 0;
        Constant phiConstant = null;
        for (int sourceSize = sources.size(), i = 0; i < sourceSize; ++i) {
            final int predBlockIndex = insn.predBlockIndexForSourcesIndex(i);
            final int sourceReg = sources.get(i).getReg();
            final int sourceRegValue = this.latticeValues[sourceReg];
            if (this.executableBlocks.get(predBlockIndex)) {
                if (sourceRegValue != 1) {
                    phiResultValue = sourceRegValue;
                    break;
                }
                if (phiConstant == null) {
                    phiConstant = this.latticeConstants[sourceReg];
                    phiResultValue = 1;
                }
                else if (!this.latticeConstants[sourceReg].equals(phiConstant)) {
                    phiResultValue = 2;
                    break;
                }
            }
        }
        if (this.setLatticeValueTo(phiResultReg, phiResultValue, phiConstant)) {
            this.addUsersToWorklist(phiResultReg, phiResultValue);
        }
    }
    
    private void simulateBlock(final SsaBasicBlock block) {
        for (final SsaInsn insn : block.getInsns()) {
            if (insn instanceof PhiInsn) {
                this.simulatePhi((PhiInsn)insn);
            }
            else {
                this.simulateStmt(insn);
            }
        }
    }
    
    private void simulatePhiBlock(final SsaBasicBlock block) {
        for (final SsaInsn insn : block.getInsns()) {
            if (!(insn instanceof PhiInsn)) {
                return;
            }
            this.simulatePhi((PhiInsn)insn);
        }
    }
    
    private static String latticeValName(final int latticeVal) {
        switch (latticeVal) {
            case 0: {
                return "TOP";
            }
            case 1: {
                return "CONSTANT";
            }
            case 2: {
                return "VARYING";
            }
            default: {
                return "UNKNOWN";
            }
        }
    }
    
    private void simulateBranch(final SsaInsn insn) {
        final Rop opcode = insn.getOpcode();
        final RegisterSpecList sources = insn.getSources();
        boolean constantBranch = false;
        boolean constantSuccessor = false;
        Label_0540: {
            if (opcode.getBranchingness() == 4) {
                Constant cA = null;
                Constant cB = null;
                final RegisterSpec specA = sources.get(0);
                final int regA = specA.getReg();
                if (!this.ssaMeth.isRegALocal(specA) && this.latticeValues[regA] == 1) {
                    cA = this.latticeConstants[regA];
                }
                if (sources.size() == 2) {
                    final RegisterSpec specB = sources.get(1);
                    final int regB = specB.getReg();
                    if (!this.ssaMeth.isRegALocal(specB) && this.latticeValues[regB] == 1) {
                        cB = this.latticeConstants[regB];
                    }
                }
                if (cA != null && sources.size() == 1) {
                    Label_0324: {
                        switch (((TypedConstant)cA).getBasicType()) {
                            case 6: {
                                constantBranch = true;
                                final int vA = ((CstInteger)cA).getValue();
                                switch (opcode.getOpcode()) {
                                    case 7: {
                                        constantSuccessor = (vA == 0);
                                        break Label_0324;
                                    }
                                    case 8: {
                                        constantSuccessor = (vA != 0);
                                        break Label_0324;
                                    }
                                    case 9: {
                                        constantSuccessor = (vA < 0);
                                        break Label_0324;
                                    }
                                    case 10: {
                                        constantSuccessor = (vA >= 0);
                                        break Label_0324;
                                    }
                                    case 11: {
                                        constantSuccessor = (vA <= 0);
                                        break Label_0324;
                                    }
                                    case 12: {
                                        constantSuccessor = (vA > 0);
                                        break Label_0324;
                                    }
                                    default: {
                                        throw new RuntimeException("Unexpected op");
                                    }
                                }
                             
                            }
                        }
                    }
                }
                else if (cA != null && cB != null) {
                    switch (((TypedConstant)cA).getBasicType()) {
                        case 6: {
                            constantBranch = true;
                            final int vA = ((CstInteger)cA).getValue();
                            final int vB = ((CstInteger)cB).getValue();
                            switch (opcode.getOpcode()) {
                                case 7: {
                                    constantSuccessor = (vA == vB);
                                    break Label_0540;
                                }
                                case 8: {
                                    constantSuccessor = (vA != vB);
                                    break Label_0540;
                                }
                                case 9: {
                                    constantSuccessor = (vA < vB);
                                    break Label_0540;
                                }
                                case 10: {
                                    constantSuccessor = (vA >= vB);
                                    break Label_0540;
                                }
                                case 11: {
                                    constantSuccessor = (vA <= vB);
                                    break Label_0540;
                                }
                                case 12: {
                                    constantSuccessor = (vA > vB);
                                    break Label_0540;
                                }
                                default: {
                                    throw new RuntimeException("Unexpected op");
                                }
                            }
                        
                        }
                    }
                }
            }
        }
        final SsaBasicBlock block = insn.getBlock();
        if (constantBranch) {
            int successorBlock;
            if (constantSuccessor) {
                successorBlock = block.getSuccessorList().get(1);
            }
            else {
                successorBlock = block.getSuccessorList().get(0);
            }
            this.addBlockToWorklist(this.ssaMeth.getBlocks().get(successorBlock));
            this.branchWorklist.add(insn);
        }
        else {
            for (int i = 0; i < block.getSuccessorList().size(); ++i) {
                final int successorBlock2 = block.getSuccessorList().get(i);
                this.addBlockToWorklist(this.ssaMeth.getBlocks().get(successorBlock2));
            }
        }
    }
    
    private Constant simulateMath(final SsaInsn insn, final int resultType) {
        final Insn ropInsn = insn.getOriginalRopInsn();
        final int opcode = insn.getOpcode().getOpcode();
        final RegisterSpecList sources = insn.getSources();
        final int regA = sources.get(0).getReg();
        Constant cA;
        if (this.latticeValues[regA] != 1) {
            cA = null;
        }
        else {
            cA = this.latticeConstants[regA];
        }
        Constant cB;
        if (sources.size() == 1) {
            final CstInsn cstInsn = (CstInsn)ropInsn;
            cB = cstInsn.getConstant();
        }
        else {
            final int regB = sources.get(1).getReg();
            if (this.latticeValues[regB] != 1) {
                cB = null;
            }
            else {
                cB = this.latticeConstants[regB];
            }
        }
        if (cA == null || cB == null) {
            return null;
        }
        switch (resultType) {
            case 6: {
                boolean skip = false;
                final int vA = ((CstInteger)cA).getValue();
                final int vB = ((CstInteger)cB).getValue();
                switch (opcode) {
                    case 14: {
                        final int vR = vA + vB;
                        break;
                    }
                    case 15: {
                        if (sources.size() == 1) {
                            final int vR = vB - vA;
                            break;
                        }
                        final int vR = vA - vB;
                        break;
                    }
                    case 16: {
                        final int vR = vA * vB;
                        break;
                    }
                    case 17: {
                        if (vB == 0) {
                            skip = true;
                            final int vR = 0;
                            break;
                        }
                        final int vR = vA / vB;
                        break;
                    }
                    case 20: {
                        final int vR = vA & vB;
                        break;
                    }
                    case 21: {
                        final int vR = vA | vB;
                        break;
                    }
                    case 22: {
                        final int vR = vA ^ vB;
                        break;
                    }
                    case 23: {
                        final int vR = vA << vB;
                        break;
                    }
                    case 24: {
                        final int vR = vA >> vB;
                        break;
                    }
                    case 25: {
                        final int vR = vA >>> vB;
                        break;
                    }
                    case 18: {
                        if (vB == 0) {
                            skip = true;
                            final int vR = 0;
                            break;
                        }
                        final int vR = vA % vB;
                        break;
                    }
                    default: {
                        throw new RuntimeException("Unexpected op");
                    }
                }
                int vR = 0;
                return skip ? null : CstInteger.make(vR);
            }
            default: {
                return null;
            }
        }
    }
    
    private void simulateStmt(final SsaInsn insn) {
        final Insn ropInsn = insn.getOriginalRopInsn();
        if (ropInsn.getOpcode().getBranchingness() != 1 || ropInsn.getOpcode().isCallLike()) {
            this.simulateBranch(insn);
        }
        final int opcode = insn.getOpcode().getOpcode();
        RegisterSpec result = insn.getResult();
        if (result == null) {
            if (opcode != 17 && opcode != 18) {
                return;
            }
            final SsaBasicBlock succ = insn.getBlock().getPrimarySuccessor();
            result = succ.getInsns().get(0).getResult();
        }
        final int resultReg = result.getReg();
        int resultValue = 2;
        Constant resultConstant = null;
        switch (opcode) {
            case 5: {
                final CstInsn cstInsn = (CstInsn)ropInsn;
                resultValue = 1;
                resultConstant = cstInsn.getConstant();
                break;
            }
            case 2: {
                if (insn.getSources().size() == 1) {
                    final int sourceReg = insn.getSources().get(0).getReg();
                    resultValue = this.latticeValues[sourceReg];
                    resultConstant = this.latticeConstants[sourceReg];
                    break;
                }
                break;
            }
            case 14:
            case 15:
            case 16:
            case 17:
            case 18:
            case 20:
            case 21:
            case 22:
            case 23:
            case 24:
            case 25: {
                resultConstant = this.simulateMath(insn, result.getBasicType());
                if (resultConstant != null) {
                    resultValue = 1;
                    break;
                }
                break;
            }
            case 56: {
                if (this.latticeValues[resultReg] == 1) {
                    resultValue = this.latticeValues[resultReg];
                    resultConstant = this.latticeConstants[resultReg];
                    break;
                }
                break;
            }
        }
        if (this.setLatticeValueTo(resultReg, resultValue, resultConstant)) {
            this.addUsersToWorklist(resultReg, resultValue);
        }
    }
    
    private void run() {
        final SsaBasicBlock firstBlock = this.ssaMeth.getEntryBlock();
        this.addBlockToWorklist(firstBlock);
        while (!this.cfgWorklist.isEmpty() || !this.cfgPhiWorklist.isEmpty() || !this.ssaWorklist.isEmpty() || !this.varyingWorklist.isEmpty()) {
            while (!this.cfgWorklist.isEmpty()) {
                final int listSize = this.cfgWorklist.size() - 1;
                final SsaBasicBlock block = this.cfgWorklist.remove(listSize);
                this.simulateBlock(block);
            }
            while (!this.cfgPhiWorklist.isEmpty()) {
                final int listSize = this.cfgPhiWorklist.size() - 1;
                final SsaBasicBlock block = this.cfgPhiWorklist.remove(listSize);
                this.simulatePhiBlock(block);
            }
            while (!this.varyingWorklist.isEmpty()) {
                final int listSize = this.varyingWorklist.size() - 1;
                final SsaInsn insn = this.varyingWorklist.remove(listSize);
                if (!this.executableBlocks.get(insn.getBlock().getIndex())) {
                    continue;
                }
                if (insn instanceof PhiInsn) {
                    this.simulatePhi((PhiInsn)insn);
                }
                else {
                    this.simulateStmt(insn);
                }
            }
            while (!this.ssaWorklist.isEmpty()) {
                final int listSize = this.ssaWorklist.size() - 1;
                final SsaInsn insn = this.ssaWorklist.remove(listSize);
                if (!this.executableBlocks.get(insn.getBlock().getIndex())) {
                    continue;
                }
                if (insn instanceof PhiInsn) {
                    this.simulatePhi((PhiInsn)insn);
                }
                else {
                    this.simulateStmt(insn);
                }
            }
        }
        this.replaceConstants();
        this.replaceBranches();
    }
    
    private void replaceConstants() {
        for (int reg = 0; reg < this.regCount; ++reg) {
            if (this.latticeValues[reg] == 1) {
                if (this.latticeConstants[reg] instanceof TypedConstant) {
                    final SsaInsn defn = this.ssaMeth.getDefinitionForRegister(reg);
                    final TypeBearer typeBearer = defn.getResult().getTypeBearer();
                    if (!typeBearer.isConstant()) {
                        final RegisterSpec dest = defn.getResult();
                        final RegisterSpec newDest = dest.withType((TypeBearer)this.latticeConstants[reg]);
                        defn.setResult(newDest);
                        for (final SsaInsn insn : this.ssaMeth.getUseListForRegister(reg)) {
                            if (insn.isPhiOrMove()) {
                                continue;
                            }
                            final NormalSsaInsn nInsn = (NormalSsaInsn)insn;
                            final RegisterSpecList sources = insn.getSources();
                            final int index = sources.indexOfRegister(reg);
                            final RegisterSpec spec = sources.get(index);
                            final RegisterSpec newSpec = spec.withType((TypeBearer)this.latticeConstants[reg]);
                            nInsn.changeOneSource(index, newSpec);
                        }
                    }
                }
            }
        }
    }
    
    private void replaceBranches() {
        for (final SsaInsn insn : this.branchWorklist) {
            int oldSuccessor = -1;
            final SsaBasicBlock block = insn.getBlock();
            final int successorSize = block.getSuccessorList().size();
            for (int i = 0; i < successorSize; ++i) {
                final int successorBlock = block.getSuccessorList().get(i);
                if (!this.executableBlocks.get(successorBlock)) {
                    oldSuccessor = successorBlock;
                }
            }
            if (successorSize == 2) {
                if (oldSuccessor == -1) {
                    continue;
                }
                final Insn originalRopInsn = insn.getOriginalRopInsn();
                block.replaceLastInsn(new PlainInsn(Rops.GOTO, originalRopInsn.getPosition(), null, RegisterSpecList.EMPTY));
                block.removeSuccessor(oldSuccessor);
            }
        }
    }
}

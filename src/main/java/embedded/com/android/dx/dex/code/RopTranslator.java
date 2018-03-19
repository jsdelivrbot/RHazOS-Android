package embedded.com.android.dx.dex.code;

import embedded.com.android.dx.dex.*;
import embedded.com.android.dx.util.*;
import embedded.com.android.dx.rop.type.*;
import embedded.com.android.dx.rop.cst.*;
import embedded.com.android.dx.rop.code.*;
import java.util.*;

public final class RopTranslator
{
    private final DexOptions dexOptions;
    private final RopMethod method;
    private final int positionInfo;
    private final LocalVariableInfo locals;
    private final BlockAddresses addresses;
    private final OutputCollector output;
    private final TranslationVisitor translationVisitor;
    private final int regCount;
    private int[] order;
    private final int paramSize;
    private boolean paramsAreInOrder;
    
    public static DalvCode translate(final RopMethod method, final int positionInfo, final LocalVariableInfo locals, final int paramSize, final DexOptions dexOptions) {
        final RopTranslator translator = new RopTranslator(method, positionInfo, locals, paramSize, dexOptions);
        return translator.translateAndGetResult();
    }
    
    private RopTranslator(final RopMethod method, final int positionInfo, final LocalVariableInfo locals, final int paramSize, final DexOptions dexOptions) {
        this.dexOptions = dexOptions;
        this.method = method;
        this.positionInfo = positionInfo;
        this.locals = locals;
        this.addresses = new BlockAddresses(method);
        this.paramSize = paramSize;
        this.order = null;
        this.paramsAreInOrder = calculateParamsAreInOrder(method, paramSize);
        final BasicBlockList blocks = method.getBlocks();
        final int bsz = blocks.size();
        int maxInsns = bsz * 3 + blocks.getInstructionCount();
        if (locals != null) {
            maxInsns += bsz + locals.getAssignmentCount();
        }
        this.regCount = blocks.getRegCount() + (this.paramsAreInOrder ? 0 : this.paramSize);
        this.output = new OutputCollector(dexOptions, maxInsns, bsz * 3, this.regCount, paramSize);
        if (locals != null) {
            this.translationVisitor = new LocalVariableAwareTranslationVisitor(this.output, locals);
        }
        else {
            this.translationVisitor = new TranslationVisitor(this.output);
        }
    }
    
    private static boolean calculateParamsAreInOrder(final RopMethod method, final int paramSize) {
        final boolean[] paramsAreInOrder = { true };
        final int initialRegCount = method.getBlocks().getRegCount();
        method.getBlocks().forEachInsn(new Insn.BaseVisitor() {
            @Override
            public void visitPlainCstInsn(final PlainCstInsn insn) {
                if (insn.getOpcode().getOpcode() == 3) {
                    final int param = ((CstInteger)insn.getConstant()).getValue();
                    paramsAreInOrder[0] = (paramsAreInOrder[0] && initialRegCount - paramSize + param == insn.getResult().getReg());
                }
            }
        });
        return paramsAreInOrder[0];
    }
    
    private DalvCode translateAndGetResult() {
        this.pickOrder();
        this.outputInstructions();
        final StdCatchBuilder catches = new StdCatchBuilder(this.method, this.order, this.addresses);
        return new DalvCode(this.positionInfo, this.output.getFinisher(), catches);
    }
    
    private void outputInstructions() {
        final BasicBlockList blocks = this.method.getBlocks();
        final int[] order = this.order;
        for (int len = order.length, i = 0; i < len; ++i) {
            final int nextI = i + 1;
            final int nextLabel = (nextI == order.length) ? -1 : order[nextI];
            this.outputBlock(blocks.labelToBlock(order[i]), nextLabel);
        }
    }
    
    private void outputBlock(final BasicBlock block, final int nextLabel) {
        final CodeAddress startAddress = this.addresses.getStart(block);
        this.output.add(startAddress);
        if (this.locals != null) {
            final RegisterSpecSet starts = this.locals.getStarts(block);
            this.output.add(new LocalSnapshot(startAddress.getPosition(), starts));
        }
        this.translationVisitor.setBlock(block, this.addresses.getLast(block));
        block.getInsns().forEach(this.translationVisitor);
        this.output.add(this.addresses.getEnd(block));
        final int succ = block.getPrimarySuccessor();
        final Insn lastInsn = block.getLastInsn();
        if (succ >= 0 && succ != nextLabel) {
            final Rop lastRop = lastInsn.getOpcode();
            if (lastRop.getBranchingness() == 4 && block.getSecondarySuccessor() == nextLabel) {
                this.output.reverseBranch(1, this.addresses.getStart(succ));
            }
            else {
                final TargetInsn insn = new TargetInsn(Dops.GOTO, lastInsn.getPosition(), RegisterSpecList.EMPTY, this.addresses.getStart(succ));
                this.output.add(insn);
            }
        }
    }
    
    private void pickOrder() {
        final BasicBlockList blocks = this.method.getBlocks();
        final int sz = blocks.size();
        final int maxLabel = blocks.getMaxLabel();
        final int[] workSet = Bits.makeBitSet(maxLabel);
        final int[] tracebackSet = Bits.makeBitSet(maxLabel);
        for (int i = 0; i < sz; ++i) {
            final BasicBlock one = blocks.get(i);
            Bits.set(workSet, one.getLabel());
        }
        final int[] order = new int[sz];
        int at = 0;
        for (int label = this.method.getFirstLabel(); label != -1; label = Bits.findFirst(workSet, 0)) {
        Label_0086:
            while (true) {
                final IntList preds = this.method.labelToPredecessors(label);
                for (int psz = preds.size(), j = 0; j < psz; ++j) {
                    final int predLabel = preds.get(j);
                    if (Bits.get(tracebackSet, predLabel)) {
                        break;
                    }
                    if (Bits.get(workSet, predLabel)) {
                        final BasicBlock pred = blocks.labelToBlock(predLabel);
                        if (pred.getPrimarySuccessor() == label) {
                            label = predLabel;
                            Bits.set(tracebackSet, label);
                            continue Label_0086;
                        }
                    }
                }
                break;
            }
            while (label != -1) {
                Bits.clear(workSet, label);
                Bits.clear(tracebackSet, label);
                order[at] = label;
                ++at;
                final BasicBlock one2 = blocks.labelToBlock(label);
                final BasicBlock preferredBlock = blocks.preferredSuccessorOf(one2);
                if (preferredBlock == null) {
                    break;
                }
                final int preferred = preferredBlock.getLabel();
                final int primary = one2.getPrimarySuccessor();
                if (Bits.get(workSet, preferred)) {
                    label = preferred;
                }
                else if (primary != preferred && primary >= 0 && Bits.get(workSet, primary)) {
                    label = primary;
                }
                else {
                    final IntList successors = one2.getSuccessors();
                    final int ssz = successors.size();
                    label = -1;
                    for (int k = 0; k < ssz; ++k) {
                        final int candidate = successors.get(k);
                        if (Bits.get(workSet, candidate)) {
                            label = candidate;
                            break;
                        }
                    }
                }
            }
        }
        if (at != sz) {
            throw new RuntimeException("shouldn't happen");
        }
        this.order = order;
    }
    
    private static RegisterSpecList getRegs(final Insn insn) {
        return getRegs(insn, insn.getResult());
    }
    
    private static RegisterSpecList getRegs(final Insn insn, final RegisterSpec resultReg) {
        RegisterSpecList regs = insn.getSources();
        if (insn.getOpcode().isCommutative() && regs.size() == 2 && resultReg.getReg() == regs.get(1).getReg()) {
            regs = RegisterSpecList.make(regs.get(1), regs.get(0));
        }
        if (resultReg == null) {
            return regs;
        }
        return regs.withFirst(resultReg);
    }
    
    private class TranslationVisitor implements Insn.Visitor
    {
        private final OutputCollector output;
        private BasicBlock block;
        private CodeAddress lastAddress;
        
        public TranslationVisitor(final OutputCollector output) {
            this.output = output;
        }
        
        public void setBlock(final BasicBlock block, final CodeAddress lastAddress) {
            this.block = block;
            this.lastAddress = lastAddress;
        }
        
        @Override
        public void visitPlainInsn(final PlainInsn insn) {
            final Rop rop = insn.getOpcode();
            if (rop.getOpcode() == 54) {
                return;
            }
            if (rop.getOpcode() == 56) {
                return;
            }
            final SourcePosition pos = insn.getPosition();
            final Dop opcode = RopToDop.dopFor(insn);
            DalvInsn di = null;
            switch (rop.getBranchingness()) {
                case 1:
                case 2:
                case 6: {
                    di = new SimpleInsn(opcode, pos, getRegs(insn));
                    break;
                }
                case 3: {
                    return;
                }
                case 4: {
                    final int target = this.block.getSuccessors().get(1);
                    di = new TargetInsn(opcode, pos, getRegs(insn), RopTranslator.this.addresses.getStart(target));
                    break;
                }
                default: {
                    throw new RuntimeException("shouldn't happen");
                }
            }
            this.addOutput(di);
        }
        
        @Override
        public void visitPlainCstInsn(final PlainCstInsn insn) {
            final SourcePosition pos = insn.getPosition();
            final Dop opcode = RopToDop.dopFor(insn);
            final Rop rop = insn.getOpcode();
            final int ropOpcode = rop.getOpcode();
            if (rop.getBranchingness() != 1) {
                throw new RuntimeException("shouldn't happen");
            }
            if (ropOpcode == 3) {
                if (!RopTranslator.this.paramsAreInOrder) {
                    final RegisterSpec dest = insn.getResult();
                    final int param = ((CstInteger)insn.getConstant()).getValue();
                    final RegisterSpec source = RegisterSpec.make(RopTranslator.this.regCount - RopTranslator.this.paramSize + param, dest.getType());
                    final DalvInsn di = new SimpleInsn(opcode, pos, RegisterSpecList.make(dest, source));
                    this.addOutput(di);
                }
            }
            else {
                final RegisterSpecList regs = getRegs(insn);
                final DalvInsn di = new CstInsn(opcode, pos, regs, insn.getConstant());
                this.addOutput(di);
            }
        }
        
        @Override
        public void visitSwitchInsn(final SwitchInsn insn) {
            final SourcePosition pos = insn.getPosition();
            final IntList cases = insn.getCases();
            final IntList successors = this.block.getSuccessors();
            final int casesSz = cases.size();
            final int succSz = successors.size();
            final int primarySuccessor = this.block.getPrimarySuccessor();
            if (casesSz != succSz - 1 || primarySuccessor != successors.get(casesSz)) {
                throw new RuntimeException("shouldn't happen");
            }
            final CodeAddress[] switchTargets = new CodeAddress[casesSz];
            for (int i = 0; i < casesSz; ++i) {
                final int label = successors.get(i);
                switchTargets[i] = RopTranslator.this.addresses.getStart(label);
            }
            final CodeAddress dataAddress = new CodeAddress(pos);
            final CodeAddress switchAddress = new CodeAddress(this.lastAddress.getPosition(), true);
            final SwitchData dataInsn = new SwitchData(pos, switchAddress, cases, switchTargets);
            final Dop opcode = dataInsn.isPacked() ? Dops.PACKED_SWITCH : Dops.SPARSE_SWITCH;
            final TargetInsn switchInsn = new TargetInsn(opcode, pos, getRegs(insn), dataAddress);
            this.addOutput(switchAddress);
            this.addOutput(switchInsn);
            this.addOutputSuffix(new OddSpacer(pos));
            this.addOutputSuffix(dataAddress);
            this.addOutputSuffix(dataInsn);
        }
        
        private RegisterSpec getNextMoveResultPseudo() {
            final int label = this.block.getPrimarySuccessor();
            if (label < 0) {
                return null;
            }
            final Insn insn = RopTranslator.this.method.getBlocks().labelToBlock(label).getInsns().get(0);
            if (insn.getOpcode().getOpcode() != 56) {
                return null;
            }
            return insn.getResult();
        }
        
        @Override
        public void visitInvokePolymorphicInsn(final InvokePolymorphicInsn insn) {
            final SourcePosition pos = insn.getPosition();
            final Dop opcode = RopToDop.dopFor(insn);
            final Rop rop = insn.getOpcode();
            if (rop.getBranchingness() != 6) {
                throw new RuntimeException("Expected BRANCH_THROW got " + rop.getBranchingness());
            }
            if (!rop.isCallLike()) {
                throw new RuntimeException("Expected call-like operation");
            }
            this.addOutput(this.lastAddress);
            final RegisterSpecList regs = insn.getSources();
            final Constant[] constants = { insn.getInvokeMethod(), insn.getCallSiteProto() };
            final DalvInsn di = new MultiCstInsn(opcode, pos, regs, constants);
            this.addOutput(di);
        }
        
        @Override
        public void visitThrowingCstInsn(final ThrowingCstInsn insn) {
            final SourcePosition pos = insn.getPosition();
            final Dop opcode = RopToDop.dopFor(insn);
            final Rop rop = insn.getOpcode();
            final Constant cst = insn.getConstant();
            if (rop.getBranchingness() != 6) {
                throw new RuntimeException("Expected BRANCH_THROW got " + rop.getBranchingness());
            }
            this.addOutput(this.lastAddress);
            if (rop.isCallLike()) {
                final RegisterSpecList regs = insn.getSources();
                final DalvInsn di = new CstInsn(opcode, pos, regs, cst);
                this.addOutput(di);
            }
            else {
                final RegisterSpec realResult = this.getNextMoveResultPseudo();
                final RegisterSpecList regs2 = getRegs(insn, realResult);
                final boolean hasResult = opcode.hasResult() || rop.getOpcode() == 43;
                if (hasResult != (realResult != null)) {
                    throw new RuntimeException("Insn with result/move-result-pseudo mismatch " + insn);
                }
                DalvInsn di2;
                if (rop.getOpcode() == 41 && opcode.getOpcode() != 35) {
                    di2 = new SimpleInsn(opcode, pos, regs2);
                }
                else {
                    di2 = new CstInsn(opcode, pos, regs2, cst);
                }
                this.addOutput(di2);
            }
        }
        
        @Override
        public void visitThrowingInsn(final ThrowingInsn insn) {
            final SourcePosition pos = insn.getPosition();
            final Dop opcode = RopToDop.dopFor(insn);
            final Rop rop = insn.getOpcode();
            if (rop.getBranchingness() != 6) {
                throw new RuntimeException("shouldn't happen");
            }
            final RegisterSpec realResult = this.getNextMoveResultPseudo();
            if (opcode.hasResult() != (realResult != null)) {
                throw new RuntimeException("Insn with result/move-result-pseudo mismatch" + insn);
            }
            this.addOutput(this.lastAddress);
            final DalvInsn di = new SimpleInsn(opcode, pos, getRegs(insn, realResult));
            this.addOutput(di);
        }
        
        @Override
        public void visitFillArrayDataInsn(final FillArrayDataInsn insn) {
            final SourcePosition pos = insn.getPosition();
            final Constant cst = insn.getConstant();
            final ArrayList<Constant> values = insn.getInitValues();
            final Rop rop = insn.getOpcode();
            if (rop.getBranchingness() != 1) {
                throw new RuntimeException("shouldn't happen");
            }
            final CodeAddress dataAddress = new CodeAddress(pos);
            final ArrayData dataInsn = new ArrayData(pos, this.lastAddress, values, cst);
            final TargetInsn fillArrayDataInsn = new TargetInsn(Dops.FILL_ARRAY_DATA, pos, getRegs(insn), dataAddress);
            this.addOutput(this.lastAddress);
            this.addOutput(fillArrayDataInsn);
            this.addOutputSuffix(new OddSpacer(pos));
            this.addOutputSuffix(dataAddress);
            this.addOutputSuffix(dataInsn);
        }
        
        protected void addOutput(final DalvInsn insn) {
            this.output.add(insn);
        }
        
        protected void addOutputSuffix(final DalvInsn insn) {
            this.output.addSuffix(insn);
        }
    }
    
    private class LocalVariableAwareTranslationVisitor extends TranslationVisitor
    {
        private LocalVariableInfo locals;
        
        public LocalVariableAwareTranslationVisitor(final OutputCollector output, final LocalVariableInfo locals) {
            super(output);
            this.locals = locals;
        }
        
        @Override
        public void visitPlainInsn(final PlainInsn insn) {
            super.visitPlainInsn(insn);
            this.addIntroductionIfNecessary(insn);
        }
        
        @Override
        public void visitPlainCstInsn(final PlainCstInsn insn) {
            super.visitPlainCstInsn(insn);
            this.addIntroductionIfNecessary(insn);
        }
        
        @Override
        public void visitSwitchInsn(final SwitchInsn insn) {
            super.visitSwitchInsn(insn);
            this.addIntroductionIfNecessary(insn);
        }
        
        @Override
        public void visitThrowingCstInsn(final ThrowingCstInsn insn) {
            super.visitThrowingCstInsn(insn);
            this.addIntroductionIfNecessary(insn);
        }
        
        @Override
        public void visitThrowingInsn(final ThrowingInsn insn) {
            super.visitThrowingInsn(insn);
            this.addIntroductionIfNecessary(insn);
        }
        
        public void addIntroductionIfNecessary(final Insn insn) {
            final RegisterSpec spec = this.locals.getAssignment(insn);
            if (spec != null) {
                this.addOutput(new LocalStart(insn.getPosition(), spec));
            }
        }
    }
}

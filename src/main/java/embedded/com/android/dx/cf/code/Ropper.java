package embedded.com.android.dx.cf.code;

import embedded.com.android.dx.cf.iface.*;
import embedded.com.android.dx.dex.*;
import embedded.com.android.dx.util.*;
import embedded.com.android.dx.rop.cst.*;
import embedded.com.android.dx.rop.type.*;
import embedded.com.android.dx.rop.code.*;
import java.util.*;

public final class Ropper
{
    private static final int PARAM_ASSIGNMENT = -1;
    private static final int RETURN = -2;
    private static final int SYNCH_RETURN = -3;
    private static final int SYNCH_SETUP_1 = -4;
    private static final int SYNCH_SETUP_2 = -5;
    private static final int SYNCH_CATCH_1 = -6;
    private static final int SYNCH_CATCH_2 = -7;
    private static final int SPECIAL_LABEL_COUNT = 7;
    private final ConcreteMethod method;
    private final ByteBlockList blocks;
    private final int maxLocals;
    private final int maxLabel;
    private final RopperMachine machine;
    private final Simulator sim;
    private final Frame[] startFrames;
    private final ArrayList<BasicBlock> result;
    private final ArrayList<IntList> resultSubroutines;
    private final CatchInfo[] catchInfos;
    private boolean synchNeedsExceptionHandler;
    private final Subroutine[] subroutines;
    private boolean hasSubroutines;
    private final ExceptionSetupLabelAllocator exceptionSetupLabelAllocator;
    
    public static RopMethod convert(final ConcreteMethod method, final TranslationAdvice advice, final MethodList methods, final DexOptions dexOptions) {
        try {
            final Ropper r = new Ropper(method, advice, methods, dexOptions);
            r.doit();
            return r.getRopMethod();
        }
        catch (SimException ex) {
            ex.addContext("...while working on method " + method.getNat().toHuman());
            throw ex;
        }
    }
    
    private Ropper(final ConcreteMethod method, final TranslationAdvice advice, final MethodList methods, final DexOptions dexOptions) {
        if (method == null) {
            throw new NullPointerException("method == null");
        }
        if (advice == null) {
            throw new NullPointerException("advice == null");
        }
        this.method = method;
        this.blocks = BasicBlocker.identifyBlocks(method);
        this.maxLabel = this.blocks.getMaxLabel();
        this.maxLocals = method.getMaxLocals();
        this.machine = new RopperMachine(this, method, advice, methods);
        this.sim = new Simulator(this.machine, method, dexOptions);
        this.startFrames = new Frame[this.maxLabel];
        this.subroutines = new Subroutine[this.maxLabel];
        this.result = new ArrayList<BasicBlock>(this.blocks.size() * 2 + 10);
        this.resultSubroutines = new ArrayList<IntList>(this.blocks.size() * 2 + 10);
        this.catchInfos = new CatchInfo[this.maxLabel];
        this.synchNeedsExceptionHandler = false;
        this.startFrames[0] = new Frame(this.maxLocals, method.getMaxStack());
        this.exceptionSetupLabelAllocator = new ExceptionSetupLabelAllocator();
    }
    
    int getFirstTempStackReg() {
        final int regCount = this.getNormalRegCount();
        return this.isSynchronized() ? (regCount + 1) : regCount;
    }
    
    private int getSpecialLabel(final int label) {
        return this.maxLabel + this.method.getCatches().size() + ~label;
    }
    
    private int getMinimumUnreservedLabel() {
        return this.maxLabel + this.method.getCatches().size() + 7;
    }
    
    private int getAvailableLabel() {
        int candidate = this.getMinimumUnreservedLabel();
        for (final BasicBlock bb : this.result) {
            final int label = bb.getLabel();
            if (label >= candidate) {
                candidate = label + 1;
            }
        }
        return candidate;
    }
    
    private boolean isSynchronized() {
        final int accessFlags = this.method.getAccessFlags();
        return (accessFlags & 0x20) != 0x0;
    }
    
    private boolean isStatic() {
        final int accessFlags = this.method.getAccessFlags();
        return (accessFlags & 0x8) != 0x0;
    }
    
    private int getNormalRegCount() {
        return this.maxLocals + this.method.getMaxStack();
    }
    
    private RegisterSpec getSynchReg() {
        final int reg = this.getNormalRegCount();
        return RegisterSpec.make((reg < 1) ? 1 : reg, Type.OBJECT);
    }
    
    private int labelToResultIndex(final int label) {
        for (int sz = this.result.size(), i = 0; i < sz; ++i) {
            final BasicBlock one = this.result.get(i);
            if (one.getLabel() == label) {
                return i;
            }
        }
        return -1;
    }
    
    private BasicBlock labelToBlock(final int label) {
        final int idx = this.labelToResultIndex(label);
        if (idx < 0) {
            throw new IllegalArgumentException("no such label " + Hex.u2(label));
        }
        return this.result.get(idx);
    }
    
    private void addBlock(final BasicBlock block, final IntList subroutines) {
        if (block == null) {
            throw new NullPointerException("block == null");
        }
        this.result.add(block);
        subroutines.throwIfMutable();
        this.resultSubroutines.add(subroutines);
    }
    
    private boolean addOrReplaceBlock(final BasicBlock block, final IntList subroutines) {
        if (block == null) {
            throw new NullPointerException("block == null");
        }
        final int idx = this.labelToResultIndex(block.getLabel());
        boolean ret;
        if (idx < 0) {
            ret = false;
        }
        else {
            this.removeBlockAndSpecialSuccessors(idx);
            ret = true;
        }
        this.result.add(block);
        subroutines.throwIfMutable();
        this.resultSubroutines.add(subroutines);
        return ret;
    }
    
    private boolean addOrReplaceBlockNoDelete(final BasicBlock block, final IntList subroutines) {
        if (block == null) {
            throw new NullPointerException("block == null");
        }
        final int idx = this.labelToResultIndex(block.getLabel());
        boolean ret;
        if (idx < 0) {
            ret = false;
        }
        else {
            this.result.remove(idx);
            this.resultSubroutines.remove(idx);
            ret = true;
        }
        this.result.add(block);
        subroutines.throwIfMutable();
        this.resultSubroutines.add(subroutines);
        return ret;
    }
    
    private void removeBlockAndSpecialSuccessors(int idx) {
        final int minLabel = this.getMinimumUnreservedLabel();
        final BasicBlock block = this.result.get(idx);
        final IntList successors = block.getSuccessors();
        final int sz = successors.size();
        this.result.remove(idx);
        this.resultSubroutines.remove(idx);
        for (int i = 0; i < sz; ++i) {
            final int label = successors.get(i);
            if (label >= minLabel) {
                idx = this.labelToResultIndex(label);
                if (idx < 0) {
                    throw new RuntimeException("Invalid label " + Hex.u2(label));
                }
                this.removeBlockAndSpecialSuccessors(idx);
            }
        }
    }
    
    private RopMethod getRopMethod() {
        final int sz = this.result.size();
        final BasicBlockList bbl = new BasicBlockList(sz);
        for (int i = 0; i < sz; ++i) {
            bbl.set(i, this.result.get(i));
        }
        bbl.setImmutable();
        return new RopMethod(bbl, this.getSpecialLabel(-1));
    }
    
    private void doit() {
        final int[] workSet = Bits.makeBitSet(this.maxLabel);
        Bits.set(workSet, 0);
        this.addSetupBlocks();
        this.setFirstFrame();
        while (true) {
            final int offset = Bits.findFirst(workSet, 0);
            if (offset < 0) {
                break;
            }
            Bits.clear(workSet, offset);
            final ByteBlock block = this.blocks.labelToBlock(offset);
            final Frame frame = this.startFrames[offset];
            try {
                this.processBlock(block, frame, workSet);
            }
            catch (SimException ex) {
                ex.addContext("...while working on block " + Hex.u2(offset));
                throw ex;
            }
        }
        this.addReturnBlock();
        this.addSynchExceptionHandlerBlock();
        this.addExceptionSetupBlocks();
        if (this.hasSubroutines) {
            this.inlineSubroutines();
        }
    }
    
    private void setFirstFrame() {
        final Prototype desc = this.method.getEffectiveDescriptor();
        this.startFrames[0].initializeWithParameters(desc.getParameterTypes());
        this.startFrames[0].setImmutable();
    }
    
    private void processBlock(final ByteBlock block, Frame frame, final int[] workSet) {
        final ByteCatchList catches = block.getCatches();
        this.machine.startBlock(catches.toRopCatchList());
        frame = frame.copy();
        this.sim.simulate(block, frame);
        frame.setImmutable();
        int extraBlockCount = this.machine.getExtraBlockCount();
        final ArrayList<Insn> insns = this.machine.getInsns();
        int insnSz = insns.size();
        final int catchSz = catches.size();
        IntList successors = block.getSuccessors();
        Subroutine calledSubroutine = null;
        int startSuccessorIndex;
        if (this.machine.hasJsr()) {
            startSuccessorIndex = 1;
            final int subroutineLabel = successors.get(1);
            if (this.subroutines[subroutineLabel] == null) {
                this.subroutines[subroutineLabel] = new Subroutine(subroutineLabel);
            }
            this.subroutines[subroutineLabel].addCallerBlock(block.getLabel());
            calledSubroutine = this.subroutines[subroutineLabel];
        }
        else if (this.machine.hasRet()) {
            final ReturnAddress ra = this.machine.getReturnAddress();
            final int subroutineLabel2 = ra.getSubroutineAddress();
            if (this.subroutines[subroutineLabel2] == null) {
                this.subroutines[subroutineLabel2] = new Subroutine(null, subroutineLabel2, block.getLabel());
            }
            else {
                this.subroutines[subroutineLabel2].addRetBlock(block.getLabel());
            }
            successors = this.subroutines[subroutineLabel2].getSuccessors();
            this.subroutines[subroutineLabel2].mergeToSuccessors(frame, workSet);
            startSuccessorIndex = successors.size();
        }
        else if (this.machine.wereCatchesUsed()) {
            startSuccessorIndex = catchSz;
        }
        else {
            startSuccessorIndex = 0;
        }
        int succSz = successors.size();
        for (int i = startSuccessorIndex; i < succSz; ++i) {
            final int succ = successors.get(i);
            try {
                this.mergeAndWorkAsNecessary(succ, block.getLabel(), calledSubroutine, frame, workSet);
            }
            catch (SimException ex) {
                ex.addContext("...while merging to block " + Hex.u2(succ));
                throw ex;
            }
        }
        if (succSz == 0 && this.machine.returns()) {
            successors = IntList.makeImmutable(this.getSpecialLabel(-2));
            succSz = 1;
        }
        int primarySucc;
        if (succSz == 0) {
            primarySucc = -1;
        }
        else {
            primarySucc = this.machine.getPrimarySuccessorIndex();
            if (primarySucc >= 0) {
                primarySucc = successors.get(primarySucc);
            }
        }
        final boolean synch = this.isSynchronized() && this.machine.canThrow();
        if (synch || catchSz != 0) {
            boolean catchesAny = false;
            final IntList newSucc = new IntList(succSz);
            for (int j = 0; j < catchSz; ++j) {
                final ByteCatchList.Item one = catches.get(j);
                final CstType exceptionClass = one.getExceptionClass();
                final int targ = one.getHandlerPc();
                catchesAny |= (exceptionClass == CstType.OBJECT);
                final Frame f = frame.makeExceptionHandlerStartFrame(exceptionClass);
                try {
                    this.mergeAndWorkAsNecessary(targ, block.getLabel(), null, f, workSet);
                }
                catch (SimException ex2) {
                    ex2.addContext("...while merging exception to block " + Hex.u2(targ));
                    throw ex2;
                }
                CatchInfo handlers = this.catchInfos[targ];
                if (handlers == null) {
                    handlers = new CatchInfo();
                    this.catchInfos[targ] = handlers;
                }
                final ExceptionHandlerSetup handler = handlers.getSetup(exceptionClass.getClassType());
                newSucc.add(handler.getLabel());
            }
            if (synch && !catchesAny) {
                newSucc.add(this.getSpecialLabel(-6));
                this.synchNeedsExceptionHandler = true;
                for (int j = insnSz - extraBlockCount - 1; j < insnSz; ++j) {
                    Insn insn = insns.get(j);
                    if (insn.canThrow()) {
                        insn = insn.withAddedCatch(Type.OBJECT);
                        insns.set(j, insn);
                    }
                }
            }
            if (primarySucc >= 0) {
                newSucc.add(primarySucc);
            }
            newSucc.setImmutable();
            successors = newSucc;
        }
        final int primarySuccListIndex = successors.indexOf(primarySucc);
        while (extraBlockCount > 0) {
            final Insn extraInsn = insns.get(--insnSz);
            final boolean needsGoto = extraInsn.getOpcode().getBranchingness() == 1;
            final InsnList il = new InsnList(needsGoto ? 2 : 1);
            IntList extraBlockSuccessors = successors;
            il.set(0, extraInsn);
            if (needsGoto) {
                il.set(1, new PlainInsn(Rops.GOTO, extraInsn.getPosition(), null, RegisterSpecList.EMPTY));
                extraBlockSuccessors = IntList.makeImmutable(primarySucc);
            }
            il.setImmutable();
            final int label = this.getAvailableLabel();
            final BasicBlock bb = new BasicBlock(label, il, extraBlockSuccessors, primarySucc);
            this.addBlock(bb, frame.getSubroutines());
            successors = successors.mutableCopy();
            successors.set(primarySuccListIndex, label);
            successors.setImmutable();
            primarySucc = label;
            --extraBlockCount;
        }
        final Insn lastInsn = (insnSz == 0) ? null : insns.get(insnSz - 1);
        if (lastInsn == null || lastInsn.getOpcode().getBranchingness() == 1) {
            final SourcePosition pos = (lastInsn == null) ? SourcePosition.NO_INFO : lastInsn.getPosition();
            insns.add(new PlainInsn(Rops.GOTO, pos, null, RegisterSpecList.EMPTY));
            ++insnSz;
        }
        final InsnList il2 = new InsnList(insnSz);
        for (int k = 0; k < insnSz; ++k) {
            il2.set(k, insns.get(k));
        }
        il2.setImmutable();
        final BasicBlock bb2 = new BasicBlock(block.getLabel(), il2, successors, primarySucc);
        this.addOrReplaceBlock(bb2, frame.getSubroutines());
    }
    
    private void mergeAndWorkAsNecessary(final int label, final int pred, final Subroutine calledSubroutine, final Frame frame, final int[] workSet) {
        final Frame existing = this.startFrames[label];
        if (existing != null) {
            Frame merged;
            if (calledSubroutine != null) {
                merged = existing.mergeWithSubroutineCaller(frame, calledSubroutine.getStartBlock(), pred);
            }
            else {
                merged = existing.mergeWith(frame);
            }
            if (merged != existing) {
                this.startFrames[label] = merged;
                Bits.set(workSet, label);
            }
        }
        else {
            if (calledSubroutine != null) {
                this.startFrames[label] = frame.makeNewSubroutineStartFrame(label, pred);
            }
            else {
                this.startFrames[label] = frame;
            }
            Bits.set(workSet, label);
        }
    }
    
    private void addSetupBlocks() {
        final LocalVariableList localVariables = this.method.getLocalVariables();
        final SourcePosition pos = this.method.makeSourcePosistion(0);
        final Prototype desc = this.method.getEffectiveDescriptor();
        final StdTypeList params = desc.getParameterTypes();
        final int sz = params.size();
        InsnList insns = new InsnList(sz + 1);
        int at = 0;
        for (int i = 0; i < sz; ++i) {
            final Type one = params.get(i);
            final LocalVariableList.Item local = localVariables.pcAndIndexToLocal(0, at);
            final RegisterSpec result = (local == null) ? RegisterSpec.make(at, one) : RegisterSpec.makeLocalOptional(at, one, local.getLocalItem());
            final Insn insn = new PlainCstInsn(Rops.opMoveParam(one), pos, result, RegisterSpecList.EMPTY, CstInteger.make(at));
            insns.set(i, insn);
            at += one.getCategory();
        }
        insns.set(sz, new PlainInsn(Rops.GOTO, pos, null, RegisterSpecList.EMPTY));
        insns.setImmutable();
        final boolean synch = this.isSynchronized();
        final int label = synch ? this.getSpecialLabel(-4) : 0;
        BasicBlock bb = new BasicBlock(this.getSpecialLabel(-1), insns, IntList.makeImmutable(label), label);
        this.addBlock(bb, IntList.EMPTY);
        if (synch) {
            final RegisterSpec synchReg = this.getSynchReg();
            if (this.isStatic()) {
                final Insn insn = new ThrowingCstInsn(Rops.CONST_OBJECT, pos, RegisterSpecList.EMPTY, StdTypeList.EMPTY, this.method.getDefiningClass());
                insns = new InsnList(1);
                insns.set(0, insn);
            }
            else {
                insns = new InsnList(2);
                final Insn insn = new PlainCstInsn(Rops.MOVE_PARAM_OBJECT, pos, synchReg, RegisterSpecList.EMPTY, CstInteger.VALUE_0);
                insns.set(0, insn);
                insns.set(1, new PlainInsn(Rops.GOTO, pos, null, RegisterSpecList.EMPTY));
            }
            final int label2 = this.getSpecialLabel(-5);
            insns.setImmutable();
            bb = new BasicBlock(label, insns, IntList.makeImmutable(label2), label2);
            this.addBlock(bb, IntList.EMPTY);
            insns = new InsnList(this.isStatic() ? 2 : 1);
            if (this.isStatic()) {
                insns.set(0, new PlainInsn(Rops.opMoveResultPseudo(synchReg), pos, synchReg, RegisterSpecList.EMPTY));
            }
            final Insn insn = new ThrowingInsn(Rops.MONITOR_ENTER, pos, RegisterSpecList.make(synchReg), StdTypeList.EMPTY);
            insns.set(this.isStatic() ? 1 : 0, insn);
            insns.setImmutable();
            bb = new BasicBlock(label2, insns, IntList.makeImmutable(0), 0);
            this.addBlock(bb, IntList.EMPTY);
        }
    }
    
    private void addReturnBlock() {
        final Rop returnOp = this.machine.getReturnOp();
        if (returnOp == null) {
            return;
        }
        final SourcePosition returnPos = this.machine.getReturnPosition();
        int label = this.getSpecialLabel(-2);
        if (this.isSynchronized()) {
            final InsnList insns = new InsnList(1);
            final Insn insn = new ThrowingInsn(Rops.MONITOR_EXIT, returnPos, RegisterSpecList.make(this.getSynchReg()), StdTypeList.EMPTY);
            insns.set(0, insn);
            insns.setImmutable();
            final int nextLabel = this.getSpecialLabel(-3);
            final BasicBlock bb = new BasicBlock(label, insns, IntList.makeImmutable(nextLabel), nextLabel);
            this.addBlock(bb, IntList.EMPTY);
            label = nextLabel;
        }
        final InsnList insns = new InsnList(1);
        final TypeList sourceTypes = returnOp.getSources();
        RegisterSpecList sources;
        if (sourceTypes.size() == 0) {
            sources = RegisterSpecList.EMPTY;
        }
        else {
            final RegisterSpec source = RegisterSpec.make(0, sourceTypes.getType(0));
            sources = RegisterSpecList.make(source);
        }
        final Insn insn2 = new PlainInsn(returnOp, returnPos, null, sources);
        insns.set(0, insn2);
        insns.setImmutable();
        final BasicBlock bb2 = new BasicBlock(label, insns, IntList.EMPTY, -1);
        this.addBlock(bb2, IntList.EMPTY);
    }
    
    private void addSynchExceptionHandlerBlock() {
        if (!this.synchNeedsExceptionHandler) {
            return;
        }
        final SourcePosition pos = this.method.makeSourcePosistion(0);
        final RegisterSpec exReg = RegisterSpec.make(0, Type.THROWABLE);
        InsnList insns = new InsnList(2);
        Insn insn = new PlainInsn(Rops.opMoveException(Type.THROWABLE), pos, exReg, RegisterSpecList.EMPTY);
        insns.set(0, insn);
        insn = new ThrowingInsn(Rops.MONITOR_EXIT, pos, RegisterSpecList.make(this.getSynchReg()), StdTypeList.EMPTY);
        insns.set(1, insn);
        insns.setImmutable();
        final int label2 = this.getSpecialLabel(-7);
        BasicBlock bb = new BasicBlock(this.getSpecialLabel(-6), insns, IntList.makeImmutable(label2), label2);
        this.addBlock(bb, IntList.EMPTY);
        insns = new InsnList(1);
        insn = new ThrowingInsn(Rops.THROW, pos, RegisterSpecList.make(exReg), StdTypeList.EMPTY);
        insns.set(0, insn);
        insns.setImmutable();
        bb = new BasicBlock(label2, insns, IntList.EMPTY, -1);
        this.addBlock(bb, IntList.EMPTY);
    }
    
    private void addExceptionSetupBlocks() {
        for (int len = this.catchInfos.length, i = 0; i < len; ++i) {
            final CatchInfo catches = this.catchInfos[i];
            if (catches != null) {
                for (final ExceptionHandlerSetup one : catches.getSetups()) {
                    final Insn proto = this.labelToBlock(i).getFirstInsn();
                    final SourcePosition pos = proto.getPosition();
                    final InsnList il = new InsnList(2);
                    Insn insn = new PlainInsn(Rops.opMoveException(one.getCaughtType()), pos, RegisterSpec.make(this.maxLocals, one.getCaughtType()), RegisterSpecList.EMPTY);
                    il.set(0, insn);
                    insn = new PlainInsn(Rops.GOTO, pos, null, RegisterSpecList.EMPTY);
                    il.set(1, insn);
                    il.setImmutable();
                    final BasicBlock bb = new BasicBlock(one.getLabel(), il, IntList.makeImmutable(i), i);
                    this.addBlock(bb, this.startFrames[i].getSubroutines());
                }
            }
        }
    }
    
    private boolean isSubroutineCaller(final BasicBlock bb) {
        final IntList successors = bb.getSuccessors();
        if (successors.size() < 2) {
            return false;
        }
        final int subLabel = successors.get(1);
        return subLabel < this.subroutines.length && this.subroutines[subLabel] != null;
    }
    
    private void inlineSubroutines() {
        final IntList reachableSubroutineCallerLabels = new IntList(4);
        this.forEachNonSubBlockDepthFirst(0, new BasicBlock.Visitor() {
            @Override
            public void visitBlock(final BasicBlock b) {
                if (Ropper.this.isSubroutineCaller(b)) {
                    reachableSubroutineCallerLabels.add(b.getLabel());
                }
            }
        });
        final int largestAllocedLabel = this.getAvailableLabel();
        final ArrayList<IntList> labelToSubroutines = new ArrayList<IntList>(largestAllocedLabel);
        for (int i = 0; i < largestAllocedLabel; ++i) {
            labelToSubroutines.add(null);
        }
        for (int i = 0; i < this.result.size(); ++i) {
            final BasicBlock b = this.result.get(i);
            if (b != null) {
                final IntList subroutineList = this.resultSubroutines.get(i);
                labelToSubroutines.set(b.getLabel(), subroutineList);
            }
        }
        for (int sz = reachableSubroutineCallerLabels.size(), j = 0; j < sz; ++j) {
            final int label = reachableSubroutineCallerLabels.get(j);
            new SubroutineInliner(new LabelAllocator(this.getAvailableLabel()), labelToSubroutines).inlineSubroutineCalledFrom(this.labelToBlock(label));
        }
        this.deleteUnreachableBlocks();
    }
    
    private void deleteUnreachableBlocks() {
        final IntList reachableLabels = new IntList(this.result.size());
        this.resultSubroutines.clear();
        this.forEachNonSubBlockDepthFirst(this.getSpecialLabel(-1), new BasicBlock.Visitor() {
            @Override
            public void visitBlock(final BasicBlock b) {
                reachableLabels.add(b.getLabel());
            }
        });
        reachableLabels.sort();
        for (int i = this.result.size() - 1; i >= 0; --i) {
            if (reachableLabels.indexOf(this.result.get(i).getLabel()) < 0) {
                this.result.remove(i);
            }
        }
    }
    
    private Subroutine subroutineFromRetBlock(final int label) {
        for (int i = this.subroutines.length - 1; i >= 0; --i) {
            if (this.subroutines[i] != null) {
                final Subroutine subroutine = this.subroutines[i];
                if (subroutine.retBlocks.get(label)) {
                    return subroutine;
                }
            }
        }
        return null;
    }
    
    private InsnList filterMoveReturnAddressInsns(final InsnList insns) {
        int newSz = 0;
        final int sz = insns.size();
        for (int i = 0; i < sz; ++i) {
            if (insns.get(i).getOpcode() != Rops.MOVE_RETURN_ADDRESS) {
                ++newSz;
            }
        }
        if (newSz == sz) {
            return insns;
        }
        final InsnList newInsns = new InsnList(newSz);
        int newIndex = 0;
        for (int j = 0; j < sz; ++j) {
            final Insn insn = insns.get(j);
            if (insn.getOpcode() != Rops.MOVE_RETURN_ADDRESS) {
                newInsns.set(newIndex++, insn);
            }
        }
        newInsns.setImmutable();
        return newInsns;
    }
    
    private void forEachNonSubBlockDepthFirst(final int firstLabel, final BasicBlock.Visitor v) {
        this.forEachNonSubBlockDepthFirst0(this.labelToBlock(firstLabel), v, new BitSet(this.maxLabel));
    }
    
    private void forEachNonSubBlockDepthFirst0(final BasicBlock next, final BasicBlock.Visitor v, final BitSet visited) {
        v.visitBlock(next);
        visited.set(next.getLabel());
        final IntList successors = next.getSuccessors();
        for (int sz = successors.size(), i = 0; i < sz; ++i) {
            final int succ = successors.get(i);
            if (!visited.get(succ)) {
                if (!this.isSubroutineCaller(next) || i <= 0) {
                    final int idx = this.labelToResultIndex(succ);
                    if (idx >= 0) {
                        this.forEachNonSubBlockDepthFirst0(this.result.get(idx), v, visited);
                    }
                }
            }
        }
    }
    
    private class CatchInfo
    {
        private final Map<Type, ExceptionHandlerSetup> setups;
        
        private CatchInfo() {
            this.setups = new HashMap<Type, ExceptionHandlerSetup>();
        }
        
        ExceptionHandlerSetup getSetup(final Type caughtType) {
            ExceptionHandlerSetup handler = this.setups.get(caughtType);
            if (handler == null) {
                final int handlerSetupLabel = Ropper.this.exceptionSetupLabelAllocator.getNextLabel();
                handler = new ExceptionHandlerSetup(caughtType, handlerSetupLabel);
                this.setups.put(caughtType, handler);
            }
            return handler;
        }
        
        Collection<ExceptionHandlerSetup> getSetups() {
            return this.setups.values();
        }
    }
    
    private static class ExceptionHandlerSetup
    {
        private Type caughtType;
        private int label;
        
        ExceptionHandlerSetup(final Type caughtType, final int label) {
            this.caughtType = caughtType;
            this.label = label;
        }
        
        Type getCaughtType() {
            return this.caughtType;
        }
        
        public int getLabel() {
            return this.label;
        }
    }
    
    private class Subroutine
    {
        private BitSet callerBlocks;
        private BitSet retBlocks;
        private int startBlock;
        
        Subroutine(final int startBlock) {
            this.startBlock = startBlock;
            this.retBlocks = new BitSet(Ropper.this.maxLabel);
            this.callerBlocks = new BitSet(Ropper.this.maxLabel);
            Ropper.this.hasSubroutines = true;
        }
        
        Subroutine(final Ropper ropper, final int startBlock, final int retBlock) {
            this(startBlock);
            this.addRetBlock(retBlock);
        }
        
        int getStartBlock() {
            return this.startBlock;
        }
        
        void addRetBlock(final int retBlock) {
            this.retBlocks.set(retBlock);
        }
        
        void addCallerBlock(final int label) {
            this.callerBlocks.set(label);
        }
        
        IntList getSuccessors() {
            final IntList successors = new IntList(this.callerBlocks.size());
            for (int label = this.callerBlocks.nextSetBit(0); label >= 0; label = this.callerBlocks.nextSetBit(label + 1)) {
                final BasicBlock subCaller = Ropper.this.labelToBlock(label);
                successors.add(subCaller.getSuccessors().get(0));
            }
            successors.setImmutable();
            return successors;
        }
        
        void mergeToSuccessors(final Frame frame, final int[] workSet) {
            for (int label = this.callerBlocks.nextSetBit(0); label >= 0; label = this.callerBlocks.nextSetBit(label + 1)) {
                final BasicBlock subCaller = Ropper.this.labelToBlock(label);
                final int succLabel = subCaller.getSuccessors().get(0);
                final Frame subFrame = frame.subFrameForLabel(this.startBlock, label);
                if (subFrame != null) {
                    Ropper.this.mergeAndWorkAsNecessary(succLabel, -1, null, subFrame, workSet);
                }
                else {
                    Bits.set(workSet, label);
                }
            }
        }
    }
    
    private static class LabelAllocator
    {
        int nextAvailableLabel;
        
        LabelAllocator(final int startLabel) {
            this.nextAvailableLabel = startLabel;
        }
        
        int getNextLabel() {
            return this.nextAvailableLabel++;
        }
    }
    
    private class ExceptionSetupLabelAllocator extends LabelAllocator
    {
        int maxSetupLabel;
        
        ExceptionSetupLabelAllocator() {
            super(Ropper.this.maxLabel);
            this.maxSetupLabel = Ropper.this.maxLabel + Ropper.this.method.getCatches().size();
        }
        
        @Override
        int getNextLabel() {
            if (this.nextAvailableLabel >= this.maxSetupLabel) {
                throw new IndexOutOfBoundsException();
            }
            return this.nextAvailableLabel++;
        }
    }
    
    private class SubroutineInliner
    {
        private final HashMap<Integer, Integer> origLabelToCopiedLabel;
        private final BitSet workList;
        private int subroutineStart;
        private int subroutineSuccessor;
        private final LabelAllocator labelAllocator;
        private final ArrayList<IntList> labelToSubroutines;
        
        SubroutineInliner(final LabelAllocator labelAllocator, final ArrayList<IntList> labelToSubroutines) {
            this.origLabelToCopiedLabel = new HashMap<Integer, Integer>();
            this.workList = new BitSet(Ropper.this.maxLabel);
            this.labelAllocator = labelAllocator;
            this.labelToSubroutines = labelToSubroutines;
        }
        
        void inlineSubroutineCalledFrom(final BasicBlock b) {
            this.subroutineSuccessor = b.getSuccessors().get(0);
            this.subroutineStart = b.getSuccessors().get(1);
            final int newSubStartLabel = this.mapOrAllocateLabel(this.subroutineStart);
            for (int label = this.workList.nextSetBit(0); label >= 0; label = this.workList.nextSetBit(0)) {
                this.workList.clear(label);
                final int newLabel = this.origLabelToCopiedLabel.get(label);
                this.copyBlock(label, newLabel);
                if (Ropper.this.isSubroutineCaller(Ropper.this.labelToBlock(label))) {
                    new SubroutineInliner(this.labelAllocator, this.labelToSubroutines).inlineSubroutineCalledFrom(Ropper.this.labelToBlock(newLabel));
                }
            }
            Ropper.this.addOrReplaceBlockNoDelete(new BasicBlock(b.getLabel(), b.getInsns(), IntList.makeImmutable(newSubStartLabel), newSubStartLabel), this.labelToSubroutines.get(b.getLabel()));
        }
        
        private void copyBlock(final int origLabel, final int newLabel) {
            final BasicBlock origBlock = Ropper.this.labelToBlock(origLabel);
            final IntList origSuccessors = origBlock.getSuccessors();
            int primarySuccessor = -1;
            IntList successors;
            if (Ropper.this.isSubroutineCaller(origBlock)) {
                successors = IntList.makeImmutable(this.mapOrAllocateLabel(origSuccessors.get(0)), origSuccessors.get(1));
            }
            else {
                final Subroutine subroutine;
                if (null != (subroutine = Ropper.this.subroutineFromRetBlock(origLabel))) {
                    if (subroutine.startBlock != this.subroutineStart) {
                        throw new RuntimeException("ret instruction returns to label " + Hex.u2(subroutine.startBlock) + " expected: " + Hex.u2(this.subroutineStart));
                    }
                    successors = IntList.makeImmutable(this.subroutineSuccessor);
                    primarySuccessor = this.subroutineSuccessor;
                }
                else {
                    final int origPrimary = origBlock.getPrimarySuccessor();
                    final int sz = origSuccessors.size();
                    successors = new IntList(sz);
                    for (int i = 0; i < sz; ++i) {
                        final int origSuccLabel = origSuccessors.get(i);
                        final int newSuccLabel = this.mapOrAllocateLabel(origSuccLabel);
                        successors.add(newSuccLabel);
                        if (origPrimary == origSuccLabel) {
                            primarySuccessor = newSuccLabel;
                        }
                    }
                    successors.setImmutable();
                }
            }
            Ropper.this.addBlock(new BasicBlock(newLabel, Ropper.this.filterMoveReturnAddressInsns(origBlock.getInsns()), successors, primarySuccessor), this.labelToSubroutines.get(newLabel));
        }
        
        private boolean involvedInSubroutine(final int label, final int subroutineStart) {
            final IntList subroutinesList = this.labelToSubroutines.get(label);
            return subroutinesList != null && subroutinesList.size() > 0 && subroutinesList.top() == subroutineStart;
        }
        
        private int mapOrAllocateLabel(final int origLabel) {
            final Integer mappedLabel = this.origLabelToCopiedLabel.get(origLabel);
            int resultLabel;
            if (mappedLabel != null) {
                resultLabel = mappedLabel;
            }
            else if (!this.involvedInSubroutine(origLabel, this.subroutineStart)) {
                resultLabel = origLabel;
            }
            else {
                resultLabel = this.labelAllocator.getNextLabel();
                this.workList.set(origLabel);
                this.origLabelToCopiedLabel.put(origLabel, resultLabel);
                while (this.labelToSubroutines.size() <= resultLabel) {
                    this.labelToSubroutines.add(null);
                }
                this.labelToSubroutines.set(resultLabel, this.labelToSubroutines.get(origLabel));
            }
            return resultLabel;
        }
    }
}

package embedded.com.android.dx.rop.code;

import embedded.com.android.dx.util.*;
import embedded.com.android.dx.rop.type.*;

public final class BasicBlockList extends LabeledList
{
    private int regCount;
    
    public BasicBlockList(final int size) {
        super(size);
        this.regCount = -1;
    }
    
    private BasicBlockList(final BasicBlockList old) {
        super(old);
        this.regCount = old.regCount;
    }
    
    public BasicBlock get(final int n) {
        return (BasicBlock)this.get0(n);
    }
    
    public void set(final int n, final BasicBlock bb) {
        super.set(n, bb);
        this.regCount = -1;
    }
    
    public int getRegCount() {
        if (this.regCount == -1) {
            final RegCountVisitor visitor = new RegCountVisitor();
            this.forEachInsn(visitor);
            this.regCount = visitor.getRegCount();
        }
        return this.regCount;
    }
    
    public int getInstructionCount() {
        final int sz = this.size();
        int result = 0;
        for (int i = 0; i < sz; ++i) {
            final BasicBlock one = (BasicBlock)this.getOrNull0(i);
            if (one != null) {
                result += one.getInsns().size();
            }
        }
        return result;
    }
    
    public int getEffectiveInstructionCount() {
        final int sz = this.size();
        int result = 0;
        for (int i = 0; i < sz; ++i) {
            final BasicBlock one = (BasicBlock)this.getOrNull0(i);
            if (one != null) {
                final InsnList insns = one.getInsns();
                for (int insnsSz = insns.size(), j = 0; j < insnsSz; ++j) {
                    final Insn insn = insns.get(j);
                    if (insn.getOpcode().getOpcode() != 54) {
                        ++result;
                    }
                }
            }
        }
        return result;
    }
    
    public BasicBlock labelToBlock(final int label) {
        final int idx = this.indexOfLabel(label);
        if (idx < 0) {
            throw new IllegalArgumentException("no such label: " + Hex.u2(label));
        }
        return this.get(idx);
    }
    
    public void forEachInsn(final Insn.Visitor visitor) {
        for (int sz = this.size(), i = 0; i < sz; ++i) {
            final BasicBlock one = this.get(i);
            final InsnList insns = one.getInsns();
            insns.forEach(visitor);
        }
    }
    
    public BasicBlockList withRegisterOffset(final int delta) {
        final int sz = this.size();
        final BasicBlockList result = new BasicBlockList(sz);
        for (int i = 0; i < sz; ++i) {
            final BasicBlock one = (BasicBlock)this.get0(i);
            if (one != null) {
                result.set(i, one.withRegisterOffset(delta));
            }
        }
        if (this.isImmutable()) {
            result.setImmutable();
        }
        return result;
    }
    
    public BasicBlockList getMutableCopy() {
        return new BasicBlockList(this);
    }
    
    public BasicBlock preferredSuccessorOf(final BasicBlock block) {
        final int primarySuccessor = block.getPrimarySuccessor();
        final IntList successors = block.getSuccessors();
        final int succSize = successors.size();
        switch (succSize) {
            case 0: {
                return null;
            }
            case 1: {
                return this.labelToBlock(successors.get(0));
            }
            default: {
                if (primarySuccessor != -1) {
                    return this.labelToBlock(primarySuccessor);
                }
                return this.labelToBlock(successors.get(0));
            }
        }
    }
    
    public boolean catchesEqual(final BasicBlock block1, final BasicBlock block2) {
        final TypeList catches1 = block1.getExceptionHandlerTypes();
        final TypeList catches2 = block2.getExceptionHandlerTypes();
        if (!StdTypeList.equalContents(catches1, catches2)) {
            return false;
        }
        final IntList succ1 = block1.getSuccessors();
        final IntList succ2 = block2.getSuccessors();
        final int size = succ1.size();
        final int primary1 = block1.getPrimarySuccessor();
        final int primary2 = block2.getPrimarySuccessor();
        if ((primary1 == -1 || primary2 == -1) && primary1 != primary2) {
            return false;
        }
        for (int i = 0; i < size; ++i) {
            final int label1 = succ1.get(i);
            final int label2 = succ2.get(i);
            if (label1 == primary1) {
                if (label2 != primary2) {
                    return false;
                }
            }
            else if (label1 != label2) {
                return false;
            }
        }
        return true;
    }
    
    private static class RegCountVisitor implements Insn.Visitor
    {
        private int regCount;
        
        public RegCountVisitor() {
            this.regCount = 0;
        }
        
        public int getRegCount() {
            return this.regCount;
        }
        
        @Override
        public void visitPlainInsn(final PlainInsn insn) {
            this.visit(insn);
        }
        
        @Override
        public void visitPlainCstInsn(final PlainCstInsn insn) {
            this.visit(insn);
        }
        
        @Override
        public void visitSwitchInsn(final SwitchInsn insn) {
            this.visit(insn);
        }
        
        @Override
        public void visitThrowingCstInsn(final ThrowingCstInsn insn) {
            this.visit(insn);
        }
        
        @Override
        public void visitThrowingInsn(final ThrowingInsn insn) {
            this.visit(insn);
        }
        
        @Override
        public void visitFillArrayDataInsn(final FillArrayDataInsn insn) {
            this.visit(insn);
        }
        
        @Override
        public void visitInvokePolymorphicInsn(final InvokePolymorphicInsn insn) {
            this.visit(insn);
        }
        
        private void visit(final Insn insn) {
            final RegisterSpec result = insn.getResult();
            if (result != null) {
                this.processReg(result);
            }
            final RegisterSpecList sources = insn.getSources();
            for (int sz = sources.size(), i = 0; i < sz; ++i) {
                this.processReg(sources.get(i));
            }
        }
        
        private void processReg(final RegisterSpec spec) {
            final int reg = spec.getNextReg();
            if (reg > this.regCount) {
                this.regCount = reg;
            }
        }
    }
}

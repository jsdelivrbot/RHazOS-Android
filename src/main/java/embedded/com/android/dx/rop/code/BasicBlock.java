package embedded.com.android.dx.rop.code;

import embedded.com.android.dx.rop.type.*;
import embedded.com.android.dx.util.*;

public final class BasicBlock implements LabeledItem
{
    private final int label;
    private final InsnList insns;
    private final IntList successors;
    private final int primarySuccessor;
    
    public BasicBlock(final int label, final InsnList insns, final IntList successors, final int primarySuccessor) {
        if (label < 0) {
            throw new IllegalArgumentException("label < 0");
        }
        try {
            insns.throwIfMutable();
        }
        catch (NullPointerException ex) {
            throw new NullPointerException("insns == null");
        }
        final int sz = insns.size();
        if (sz == 0) {
            throw new IllegalArgumentException("insns.size() == 0");
        }
        for (int i = sz - 2; i >= 0; --i) {
            final Rop one = insns.get(i).getOpcode();
            if (one.getBranchingness() != 1) {
                throw new IllegalArgumentException("insns[" + i + "] is a " + "branch or can throw");
            }
        }
        final Insn lastInsn = insns.get(sz - 1);
        if (lastInsn.getOpcode().getBranchingness() == 1) {
            throw new IllegalArgumentException("insns does not end with a branch or throwing instruction");
        }
        try {
            successors.throwIfMutable();
        }
        catch (NullPointerException ex2) {
            throw new NullPointerException("successors == null");
        }
        if (primarySuccessor < -1) {
            throw new IllegalArgumentException("primarySuccessor < -1");
        }
        if (primarySuccessor >= 0 && !successors.contains(primarySuccessor)) {
            throw new IllegalArgumentException("primarySuccessor " + primarySuccessor + " not in successors " + successors);
        }
        this.label = label;
        this.insns = insns;
        this.successors = successors;
        this.primarySuccessor = primarySuccessor;
    }
    
    @Override
    public boolean equals(final Object other) {
        return this == other;
    }
    
    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }
    
    @Override
    public int getLabel() {
        return this.label;
    }
    
    public InsnList getInsns() {
        return this.insns;
    }
    
    public IntList getSuccessors() {
        return this.successors;
    }
    
    public int getPrimarySuccessor() {
        return this.primarySuccessor;
    }
    
    public int getSecondarySuccessor() {
        if (this.successors.size() != 2) {
            throw new UnsupportedOperationException("block doesn't have exactly two successors");
        }
        int succ = this.successors.get(0);
        if (succ == this.primarySuccessor) {
            succ = this.successors.get(1);
        }
        return succ;
    }
    
    public Insn getFirstInsn() {
        return this.insns.get(0);
    }
    
    public Insn getLastInsn() {
        return this.insns.getLast();
    }
    
    public boolean canThrow() {
        return this.insns.getLast().canThrow();
    }
    
    public boolean hasExceptionHandlers() {
        final Insn lastInsn = this.insns.getLast();
        return lastInsn.getCatches().size() != 0;
    }
    
    public TypeList getExceptionHandlerTypes() {
        final Insn lastInsn = this.insns.getLast();
        return lastInsn.getCatches();
    }
    
    public BasicBlock withRegisterOffset(final int delta) {
        return new BasicBlock(this.label, this.insns.withRegisterOffset(delta), this.successors, this.primarySuccessor);
    }
    
    @Override
    public String toString() {
        return '{' + Hex.u2(this.label) + '}';
    }
    
    public interface Visitor
    {
        void visitBlock(final BasicBlock p0);
    }
}

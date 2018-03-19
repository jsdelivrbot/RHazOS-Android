package embedded.com.android.dx.rop.code;

import embedded.com.android.dx.util.*;
import java.util.*;

public final class LocalVariableInfo extends MutabilityControl
{
    private final int regCount;
    private final RegisterSpecSet emptySet;
    private final RegisterSpecSet[] blockStarts;
    private final HashMap<Insn, RegisterSpec> insnAssignments;
    
    public LocalVariableInfo(final RopMethod method) {
        if (method == null) {
            throw new NullPointerException("method == null");
        }
        final BasicBlockList blocks = method.getBlocks();
        final int maxLabel = blocks.getMaxLabel();
        this.regCount = blocks.getRegCount();
        this.emptySet = new RegisterSpecSet(this.regCount);
        this.blockStarts = new RegisterSpecSet[maxLabel];
        this.insnAssignments = new HashMap<Insn, RegisterSpec>(blocks.getInstructionCount());
        this.emptySet.setImmutable();
    }
    
    public void setStarts(final int label, final RegisterSpecSet specs) {
        this.throwIfImmutable();
        if (specs == null) {
            throw new NullPointerException("specs == null");
        }
        try {
            this.blockStarts[label] = specs;
        }
        catch (ArrayIndexOutOfBoundsException ex) {
            throw new IllegalArgumentException("bogus label");
        }
    }
    
    public boolean mergeStarts(final int label, final RegisterSpecSet specs) {
        final RegisterSpecSet start = this.getStarts0(label);
        final boolean changed = false;
        if (start == null) {
            this.setStarts(label, specs);
            return true;
        }
        RegisterSpecSet newStart = start.mutableCopy();
        if (start.size() != 0) {
            newStart.intersect(specs, true);
        }
        else {
            newStart = specs.mutableCopy();
        }
        if (start.equals(newStart)) {
            return false;
        }
        newStart.setImmutable();
        this.setStarts(label, newStart);
        return true;
    }
    
    public RegisterSpecSet getStarts(final int label) {
        final RegisterSpecSet result = this.getStarts0(label);
        return (result != null) ? result : this.emptySet;
    }
    
    public RegisterSpecSet getStarts(final BasicBlock block) {
        return this.getStarts(block.getLabel());
    }
    
    public RegisterSpecSet mutableCopyOfStarts(final int label) {
        final RegisterSpecSet result = this.getStarts0(label);
        return (result != null) ? result.mutableCopy() : new RegisterSpecSet(this.regCount);
    }
    
    public void addAssignment(final Insn insn, final RegisterSpec spec) {
        this.throwIfImmutable();
        if (insn == null) {
            throw new NullPointerException("insn == null");
        }
        if (spec == null) {
            throw new NullPointerException("spec == null");
        }
        this.insnAssignments.put(insn, spec);
    }
    
    public RegisterSpec getAssignment(final Insn insn) {
        return this.insnAssignments.get(insn);
    }
    
    public int getAssignmentCount() {
        return this.insnAssignments.size();
    }
    
    public void debugDump() {
        for (int label = 0; label < this.blockStarts.length; ++label) {
            if (this.blockStarts[label] != null) {
                if (this.blockStarts[label] == this.emptySet) {
                    System.out.printf("%04x: empty set\n", label);
                }
                else {
                    System.out.printf("%04x: %s\n", label, this.blockStarts[label]);
                }
            }
        }
    }
    
    private RegisterSpecSet getStarts0(final int label) {
        try {
            return this.blockStarts[label];
        }
        catch (ArrayIndexOutOfBoundsException ex) {
            throw new IllegalArgumentException("bogus label");
        }
    }
}

package embedded.com.android.dx.ssa;

import embedded.com.android.dx.util.*;
import embedded.com.android.dx.rop.code.*;
import java.util.*;

public class LocalVariableInfo extends MutabilityControl
{
    private final int regCount;
    private final RegisterSpecSet emptySet;
    private final RegisterSpecSet[] blockStarts;
    private final HashMap<SsaInsn, RegisterSpec> insnAssignments;
    
    public LocalVariableInfo(final SsaMethod method) {
        if (method == null) {
            throw new NullPointerException("method == null");
        }
        final List<SsaBasicBlock> blocks = method.getBlocks();
        this.regCount = method.getRegCount();
        this.emptySet = new RegisterSpecSet(this.regCount);
        this.blockStarts = new RegisterSpecSet[blocks.size()];
        this.insnAssignments = new HashMap<SsaInsn, RegisterSpec>();
        this.emptySet.setImmutable();
    }
    
    public void setStarts(final int index, final RegisterSpecSet specs) {
        this.throwIfImmutable();
        if (specs == null) {
            throw new NullPointerException("specs == null");
        }
        try {
            this.blockStarts[index] = specs;
        }
        catch (ArrayIndexOutOfBoundsException ex) {
            throw new IllegalArgumentException("bogus index");
        }
    }
    
    public boolean mergeStarts(final int index, final RegisterSpecSet specs) {
        final RegisterSpecSet start = this.getStarts0(index);
        final boolean changed = false;
        if (start == null) {
            this.setStarts(index, specs);
            return true;
        }
        final RegisterSpecSet newStart = start.mutableCopy();
        newStart.intersect(specs, true);
        if (start.equals(newStart)) {
            return false;
        }
        newStart.setImmutable();
        this.setStarts(index, newStart);
        return true;
    }
    
    public RegisterSpecSet getStarts(final int index) {
        final RegisterSpecSet result = this.getStarts0(index);
        return (result != null) ? result : this.emptySet;
    }
    
    public RegisterSpecSet getStarts(final SsaBasicBlock block) {
        return this.getStarts(block.getIndex());
    }
    
    public RegisterSpecSet mutableCopyOfStarts(final int index) {
        final RegisterSpecSet result = this.getStarts0(index);
        return (result != null) ? result.mutableCopy() : new RegisterSpecSet(this.regCount);
    }
    
    public void addAssignment(final SsaInsn insn, final RegisterSpec spec) {
        this.throwIfImmutable();
        if (insn == null) {
            throw new NullPointerException("insn == null");
        }
        if (spec == null) {
            throw new NullPointerException("spec == null");
        }
        this.insnAssignments.put(insn, spec);
    }
    
    public RegisterSpec getAssignment(final SsaInsn insn) {
        return this.insnAssignments.get(insn);
    }
    
    public int getAssignmentCount() {
        return this.insnAssignments.size();
    }
    
    public void debugDump() {
        for (int index = 0; index < this.blockStarts.length; ++index) {
            if (this.blockStarts[index] != null) {
                if (this.blockStarts[index] == this.emptySet) {
                    System.out.printf("%04x: empty set\n", index);
                }
                else {
                    System.out.printf("%04x: %s\n", index, this.blockStarts[index]);
                }
            }
        }
    }
    
    private RegisterSpecSet getStarts0(final int index) {
        try {
            return this.blockStarts[index];
        }
        catch (ArrayIndexOutOfBoundsException ex) {
            throw new IllegalArgumentException("bogus index");
        }
    }
}

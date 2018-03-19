package embedded.com.android.dx.ssa;

import embedded.com.android.dx.util.*;
import embedded.com.android.dx.rop.type.*;
import embedded.com.android.dx.rop.code.*;

public abstract class SsaInsn implements ToHuman, Cloneable
{
    private final SsaBasicBlock block;
    private RegisterSpec result;
    
    protected SsaInsn(final RegisterSpec result, final SsaBasicBlock block) {
        if (block == null) {
            throw new NullPointerException("block == null");
        }
        this.block = block;
        this.result = result;
    }
    
    public static SsaInsn makeFromRop(final Insn insn, final SsaBasicBlock block) {
        return new NormalSsaInsn(insn, block);
    }
    
    public SsaInsn clone() {
        try {
            return (SsaInsn)super.clone();
        }
        catch (CloneNotSupportedException ex) {
            throw new RuntimeException("unexpected", ex);
        }
    }
    
    public RegisterSpec getResult() {
        return this.result;
    }
    
    protected void setResult(final RegisterSpec result) {
        if (result == null) {
            throw new NullPointerException("result == null");
        }
        this.result = result;
    }
    
    public abstract RegisterSpecList getSources();
    
    public SsaBasicBlock getBlock() {
        return this.block;
    }
    
    public boolean isResultReg(final int reg) {
        return this.result != null && this.result.getReg() == reg;
    }
    
    public void changeResultReg(final int reg) {
        if (this.result != null) {
            this.result = this.result.withReg(reg);
        }
    }
    
    public final void setResultLocal(final LocalItem local) {
        final LocalItem oldItem = this.result.getLocalItem();
        if (local != oldItem && (local == null || !local.equals(this.result.getLocalItem()))) {
            this.result = RegisterSpec.makeLocalOptional(this.result.getReg(), this.result.getType(), local);
        }
    }
    
    public final void mapRegisters(final RegisterMapper mapper) {
        final RegisterSpec oldResult = this.result;
        this.result = mapper.map(this.result);
        this.block.getParent().updateOneDefinition(this, oldResult);
        this.mapSourceRegisters(mapper);
    }
    
    public abstract void mapSourceRegisters(final RegisterMapper p0);
    
    public abstract Rop getOpcode();
    
    public abstract Insn getOriginalRopInsn();
    
    public RegisterSpec getLocalAssignment() {
        if (this.result != null && this.result.getLocalItem() != null) {
            return this.result;
        }
        return null;
    }
    
    public boolean isRegASource(final int reg) {
        return null != this.getSources().specForRegister(reg);
    }
    
    public abstract Insn toRopInsn();
    
    public abstract boolean isPhiOrMove();
    
    public abstract boolean hasSideEffect();
    
    public boolean isNormalMoveInsn() {
        return false;
    }
    
    public boolean isMoveException() {
        return false;
    }
    
    public abstract boolean canThrow();
    
    public abstract void accept(final Visitor p0);
    
    public interface Visitor
    {
        void visitMoveInsn(final NormalSsaInsn p0);
        
        void visitPhiInsn(final PhiInsn p0);
        
        void visitNonMoveInsn(final NormalSsaInsn p0);
    }
}

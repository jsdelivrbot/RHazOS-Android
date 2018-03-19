package embedded.com.android.dx.ssa;

import embedded.com.android.dx.rop.code.*;

public final class NormalSsaInsn extends SsaInsn implements Cloneable
{
    private Insn insn;
    
    NormalSsaInsn(final Insn insn, final SsaBasicBlock block) {
        super(insn.getResult(), block);
        this.insn = insn;
    }
    
    @Override
    public final void mapSourceRegisters(final RegisterMapper mapper) {
        final RegisterSpecList oldSources = this.insn.getSources();
        final RegisterSpecList newSources = mapper.map(oldSources);
        if (newSources != oldSources) {
            this.insn = this.insn.withNewRegisters(this.getResult(), newSources);
            this.getBlock().getParent().onSourcesChanged(this, oldSources);
        }
    }
    
    public final void changeOneSource(final int index, final RegisterSpec newSpec) {
        final RegisterSpecList origSources = this.insn.getSources();
        final int sz = origSources.size();
        final RegisterSpecList newSources = new RegisterSpecList(sz);
        for (int i = 0; i < sz; ++i) {
            newSources.set(i, (i == index) ? newSpec : origSources.get(i));
        }
        newSources.setImmutable();
        final RegisterSpec origSpec = origSources.get(index);
        if (origSpec.getReg() != newSpec.getReg()) {
            this.getBlock().getParent().onSourceChanged(this, origSpec, newSpec);
        }
        this.insn = this.insn.withNewRegisters(this.getResult(), newSources);
    }
    
    public final void setNewSources(final RegisterSpecList newSources) {
        final RegisterSpecList origSources = this.insn.getSources();
        if (origSources.size() != newSources.size()) {
            throw new RuntimeException("Sources counts don't match");
        }
        this.insn = this.insn.withNewRegisters(this.getResult(), newSources);
    }
    
    @Override
    public NormalSsaInsn clone() {
        return (NormalSsaInsn)super.clone();
    }
    
    @Override
    public RegisterSpecList getSources() {
        return this.insn.getSources();
    }
    
    @Override
    public String toHuman() {
        return this.toRopInsn().toHuman();
    }
    
    @Override
    public Insn toRopInsn() {
        return this.insn.withNewRegisters(this.getResult(), this.insn.getSources());
    }
    
    @Override
    public Rop getOpcode() {
        return this.insn.getOpcode();
    }
    
    @Override
    public Insn getOriginalRopInsn() {
        return this.insn;
    }
    
    @Override
    public RegisterSpec getLocalAssignment() {
        RegisterSpec assignment;
        if (this.insn.getOpcode().getOpcode() == 54) {
            assignment = this.insn.getSources().get(0);
        }
        else {
            assignment = this.getResult();
        }
        if (assignment == null) {
            return null;
        }
        final LocalItem local = assignment.getLocalItem();
        if (local == null) {
            return null;
        }
        return assignment;
    }
    
    public void upgradeToLiteral() {
        final RegisterSpecList oldSources = this.insn.getSources();
        this.insn = this.insn.withSourceLiteral();
        this.getBlock().getParent().onSourcesChanged(this, oldSources);
    }
    
    @Override
    public boolean isNormalMoveInsn() {
        return this.insn.getOpcode().getOpcode() == 2;
    }
    
    @Override
    public boolean isMoveException() {
        return this.insn.getOpcode().getOpcode() == 4;
    }
    
    @Override
    public boolean canThrow() {
        return this.insn.canThrow();
    }
    
    @Override
    public void accept(final Visitor v) {
        if (this.isNormalMoveInsn()) {
            v.visitMoveInsn(this);
        }
        else {
            v.visitNonMoveInsn(this);
        }
    }
    
    @Override
    public boolean isPhiOrMove() {
        return this.isNormalMoveInsn();
    }
    
    @Override
    public boolean hasSideEffect() {
        final Rop opcode = this.getOpcode();
        if (opcode.getBranchingness() != 1) {
            return true;
        }
        final boolean hasLocalSideEffect = Optimizer.getPreserveLocals() && this.getLocalAssignment() != null;
        switch (opcode.getOpcode()) {
            case 2:
            case 5:
            case 55: {
                return hasLocalSideEffect;
            }
            default: {
                return true;
            }
        }
    }
}

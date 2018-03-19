package embedded.com.android.dx.ssa;

import embedded.com.android.dx.rop.type.*;
import java.util.*;
import embedded.com.android.dx.rop.code.*;
import embedded.com.android.dx.util.*;

public final class PhiInsn extends SsaInsn
{
    private final int ropResultReg;
    private final ArrayList<Operand> operands;
    private RegisterSpecList sources;
    
    public PhiInsn(final RegisterSpec resultReg, final SsaBasicBlock block) {
        super(resultReg, block);
        this.operands = new ArrayList<Operand>();
        this.ropResultReg = resultReg.getReg();
    }
    
    public PhiInsn(final int resultReg, final SsaBasicBlock block) {
        super(RegisterSpec.make(resultReg, Type.VOID), block);
        this.operands = new ArrayList<Operand>();
        this.ropResultReg = resultReg;
    }
    
    @Override
    public PhiInsn clone() {
        throw new UnsupportedOperationException("can't clone phi");
    }
    
    public void updateSourcesToDefinitions(final SsaMethod ssaMeth) {
        for (final Operand o : this.operands) {
            final RegisterSpec def = ssaMeth.getDefinitionForRegister(o.regSpec.getReg()).getResult();
            o.regSpec = o.regSpec.withType(def.getType());
        }
        this.sources = null;
    }
    
    public void changeResultType(final TypeBearer type, final LocalItem local) {
        this.setResult(RegisterSpec.makeLocalOptional(this.getResult().getReg(), type, local));
    }
    
    public int getRopResultReg() {
        return this.ropResultReg;
    }
    
    public void addPhiOperand(final RegisterSpec registerSpec, final SsaBasicBlock predBlock) {
        this.operands.add(new Operand(registerSpec, predBlock.getIndex(), predBlock.getRopLabel()));
        this.sources = null;
    }
    
    public void removePhiRegister(final RegisterSpec registerSpec) {
        final ArrayList<Operand> operandsToRemove = new ArrayList<Operand>();
        for (final Operand o : this.operands) {
            if (o.regSpec.getReg() == registerSpec.getReg()) {
                operandsToRemove.add(o);
            }
        }
        this.operands.removeAll(operandsToRemove);
        this.sources = null;
    }
    
    public int predBlockIndexForSourcesIndex(final int sourcesIndex) {
        return this.operands.get(sourcesIndex).blockIndex;
    }
    
    @Override
    public Rop getOpcode() {
        return null;
    }
    
    @Override
    public Insn getOriginalRopInsn() {
        return null;
    }
    
    @Override
    public boolean canThrow() {
        return false;
    }
    
    @Override
    public RegisterSpecList getSources() {
        if (this.sources != null) {
            return this.sources;
        }
        if (this.operands.size() == 0) {
            return RegisterSpecList.EMPTY;
        }
        final int szSources = this.operands.size();
        this.sources = new RegisterSpecList(szSources);
        for (int i = 0; i < szSources; ++i) {
            final Operand o = this.operands.get(i);
            this.sources.set(i, o.regSpec);
        }
        this.sources.setImmutable();
        return this.sources;
    }
    
    @Override
    public boolean isRegASource(final int reg) {
        for (final Operand o : this.operands) {
            if (o.regSpec.getReg() == reg) {
                return true;
            }
        }
        return false;
    }
    
    public boolean areAllOperandsEqual() {
        if (this.operands.size() == 0) {
            return true;
        }
        final int firstReg = this.operands.get(0).regSpec.getReg();
        for (final Operand o : this.operands) {
            if (firstReg != o.regSpec.getReg()) {
                return false;
            }
        }
        return true;
    }
    
    @Override
    public final void mapSourceRegisters(final RegisterMapper mapper) {
        for (final Operand o : this.operands) {
            final RegisterSpec old = o.regSpec;
            o.regSpec = mapper.map(old);
            if (old != o.regSpec) {
                this.getBlock().getParent().onSourceChanged(this, old, o.regSpec);
            }
        }
        this.sources = null;
    }
    
    @Override
    public Insn toRopInsn() {
        throw new IllegalArgumentException("Cannot convert phi insns to rop form");
    }
    
    public List<SsaBasicBlock> predBlocksForReg(final int reg, final SsaMethod ssaMeth) {
        final ArrayList<SsaBasicBlock> ret = new ArrayList<SsaBasicBlock>();
        for (final Operand o : this.operands) {
            if (o.regSpec.getReg() == reg) {
                ret.add(ssaMeth.getBlocks().get(o.blockIndex));
            }
        }
        return ret;
    }
    
    @Override
    public boolean isPhiOrMove() {
        return true;
    }
    
    @Override
    public boolean hasSideEffect() {
        return Optimizer.getPreserveLocals() && this.getLocalAssignment() != null;
    }
    
    @Override
    public void accept(final SsaInsn.Visitor v) {
        v.visitPhiInsn(this);
    }
    
    @Override
    public String toHuman() {
        return this.toHumanWithInline(null);
    }
    
    protected final String toHumanWithInline(final String extra) {
        final StringBuffer sb = new StringBuffer(80);
        sb.append(SourcePosition.NO_INFO);
        sb.append(": phi");
        if (extra != null) {
            sb.append("(");
            sb.append(extra);
            sb.append(")");
        }
        final RegisterSpec result = this.getResult();
        if (result == null) {
            sb.append(" .");
        }
        else {
            sb.append(" ");
            sb.append(result.toHuman());
        }
        sb.append(" <-");
        final int sz = this.getSources().size();
        if (sz == 0) {
            sb.append(" .");
        }
        else {
            for (int i = 0; i < sz; ++i) {
                sb.append(" ");
                sb.append(this.sources.get(i).toHuman() + "[b=" + Hex.u2(this.operands.get(i).ropLabel) + "]");
            }
        }
        return sb.toString();
    }
    
    private static class Operand
    {
        public RegisterSpec regSpec;
        public final int blockIndex;
        public final int ropLabel;
        
        public Operand(final RegisterSpec regSpec, final int blockIndex, final int ropLabel) {
            this.regSpec = regSpec;
            this.blockIndex = blockIndex;
            this.ropLabel = ropLabel;
        }
    }
    
    public interface Visitor
    {
        void visitPhiInsn(final PhiInsn p0);
    }
}

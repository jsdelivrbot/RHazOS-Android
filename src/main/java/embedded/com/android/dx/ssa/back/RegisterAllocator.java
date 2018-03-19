package embedded.com.android.dx.ssa.back;

import embedded.com.android.dx.rop.type.*;
import embedded.com.android.dx.rop.code.*;
import embedded.com.android.dx.ssa.*;
import java.util.*;
import embedded.com.android.dx.util.*;

public abstract class RegisterAllocator
{
    protected final SsaMethod ssaMeth;
    protected final InterferenceGraph interference;
    
    public RegisterAllocator(final SsaMethod ssaMeth, final InterferenceGraph interference) {
        this.ssaMeth = ssaMeth;
        this.interference = interference;
    }
    
    public abstract boolean wantsParamsMovedHigh();
    
    public abstract RegisterMapper allocateRegisters();
    
    protected final int getCategoryForSsaReg(final int reg) {
        final SsaInsn definition = this.ssaMeth.getDefinitionForRegister(reg);
        if (definition == null) {
            return 1;
        }
        return definition.getResult().getCategory();
    }
    
    protected final RegisterSpec getDefinitionSpecForSsaReg(final int reg) {
        final SsaInsn definition = this.ssaMeth.getDefinitionForRegister(reg);
        return (definition == null) ? null : definition.getResult();
    }
    
    protected boolean isDefinitionMoveParam(final int reg) {
        final SsaInsn defInsn = this.ssaMeth.getDefinitionForRegister(reg);
        if (defInsn instanceof NormalSsaInsn) {
            final NormalSsaInsn ndefInsn = (NormalSsaInsn)defInsn;
            return ndefInsn.getOpcode().getOpcode() == 3;
        }
        return false;
    }
    
    protected final RegisterSpec insertMoveBefore(final SsaInsn insn, final RegisterSpec reg) {
        final SsaBasicBlock block = insn.getBlock();
        final ArrayList<SsaInsn> insns = block.getInsns();
        final int insnIndex = insns.indexOf(insn);
        if (insnIndex < 0) {
            throw new IllegalArgumentException("specified insn is not in this block");
        }
        if (insnIndex != insns.size() - 1) {
            throw new IllegalArgumentException("Adding move here not supported:" + insn.toHuman());
        }
        final RegisterSpec newRegSpec = RegisterSpec.make(this.ssaMeth.makeNewSsaReg(), reg.getTypeBearer());
        final SsaInsn toAdd = SsaInsn.makeFromRop(new PlainInsn(Rops.opMove(newRegSpec.getType()), SourcePosition.NO_INFO, newRegSpec, RegisterSpecList.make(reg)), block);
        insns.add(insnIndex, toAdd);
        final int newReg = newRegSpec.getReg();
        final IntSet liveOut = block.getLiveOutRegs();
        final IntIterator liveOutIter = liveOut.iterator();
        while (liveOutIter.hasNext()) {
            this.interference.add(newReg, liveOutIter.next());
        }
        final RegisterSpecList sources = insn.getSources();
        for (int szSources = sources.size(), i = 0; i < szSources; ++i) {
            this.interference.add(newReg, sources.get(i).getReg());
        }
        this.ssaMeth.onInsnsChanged();
        return newRegSpec;
    }
}

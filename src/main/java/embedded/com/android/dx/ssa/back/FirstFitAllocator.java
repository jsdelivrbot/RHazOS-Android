package embedded.com.android.dx.ssa.back;

import java.util.*;
import embedded.com.android.dx.ssa.*;
import embedded.com.android.dx.util.*;
import embedded.com.android.dx.rop.code.*;
import embedded.com.android.dx.rop.cst.*;

public class FirstFitAllocator extends RegisterAllocator
{
    private static final boolean PRESLOT_PARAMS = true;
    private final BitSet mapped;
    
    public FirstFitAllocator(final SsaMethod ssaMeth, final InterferenceGraph interference) {
        super(ssaMeth, interference);
        this.mapped = new BitSet(ssaMeth.getRegCount());
    }
    
    @Override
    public boolean wantsParamsMovedHigh() {
        return true;
    }
    
    @Override
    public RegisterMapper allocateRegisters() {
        final int oldRegCount = this.ssaMeth.getRegCount();
        final BasicRegisterMapper mapper = new BasicRegisterMapper(oldRegCount);
        int nextNewRegister = 0;
        nextNewRegister = this.ssaMeth.getParamWidth();
        for (int i = 0; i < oldRegCount; ++i) {
            if (!this.mapped.get(i)) {
                int maxCategory = this.getCategoryForSsaReg(i);
                final IntSet current = new BitIntSet(oldRegCount);
                this.interference.mergeInterferenceSet(i, current);
                boolean isPreslotted = false;
                int newReg = 0;
                if (this.isDefinitionMoveParam(i)) {
                    final NormalSsaInsn defInsn = (NormalSsaInsn)this.ssaMeth.getDefinitionForRegister(i);
                    newReg = this.paramNumberFromMoveParam(defInsn);
                    mapper.addMapping(i, newReg, maxCategory);
                    isPreslotted = true;
                }
                else {
                    mapper.addMapping(i, nextNewRegister, maxCategory);
                    newReg = nextNewRegister;
                }
                for (int j = i + 1; j < oldRegCount; ++j) {
                    if (!this.mapped.get(j)) {
                        if (!this.isDefinitionMoveParam(j)) {
                            if (!current.has(j) && (!isPreslotted || maxCategory >= this.getCategoryForSsaReg(j))) {
                                this.interference.mergeInterferenceSet(j, current);
                                maxCategory = Math.max(maxCategory, this.getCategoryForSsaReg(j));
                                mapper.addMapping(j, newReg, maxCategory);
                                this.mapped.set(j);
                            }
                        }
                    }
                }
                this.mapped.set(i);
                if (!isPreslotted) {
                    nextNewRegister += maxCategory;
                }
            }
        }
        return mapper;
    }
    
    private int paramNumberFromMoveParam(final NormalSsaInsn ndefInsn) {
        final CstInsn origInsn = (CstInsn)ndefInsn.getOriginalRopInsn();
        return ((CstInteger)origInsn.getConstant()).getValue();
    }
}

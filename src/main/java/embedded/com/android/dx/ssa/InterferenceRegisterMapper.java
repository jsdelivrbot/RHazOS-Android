package embedded.com.android.dx.ssa;

import java.util.*;
import embedded.com.android.dx.ssa.back.*;
import embedded.com.android.dx.util.*;
import embedded.com.android.dx.rop.code.*;

public class InterferenceRegisterMapper extends BasicRegisterMapper
{
    private final ArrayList<BitIntSet> newRegInterference;
    private final InterferenceGraph oldRegInterference;
    
    public InterferenceRegisterMapper(final InterferenceGraph oldRegInterference, final int countOldRegisters) {
        super(countOldRegisters);
        this.newRegInterference = new ArrayList<BitIntSet>();
        this.oldRegInterference = oldRegInterference;
    }
    
    @Override
    public void addMapping(final int oldReg, final int newReg, final int category) {
        super.addMapping(oldReg, newReg, category);
        this.addInterfence(newReg, oldReg);
        if (category == 2) {
            this.addInterfence(newReg + 1, oldReg);
        }
    }
    
    public boolean interferes(final int oldReg, final int newReg, final int category) {
        if (newReg >= this.newRegInterference.size()) {
            return false;
        }
        final IntSet existing = this.newRegInterference.get(newReg);
        if (existing == null) {
            return false;
        }
        if (category == 1) {
            return existing.has(oldReg);
        }
        return existing.has(oldReg) || this.interferes(oldReg, newReg + 1, category - 1);
    }
    
    public boolean interferes(final RegisterSpec oldSpec, final int newReg) {
        return this.interferes(oldSpec.getReg(), newReg, oldSpec.getCategory());
    }
    
    private void addInterfence(final int newReg, final int oldReg) {
        this.newRegInterference.ensureCapacity(newReg + 1);
        while (newReg >= this.newRegInterference.size()) {
            this.newRegInterference.add(new BitIntSet(newReg + 1));
        }
        this.oldRegInterference.mergeInterferenceSet(oldReg, this.newRegInterference.get(newReg));
    }
    
    public boolean areAnyPinned(final RegisterSpecList oldSpecs, final int newReg, final int targetCategory) {
        for (int sz = oldSpecs.size(), i = 0; i < sz; ++i) {
            final RegisterSpec oldSpec = oldSpecs.get(i);
            final int r = this.oldToNew(oldSpec.getReg());
            if (r == newReg || (oldSpec.getCategory() == 2 && r + 1 == newReg) || (targetCategory == 2 && r == newReg + 1)) {
                return true;
            }
        }
        return false;
    }
}

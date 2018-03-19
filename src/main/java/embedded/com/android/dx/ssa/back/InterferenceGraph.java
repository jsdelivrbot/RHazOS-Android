package embedded.com.android.dx.ssa.back;

import java.util.*;
import embedded.com.android.dx.util.*;
import embedded.com.android.dx.ssa.*;

public class InterferenceGraph
{
    private final ArrayList<IntSet> interference;
    
    public InterferenceGraph(final int countRegs) {
        this.interference = new ArrayList<IntSet>(countRegs);
        for (int i = 0; i < countRegs; ++i) {
            this.interference.add(SetFactory.makeInterferenceSet(countRegs));
        }
    }
    
    public void add(final int regV, final int regW) {
        this.ensureCapacity(Math.max(regV, regW) + 1);
        this.interference.get(regV).add(regW);
        this.interference.get(regW).add(regV);
    }
    
    public void dumpToStdout() {
        for (int oldRegCount = this.interference.size(), i = 0; i < oldRegCount; ++i) {
            final StringBuilder sb = new StringBuilder();
            sb.append("Reg " + i + ":" + this.interference.get(i).toString());
            System.out.println(sb.toString());
        }
    }
    
    public void mergeInterferenceSet(final int reg, final IntSet set) {
        if (reg < this.interference.size()) {
            set.merge(this.interference.get(reg));
        }
    }
    
    private void ensureCapacity(final int size) {
        final int countRegs = this.interference.size();
        this.interference.ensureCapacity(size);
        for (int i = countRegs; i < size; ++i) {
            this.interference.add(SetFactory.makeInterferenceSet(size));
        }
    }
}

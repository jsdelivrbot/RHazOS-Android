package embedded.com.android.dx.dex.cf;

import embedded.com.android.dx.dex.code.*;
import embedded.com.android.dx.rop.code.*;
import java.io.*;

public final class CodeStatistics
{
    private static final boolean DEBUG = false;
    public int runningDeltaRegisters;
    public int runningDeltaInsns;
    public int runningTotalInsns;
    public int dexRunningDeltaRegisters;
    public int dexRunningDeltaInsns;
    public int dexRunningTotalInsns;
    public int runningOriginalBytes;
    
    public CodeStatistics() {
        this.runningDeltaRegisters = 0;
        this.runningDeltaInsns = 0;
        this.runningTotalInsns = 0;
        this.dexRunningDeltaRegisters = 0;
        this.dexRunningDeltaInsns = 0;
        this.dexRunningTotalInsns = 0;
        this.runningOriginalBytes = 0;
    }
    
    public void updateOriginalByteCount(final int count) {
        this.runningOriginalBytes += count;
    }
    
    public void updateDexStatistics(final DalvCode nonOptCode, final DalvCode code) {
        this.dexRunningDeltaInsns += code.getInsns().codeSize() - nonOptCode.getInsns().codeSize();
        this.dexRunningDeltaRegisters += code.getInsns().getRegistersSize() - nonOptCode.getInsns().getRegistersSize();
        this.dexRunningTotalInsns += code.getInsns().codeSize();
    }
    
    public void updateRopStatistics(final RopMethod nonOptRmeth, final RopMethod rmeth) {
        final int oldCountInsns = nonOptRmeth.getBlocks().getEffectiveInstructionCount();
        final int oldCountRegs = nonOptRmeth.getBlocks().getRegCount();
        final int newCountInsns = rmeth.getBlocks().getEffectiveInstructionCount();
        this.runningDeltaInsns += newCountInsns - oldCountInsns;
        this.runningDeltaRegisters += rmeth.getBlocks().getRegCount() - oldCountRegs;
        this.runningTotalInsns += newCountInsns;
    }
    
    public void dumpStatistics(final PrintStream out) {
        out.printf("Optimizer Delta Rop Insns: %d total: %d (%.2f%%) Delta Registers: %d\n", this.runningDeltaInsns, this.runningTotalInsns, 100.0 * (this.runningDeltaInsns / (this.runningTotalInsns + Math.abs(this.runningDeltaInsns))), this.runningDeltaRegisters);
        out.printf("Optimizer Delta Dex Insns: Insns: %d total: %d (%.2f%%) Delta Registers: %d\n", this.dexRunningDeltaInsns, this.dexRunningTotalInsns, 100.0 * (this.dexRunningDeltaInsns / (this.dexRunningTotalInsns + Math.abs(this.dexRunningDeltaInsns))), this.dexRunningDeltaRegisters);
        out.printf("Original bytecode byte count: %d\n", this.runningOriginalBytes);
    }
}

package embedded.com.android.dx.dex.cf;

import java.io.*;
import embedded.com.android.dx.rop.code.*;
import embedded.com.android.dx.ssa.*;
import java.util.*;

public class OptimizerOptions
{
    private HashSet<String> optimizeList;
    private HashSet<String> dontOptimizeList;
    private boolean optimizeListsLoaded;
    
    public void loadOptimizeLists(final String optimizeListFile, final String dontOptimizeListFile) {
        if (this.optimizeListsLoaded) {
            return;
        }
        if (optimizeListFile != null && dontOptimizeListFile != null) {
            throw new RuntimeException("optimize and don't optimize lists  are mutually exclusive.");
        }
        if (optimizeListFile != null) {
            this.optimizeList = loadStringsFromFile(optimizeListFile);
        }
        if (dontOptimizeListFile != null) {
            this.dontOptimizeList = loadStringsFromFile(dontOptimizeListFile);
        }
        this.optimizeListsLoaded = true;
    }
    
    private static HashSet<String> loadStringsFromFile(final String filename) {
        final HashSet<String> result = new HashSet<String>();
        try {
            final FileReader fr = new FileReader(filename);
            final BufferedReader bfr = new BufferedReader(fr);
            String line;
            while (null != (line = bfr.readLine())) {
                result.add(line);
            }
            fr.close();
        }
        catch (IOException ex) {
            throw new RuntimeException("Error with optimize list: " + filename, ex);
        }
        return result;
    }
    
    public void compareOptimizerStep(final RopMethod nonOptRmeth, final int paramSize, final boolean isStatic, final CfOptions args, final TranslationAdvice advice, final RopMethod rmeth) {
        final EnumSet<Optimizer.OptionalStep> steps = EnumSet.allOf(Optimizer.OptionalStep.class);
        steps.remove(Optimizer.OptionalStep.CONST_COLLECTOR);
        final RopMethod skipRopMethod = Optimizer.optimize(nonOptRmeth, paramSize, isStatic, args.localInfo, advice, steps);
        final int normalInsns = rmeth.getBlocks().getEffectiveInstructionCount();
        final int skipInsns = skipRopMethod.getBlocks().getEffectiveInstructionCount();
        System.err.printf("optimize step regs:(%d/%d/%.2f%%) insns:(%d/%d/%.2f%%)\n", rmeth.getBlocks().getRegCount(), skipRopMethod.getBlocks().getRegCount(), 100.0 * ((skipRopMethod.getBlocks().getRegCount() - rmeth.getBlocks().getRegCount()) / skipRopMethod.getBlocks().getRegCount()), normalInsns, skipInsns, 100.0 * ((skipInsns - normalInsns) / skipInsns));
    }
    
    public boolean shouldOptimize(final String canonicalMethodName) {
        if (this.optimizeList != null) {
            return this.optimizeList.contains(canonicalMethodName);
        }
        return this.dontOptimizeList == null || !this.dontOptimizeList.contains(canonicalMethodName);
    }
}

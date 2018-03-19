package embedded.com.android.dx.ssa;

import embedded.com.android.dx.rop.code.*;
import java.util.*;
import embedded.com.android.dx.ssa.back.*;

public class Optimizer
{
    private static boolean preserveLocals;
    private static TranslationAdvice advice;
    
    public static boolean getPreserveLocals() {
        return Optimizer.preserveLocals;
    }
    
    public static TranslationAdvice getAdvice() {
        return Optimizer.advice;
    }
    
    public static RopMethod optimize(final RopMethod rmeth, final int paramWidth, final boolean isStatic, final boolean inPreserveLocals, final TranslationAdvice inAdvice) {
        return optimize(rmeth, paramWidth, isStatic, inPreserveLocals, inAdvice, EnumSet.allOf(OptionalStep.class));
    }
    
    public static RopMethod optimize(final RopMethod rmeth, final int paramWidth, final boolean isStatic, final boolean inPreserveLocals, final TranslationAdvice inAdvice, final EnumSet<OptionalStep> steps) {
        SsaMethod ssaMeth = null;
        Optimizer.preserveLocals = inPreserveLocals;
        Optimizer.advice = inAdvice;
        ssaMeth = SsaConverter.convertToSsaMethod(rmeth, paramWidth, isStatic);
        runSsaFormSteps(ssaMeth, steps);
        RopMethod resultMeth = SsaToRop.convertToRopMethod(ssaMeth, false);
        if (resultMeth.getBlocks().getRegCount() > Optimizer.advice.getMaxOptimalRegisterCount()) {
            resultMeth = optimizeMinimizeRegisters(rmeth, paramWidth, isStatic, steps);
        }
        return resultMeth;
    }
    
    private static RopMethod optimizeMinimizeRegisters(final RopMethod rmeth, final int paramWidth, final boolean isStatic, final EnumSet<OptionalStep> steps) {
        final SsaMethod ssaMeth = SsaConverter.convertToSsaMethod(rmeth, paramWidth, isStatic);
        final EnumSet<OptionalStep> newSteps = steps.clone();
        newSteps.remove(OptionalStep.CONST_COLLECTOR);
        runSsaFormSteps(ssaMeth, newSteps);
        final RopMethod resultMeth = SsaToRop.convertToRopMethod(ssaMeth, true);
        return resultMeth;
    }
    
    private static void runSsaFormSteps(final SsaMethod ssaMeth, final EnumSet<OptionalStep> steps) {
        boolean needsDeadCodeRemover = true;
        if (steps.contains(OptionalStep.MOVE_PARAM_COMBINER)) {
            MoveParamCombiner.process(ssaMeth);
        }
        if (steps.contains(OptionalStep.SCCP)) {
            SCCP.process(ssaMeth);
            DeadCodeRemover.process(ssaMeth);
            needsDeadCodeRemover = false;
        }
        if (steps.contains(OptionalStep.LITERAL_UPGRADE)) {
            LiteralOpUpgrader.process(ssaMeth);
            DeadCodeRemover.process(ssaMeth);
            needsDeadCodeRemover = false;
        }
        steps.remove(OptionalStep.ESCAPE_ANALYSIS);
        if (steps.contains(OptionalStep.ESCAPE_ANALYSIS)) {
            EscapeAnalysis.process(ssaMeth);
            DeadCodeRemover.process(ssaMeth);
            needsDeadCodeRemover = false;
        }
        if (steps.contains(OptionalStep.CONST_COLLECTOR)) {
            ConstCollector.process(ssaMeth);
            DeadCodeRemover.process(ssaMeth);
            needsDeadCodeRemover = false;
        }
        if (needsDeadCodeRemover) {
            DeadCodeRemover.process(ssaMeth);
        }
        PhiTypeResolver.process(ssaMeth);
    }
    
    public static SsaMethod debugEdgeSplit(final RopMethod rmeth, final int paramWidth, final boolean isStatic, final boolean inPreserveLocals, final TranslationAdvice inAdvice) {
        Optimizer.preserveLocals = inPreserveLocals;
        Optimizer.advice = inAdvice;
        return SsaConverter.testEdgeSplit(rmeth, paramWidth, isStatic);
    }
    
    public static SsaMethod debugPhiPlacement(final RopMethod rmeth, final int paramWidth, final boolean isStatic, final boolean inPreserveLocals, final TranslationAdvice inAdvice) {
        Optimizer.preserveLocals = inPreserveLocals;
        Optimizer.advice = inAdvice;
        return SsaConverter.testPhiPlacement(rmeth, paramWidth, isStatic);
    }
    
    public static SsaMethod debugRenaming(final RopMethod rmeth, final int paramWidth, final boolean isStatic, final boolean inPreserveLocals, final TranslationAdvice inAdvice) {
        Optimizer.preserveLocals = inPreserveLocals;
        Optimizer.advice = inAdvice;
        return SsaConverter.convertToSsaMethod(rmeth, paramWidth, isStatic);
    }
    
    public static SsaMethod debugDeadCodeRemover(final RopMethod rmeth, final int paramWidth, final boolean isStatic, final boolean inPreserveLocals, final TranslationAdvice inAdvice) {
        Optimizer.preserveLocals = inPreserveLocals;
        Optimizer.advice = inAdvice;
        final SsaMethod ssaMeth = SsaConverter.convertToSsaMethod(rmeth, paramWidth, isStatic);
        DeadCodeRemover.process(ssaMeth);
        return ssaMeth;
    }
    
    public static SsaMethod debugNoRegisterAllocation(final RopMethod rmeth, final int paramWidth, final boolean isStatic, final boolean inPreserveLocals, final TranslationAdvice inAdvice, final EnumSet<OptionalStep> steps) {
        Optimizer.preserveLocals = inPreserveLocals;
        Optimizer.advice = inAdvice;
        final SsaMethod ssaMeth = SsaConverter.convertToSsaMethod(rmeth, paramWidth, isStatic);
        runSsaFormSteps(ssaMeth, steps);
        LivenessAnalyzer.constructInterferenceGraph(ssaMeth);
        return ssaMeth;
    }
    
    static {
        Optimizer.preserveLocals = true;
    }
    
    public enum OptionalStep
    {
        MOVE_PARAM_COMBINER, 
        SCCP, 
        LITERAL_UPGRADE, 
        CONST_COLLECTOR, 
        ESCAPE_ANALYSIS;
    }
}

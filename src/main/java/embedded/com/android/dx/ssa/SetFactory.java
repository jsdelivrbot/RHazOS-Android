package embedded.com.android.dx.ssa;

import embedded.com.android.dx.util.*;

public final class SetFactory
{
    private static final int DOMFRONT_SET_THRESHOLD_SIZE = 3072;
    private static final int INTERFERENCE_SET_THRESHOLD_SIZE = 3072;
    private static final int LIVENESS_SET_THRESHOLD_SIZE = 3072;
    
    static IntSet makeDomFrontSet(final int szBlocks) {
        return (szBlocks <= 3072) ? new BitIntSet(szBlocks) : new ListIntSet();
    }
    
    public static IntSet makeInterferenceSet(final int countRegs) {
        return (countRegs <= 3072) ? new BitIntSet(countRegs) : new ListIntSet();
    }
    
    static IntSet makeLivenessSet(final int countRegs) {
        return (countRegs <= 3072) ? new BitIntSet(countRegs) : new ListIntSet();
    }
}

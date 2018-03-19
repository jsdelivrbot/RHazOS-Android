package embedded.com.android.dx.ssa.back;

import embedded.com.android.dx.ssa.*;

public class NullRegisterAllocator extends RegisterAllocator
{
    public NullRegisterAllocator(final SsaMethod ssaMeth, final InterferenceGraph interference) {
        super(ssaMeth, interference);
    }
    
    @Override
    public boolean wantsParamsMovedHigh() {
        return false;
    }
    
    @Override
    public RegisterMapper allocateRegisters() {
        final int oldRegCount = this.ssaMeth.getRegCount();
        final BasicRegisterMapper mapper = new BasicRegisterMapper(oldRegCount);
        for (int i = 0; i < oldRegCount; ++i) {
            mapper.addMapping(i, i * 2, 2);
        }
        return mapper;
    }
}

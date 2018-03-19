package embedded.com.android.dx.rop.code;

public final class ConservativeTranslationAdvice implements TranslationAdvice
{
    public static final ConservativeTranslationAdvice THE_ONE;
    
    @Override
    public boolean hasConstantOperation(final Rop opcode, final RegisterSpec sourceA, final RegisterSpec sourceB) {
        return false;
    }
    
    @Override
    public boolean requiresSourcesInOrder(final Rop opcode, final RegisterSpecList sources) {
        return false;
    }
    
    @Override
    public int getMaxOptimalRegisterCount() {
        return Integer.MAX_VALUE;
    }
    
    static {
        THE_ONE = new ConservativeTranslationAdvice();
    }
}

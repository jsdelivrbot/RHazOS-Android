package embedded.com.android.dx.rop.code;

public interface TranslationAdvice
{
    boolean hasConstantOperation(final Rop p0, final RegisterSpec p1, final RegisterSpec p2);
    
    boolean requiresSourcesInOrder(final Rop p0, final RegisterSpecList p1);
    
    int getMaxOptimalRegisterCount();
}

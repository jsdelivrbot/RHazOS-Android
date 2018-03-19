package embedded.com.android.dx.rop.cst;

public interface ConstantPool
{
    int size();
    
    Constant get(final int p0);
    
    Constant get0Ok(final int p0);
    
    Constant getOrNull(final int p0);
    
    Constant[] getEntries();
}

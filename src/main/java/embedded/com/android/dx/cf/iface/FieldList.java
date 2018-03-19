package embedded.com.android.dx.cf.iface;

public interface FieldList
{
    boolean isMutable();
    
    int size();
    
    Field get(final int p0);
}

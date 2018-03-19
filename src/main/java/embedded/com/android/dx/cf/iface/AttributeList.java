package embedded.com.android.dx.cf.iface;

public interface AttributeList
{
    boolean isMutable();
    
    int size();
    
    Attribute get(final int p0);
    
    int byteLength();
    
    Attribute findFirst(final String p0);
    
    Attribute findNext(final Attribute p0);
}

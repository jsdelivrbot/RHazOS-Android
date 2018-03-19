package embedded.com.android.dx.util;

public interface IntSet
{
    void add(final int p0);
    
    void remove(final int p0);
    
    boolean has(final int p0);
    
    void merge(final IntSet p0);
    
    int elements();
    
    IntIterator iterator();
}

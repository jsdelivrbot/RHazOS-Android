package embedded.com.android.dx.io.instructions;

public interface CodeCursor
{
    int cursor();
    
    int baseAddressForCursor();
    
    void setBaseAddress(final int p0, final int p1);
}

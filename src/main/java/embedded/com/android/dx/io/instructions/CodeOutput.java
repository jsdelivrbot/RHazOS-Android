package embedded.com.android.dx.io.instructions;

public interface CodeOutput extends CodeCursor
{
    void write(final short p0);
    
    void write(final short p0, final short p1);
    
    void write(final short p0, final short p1, final short p2);
    
    void write(final short p0, final short p1, final short p2, final short p3);
    
    void write(final short p0, final short p1, final short p2, final short p3, final short p4);
    
    void writeInt(final int p0);
    
    void writeLong(final long p0);
    
    void write(final byte[] p0);
    
    void write(final short[] p0);
    
    void write(final int[] p0);
    
    void write(final long[] p0);
}

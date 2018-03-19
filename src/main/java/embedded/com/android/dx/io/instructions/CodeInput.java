package embedded.com.android.dx.io.instructions;

import java.io.*;

public interface CodeInput extends CodeCursor
{
    boolean hasMore();
    
    int read() throws EOFException;
    
    int readInt() throws EOFException;
    
    long readLong() throws EOFException;
}

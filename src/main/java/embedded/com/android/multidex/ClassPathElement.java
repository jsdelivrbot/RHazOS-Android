package embedded.com.android.multidex;

import java.io.*;

interface ClassPathElement
{
    public static final char SEPARATOR_CHAR = '/';
    
    InputStream open(final String p0) throws IOException;
    
    void close() throws IOException;
    
    Iterable<String> list();
}

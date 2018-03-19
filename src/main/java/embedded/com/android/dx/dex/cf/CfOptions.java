package embedded.com.android.dx.dex.cf;

import java.io.*;

public class CfOptions
{
    public int positionInfo;
    public boolean localInfo;
    public boolean strictNameCheck;
    public boolean optimize;
    public String optimizeListFile;
    public String dontOptimizeListFile;
    public boolean statistics;
    public PrintStream warn;
    
    public CfOptions() {
        this.positionInfo = 2;
        this.localInfo = false;
        this.strictNameCheck = true;
        this.optimize = false;
        this.optimizeListFile = null;
        this.dontOptimizeListFile = null;
        this.warn = System.err;
    }
}

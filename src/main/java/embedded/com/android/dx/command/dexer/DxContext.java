package embedded.com.android.dx.command.dexer;

import embedded.com.android.dx.dex.cf.*;
import java.io.*;

public class DxContext
{
    public final CodeStatistics codeStatistics;
    public final OptimizerOptions optimizerOptions;
    public final PrintStream out;
    public final PrintStream err;
    final PrintStream noop;
    
    public DxContext(final OutputStream out, final OutputStream err) {
        this.codeStatistics = new CodeStatistics();
        this.optimizerOptions = new OptimizerOptions();
        this.noop = new PrintStream(new OutputStream() {
            @Override
            public void write(final int b) throws IOException {
            }
        });
        this.out = new PrintStream(out);
        this.err = new PrintStream(err);
    }
    
    public DxContext() {
        this(System.out, System.err);
    }
}

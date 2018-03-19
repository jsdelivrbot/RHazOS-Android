package embedded.com.android.dex.util;

import java.io.*;

public class ExceptionWithContext extends RuntimeException
{
    private StringBuffer context;
    
    public static ExceptionWithContext withContext(final Throwable ex, final String str) {
        ExceptionWithContext ewc;
        if (ex instanceof ExceptionWithContext) {
            ewc = (ExceptionWithContext)ex;
        }
        else {
            ewc = new ExceptionWithContext(ex);
        }
        ewc.addContext(str);
        return ewc;
    }
    
    public ExceptionWithContext(final String message) {
        this(message, null);
    }
    
    public ExceptionWithContext(final Throwable cause) {
        this(null, cause);
    }
    
    public ExceptionWithContext(final String message, final Throwable cause) {
        super((message != null) ? message : ((cause != null) ? cause.getMessage() : null), cause);
        if (cause instanceof ExceptionWithContext) {
            final String ctx = ((ExceptionWithContext)cause).context.toString();
            (this.context = new StringBuffer(ctx.length() + 200)).append(ctx);
        }
        else {
            this.context = new StringBuffer(200);
        }
    }
    
    @Override
    public void printStackTrace(final PrintStream out) {
        super.printStackTrace(out);
        out.println(this.context);
    }
    
    @Override
    public void printStackTrace(final PrintWriter out) {
        super.printStackTrace(out);
        out.println(this.context);
    }
    
    public void addContext(final String str) {
        if (str == null) {
            throw new NullPointerException("str == null");
        }
        this.context.append(str);
        if (!str.endsWith("\n")) {
            this.context.append('\n');
        }
    }
    
    public String getContext() {
        return this.context.toString();
    }
    
    public void printContext(final PrintStream out) {
        out.println(this.getMessage());
        out.print(this.context);
    }
    
    public void printContext(final PrintWriter out) {
        out.println(this.getMessage());
        out.print(this.context);
    }
}

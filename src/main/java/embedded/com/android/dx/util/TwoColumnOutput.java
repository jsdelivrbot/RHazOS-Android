package embedded.com.android.dx.util;

import java.io.*;

public final class TwoColumnOutput
{
    private final Writer out;
    private final int leftWidth;
    private final StringBuffer leftBuf;
    private final StringBuffer rightBuf;
    private final IndentingWriter leftColumn;
    private final IndentingWriter rightColumn;
    
    public static String toString(final String s1, final int width1, final String spacer, final String s2, final int width2) {
        final int len1 = s1.length();
        final int len2 = s2.length();
        final StringWriter sw = new StringWriter((len1 + len2) * 3);
        final TwoColumnOutput twoOut = new TwoColumnOutput(sw, width1, width2, spacer);
        try {
            twoOut.getLeft().write(s1);
            twoOut.getRight().write(s2);
        }
        catch (IOException ex) {
            throw new RuntimeException("shouldn't happen", ex);
        }
        twoOut.flush();
        return sw.toString();
    }
    
    public TwoColumnOutput(final Writer out, final int leftWidth, final int rightWidth, final String spacer) {
        if (out == null) {
            throw new NullPointerException("out == null");
        }
        if (leftWidth < 1) {
            throw new IllegalArgumentException("leftWidth < 1");
        }
        if (rightWidth < 1) {
            throw new IllegalArgumentException("rightWidth < 1");
        }
        if (spacer == null) {
            throw new NullPointerException("spacer == null");
        }
        final StringWriter leftWriter = new StringWriter(1000);
        final StringWriter rightWriter = new StringWriter(1000);
        this.out = out;
        this.leftWidth = leftWidth;
        this.leftBuf = leftWriter.getBuffer();
        this.rightBuf = rightWriter.getBuffer();
        this.leftColumn = new IndentingWriter(leftWriter, leftWidth);
        this.rightColumn = new IndentingWriter(rightWriter, rightWidth, spacer);
    }
    
    public TwoColumnOutput(final OutputStream out, final int leftWidth, final int rightWidth, final String spacer) {
        this(new OutputStreamWriter(out), leftWidth, rightWidth, spacer);
    }
    
    public Writer getLeft() {
        return this.leftColumn;
    }
    
    public Writer getRight() {
        return this.rightColumn;
    }
    
    public void flush() {
        try {
            appendNewlineIfNecessary(this.leftBuf, this.leftColumn);
            appendNewlineIfNecessary(this.rightBuf, this.rightColumn);
            this.outputFullLines();
            this.flushLeft();
            this.flushRight();
        }
        catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    private void outputFullLines() throws IOException {
        while (true) {
            final int leftLen = this.leftBuf.indexOf("\n");
            if (leftLen < 0) {
                return;
            }
            final int rightLen = this.rightBuf.indexOf("\n");
            if (rightLen < 0) {
                return;
            }
            if (leftLen != 0) {
                this.out.write(this.leftBuf.substring(0, leftLen));
            }
            if (rightLen != 0) {
                writeSpaces(this.out, this.leftWidth - leftLen);
                this.out.write(this.rightBuf.substring(0, rightLen));
            }
            this.out.write(10);
            this.leftBuf.delete(0, leftLen + 1);
            this.rightBuf.delete(0, rightLen + 1);
        }
    }
    
    private void flushLeft() throws IOException {
        appendNewlineIfNecessary(this.leftBuf, this.leftColumn);
        while (this.leftBuf.length() != 0) {
            this.rightColumn.write(10);
            this.outputFullLines();
        }
    }
    
    private void flushRight() throws IOException {
        appendNewlineIfNecessary(this.rightBuf, this.rightColumn);
        while (this.rightBuf.length() != 0) {
            this.leftColumn.write(10);
            this.outputFullLines();
        }
    }
    
    private static void appendNewlineIfNecessary(final StringBuffer buf, final Writer out) throws IOException {
        final int len = buf.length();
        if (len != 0 && buf.charAt(len - 1) != '\n') {
            out.write(10);
        }
    }
    
    private static void writeSpaces(final Writer out, int amt) throws IOException {
        while (amt > 0) {
            out.write(32);
            --amt;
        }
    }
}

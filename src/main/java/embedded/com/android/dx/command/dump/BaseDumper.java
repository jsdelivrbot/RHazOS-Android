package embedded.com.android.dx.command.dump;

import embedded.com.android.dx.dex.*;
import embedded.com.android.dx.cf.code.*;
import embedded.com.android.dx.cf.iface.*;
import embedded.com.android.dx.util.*;
import java.io.*;

public abstract class BaseDumper implements ParseObserver
{
    private final byte[] bytes;
    private final boolean rawBytes;
    private final PrintStream out;
    private final int width;
    private final String filePath;
    private final boolean strictParse;
    private final int hexCols;
    private int indent;
    private String separator;
    private int at;
    protected Args args;
    protected final DexOptions dexOptions;
    
    public BaseDumper(final byte[] bytes, final PrintStream out, final String filePath, final Args args) {
        this.bytes = bytes;
        this.rawBytes = args.rawBytes;
        this.out = out;
        this.width = ((args.width <= 0) ? 79 : args.width);
        this.filePath = filePath;
        this.strictParse = args.strictParse;
        this.indent = 0;
        this.separator = (this.rawBytes ? "|" : "");
        this.at = 0;
        this.args = args;
        this.dexOptions = new DexOptions();
        int hexCols = (this.width - 5) / 15 + 1 & 0xFFFFFFFE;
        if (hexCols < 6) {
            hexCols = 6;
        }
        else if (hexCols > 10) {
            hexCols = 10;
        }
        this.hexCols = hexCols;
    }
    
    static int computeParamWidth(final ConcreteMethod meth, final boolean isStatic) {
        return meth.getEffectiveDescriptor().getParameterTypes().getWordCount();
    }
    
    @Override
    public void changeIndent(final int indentDelta) {
        this.indent += indentDelta;
        this.separator = (this.rawBytes ? "|" : "");
        for (int i = 0; i < this.indent; ++i) {
            this.separator += "  ";
        }
    }
    
    @Override
    public void parsed(final ByteArray bytes, int offset, final int len, final String human) {
        offset = bytes.underlyingOffset(offset, this.getBytes());
        final boolean rawBytes = this.getRawBytes();
        if (offset < this.at) {
            this.println("<dump skipped backwards to " + Hex.u4(offset) + ">");
            this.at = offset;
        }
        else if (offset > this.at) {
            final String hex = rawBytes ? this.hexDump(this.at, offset - this.at) : "";
            this.print(this.twoColumns(hex, "<skipped to " + Hex.u4(offset) + ">"));
            this.at = offset;
        }
        final String hex = rawBytes ? this.hexDump(offset, len) : "";
        this.print(this.twoColumns(hex, human));
        this.at += len;
    }
    
    @Override
    public void startParsingMember(final ByteArray bytes, final int offset, final String name, final String descriptor) {
    }
    
    @Override
    public void endParsingMember(final ByteArray bytes, final int offset, final String name, final String descriptor, final Member member) {
    }
    
    protected final int getAt() {
        return this.at;
    }
    
    protected final void setAt(final ByteArray arr, final int offset) {
        this.at = arr.underlyingOffset(offset, this.bytes);
    }
    
    protected final byte[] getBytes() {
        return this.bytes;
    }
    
    protected final String getFilePath() {
        return this.filePath;
    }
    
    protected final boolean getStrictParse() {
        return this.strictParse;
    }
    
    protected final void print(final String s) {
        this.out.print(s);
    }
    
    protected final void println(final String s) {
        this.out.println(s);
    }
    
    protected final boolean getRawBytes() {
        return this.rawBytes;
    }
    
    protected final int getWidth1() {
        if (this.rawBytes) {
            return 5 + this.hexCols * 2 + this.hexCols / 2;
        }
        return 0;
    }
    
    protected final int getWidth2() {
        final int w1 = this.rawBytes ? (this.getWidth1() + 1) : 0;
        return this.width - w1 - this.indent * 2;
    }
    
    protected final String hexDump(final int offset, final int len) {
        return Hex.dump(this.bytes, offset, len, offset, this.hexCols, 4);
    }
    
    protected final String twoColumns(final String s1, final String s2) {
        final int w1 = this.getWidth1();
        final int w2 = this.getWidth2();
        try {
            if (w1 == 0) {
                final int len2 = s2.length();
                final StringWriter sw = new StringWriter(len2 * 2);
                final IndentingWriter iw = new IndentingWriter(sw, w2, this.separator);
                iw.write(s2);
                if (len2 == 0 || s2.charAt(len2 - 1) != '\n') {
                    iw.write(10);
                }
                iw.flush();
                return sw.toString();
            }
            return TwoColumnOutput.toString(s1, w1, this.separator, s2, w2);
        }
        catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}

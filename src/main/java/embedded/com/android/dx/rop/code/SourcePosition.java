package embedded.com.android.dx.rop.code;

import embedded.com.android.dx.rop.cst.*;
import embedded.com.android.dx.util.*;

public final class SourcePosition
{
    public static final SourcePosition NO_INFO;
    private final CstString sourceFile;
    private final int address;
    private final int line;
    
    public SourcePosition(final CstString sourceFile, final int address, final int line) {
        if (address < -1) {
            throw new IllegalArgumentException("address < -1");
        }
        if (line < -1) {
            throw new IllegalArgumentException("line < -1");
        }
        this.sourceFile = sourceFile;
        this.address = address;
        this.line = line;
    }
    
    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer(50);
        if (this.sourceFile != null) {
            sb.append(this.sourceFile.toHuman());
            sb.append(":");
        }
        if (this.line >= 0) {
            sb.append(this.line);
        }
        sb.append('@');
        if (this.address < 0) {
            sb.append("????");
        }
        else {
            sb.append(Hex.u2(this.address));
        }
        return sb.toString();
    }
    
    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof SourcePosition)) {
            return false;
        }
        if (this == other) {
            return true;
        }
        final SourcePosition pos = (SourcePosition)other;
        return this.address == pos.address && this.sameLineAndFile(pos);
    }
    
    @Override
    public int hashCode() {
        return this.sourceFile.hashCode() + this.address + this.line;
    }
    
    public boolean sameLine(final SourcePosition other) {
        return this.line == other.line;
    }
    
    public boolean sameLineAndFile(final SourcePosition other) {
        return this.line == other.line && (this.sourceFile == other.sourceFile || (this.sourceFile != null && this.sourceFile.equals(other.sourceFile)));
    }
    
    public CstString getSourceFile() {
        return this.sourceFile;
    }
    
    public int getAddress() {
        return this.address;
    }
    
    public int getLine() {
        return this.line;
    }
    
    static {
        NO_INFO = new SourcePosition(null, -1, -1);
    }
}

package embedded.com.android.dx.dex.code;

import embedded.com.android.dx.io.*;

public final class Dop
{
    private final int opcode;
    private final int family;
    private final int nextOpcode;
    private final InsnFormat format;
    private final boolean hasResult;
    
    public Dop(final int opcode, final int family, final int nextOpcode, final InsnFormat format, final boolean hasResult) {
        if (!Opcodes.isValidShape(opcode)) {
            throw new IllegalArgumentException("bogus opcode");
        }
        if (!Opcodes.isValidShape(family)) {
            throw new IllegalArgumentException("bogus family");
        }
        if (!Opcodes.isValidShape(nextOpcode)) {
            throw new IllegalArgumentException("bogus nextOpcode");
        }
        if (format == null) {
            throw new NullPointerException("format == null");
        }
        this.opcode = opcode;
        this.family = family;
        this.nextOpcode = nextOpcode;
        this.format = format;
        this.hasResult = hasResult;
    }
    
    @Override
    public String toString() {
        return this.getName();
    }
    
    public int getOpcode() {
        return this.opcode;
    }
    
    public int getFamily() {
        return this.family;
    }
    
    public InsnFormat getFormat() {
        return this.format;
    }
    
    public boolean hasResult() {
        return this.hasResult;
    }
    
    public String getName() {
        return OpcodeInfo.getName(this.opcode);
    }
    
    public int getNextOpcode() {
        return this.nextOpcode;
    }
    
    public Dop getOppositeTest() {
        switch (this.opcode) {
            case 50: {
                return Dops.IF_NE;
            }
            case 51: {
                return Dops.IF_EQ;
            }
            case 52: {
                return Dops.IF_GE;
            }
            case 53: {
                return Dops.IF_LT;
            }
            case 54: {
                return Dops.IF_LE;
            }
            case 55: {
                return Dops.IF_GT;
            }
            case 56: {
                return Dops.IF_NEZ;
            }
            case 57: {
                return Dops.IF_EQZ;
            }
            case 58: {
                return Dops.IF_GEZ;
            }
            case 59: {
                return Dops.IF_LTZ;
            }
            case 60: {
                return Dops.IF_LEZ;
            }
            case 61: {
                return Dops.IF_GTZ;
            }
            default: {
                throw new IllegalArgumentException("bogus opcode: " + this);
            }
        }
    }
}

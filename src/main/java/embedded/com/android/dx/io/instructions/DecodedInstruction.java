package embedded.com.android.dx.io.instructions;

import embedded.com.android.dx.io.*;
import java.io.*;
import embedded.com.android.dex.*;
import embedded.com.android.dx.util.*;

public abstract class DecodedInstruction
{
    private final InstructionCodec format;
    private final int opcode;
    private final int index;
    private final IndexType indexType;
    private final int target;
    private final long literal;
    
    public static DecodedInstruction decode(final CodeInput in) throws EOFException {
        final int opcodeUnit = in.read();
        final int opcode = Opcodes.extractOpcodeFromUnit(opcodeUnit);
        final InstructionCodec format = OpcodeInfo.getFormat(opcode);
        return format.decode(opcodeUnit, in);
    }
    
    public static DecodedInstruction[] decodeAll(final short[] encodedInstructions) {
        final int size = encodedInstructions.length;
        final DecodedInstruction[] decoded = new DecodedInstruction[size];
        final ShortArrayCodeInput in = new ShortArrayCodeInput(encodedInstructions);
        try {
            while (in.hasMore()) {
                decoded[in.cursor()] = decode(in);
            }
        }
        catch (EOFException ex) {
            throw new DexException(ex);
        }
        return decoded;
    }
    
    public DecodedInstruction(final InstructionCodec format, final int opcode, final int index, final IndexType indexType, final int target, final long literal) {
        if (format == null) {
            throw new NullPointerException("format == null");
        }
        if (!Opcodes.isValidShape(opcode)) {
            throw new IllegalArgumentException("invalid opcode");
        }
        this.format = format;
        this.opcode = opcode;
        this.index = index;
        this.indexType = indexType;
        this.target = target;
        this.literal = literal;
    }
    
    public final InstructionCodec getFormat() {
        return this.format;
    }
    
    public final int getOpcode() {
        return this.opcode;
    }
    
    public final short getOpcodeUnit() {
        return (short)this.opcode;
    }
    
    public final int getIndex() {
        return this.index;
    }
    
    public final short getIndexUnit() {
        return (short)this.index;
    }
    
    public final IndexType getIndexType() {
        return this.indexType;
    }
    
    public final int getTarget() {
        return this.target;
    }
    
    public final int getTarget(final int baseAddress) {
        return this.target - baseAddress;
    }
    
    public final short getTargetUnit(final int baseAddress) {
        final int relativeTarget = this.getTarget(baseAddress);
        if (relativeTarget != (short)relativeTarget) {
            throw new DexException("Target out of range: " + Hex.s4(relativeTarget));
        }
        return (short)relativeTarget;
    }
    
    public final int getTargetByte(final int baseAddress) {
        final int relativeTarget = this.getTarget(baseAddress);
        if (relativeTarget != (byte)relativeTarget) {
            throw new DexException("Target out of range: " + Hex.s4(relativeTarget));
        }
        return relativeTarget & 0xFF;
    }
    
    public final long getLiteral() {
        return this.literal;
    }
    
    public final int getLiteralInt() {
        if (this.literal != (int)this.literal) {
            throw new DexException("Literal out of range: " + Hex.u8(this.literal));
        }
        return (int)this.literal;
    }
    
    public final short getLiteralUnit() {
        if (this.literal != (short)this.literal) {
            throw new DexException("Literal out of range: " + Hex.u8(this.literal));
        }
        return (short)this.literal;
    }
    
    public final int getLiteralByte() {
        if (this.literal != (byte)this.literal) {
            throw new DexException("Literal out of range: " + Hex.u8(this.literal));
        }
        return (int)this.literal & 0xFF;
    }
    
    public final int getLiteralNibble() {
        if (this.literal < -8L || this.literal > 7L) {
            throw new DexException("Literal out of range: " + Hex.u8(this.literal));
        }
        return (int)this.literal & 0xF;
    }
    
    public abstract int getRegisterCount();
    
    public int getA() {
        return 0;
    }
    
    public int getB() {
        return 0;
    }
    
    public int getC() {
        return 0;
    }
    
    public int getD() {
        return 0;
    }
    
    public int getE() {
        return 0;
    }
    
    public final short getRegisterCountUnit() {
        final int registerCount = this.getRegisterCount();
        if ((registerCount & 0xFFFF0000) != 0x0) {
            throw new DexException("Register count out of range: " + Hex.u8(registerCount));
        }
        return (short)registerCount;
    }
    
    public final short getAUnit() {
        final int a = this.getA();
        if ((a & 0xFFFF0000) != 0x0) {
            throw new DexException("Register A out of range: " + Hex.u8(a));
        }
        return (short)a;
    }
    
    public final short getAByte() {
        final int a = this.getA();
        if ((a & 0xFFFFFF00) != 0x0) {
            throw new DexException("Register A out of range: " + Hex.u8(a));
        }
        return (short)a;
    }
    
    public final short getANibble() {
        final int a = this.getA();
        if ((a & 0xFFFFFFF0) != 0x0) {
            throw new DexException("Register A out of range: " + Hex.u8(a));
        }
        return (short)a;
    }
    
    public final short getBUnit() {
        final int b = this.getB();
        if ((b & 0xFFFF0000) != 0x0) {
            throw new DexException("Register B out of range: " + Hex.u8(b));
        }
        return (short)b;
    }
    
    public final short getBByte() {
        final int b = this.getB();
        if ((b & 0xFFFFFF00) != 0x0) {
            throw new DexException("Register B out of range: " + Hex.u8(b));
        }
        return (short)b;
    }
    
    public final short getBNibble() {
        final int b = this.getB();
        if ((b & 0xFFFFFFF0) != 0x0) {
            throw new DexException("Register B out of range: " + Hex.u8(b));
        }
        return (short)b;
    }
    
    public final short getCUnit() {
        final int c = this.getC();
        if ((c & 0xFFFF0000) != 0x0) {
            throw new DexException("Register C out of range: " + Hex.u8(c));
        }
        return (short)c;
    }
    
    public final short getCByte() {
        final int c = this.getC();
        if ((c & 0xFFFFFF00) != 0x0) {
            throw new DexException("Register C out of range: " + Hex.u8(c));
        }
        return (short)c;
    }
    
    public final short getCNibble() {
        final int c = this.getC();
        if ((c & 0xFFFFFFF0) != 0x0) {
            throw new DexException("Register C out of range: " + Hex.u8(c));
        }
        return (short)c;
    }
    
    public final short getDUnit() {
        final int d = this.getD();
        if ((d & 0xFFFF0000) != 0x0) {
            throw new DexException("Register D out of range: " + Hex.u8(d));
        }
        return (short)d;
    }
    
    public final short getDByte() {
        final int d = this.getD();
        if ((d & 0xFFFFFF00) != 0x0) {
            throw new DexException("Register D out of range: " + Hex.u8(d));
        }
        return (short)d;
    }
    
    public final short getDNibble() {
        final int d = this.getD();
        if ((d & 0xFFFFFFF0) != 0x0) {
            throw new DexException("Register D out of range: " + Hex.u8(d));
        }
        return (short)d;
    }
    
    public final short getENibble() {
        final int e = this.getE();
        if ((e & 0xFFFFFFF0) != 0x0) {
            throw new DexException("Register E out of range: " + Hex.u8(e));
        }
        return (short)e;
    }
    
    public final void encode(final CodeOutput out) {
        this.format.encode(this, out);
    }
    
    public abstract DecodedInstruction withIndex(final int p0);
    
    public DecodedInstruction withProtoIndex(final int newIndex, final int newProtoIndex) {
        throw new IllegalStateException(this.getClass().toString());
    }
    
    public short getProtoIndex() {
        throw new IllegalStateException(this.getClass().toString());
    }
}

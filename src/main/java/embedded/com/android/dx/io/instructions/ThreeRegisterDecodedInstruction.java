package embedded.com.android.dx.io.instructions;

import embedded.com.android.dx.io.*;

public final class ThreeRegisterDecodedInstruction extends DecodedInstruction
{
    private final int a;
    private final int b;
    private final int c;
    
    public ThreeRegisterDecodedInstruction(final InstructionCodec format, final int opcode, final int index, final IndexType indexType, final int target, final long literal, final int a, final int b, final int c) {
        super(format, opcode, index, indexType, target, literal);
        this.a = a;
        this.b = b;
        this.c = c;
    }
    
    @Override
    public int getRegisterCount() {
        return 3;
    }
    
    @Override
    public int getA() {
        return this.a;
    }
    
    @Override
    public int getB() {
        return this.b;
    }
    
    @Override
    public int getC() {
        return this.c;
    }
    
    @Override
    public DecodedInstruction withIndex(final int newIndex) {
        return new ThreeRegisterDecodedInstruction(this.getFormat(), this.getOpcode(), newIndex, this.getIndexType(), this.getTarget(), this.getLiteral(), this.a, this.b, this.c);
    }
}

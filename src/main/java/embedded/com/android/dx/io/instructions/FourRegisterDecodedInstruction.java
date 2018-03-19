package embedded.com.android.dx.io.instructions;

import embedded.com.android.dx.io.*;

public final class FourRegisterDecodedInstruction extends DecodedInstruction
{
    private final int a;
    private final int b;
    private final int c;
    private final int d;
    
    public FourRegisterDecodedInstruction(final InstructionCodec format, final int opcode, final int index, final IndexType indexType, final int target, final long literal, final int a, final int b, final int c, final int d) {
        super(format, opcode, index, indexType, target, literal);
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
    }
    
    @Override
    public int getRegisterCount() {
        return 4;
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
    public int getD() {
        return this.d;
    }
    
    @Override
    public DecodedInstruction withIndex(final int newIndex) {
        return new FourRegisterDecodedInstruction(this.getFormat(), this.getOpcode(), newIndex, this.getIndexType(), this.getTarget(), this.getLiteral(), this.a, this.b, this.c, this.d);
    }
}

package embedded.com.android.dx.io.instructions;

import embedded.com.android.dx.io.*;

public final class TwoRegisterDecodedInstruction extends DecodedInstruction
{
    private final int a;
    private final int b;
    
    public TwoRegisterDecodedInstruction(final InstructionCodec format, final int opcode, final int index, final IndexType indexType, final int target, final long literal, final int a, final int b) {
        super(format, opcode, index, indexType, target, literal);
        this.a = a;
        this.b = b;
    }
    
    @Override
    public int getRegisterCount() {
        return 2;
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
    public DecodedInstruction withIndex(final int newIndex) {
        return new TwoRegisterDecodedInstruction(this.getFormat(), this.getOpcode(), newIndex, this.getIndexType(), this.getTarget(), this.getLiteral(), this.a, this.b);
    }
}

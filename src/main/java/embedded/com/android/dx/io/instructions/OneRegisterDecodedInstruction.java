package embedded.com.android.dx.io.instructions;

import embedded.com.android.dx.io.*;

public final class OneRegisterDecodedInstruction extends DecodedInstruction
{
    private final int a;
    
    public OneRegisterDecodedInstruction(final InstructionCodec format, final int opcode, final int index, final IndexType indexType, final int target, final long literal, final int a) {
        super(format, opcode, index, indexType, target, literal);
        this.a = a;
    }
    
    @Override
    public int getRegisterCount() {
        return 1;
    }
    
    @Override
    public int getA() {
        return this.a;
    }
    
    @Override
    public DecodedInstruction withIndex(final int newIndex) {
        return new OneRegisterDecodedInstruction(this.getFormat(), this.getOpcode(), newIndex, this.getIndexType(), this.getTarget(), this.getLiteral(), this.a);
    }
}

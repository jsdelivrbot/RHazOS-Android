package embedded.com.android.dx.io.instructions;

import embedded.com.android.dx.io.*;

public final class RegisterRangeDecodedInstruction extends DecodedInstruction
{
    private final int a;
    private final int registerCount;
    
    public RegisterRangeDecodedInstruction(final InstructionCodec format, final int opcode, final int index, final IndexType indexType, final int target, final long literal, final int a, final int registerCount) {
        super(format, opcode, index, indexType, target, literal);
        this.a = a;
        this.registerCount = registerCount;
    }
    
    @Override
    public int getRegisterCount() {
        return this.registerCount;
    }
    
    @Override
    public int getA() {
        return this.a;
    }
    
    @Override
    public DecodedInstruction withIndex(final int newIndex) {
        return new RegisterRangeDecodedInstruction(this.getFormat(), this.getOpcode(), newIndex, this.getIndexType(), this.getTarget(), this.getLiteral(), this.a, this.registerCount);
    }
}

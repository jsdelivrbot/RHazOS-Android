package embedded.com.android.dx.io.instructions;

import embedded.com.android.dx.io.*;

public final class ZeroRegisterDecodedInstruction extends DecodedInstruction
{
    public ZeroRegisterDecodedInstruction(final InstructionCodec format, final int opcode, final int index, final IndexType indexType, final int target, final long literal) {
        super(format, opcode, index, indexType, target, literal);
    }
    
    @Override
    public int getRegisterCount() {
        return 0;
    }
    
    @Override
    public DecodedInstruction withIndex(final int newIndex) {
        return new ZeroRegisterDecodedInstruction(this.getFormat(), this.getOpcode(), newIndex, this.getIndexType(), this.getTarget(), this.getLiteral());
    }
}

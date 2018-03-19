package embedded.com.android.dx.io.instructions;

import embedded.com.android.dx.io.*;

public class InvokePolymorphicRangeDecodedInstruction extends DecodedInstruction
{
    private final int c;
    private final int registerCount;
    private final int protoIndex;
    
    public InvokePolymorphicRangeDecodedInstruction(final InstructionCodec format, final int opcode, final int methodIndex, final IndexType indexType, final int c, final int registerCount, final int protoIndex) {
        super(format, opcode, methodIndex, indexType, 0, 0L);
        if (protoIndex != (short)protoIndex) {
            throw new IllegalArgumentException("protoIndex doesn't fit in a short: " + protoIndex);
        }
        this.c = c;
        this.registerCount = registerCount;
        this.protoIndex = protoIndex;
    }
    
    @Override
    public int getRegisterCount() {
        return this.registerCount;
    }
    
    @Override
    public int getC() {
        return this.c;
    }
    
    @Override
    public DecodedInstruction withProtoIndex(final int newIndex, final int newProtoIndex) {
        return new InvokePolymorphicRangeDecodedInstruction(this.getFormat(), this.getOpcode(), newIndex, this.getIndexType(), this.c, this.registerCount, newProtoIndex);
    }
    
    @Override
    public DecodedInstruction withIndex(final int newIndex) {
        throw new UnsupportedOperationException("use withProtoIndex to update both the method and proto indices for invoke-polymorphic/range");
    }
    
    @Override
    public short getProtoIndex() {
        return (short)this.protoIndex;
    }
}

package embedded.com.android.dx.io.instructions;

import embedded.com.android.dx.io.*;

public class InvokePolymorphicDecodedInstruction extends DecodedInstruction
{
    private final int protoIndex;
    private final int[] registers;
    
    public InvokePolymorphicDecodedInstruction(final InstructionCodec format, final int opcode, final int methodIndex, final IndexType indexType, final int protoIndex, final int[] registers) {
        super(format, opcode, methodIndex, indexType, 0, 0L);
        if (protoIndex != (short)protoIndex) {
            throw new IllegalArgumentException("protoIndex doesn't fit in a short: " + protoIndex);
        }
        this.protoIndex = protoIndex;
        this.registers = registers;
    }
    
    @Override
    public int getRegisterCount() {
        return this.registers.length;
    }
    
    @Override
    public DecodedInstruction withIndex(final int newIndex) {
        throw new UnsupportedOperationException("use withProtoIndex to update both the method and proto indices for invoke-polymorphic");
    }
    
    @Override
    public DecodedInstruction withProtoIndex(final int newIndex, final int newProtoIndex) {
        return new InvokePolymorphicDecodedInstruction(this.getFormat(), this.getOpcode(), newIndex, this.getIndexType(), newProtoIndex, this.registers);
    }
    
    @Override
    public int getC() {
        return (this.registers.length > 0) ? this.registers[0] : 0;
    }
    
    @Override
    public int getD() {
        return (this.registers.length > 1) ? this.registers[1] : 0;
    }
    
    @Override
    public int getE() {
        return (this.registers.length > 2) ? this.registers[2] : 0;
    }
    
    public int getF() {
        return (this.registers.length > 3) ? this.registers[3] : 0;
    }
    
    public int getG() {
        return (this.registers.length > 4) ? this.registers[4] : 0;
    }
    
    @Override
    public short getProtoIndex() {
        return (short)this.protoIndex;
    }
}

package embedded.com.android.dx.io.instructions;

import embedded.com.android.dx.io.*;

public final class FillArrayDataPayloadDecodedInstruction extends DecodedInstruction
{
    private final Object data;
    private final int size;
    private final int elementWidth;
    
    private FillArrayDataPayloadDecodedInstruction(final InstructionCodec format, final int opcode, final Object data, final int size, final int elementWidth) {
        super(format, opcode, 0, null, 0, 0L);
        this.data = data;
        this.size = size;
        this.elementWidth = elementWidth;
    }
    
    public FillArrayDataPayloadDecodedInstruction(final InstructionCodec format, final int opcode, final byte[] data) {
        this(format, opcode, data, data.length, 1);
    }
    
    public FillArrayDataPayloadDecodedInstruction(final InstructionCodec format, final int opcode, final short[] data) {
        this(format, opcode, data, data.length, 2);
    }
    
    public FillArrayDataPayloadDecodedInstruction(final InstructionCodec format, final int opcode, final int[] data) {
        this(format, opcode, data, data.length, 4);
    }
    
    public FillArrayDataPayloadDecodedInstruction(final InstructionCodec format, final int opcode, final long[] data) {
        this(format, opcode, data, data.length, 8);
    }
    
    @Override
    public int getRegisterCount() {
        return 0;
    }
    
    public short getElementWidthUnit() {
        return (short)this.elementWidth;
    }
    
    public int getSize() {
        return this.size;
    }
    
    public Object getData() {
        return this.data;
    }
    
    @Override
    public DecodedInstruction withIndex(final int newIndex) {
        throw new UnsupportedOperationException("no index in instruction");
    }
}

package embedded.com.android.dx.io.instructions;

import embedded.com.android.dx.io.*;

public final class PackedSwitchPayloadDecodedInstruction extends DecodedInstruction
{
    private final int firstKey;
    private final int[] targets;
    
    public PackedSwitchPayloadDecodedInstruction(final InstructionCodec format, final int opcode, final int firstKey, final int[] targets) {
        super(format, opcode, 0, null, 0, 0L);
        this.firstKey = firstKey;
        this.targets = targets;
    }
    
    @Override
    public int getRegisterCount() {
        return 0;
    }
    
    public int getFirstKey() {
        return this.firstKey;
    }
    
    public int[] getTargets() {
        return this.targets;
    }
    
    @Override
    public DecodedInstruction withIndex(final int newIndex) {
        throw new UnsupportedOperationException("no index in instruction");
    }
}

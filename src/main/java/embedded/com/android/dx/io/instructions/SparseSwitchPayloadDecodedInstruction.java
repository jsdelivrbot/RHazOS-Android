package embedded.com.android.dx.io.instructions;

import embedded.com.android.dx.io.*;

public final class SparseSwitchPayloadDecodedInstruction extends DecodedInstruction
{
    private final int[] keys;
    private final int[] targets;
    
    public SparseSwitchPayloadDecodedInstruction(final InstructionCodec format, final int opcode, final int[] keys, final int[] targets) {
        super(format, opcode, 0, null, 0, 0L);
        if (keys.length != targets.length) {
            throw new IllegalArgumentException("keys/targets length mismatch");
        }
        this.keys = keys;
        this.targets = targets;
    }
    
    @Override
    public int getRegisterCount() {
        return 0;
    }
    
    public int[] getKeys() {
        return this.keys;
    }
    
    public int[] getTargets() {
        return this.targets;
    }
    
    @Override
    public DecodedInstruction withIndex(final int newIndex) {
        throw new UnsupportedOperationException("no index in instruction");
    }
}

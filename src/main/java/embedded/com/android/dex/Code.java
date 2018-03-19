package embedded.com.android.dex;

public final class Code
{
    private final int registersSize;
    private final int insSize;
    private final int outsSize;
    private final int debugInfoOffset;
    private final short[] instructions;
    private final Try[] tries;
    private final CatchHandler[] catchHandlers;
    
    public Code(final int registersSize, final int insSize, final int outsSize, final int debugInfoOffset, final short[] instructions, final Try[] tries, final CatchHandler[] catchHandlers) {
        this.registersSize = registersSize;
        this.insSize = insSize;
        this.outsSize = outsSize;
        this.debugInfoOffset = debugInfoOffset;
        this.instructions = instructions;
        this.tries = tries;
        this.catchHandlers = catchHandlers;
    }
    
    public int getRegistersSize() {
        return this.registersSize;
    }
    
    public int getInsSize() {
        return this.insSize;
    }
    
    public int getOutsSize() {
        return this.outsSize;
    }
    
    public int getDebugInfoOffset() {
        return this.debugInfoOffset;
    }
    
    public short[] getInstructions() {
        return this.instructions;
    }
    
    public Try[] getTries() {
        return this.tries;
    }
    
    public CatchHandler[] getCatchHandlers() {
        return this.catchHandlers;
    }
    
    public static class Try
    {
        final int startAddress;
        final int instructionCount;
        final int catchHandlerIndex;
        
        Try(final int startAddress, final int instructionCount, final int catchHandlerIndex) {
            this.startAddress = startAddress;
            this.instructionCount = instructionCount;
            this.catchHandlerIndex = catchHandlerIndex;
        }
        
        public int getStartAddress() {
            return this.startAddress;
        }
        
        public int getInstructionCount() {
            return this.instructionCount;
        }
        
        public int getCatchHandlerIndex() {
            return this.catchHandlerIndex;
        }
    }
    
    public static class CatchHandler
    {
        final int[] typeIndexes;
        final int[] addresses;
        final int catchAllAddress;
        final int offset;
        
        public CatchHandler(final int[] typeIndexes, final int[] addresses, final int catchAllAddress, final int offset) {
            this.typeIndexes = typeIndexes;
            this.addresses = addresses;
            this.catchAllAddress = catchAllAddress;
            this.offset = offset;
        }
        
        public int[] getTypeIndexes() {
            return this.typeIndexes;
        }
        
        public int[] getAddresses() {
            return this.addresses;
        }
        
        public int getCatchAllAddress() {
            return this.catchAllAddress;
        }
        
        public int getOffset() {
            return this.offset;
        }
    }
}

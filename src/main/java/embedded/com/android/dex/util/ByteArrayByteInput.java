package embedded.com.android.dex.util;

public final class ByteArrayByteInput implements ByteInput
{
    private final byte[] bytes;
    private int position;
    
    public ByteArrayByteInput(final byte... bytes) {
        this.bytes = bytes;
    }
    
    @Override
    public byte readByte() {
        return this.bytes[this.position++];
    }
}

package embedded.com.android.dex;

import embedded.com.android.dex.util.*;

public final class EncodedValue implements Comparable<EncodedValue>
{
    private final byte[] data;
    
    public EncodedValue(final byte[] data) {
        this.data = data;
    }
    
    public ByteInput asByteInput() {
        return new ByteArrayByteInput(this.data);
    }
    
    public byte[] getBytes() {
        return this.data;
    }
    
    public void writeTo(final Dex.Section out) {
        out.write(this.data);
    }
    
    @Override
    public int compareTo(final EncodedValue other) {
        for (int size = Math.min(this.data.length, other.data.length), i = 0; i < size; ++i) {
            if (this.data[i] != other.data[i]) {
                return (this.data[i] & 0xFF) - (other.data[i] & 0xFF);
            }
        }
        return this.data.length - other.data.length;
    }
    
    @Override
    public String toString() {
        return Integer.toHexString(this.data[0] & 0xFF) + "...(" + this.data.length + ")";
    }
}

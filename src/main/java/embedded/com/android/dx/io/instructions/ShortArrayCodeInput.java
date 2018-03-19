package embedded.com.android.dx.io.instructions;

import java.io.*;

public final class ShortArrayCodeInput extends BaseCodeCursor implements CodeInput
{
    private final short[] array;
    
    public ShortArrayCodeInput(final short[] array) {
        if (array == null) {
            throw new NullPointerException("array == null");
        }
        this.array = array;
    }
    
    @Override
    public boolean hasMore() {
        return this.cursor() < this.array.length;
    }
    
    @Override
    public int read() throws EOFException {
        try {
            final int value = this.array[this.cursor()];
            this.advance(1);
            return value & 0xFFFF;
        }
        catch (ArrayIndexOutOfBoundsException ex) {
            throw new EOFException();
        }
    }
    
    @Override
    public int readInt() throws EOFException {
        final int short0 = this.read();
        final int short2 = this.read();
        return short0 | short2 << 16;
    }
    
    @Override
    public long readLong() throws EOFException {
        final long short0 = this.read();
        final long short2 = this.read();
        final long short3 = this.read();
        final long short4 = this.read();
        return short0 | short2 << 16 | short3 << 32 | short4 << 48;
    }
}

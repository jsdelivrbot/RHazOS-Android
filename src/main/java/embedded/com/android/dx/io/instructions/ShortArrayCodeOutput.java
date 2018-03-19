package embedded.com.android.dx.io.instructions;

public final class ShortArrayCodeOutput extends BaseCodeCursor implements CodeOutput
{
    private final short[] array;
    
    public ShortArrayCodeOutput(final int maxSize) {
        if (maxSize < 0) {
            throw new IllegalArgumentException("maxSize < 0");
        }
        this.array = new short[maxSize];
    }
    
    public short[] getArray() {
        final int cursor = this.cursor();
        if (cursor == this.array.length) {
            return this.array;
        }
        final short[] result = new short[cursor];
        System.arraycopy(this.array, 0, result, 0, cursor);
        return result;
    }
    
    @Override
    public void write(final short codeUnit) {
        this.array[this.cursor()] = codeUnit;
        this.advance(1);
    }
    
    @Override
    public void write(final short u0, final short u1) {
        this.write(u0);
        this.write(u1);
    }
    
    @Override
    public void write(final short u0, final short u1, final short u2) {
        this.write(u0);
        this.write(u1);
        this.write(u2);
    }
    
    @Override
    public void write(final short u0, final short u1, final short u2, final short u3) {
        this.write(u0);
        this.write(u1);
        this.write(u2);
        this.write(u3);
    }
    
    @Override
    public void write(final short u0, final short u1, final short u2, final short u3, final short u4) {
        this.write(u0);
        this.write(u1);
        this.write(u2);
        this.write(u3);
        this.write(u4);
    }
    
    @Override
    public void writeInt(final int value) {
        this.write((short)value);
        this.write((short)(value >> 16));
    }
    
    @Override
    public void writeLong(final long value) {
        this.write((short)value);
        this.write((short)(value >> 16));
        this.write((short)(value >> 32));
        this.write((short)(value >> 48));
    }
    
    @Override
    public void write(final byte[] data) {
        int value = 0;
        boolean even = true;
        for (final byte b : data) {
            if (even) {
                value = (b & 0xFF);
                even = false;
            }
            else {
                value |= b << 8;
                this.write((short)value);
                even = true;
            }
        }
        if (!even) {
            this.write((short)value);
        }
    }
    
    @Override
    public void write(final short[] data) {
        for (final short unit : data) {
            this.write(unit);
        }
    }
    
    @Override
    public void write(final int[] data) {
        for (final int i : data) {
            this.writeInt(i);
        }
    }
    
    @Override
    public void write(final long[] data) {
        for (final long l : data) {
            this.writeLong(l);
        }
    }
}

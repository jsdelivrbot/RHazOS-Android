package embedded.com.android.dx.util;

import java.io.*;

public final class ByteArray
{
    private final byte[] bytes;
    private final int start;
    private final int size;
    
    public ByteArray(final byte[] bytes, final int start, final int end) {
        if (bytes == null) {
            throw new NullPointerException("bytes == null");
        }
        if (start < 0) {
            throw new IllegalArgumentException("start < 0");
        }
        if (end < start) {
            throw new IllegalArgumentException("end < start");
        }
        if (end > bytes.length) {
            throw new IllegalArgumentException("end > bytes.length");
        }
        this.bytes = bytes;
        this.start = start;
        this.size = end - start;
    }
    
    public ByteArray(final byte[] bytes) {
        this(bytes, 0, bytes.length);
    }
    
    public int size() {
        return this.size;
    }
    
    public ByteArray slice(final int start, final int end) {
        this.checkOffsets(start, end);
        return new ByteArray(this.bytes, start + this.start, end + this.start);
    }
    
    public int underlyingOffset(final int offset, final byte[] bytes) {
        if (bytes != this.bytes) {
            throw new IllegalArgumentException("wrong bytes");
        }
        return this.start + offset;
    }
    
    public int getByte(final int off) {
        this.checkOffsets(off, off + 1);
        return this.getByte0(off);
    }
    
    public int getShort(final int off) {
        this.checkOffsets(off, off + 2);
        return this.getByte0(off) << 8 | this.getUnsignedByte0(off + 1);
    }
    
    public int getInt(final int off) {
        this.checkOffsets(off, off + 4);
        return this.getByte0(off) << 24 | this.getUnsignedByte0(off + 1) << 16 | this.getUnsignedByte0(off + 2) << 8 | this.getUnsignedByte0(off + 3);
    }
    
    public long getLong(final int off) {
        this.checkOffsets(off, off + 8);
        final int part1 = this.getByte0(off) << 24 | this.getUnsignedByte0(off + 1) << 16 | this.getUnsignedByte0(off + 2) << 8 | this.getUnsignedByte0(off + 3);
        final int part2 = this.getByte0(off + 4) << 24 | this.getUnsignedByte0(off + 5) << 16 | this.getUnsignedByte0(off + 6) << 8 | this.getUnsignedByte0(off + 7);
        return (part2 & 0xFFFFFFFFL) | part1 << 32;
    }
    
    public int getUnsignedByte(final int off) {
        this.checkOffsets(off, off + 1);
        return this.getUnsignedByte0(off);
    }
    
    public int getUnsignedShort(final int off) {
        this.checkOffsets(off, off + 2);
        return this.getUnsignedByte0(off) << 8 | this.getUnsignedByte0(off + 1);
    }
    
    public void getBytes(final byte[] out, final int offset) {
        if (out.length - offset < this.size) {
            throw new IndexOutOfBoundsException("(out.length - offset) < size()");
        }
        System.arraycopy(this.bytes, this.start, out, offset, this.size);
    }
    
    private void checkOffsets(final int s, final int e) {
        if (s < 0 || e < s || e > this.size) {
            throw new IllegalArgumentException("bad range: " + s + ".." + e + "; actual size " + this.size);
        }
    }
    
    private int getByte0(final int off) {
        return this.bytes[this.start + off];
    }
    
    private int getUnsignedByte0(final int off) {
        return this.bytes[this.start + off] & 0xFF;
    }
    
    public MyDataInputStream makeDataInputStream() {
        return new MyDataInputStream(this.makeInputStream());
    }
    
    public MyInputStream makeInputStream() {
        return new MyInputStream();
    }
    
    public class MyInputStream extends InputStream
    {
        private int cursor;
        private int mark;
        
        public MyInputStream() {
            this.cursor = 0;
            this.mark = 0;
        }
        
        @Override
        public int read() throws IOException {
            if (this.cursor >= ByteArray.this.size) {
                return -1;
            }
            final int result = ByteArray.this.getUnsignedByte0(this.cursor);
            ++this.cursor;
            return result;
        }
        
        @Override
        public int read(final byte[] arr, final int offset, int length) {
            if (offset + length > arr.length) {
                length = arr.length - offset;
            }
            final int maxLength = ByteArray.this.size - this.cursor;
            if (length > maxLength) {
                length = maxLength;
            }
            System.arraycopy(ByteArray.this.bytes, this.cursor + ByteArray.this.start, arr, offset, length);
            this.cursor += length;
            return length;
        }
        
        @Override
        public int available() {
            return ByteArray.this.size - this.cursor;
        }
        
        @Override
        public void mark(final int reserve) {
            this.mark = this.cursor;
        }
        
        @Override
        public void reset() {
            this.cursor = this.mark;
        }
        
        @Override
        public boolean markSupported() {
            return true;
        }
    }
    
    public static class MyDataInputStream extends DataInputStream
    {
        private final MyInputStream wrapped;
        
        public MyDataInputStream(final MyInputStream wrapped) {
            super(wrapped);
            this.wrapped = wrapped;
        }
    }
    
    public interface GetCursor
    {
        int getCursor();
    }
}

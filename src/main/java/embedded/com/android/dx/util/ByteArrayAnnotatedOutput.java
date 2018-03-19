package embedded.com.android.dx.util;

import java.util.*;
import embedded.com.android.dex.util.*;
import embedded.com.android.dex.*;
import java.io.*;

public final class ByteArrayAnnotatedOutput implements AnnotatedOutput, ByteOutput
{
    private static final int DEFAULT_SIZE = 1000;
    private final boolean stretchy;
    private byte[] data;
    private int cursor;
    private boolean verbose;
    private ArrayList<Annotation> annotations;
    private int annotationWidth;
    private int hexCols;
    
    public ByteArrayAnnotatedOutput(final byte[] data) {
        this(data, false);
    }
    
    public ByteArrayAnnotatedOutput() {
        this(1000);
    }
    
    public ByteArrayAnnotatedOutput(final int size) {
        this(new byte[size], true);
    }
    
    private ByteArrayAnnotatedOutput(final byte[] data, final boolean stretchy) {
        if (data == null) {
            throw new NullPointerException("data == null");
        }
        this.stretchy = stretchy;
        this.data = data;
        this.cursor = 0;
        this.verbose = false;
        this.annotations = null;
        this.annotationWidth = 0;
        this.hexCols = 0;
    }
    
    public byte[] getArray() {
        return this.data;
    }
    
    public byte[] toByteArray() {
        final byte[] result = new byte[this.cursor];
        System.arraycopy(this.data, 0, result, 0, this.cursor);
        return result;
    }
    
    @Override
    public int getCursor() {
        return this.cursor;
    }
    
    @Override
    public void assertCursor(final int expectedCursor) {
        if (this.cursor != expectedCursor) {
            throw new ExceptionWithContext("expected cursor " + expectedCursor + "; actual value: " + this.cursor);
        }
    }
    
    @Override
    public void writeByte(final int value) {
        final int writeAt = this.cursor;
        final int end = writeAt + 1;
        if (this.stretchy) {
            this.ensureCapacity(end);
        }
        else if (end > this.data.length) {
            throwBounds();
            return;
        }
        this.data[writeAt] = (byte)value;
        this.cursor = end;
    }
    
    @Override
    public void writeShort(final int value) {
        final int writeAt = this.cursor;
        final int end = writeAt + 2;
        if (this.stretchy) {
            this.ensureCapacity(end);
        }
        else if (end > this.data.length) {
            throwBounds();
            return;
        }
        this.data[writeAt] = (byte)value;
        this.data[writeAt + 1] = (byte)(value >> 8);
        this.cursor = end;
    }
    
    @Override
    public void writeInt(final int value) {
        final int writeAt = this.cursor;
        final int end = writeAt + 4;
        if (this.stretchy) {
            this.ensureCapacity(end);
        }
        else if (end > this.data.length) {
            throwBounds();
            return;
        }
        this.data[writeAt] = (byte)value;
        this.data[writeAt + 1] = (byte)(value >> 8);
        this.data[writeAt + 2] = (byte)(value >> 16);
        this.data[writeAt + 3] = (byte)(value >> 24);
        this.cursor = end;
    }
    
    @Override
    public void writeLong(final long value) {
        final int writeAt = this.cursor;
        final int end = writeAt + 8;
        if (this.stretchy) {
            this.ensureCapacity(end);
        }
        else if (end > this.data.length) {
            throwBounds();
            return;
        }
        int half = (int)value;
        this.data[writeAt] = (byte)half;
        this.data[writeAt + 1] = (byte)(half >> 8);
        this.data[writeAt + 2] = (byte)(half >> 16);
        this.data[writeAt + 3] = (byte)(half >> 24);
        half = (int)(value >> 32);
        this.data[writeAt + 4] = (byte)half;
        this.data[writeAt + 5] = (byte)(half >> 8);
        this.data[writeAt + 6] = (byte)(half >> 16);
        this.data[writeAt + 7] = (byte)(half >> 24);
        this.cursor = end;
    }
    
    @Override
    public int writeUleb128(final int value) {
        if (this.stretchy) {
            this.ensureCapacity(this.cursor + 5);
        }
        final int cursorBefore = this.cursor;
        Leb128.writeUnsignedLeb128(this, value);
        return this.cursor - cursorBefore;
    }
    
    @Override
    public int writeSleb128(final int value) {
        if (this.stretchy) {
            this.ensureCapacity(this.cursor + 5);
        }
        final int cursorBefore = this.cursor;
        Leb128.writeSignedLeb128(this, value);
        return this.cursor - cursorBefore;
    }
    
    @Override
    public void write(final ByteArray bytes) {
        final int blen = bytes.size();
        final int writeAt = this.cursor;
        final int end = writeAt + blen;
        if (this.stretchy) {
            this.ensureCapacity(end);
        }
        else if (end > this.data.length) {
            throwBounds();
            return;
        }
        bytes.getBytes(this.data, writeAt);
        this.cursor = end;
    }
    
    @Override
    public void write(final byte[] bytes, final int offset, final int length) {
        final int writeAt = this.cursor;
        final int end = writeAt + length;
        final int bytesEnd = offset + length;
        if ((offset | length | end) < 0 || bytesEnd > bytes.length) {
            throw new IndexOutOfBoundsException("bytes.length " + bytes.length + "; " + offset + "..!" + end);
        }
        if (this.stretchy) {
            this.ensureCapacity(end);
        }
        else if (end > this.data.length) {
            throwBounds();
            return;
        }
        System.arraycopy(bytes, offset, this.data, writeAt, length);
        this.cursor = end;
    }
    
    @Override
    public void write(final byte[] bytes) {
        this.write(bytes, 0, bytes.length);
    }
    
    @Override
    public void writeZeroes(final int count) {
        if (count < 0) {
            throw new IllegalArgumentException("count < 0");
        }
        final int end = this.cursor + count;
        if (this.stretchy) {
            this.ensureCapacity(end);
        }
        else if (end > this.data.length) {
            throwBounds();
            return;
        }
        this.cursor = end;
    }
    
    @Override
    public void alignTo(final int alignment) {
        final int mask = alignment - 1;
        if (alignment < 0 || (mask & alignment) != 0x0) {
            throw new IllegalArgumentException("bogus alignment");
        }
        final int end = this.cursor + mask & ~mask;
        if (this.stretchy) {
            this.ensureCapacity(end);
        }
        else if (end > this.data.length) {
            throwBounds();
            return;
        }
        this.cursor = end;
    }
    
    @Override
    public boolean annotates() {
        return this.annotations != null;
    }
    
    @Override
    public boolean isVerbose() {
        return this.verbose;
    }
    
    @Override
    public void annotate(final String msg) {
        if (this.annotations == null) {
            return;
        }
        this.endAnnotation();
        this.annotations.add(new Annotation(this.cursor, msg));
    }
    
    @Override
    public void annotate(final int amt, final String msg) {
        if (this.annotations == null) {
            return;
        }
        this.endAnnotation();
        final int asz = this.annotations.size();
        final int lastEnd = (asz == 0) ? 0 : this.annotations.get(asz - 1).getEnd();
        int startAt;
        if (lastEnd <= this.cursor) {
            startAt = this.cursor;
        }
        else {
            startAt = lastEnd;
        }
        this.annotations.add(new Annotation(startAt, startAt + amt, msg));
    }
    
    @Override
    public void endAnnotation() {
        if (this.annotations == null) {
            return;
        }
        final int sz = this.annotations.size();
        if (sz != 0) {
            this.annotations.get(sz - 1).setEndIfUnset(this.cursor);
        }
    }
    
    @Override
    public int getAnnotationWidth() {
        final int leftWidth = 8 + this.hexCols * 2 + this.hexCols / 2;
        return this.annotationWidth - leftWidth;
    }
    
    public void enableAnnotations(final int annotationWidth, final boolean verbose) {
        if (this.annotations != null || this.cursor != 0) {
            throw new RuntimeException("cannot enable annotations");
        }
        if (annotationWidth < 40) {
            throw new IllegalArgumentException("annotationWidth < 40");
        }
        int hexCols = (annotationWidth - 7) / 15 + 1 & 0xFFFFFFFE;
        if (hexCols < 6) {
            hexCols = 6;
        }
        else if (hexCols > 10) {
            hexCols = 10;
        }
        this.annotations = new ArrayList<Annotation>(1000);
        this.annotationWidth = annotationWidth;
        this.hexCols = hexCols;
        this.verbose = verbose;
    }
    
    public void finishAnnotating() {
        this.endAnnotation();
        if (this.annotations != null) {
            int asz = this.annotations.size();
            while (asz > 0) {
                final Annotation last = this.annotations.get(asz - 1);
                if (last.getStart() > this.cursor) {
                    this.annotations.remove(asz - 1);
                    --asz;
                }
                else {
                    if (last.getEnd() > this.cursor) {
                        last.setEnd(this.cursor);
                        break;
                    }
                    break;
                }
            }
        }
    }
    
    public void writeAnnotationsTo(final Writer out) throws IOException {
        final int width2 = this.getAnnotationWidth();
        final int width3 = this.annotationWidth - width2 - 1;
        final TwoColumnOutput twoc = new TwoColumnOutput(out, width3, width2, "|");
        final Writer left = twoc.getLeft();
        final Writer right = twoc.getRight();
        int leftAt;
        int rightAt;
        int rightSz;
        int end;
        for (leftAt = 0, rightAt = 0, rightSz = this.annotations.size(); leftAt < this.cursor && rightAt < rightSz; leftAt = end) {
            final Annotation a = this.annotations.get(rightAt);
            int start = a.getStart();
            String text;
            if (leftAt < start) {
                end = start;
                start = leftAt;
                text = "";
            }
            else {
                end = a.getEnd();
                text = a.getText();
                ++rightAt;
            }
            left.write(Hex.dump(this.data, start, end - start, start, this.hexCols, 6));
            right.write(text);
            twoc.flush();
        }
        if (leftAt < this.cursor) {
            left.write(Hex.dump(this.data, leftAt, this.cursor - leftAt, leftAt, this.hexCols, 6));
        }
        while (rightAt < rightSz) {
            right.write(this.annotations.get(rightAt).getText());
            ++rightAt;
        }
        twoc.flush();
    }
    
    private static void throwBounds() {
        throw new IndexOutOfBoundsException("attempt to write past the end");
    }
    
    private void ensureCapacity(final int desiredSize) {
        if (this.data.length < desiredSize) {
            final byte[] newData = new byte[desiredSize * 2 + 1000];
            System.arraycopy(this.data, 0, newData, 0, this.cursor);
            this.data = newData;
        }
    }
    
    private static class Annotation
    {
        private final int start;
        private int end;
        private final String text;
        
        public Annotation(final int start, final int end, final String text) {
            this.start = start;
            this.end = end;
            this.text = text;
        }
        
        public Annotation(final int start, final String text) {
            this(start, Integer.MAX_VALUE, text);
        }
        
        public void setEndIfUnset(final int end) {
            if (this.end == Integer.MAX_VALUE) {
                this.end = end;
            }
        }
        
        public void setEnd(final int end) {
            this.end = end;
        }
        
        public int getStart() {
            return this.start;
        }
        
        public int getEnd() {
            return this.end;
        }
        
        public String getText() {
            return this.text;
        }
    }
}

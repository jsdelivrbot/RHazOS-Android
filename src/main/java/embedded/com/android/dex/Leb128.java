package embedded.com.android.dex;

import embedded.com.android.dex.util.*;

public final class Leb128
{
    public static int unsignedLeb128Size(final int value) {
        int remaining;
        int count;
        for (remaining = value >> 7, count = 0; remaining != 0; remaining >>= 7, ++count) {}
        return count + 1;
    }
    
    public static int readSignedLeb128(final ByteInput in) {
        int result = 0;
        int count = 0;
        int signBits = -1;
        int cur;
        do {
            cur = (in.readByte() & 0xFF);
            result |= (cur & 0x7F) << count * 7;
            signBits <<= 7;
            ++count;
        } while ((cur & 0x80) == 0x80 && count < 5);
        if ((cur & 0x80) == 0x80) {
            throw new DexException("invalid LEB128 sequence");
        }
        if ((signBits >> 1 & result) != 0x0) {
            result |= signBits;
        }
        return result;
    }
    
    public static int readUnsignedLeb128(final ByteInput in) {
        int result = 0;
        int count = 0;
        int cur;
        do {
            cur = (in.readByte() & 0xFF);
            result |= (cur & 0x7F) << count * 7;
            ++count;
        } while ((cur & 0x80) == 0x80 && count < 5);
        if ((cur & 0x80) == 0x80) {
            throw new DexException("invalid LEB128 sequence");
        }
        return result;
    }
    
    public static void writeUnsignedLeb128(final ByteOutput out, int value) {
        for (int remaining = value >>> 7; remaining != 0; remaining >>>= 7) {
            out.writeByte((byte)((value & 0x7F) | 0x80));
            value = remaining;
        }
        out.writeByte((byte)(value & 0x7F));
    }
    
    public static void writeSignedLeb128(final ByteOutput out, int value) {
        int remaining = value >> 7;
        boolean hasMore = true;
        final int end = ((value & Integer.MIN_VALUE) == 0x0) ? 0 : -1;
        while (hasMore) {
            hasMore = (remaining != end || (remaining & 0x1) != (value >> 6 & 0x1));
            out.writeByte((byte)((value & 0x7F) | (hasMore ? 128 : 0)));
            value = remaining;
            remaining >>= 7;
        }
    }
}

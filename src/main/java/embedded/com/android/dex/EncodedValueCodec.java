package embedded.com.android.dex;

import embedded.com.android.dex.util.*;

public final class EncodedValueCodec
{
    public static void writeSignedIntegralValue(final ByteOutput out, final int type, long value) {
        final int requiredBits = 65 - Long.numberOfLeadingZeros(value ^ value >> 63);
        int requiredBytes = requiredBits + 7 >> 3;
        out.writeByte(type | requiredBytes - 1 << 5);
        while (requiredBytes > 0) {
            out.writeByte((byte)value);
            value >>= 8;
            --requiredBytes;
        }
    }
    
    public static void writeUnsignedIntegralValue(final ByteOutput out, final int type, long value) {
        int requiredBits = 64 - Long.numberOfLeadingZeros(value);
        if (requiredBits == 0) {
            requiredBits = 1;
        }
        int requiredBytes = requiredBits + 7 >> 3;
        out.writeByte(type | requiredBytes - 1 << 5);
        while (requiredBytes > 0) {
            out.writeByte((byte)value);
            value >>= 8;
            --requiredBytes;
        }
    }
    
    public static void writeRightZeroExtendedValue(final ByteOutput out, final int type, long value) {
        int requiredBits = 64 - Long.numberOfTrailingZeros(value);
        if (requiredBits == 0) {
            requiredBits = 1;
        }
        int requiredBytes = requiredBits + 7 >> 3;
        value >>= 64 - requiredBytes * 8;
        out.writeByte(type | requiredBytes - 1 << 5);
        while (requiredBytes > 0) {
            out.writeByte((byte)value);
            value >>= 8;
            --requiredBytes;
        }
    }
    
    public static int readSignedInt(final ByteInput in, final int zwidth) {
        int result = 0;
        for (int i = zwidth; i >= 0; --i) {
            result = (result >>> 8 | (in.readByte() & 0xFF) << 24);
        }
        result >>= (3 - zwidth) * 8;
        return result;
    }
    
    public static int readUnsignedInt(final ByteInput in, final int zwidth, final boolean fillOnRight) {
        int result = 0;
        if (!fillOnRight) {
            for (int i = zwidth; i >= 0; --i) {
                result = (result >>> 8 | (in.readByte() & 0xFF) << 24);
            }
            result >>>= (3 - zwidth) * 8;
        }
        else {
            for (int i = zwidth; i >= 0; --i) {
                result = (result >>> 8 | (in.readByte() & 0xFF) << 24);
            }
        }
        return result;
    }
    
    public static long readSignedLong(final ByteInput in, final int zwidth) {
        long result = 0L;
        for (int i = zwidth; i >= 0; --i) {
            result = (result >>> 8 | (in.readByte() & 0xFFL) << 56);
        }
        result >>= (7 - zwidth) * 8;
        return result;
    }
    
    public static long readUnsignedLong(final ByteInput in, final int zwidth, final boolean fillOnRight) {
        long result = 0L;
        if (!fillOnRight) {
            for (int i = zwidth; i >= 0; --i) {
                result = (result >>> 8 | (in.readByte() & 0xFFL) << 56);
            }
            result >>>= (7 - zwidth) * 8;
        }
        else {
            for (int i = zwidth; i >= 0; --i) {
                result = (result >>> 8 | (in.readByte() & 0xFFL) << 56);
            }
        }
        return result;
    }
}

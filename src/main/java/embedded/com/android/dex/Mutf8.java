package embedded.com.android.dex;

import embedded.com.android.dex.util.*;
import java.io.*;

public final class Mutf8
{
    public static String decode(final ByteInput in, final char[] out) throws UTFDataFormatException {
        int s = 0;
        while (true) {
            final char a = (char)(in.readByte() & 0xFF);
            if (a == '\0') {
                return new String(out, 0, s);
            }
            if ((out[s] = a) < '\u0080') {
                ++s;
            }
            else if ((a & '\u00e0') == '\u00c0') {
                final int b = in.readByte() & 0xFF;
                if ((b & 0xC0) != 0x80) {
                    throw new UTFDataFormatException("bad second byte");
                }
                out[s++] = (char)((a & '\u001f') << 6 | (b & 0x3F));
            }
            else {
                if ((a & '\u00f0') != '\u00e0') {
                    throw new UTFDataFormatException("bad byte");
                }
                final int b = in.readByte() & 0xFF;
                final int c = in.readByte() & 0xFF;
                if ((b & 0xC0) != 0x80 || (c & 0xC0) != 0x80) {
                    throw new UTFDataFormatException("bad second or third byte");
                }
                out[s++] = (char)((a & '\u000f') << 12 | (b & 0x3F) << 6 | (c & 0x3F));
            }
        }
    }
    
    private static long countBytes(final String s, final boolean shortLength) throws UTFDataFormatException {
        long result = 0L;
        for (int length = s.length(), i = 0; i < length; ++i) {
            final char ch = s.charAt(i);
            if (ch != '\0' && ch <= '\u007f') {
                ++result;
            }
            else if (ch <= '\u07ff') {
                result += 2L;
            }
            else {
                result += 3L;
            }
            if (shortLength && result > 65535L) {
                throw new UTFDataFormatException("String more than 65535 UTF bytes long");
            }
        }
        return result;
    }
    
    public static void encode(final byte[] dst, int offset, final String s) {
        for (int length = s.length(), i = 0; i < length; ++i) {
            final char ch = s.charAt(i);
            if (ch != '\0' && ch <= '\u007f') {
                dst[offset++] = (byte)ch;
            }
            else if (ch <= '\u07ff') {
                dst[offset++] = (byte)('\u00c0' | ('\u001f' & ch >> 6));
                dst[offset++] = (byte)('\u0080' | ('?' & ch));
            }
            else {
                dst[offset++] = (byte)('\u00e0' | ('\u000f' & ch >> 12));
                dst[offset++] = (byte)('\u0080' | ('?' & ch >> 6));
                dst[offset++] = (byte)('\u0080' | ('?' & ch));
            }
        }
    }
    
    public static byte[] encode(final String s) throws UTFDataFormatException {
        final int utfCount = (int)countBytes(s, true);
        final byte[] result = new byte[utfCount];
        encode(result, 0, s);
        return result;
    }
}

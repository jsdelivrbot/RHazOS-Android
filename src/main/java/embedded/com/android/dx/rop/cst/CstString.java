package embedded.com.android.dx.rop.cst;

import embedded.com.android.dx.util.*;
import embedded.com.android.dx.rop.type.*;

public final class CstString extends TypedConstant
{
    public static final CstString EMPTY_STRING;
    private final String string;
    private final ByteArray bytes;
    
    public static byte[] stringToUtf8Bytes(final String string) {
        final int len = string.length();
        final byte[] bytes = new byte[len * 3];
        int outAt = 0;
        for (int i = 0; i < len; ++i) {
            final char c = string.charAt(i);
            if (c != '\0' && c < '\u0080') {
                bytes[outAt] = (byte)c;
                ++outAt;
            }
            else if (c < '\u0800') {
                bytes[outAt] = (byte)((c >> 6 & '\u001f') | '\u00c0');
                bytes[outAt + 1] = (byte)((c & '?') | '\u0080');
                outAt += 2;
            }
            else {
                bytes[outAt] = (byte)((c >> 12 & '\u000f') | '\u00e0');
                bytes[outAt + 1] = (byte)((c >> 6 & '?') | '\u0080');
                bytes[outAt + 2] = (byte)((c & '?') | '\u0080');
                outAt += 3;
            }
        }
        final byte[] result = new byte[outAt];
        System.arraycopy(bytes, 0, result, 0, outAt);
        return result;
    }
    
    public static String utf8BytesToString(final ByteArray bytes) {
        int length = bytes.size();
        final char[] chars = new char[length];
        int outAt = 0;
        int at = 0;
        while (length > 0) {
            final int v0 = bytes.getUnsignedByte(at);
            char out = '\0';
            switch (v0 >> 4) {
                case 0:
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                case 7: {
                    --length;
                    if (v0 == 0) {
                        return throwBadUtf8(v0, at);
                    }
                    out = (char)v0;
                    ++at;
                    break;
                }
                case 12:
                case 13: {
                    length -= 2;
                    if (length < 0) {
                        return throwBadUtf8(v0, at);
                    }
                    final int v2 = bytes.getUnsignedByte(at + 1);
                    if ((v2 & 0xC0) != 0x80) {
                        return throwBadUtf8(v2, at + 1);
                    }
                    final int value = (v0 & 0x1F) << 6 | (v2 & 0x3F);
                    if (value != 0 && value < 128) {
                        return throwBadUtf8(v2, at + 1);
                    }
                    out = (char)value;
                    at += 2;
                    break;
                }
                case 14: {
                    length -= 3;
                    if (length < 0) {
                        return throwBadUtf8(v0, at);
                    }
                    final int v2 = bytes.getUnsignedByte(at + 1);
                    if ((v2 & 0xC0) != 0x80) {
                        return throwBadUtf8(v2, at + 1);
                    }
                    final int v3 = bytes.getUnsignedByte(at + 2);
                    if ((v2 & 0xC0) != 0x80) {
                        return throwBadUtf8(v3, at + 2);
                    }
                    final int value2 = (v0 & 0xF) << 12 | (v2 & 0x3F) << 6 | (v3 & 0x3F);
                    if (value2 < 2048) {
                        return throwBadUtf8(v3, at + 2);
                    }
                    out = (char)value2;
                    at += 3;
                    break;
                }
                default: {
                    return throwBadUtf8(v0, at);
                }
            }
            chars[outAt] = out;
            ++outAt;
        }
        return new String(chars, 0, outAt);
    }
    
    private static String throwBadUtf8(final int value, final int offset) {
        throw new IllegalArgumentException("bad utf-8 byte " + Hex.u1(value) + " at offset " + Hex.u4(offset));
    }
    
    public CstString(final String string) {
        if (string == null) {
            throw new NullPointerException("string == null");
        }
        this.string = string.intern();
        this.bytes = new ByteArray(stringToUtf8Bytes(string));
    }
    
    public CstString(final ByteArray bytes) {
        if (bytes == null) {
            throw new NullPointerException("bytes == null");
        }
        this.bytes = bytes;
        this.string = utf8BytesToString(bytes).intern();
    }
    
    @Override
    public boolean equals(final Object other) {
        return other instanceof CstString && this.string.equals(((CstString)other).string);
    }
    
    @Override
    public int hashCode() {
        return this.string.hashCode();
    }
    
    @Override
    protected int compareTo0(final Constant other) {
        return this.string.compareTo(((CstString)other).string);
    }
    
    @Override
    public String toString() {
        return "string{\"" + this.toHuman() + "\"}";
    }
    
    @Override
    public String typeName() {
        return "utf8";
    }
    
    @Override
    public boolean isCategory2() {
        return false;
    }
    
    @Override
    public String toHuman() {
        final int len = this.string.length();
        final StringBuilder sb = new StringBuilder(len * 3 / 2);
        for (int i = 0; i < len; ++i) {
            final char c = this.string.charAt(i);
            if (c >= ' ' && c < '\u007f') {
                if (c == '\'' || c == '\"' || c == '\\') {
                    sb.append('\\');
                }
                sb.append(c);
            }
            else if (c <= '\u007f') {
                switch (c) {
                    case '\n': {
                        sb.append("\\n");
                        break;
                    }
                    case '\r': {
                        sb.append("\\r");
                        break;
                    }
                    case '\t': {
                        sb.append("\\t");
                        break;
                    }
                    default: {
                        final char nextChar = (i < len - 1) ? this.string.charAt(i + 1) : '\0';
                        boolean displayZero = nextChar >= '0' && nextChar <= '7';
                        sb.append('\\');
                        for (int shift = 6; shift >= 0; shift -= 3) {
                            final char outChar = (char)((c >> shift & '\u0007') + '0');
                            if (outChar != '0' || displayZero) {
                                sb.append(outChar);
                                displayZero = true;
                            }
                        }
                        if (!displayZero) {
                            sb.append('0');
                            break;
                        }
                        break;
                    }
                }
            }
            else {
                sb.append("\\u");
                sb.append(Character.forDigit(c >> 12, 16));
                sb.append(Character.forDigit(c >> 8 & '\u000f', 16));
                sb.append(Character.forDigit(c >> 4 & '\u000f', 16));
                sb.append(Character.forDigit(c & '\u000f', 16));
            }
        }
        return sb.toString();
    }
    
    public String toQuoted() {
        return '\"' + this.toHuman() + '\"';
    }
    
    public String toQuoted(final int maxLength) {
        String string = this.toHuman();
        final int length = string.length();
        String ellipses;
        if (length <= maxLength - 2) {
            ellipses = "";
        }
        else {
            string = string.substring(0, maxLength - 5);
            ellipses = "...";
        }
        return '\"' + string + ellipses + '\"';
    }
    
    public String getString() {
        return this.string;
    }
    
    public ByteArray getBytes() {
        return this.bytes;
    }
    
    public int getUtf8Size() {
        return this.bytes.size();
    }
    
    public int getUtf16Size() {
        return this.string.length();
    }
    
    @Override
    public Type getType() {
        return Type.STRING;
    }
    
    static {
        EMPTY_STRING = new CstString("");
    }
}

package embedded.com.android.dx.util;

public final class HexParser
{
    public static byte[] parse(final String src) {
        final int len = src.length();
        byte[] result = new byte[len / 2];
        int at = 0;
        int outAt = 0;
        while (at < len) {
            int nlAt = src.indexOf(10, at);
            if (nlAt < 0) {
                nlAt = len;
            }
            final int poundAt = src.indexOf(35, at);
            String line;
            if (poundAt >= 0 && poundAt < nlAt) {
                line = src.substring(at, poundAt);
            }
            else {
                line = src.substring(at, nlAt);
            }
            at = nlAt + 1;
            final int colonAt = line.indexOf(58);
            if (colonAt != -1) {
                final int quoteAt = line.indexOf(34);
                if (quoteAt == -1 || quoteAt >= colonAt) {
                    final String atStr = line.substring(0, colonAt).trim();
                    line = line.substring(colonAt + 1);
                    final int alleged = Integer.parseInt(atStr, 16);
                    if (alleged != outAt) {
                        throw new RuntimeException("bogus offset marker: " + atStr);
                    }
                }
            }
            final int lineLen = line.length();
            int value = -1;
            boolean quoteMode = false;
            for (int i = 0; i < lineLen; ++i) {
                final char c = line.charAt(i);
                if (quoteMode) {
                    if (c == '\"') {
                        quoteMode = false;
                    }
                    else {
                        result[outAt] = (byte)c;
                        ++outAt;
                    }
                }
                else if (c > ' ') {
                    if (c == '\"') {
                        if (value != -1) {
                            throw new RuntimeException("spare digit around offset " + Hex.u4(outAt));
                        }
                        quoteMode = true;
                    }
                    else {
                        final int digVal = Character.digit(c, 16);
                        if (digVal == -1) {
                            throw new RuntimeException("bogus digit character: \"" + c + "\"");
                        }
                        if (value == -1) {
                            value = digVal;
                        }
                        else {
                            result[outAt] = (byte)(value << 4 | digVal);
                            ++outAt;
                            value = -1;
                        }
                    }
                }
            }
            if (value != -1) {
                throw new RuntimeException("spare digit around offset " + Hex.u4(outAt));
            }
            if (quoteMode) {
                throw new RuntimeException("unterminated quote around offset " + Hex.u4(outAt));
            }
        }
        if (outAt < result.length) {
            final byte[] newr = new byte[outAt];
            System.arraycopy(result, 0, newr, 0, outAt);
            result = newr;
        }
        return result;
    }
}

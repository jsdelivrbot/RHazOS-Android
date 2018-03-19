package embedded.com.android.dx.util;

public final class Bits
{
    public static int[] makeBitSet(final int max) {
        final int size = max + 31 >> 5;
        return new int[size];
    }
    
    public static int getMax(final int[] bits) {
        return bits.length * 32;
    }
    
    public static boolean get(final int[] bits, final int idx) {
        final int arrayIdx = idx >> 5;
        final int bit = 1 << (idx & 0x1F);
        return (bits[arrayIdx] & bit) != 0x0;
    }
    
    public static void set(final int[] bits, final int idx, final boolean value) {
        final int arrayIdx = idx >> 5;
        final int bit = 1 << (idx & 0x1F);
        if (value) {
            final int n = arrayIdx;
            bits[n] |= bit;
        }
        else {
            final int n2 = arrayIdx;
            bits[n2] &= ~bit;
        }
    }
    
    public static void set(final int[] bits, final int idx) {
        final int arrayIdx = idx >> 5;
        final int bit = 1 << (idx & 0x1F);
        final int n = arrayIdx;
        bits[n] |= bit;
    }
    
    public static void clear(final int[] bits, final int idx) {
        final int arrayIdx = idx >> 5;
        final int bit = 1 << (idx & 0x1F);
        final int n = arrayIdx;
        bits[n] &= ~bit;
    }
    
    public static boolean isEmpty(final int[] bits) {
        for (int len = bits.length, i = 0; i < len; ++i) {
            if (bits[i] != 0) {
                return false;
            }
        }
        return true;
    }
    
    public static int bitCount(final int[] bits) {
        final int len = bits.length;
        int count = 0;
        for (int i = 0; i < len; ++i) {
            count += Integer.bitCount(bits[i]);
        }
        return count;
    }
    
    public static boolean anyInRange(final int[] bits, final int start, final int end) {
        final int idx = findFirst(bits, start);
        return idx >= 0 && idx < end;
    }
    
    public static int findFirst(final int[] bits, final int idx) {
        final int len = bits.length;
        int minBit = idx & 0x1F;
        for (int arrayIdx = idx >> 5; arrayIdx < len; ++arrayIdx) {
            final int word = bits[arrayIdx];
            if (word != 0) {
                final int bitIdx = findFirst(word, minBit);
                if (bitIdx >= 0) {
                    return (arrayIdx << 5) + bitIdx;
                }
            }
            minBit = 0;
        }
        return -1;
    }
    
    public static int findFirst(int value, final int idx) {
        value &= ~((1 << idx) - 1);
        final int result = Integer.numberOfTrailingZeros(value);
        return (result == 32) ? -1 : result;
    }
    
    public static void or(final int[] a, final int[] b) {
        for (int i = 0; i < b.length; ++i) {
            final int n = i;
            a[n] |= b[i];
        }
    }
    
    public static String toHuman(final int[] bits) {
        final StringBuilder sb = new StringBuilder();
        boolean needsComma = false;
        sb.append('{');
        for (int bitsLength = 32 * bits.length, i = 0; i < bitsLength; ++i) {
            if (get(bits, i)) {
                if (needsComma) {
                    sb.append(',');
                }
                needsComma = true;
                sb.append(i);
            }
        }
        sb.append('}');
        return sb.toString();
    }
}

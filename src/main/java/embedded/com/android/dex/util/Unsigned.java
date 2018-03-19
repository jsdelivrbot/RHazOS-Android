package embedded.com.android.dex.util;

public final class Unsigned
{
    public static int compare(final short ushortA, final short ushortB) {
        if (ushortA == ushortB) {
            return 0;
        }
        final int a = ushortA & 0xFFFF;
        final int b = ushortB & 0xFFFF;
        return (a < b) ? -1 : 1;
    }
    
    public static int compare(final int uintA, final int uintB) {
        if (uintA == uintB) {
            return 0;
        }
        final long a = uintA & 0xFFFFFFFFL;
        final long b = uintB & 0xFFFFFFFFL;
        return (a < b) ? -1 : 1;
    }
}

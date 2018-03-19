package embedded.com.android.dex.util;

import java.io.*;

public final class FileUtils
{
    public static byte[] readFile(final String fileName) {
        final File file = new File(fileName);
        return readFile(file);
    }
    
    public static byte[] readFile(final File file) {
        if (!file.exists()) {
            throw new RuntimeException(file + ": file not found");
        }
        if (!file.isFile()) {
            throw new RuntimeException(file + ": not a file");
        }
        if (!file.canRead()) {
            throw new RuntimeException(file + ": file not readable");
        }
        final long longLength = file.length();
        int length = (int)longLength;
        if (length != longLength) {
            throw new RuntimeException(file + ": file too long");
        }
        final byte[] result = new byte[length];
        try {
            final FileInputStream in = new FileInputStream(file);
            int at = 0;
            while (length > 0) {
                final int amt = in.read(result, at, length);
                if (amt == -1) {
                    throw new RuntimeException(file + ": unexpected EOF");
                }
                at += amt;
                length -= amt;
            }
            in.close();
        }
        catch (IOException ex) {
            throw new RuntimeException(file + ": trouble reading", ex);
        }
        return result;
    }
    
    public static boolean hasArchiveSuffix(final String fileName) {
        return fileName.endsWith(".zip") || fileName.endsWith(".jar") || fileName.endsWith(".apk");
    }
}

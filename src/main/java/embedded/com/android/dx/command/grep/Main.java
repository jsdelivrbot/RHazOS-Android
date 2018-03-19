package embedded.com.android.dx.command.grep;

import embedded.com.android.dex.*;
import java.util.regex.*;
import java.io.*;

public final class Main
{
    public static void main(final String[] args) throws IOException {
        final String dexFile = args[0];
        final String pattern = args[1];
        final Dex dex = new Dex(new File(dexFile));
        final int count = new Grep(dex, Pattern.compile(pattern), new PrintWriter(System.out)).grep();
        System.exit((count <= 0) ? 1 : 0);
    }
}

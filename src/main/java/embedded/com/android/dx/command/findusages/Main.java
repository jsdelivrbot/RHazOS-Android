package embedded.com.android.dx.command.findusages;

import embedded.com.android.dex.*;
import java.io.*;

public final class Main
{
    public static void main(final String[] args) throws IOException {
        final String dexFile = args[0];
        final String declaredBy = args[1];
        final String memberName = args[2];
        final Dex dex = new Dex(new File(dexFile));
        final PrintWriter out = new PrintWriter(System.out);
        new FindUsages(dex, declaredBy, memberName, out).findUsages();
        out.flush();
    }
}

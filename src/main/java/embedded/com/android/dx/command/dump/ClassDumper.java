package embedded.com.android.dx.command.dump;

import java.io.*;
import embedded.com.android.dx.util.*;
import embedded.com.android.dx.cf.direct.*;
import embedded.com.android.dx.cf.iface.*;

public final class ClassDumper extends BaseDumper
{
    public static void dump(final byte[] bytes, final PrintStream out, final String filePath, final Args args) {
        final ClassDumper cd = new ClassDumper(bytes, out, filePath, args);
        cd.dump();
    }
    
    private ClassDumper(final byte[] bytes, final PrintStream out, final String filePath, final Args args) {
        super(bytes, out, filePath, args);
    }
    
    public void dump() {
        final byte[] bytes = this.getBytes();
        final ByteArray ba = new ByteArray(bytes);
        final DirectClassFile cf = new DirectClassFile(ba, this.getFilePath(), this.getStrictParse());
        cf.setAttributeFactory(StdAttributeFactory.THE_ONE);
        cf.setObserver(this);
        cf.getMagic();
        final int at = this.getAt();
        if (at != bytes.length) {
            this.parsed(ba, at, bytes.length - at, "<extra data at end of file>");
        }
    }
}

package fr.rhaz.os.android;

import java.io.File;

public class Deodexer {

    public static void deodex(File outdir, File file){
        File out = new File(outdir, file.getName());
        embedded.com.android.dx.command.Main.main(new String[]{"--dex", "--output=" + out.getPath(), file.getPath()});
    }
}

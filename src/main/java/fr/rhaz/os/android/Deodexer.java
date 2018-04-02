package fr.rhaz.os.android;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.jar.JarFile;

public class Deodexer {

    public static void deodex(File outdir, File file) throws Exception {
        try {
            File out = new File(outdir, file.getName());

            if(new JarFile(file).getEntry("classes.dex") != null) {
                if (!file.getPath().equals(out.getPath())) FileUtils.copyFile(file, out);
                return;
            }

            if(file.length() > 100000)
                throw new Exception("too big");

            embedded.com.android.dx.command.Main.main(new String[]{"--dex", "--core-library", "--keep-classes", "--output=" + out.getPath(), file.getPath()});
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

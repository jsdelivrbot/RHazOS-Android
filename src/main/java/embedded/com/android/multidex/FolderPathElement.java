package embedded.com.android.multidex;

import java.io.*;
import java.util.*;

class FolderPathElement implements ClassPathElement
{
    private File baseFolder;
    
    public FolderPathElement(final File baseFolder) {
        this.baseFolder = baseFolder;
    }
    
    @Override
    public InputStream open(final String path) throws FileNotFoundException {
        return new FileInputStream(new File(this.baseFolder, path.replace('/', File.separatorChar)));
    }
    
    @Override
    public void close() {
    }
    
    @Override
    public Iterable<String> list() {
        final ArrayList<String> result = new ArrayList<String>();
        this.collect(this.baseFolder, "", result);
        return result;
    }
    
    private void collect(final File folder, final String prefix, final ArrayList<String> result) {
        for (final File file : folder.listFiles()) {
            if (file.isDirectory()) {
                this.collect(file, prefix + '/' + file.getName(), result);
            }
            else {
                result.add(prefix + '/' + file.getName());
            }
        }
    }
}

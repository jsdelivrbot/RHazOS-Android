package embedded.com.android.multidex;

import java.util.zip.*;
import java.util.regex.*;
import java.io.*;
import embedded.com.android.dx.cf.direct.*;
import java.util.*;

class Path
{
    List<ClassPathElement> elements;
    private final String definition;
    private final ByteArrayOutputStream baos;
    private final byte[] readBuffer;
    
    static ClassPathElement getClassPathElement(final File file) throws ZipException, IOException {
        if (file.isDirectory()) {
            return new FolderPathElement(file);
        }
        if (file.isFile()) {
            return new ArchivePathElement(new ZipFile(file));
        }
        if (file.exists()) {
            throw new IOException("\"" + file.getPath() + "\" is not a directory neither a zip file");
        }
        throw new FileNotFoundException("File \"" + file.getPath() + "\" not found");
    }
    
    Path(final String definition) throws IOException {
        this.elements = new ArrayList<ClassPathElement>();
        this.baos = new ByteArrayOutputStream(40960);
        this.readBuffer = new byte[20480];
        this.definition = definition;
        for (final String filePath : definition.split(Pattern.quote(File.pathSeparator))) {
            try {
                this.addElement(getClassPathElement(new File(filePath)));
            }
            catch (IOException e) {
                throw new IOException("Wrong classpath: " + e.getMessage(), e);
            }
        }
    }
    
    private static byte[] readStream(final InputStream in, final ByteArrayOutputStream baos, final byte[] readBuffer) throws IOException {
        try {
            while (true) {
                final int amt = in.read(readBuffer);
                if (amt < 0) {
                    break;
                }
                baos.write(readBuffer, 0, amt);
            }
        }
        finally {
            in.close();
        }
        return baos.toByteArray();
    }
    
    @Override
    public String toString() {
        return this.definition;
    }
    
    Iterable<ClassPathElement> getElements() {
        return this.elements;
    }
    
    private void addElement(final ClassPathElement element) {
        assert element != null;
        this.elements.add(element);
    }
    
    synchronized DirectClassFile getClass(final String path) throws FileNotFoundException {
        DirectClassFile classFile = null;
        for (final ClassPathElement element : this.elements) {
            try {
                final InputStream in = element.open(path);
                try {
                    final byte[] bytes = readStream(in, this.baos, this.readBuffer);
                    this.baos.reset();
                    classFile = new DirectClassFile(bytes, path, false);
                    classFile.setAttributeFactory(StdAttributeFactory.THE_ONE);
                }
                finally {
                    in.close();
                }
            }
            catch (IOException ex) {
                continue;
            }
            break;
        }
        if (classFile == null) {
            throw new FileNotFoundException("File \"" + path + "\" not found");
        }
        return classFile;
    }
}

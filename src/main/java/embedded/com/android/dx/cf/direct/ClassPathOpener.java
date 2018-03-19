package embedded.com.android.dx.cf.direct;

import embedded.com.android.dex.util.*;
import java.util.zip.*;
import java.util.*;
import java.io.*;

public class ClassPathOpener
{
    private final String pathname;
    private final Consumer consumer;
    private final boolean sort;
    private FileNameFilter filter;
    public static final FileNameFilter acceptAll;
    
    public ClassPathOpener(final String pathname, final boolean sort, final Consumer consumer) {
        this(pathname, sort, ClassPathOpener.acceptAll, consumer);
    }
    
    public ClassPathOpener(final String pathname, final boolean sort, final FileNameFilter filter, final Consumer consumer) {
        this.pathname = pathname;
        this.sort = sort;
        this.consumer = consumer;
        this.filter = filter;
    }
    
    public boolean process() {
        final File file = new File(this.pathname);
        return this.processOne(file, true);
    }
    
    private boolean processOne(final File file, final boolean topLevel) {
        try {
            if (file.isDirectory()) {
                return this.processDirectory(file, topLevel);
            }
            final String path = file.getPath();
            if (path.endsWith(".zip") || path.endsWith(".jar") || path.endsWith(".apk")) {
                return this.processArchive(file);
            }
            if (this.filter.accept(path)) {
                final byte[] bytes = FileUtils.readFile(file);
                return this.consumer.processFileBytes(path, file.lastModified(), bytes);
            }
            return false;
        }
        catch (Exception ex) {
            this.consumer.onException(ex);
            return false;
        }
    }
    
    private static int compareClassNames(String a, String b) {
        a = a.replace('$', '0');
        b = b.replace('$', '0');
        a = a.replace("package-info", "");
        b = b.replace("package-info", "");
        return a.compareTo(b);
    }
    
    private boolean processDirectory(File dir, final boolean topLevel) {
        if (topLevel) {
            dir = new File(dir, ".");
        }
        final File[] files = dir.listFiles();
        final int len = files.length;
        boolean any = false;
        if (this.sort) {
            Arrays.sort(files, new Comparator<File>() {
                @Override
                public int compare(final File a, final File b) {
                    return compareClassNames(a.getName(), b.getName());
                }
            });
        }
        for (int i = 0; i < len; ++i) {
            any |= this.processOne(files[i], false);
        }
        return any;
    }
    
    private boolean processArchive(final File file) throws IOException {
        final ZipFile zip = new ZipFile(file);
        final ArrayList<? extends ZipEntry> entriesList = Collections.list(zip.entries());
        if (this.sort) {
            Collections.sort(entriesList, new Comparator<ZipEntry>() {
                @Override
                public int compare(final ZipEntry a, final ZipEntry b) {
                    return compareClassNames(a.getName(), b.getName());
                }
            });
        }
        this.consumer.onProcessArchiveStart(file);
        final ByteArrayOutputStream baos = new ByteArrayOutputStream(40000);
        final byte[] buf = new byte[20000];
        boolean any = false;
        for (final ZipEntry one : entriesList) {
            final boolean isDirectory = one.isDirectory();
            final String path = one.getName();
            if (this.filter.accept(path)) {
                byte[] bytes;
                if (!isDirectory) {
                    final InputStream in = zip.getInputStream(one);
                    baos.reset();
                    int read;
                    while ((read = in.read(buf)) != -1) {
                        baos.write(buf, 0, read);
                    }
                    in.close();
                    bytes = baos.toByteArray();
                }
                else {
                    bytes = new byte[0];
                }
                any |= this.consumer.processFileBytes(path, one.getTime(), bytes);
            }
        }
        zip.close();
        return any;
    }
    
    static {
        acceptAll = new FileNameFilter() {
            @Override
            public boolean accept(final String path) {
                return true;
            }
        };
    }
    
    public interface FileNameFilter
    {
        boolean accept(final String p0);
    }
    
    public interface Consumer
    {
        boolean processFileBytes(final String p0, final long p1, final byte[] p2);
        
        void onException(final Exception p0);
        
        void onProcessArchiveStart(final File p0);
    }
}

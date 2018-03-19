package embedded.com.android.multidex;

import java.util.zip.*;
import java.io.*;
import java.util.*;

class ArchivePathElement implements ClassPathElement
{
    private final ZipFile archive;
    
    public ArchivePathElement(final ZipFile archive) {
        this.archive = archive;
    }
    
    @Override
    public InputStream open(final String path) throws IOException {
        final ZipEntry entry = this.archive.getEntry(path);
        if (entry == null) {
            throw new FileNotFoundException("File \"" + path + "\" not found");
        }
        if (entry.isDirectory()) {
            throw new DirectoryEntryException();
        }
        return this.archive.getInputStream(entry);
    }
    
    @Override
    public void close() throws IOException {
        this.archive.close();
    }
    
    @Override
    public Iterable<String> list() {
        return new Iterable<String>() {
            @Override
            public Iterator<String> iterator() {
                return new Iterator<String>() {
                    Enumeration<? extends ZipEntry> delegate = ArchivePathElement.this.archive.entries();
                    ZipEntry next = null;
                    
                    @Override
                    public boolean hasNext() {
                        while (this.next == null && this.delegate.hasMoreElements()) {
                            this.next = (ZipEntry)this.delegate.nextElement();
                            if (this.next.isDirectory()) {
                                this.next = null;
                            }
                        }
                        return this.next != null;
                    }
                    
                    @Override
                    public String next() {
                        if (this.hasNext()) {
                            final String name = this.next.getName();
                            this.next = null;
                            return name;
                        }
                        throw new NoSuchElementException();
                    }
                    
                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }
    
    static class DirectoryEntryException extends IOException
    {
    }
}

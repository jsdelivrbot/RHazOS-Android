package embedded.com.android.multidex;

import java.util.zip.*;
import java.util.*;
import embedded.com.android.dx.cf.direct.*;
import java.io.*;
import embedded.com.android.dx.cf.attrib.*;
import embedded.com.android.dx.cf.iface.*;

public class MainDexListBuilder
{
    private static final String CLASS_EXTENSION = ".class";
    private static final int STATUS_ERROR = 1;
    private static final String EOL;
    private static String USAGE_MESSAGE;
    private static final String DISABLE_ANNOTATION_RESOLUTION_WORKAROUND = "--disable-annotation-resolution-workaround";
    private Set<String> filesToKeep;
    
    public static void main(final String[] args) {
        int argIndex = 0;
        boolean keepAnnotated = true;
        while (argIndex < args.length - 2) {
            if (args[argIndex].equals("--disable-annotation-resolution-workaround")) {
                keepAnnotated = false;
            }
            else {
                System.err.println("Invalid option " + args[argIndex]);
                printUsage();
                System.exit(1);
            }
            ++argIndex;
        }
        if (args.length - argIndex != 2) {
            printUsage();
            System.exit(1);
        }
        try {
            final MainDexListBuilder builder = new MainDexListBuilder(keepAnnotated, args[argIndex], args[argIndex + 1]);
            final Set<String> toKeep = builder.getMainDexList();
            printList(toKeep);
        }
        catch (IOException e) {
            System.err.println("A fatal error occured: " + e.getMessage());
            System.exit(1);
        }
    }
    
    public MainDexListBuilder(final boolean keepAnnotated, final String rootJar, final String pathString) throws IOException {
        this.filesToKeep = new HashSet<String>();
        ZipFile jarOfRoots = null;
        Path path = null;
        try {
            try {
                jarOfRoots = new ZipFile(rootJar);
            }
            catch (IOException e) {
                throw new IOException("\"" + rootJar + "\" can not be read as a zip archive. (" + e.getMessage() + ")", e);
            }
            path = new Path(pathString);
            final ClassReferenceListBuilder mainListBuilder = new ClassReferenceListBuilder(path);
            mainListBuilder.addRoots(jarOfRoots);
            for (final String className : mainListBuilder.getClassNames()) {
                this.filesToKeep.add(className + ".class");
            }
            if (keepAnnotated) {
                this.keepAnnotated(path);
            }
        }
        finally {
            try {
                jarOfRoots.close();
            }
            catch (IOException ex) {}
            if (path != null) {
                for (final ClassPathElement element : path.elements) {
                    try {
                        element.close();
                    }
                    catch (IOException ex2) {}
                }
            }
        }
    }
    
    public Set<String> getMainDexList() {
        return this.filesToKeep;
    }
    
    private static void printUsage() {
        System.err.print(MainDexListBuilder.USAGE_MESSAGE);
    }
    
    private static void printList(final Set<String> fileNames) {
        for (final String fileName : fileNames) {
            System.out.println(fileName);
        }
    }
    
    private void keepAnnotated(final Path path) throws FileNotFoundException {
        for (final ClassPathElement element : path.getElements()) {
        Label_0042:
            for (final String name : element.list()) {
                if (name.endsWith(".class")) {
                    final DirectClassFile clazz = path.getClass(name);
                    if (this.hasRuntimeVisibleAnnotation(clazz)) {
                        this.filesToKeep.add(name);
                    }
                    else {
                        final MethodList methods = clazz.getMethods();
                        for (int i = 0; i < methods.size(); ++i) {
                            if (this.hasRuntimeVisibleAnnotation(methods.get(i))) {
                                this.filesToKeep.add(name);
                                continue Label_0042;
                            }
                        }
                        final FieldList fields = clazz.getFields();
                        for (int j = 0; j < fields.size(); ++j) {
                            if (this.hasRuntimeVisibleAnnotation(fields.get(j))) {
                                this.filesToKeep.add(name);
                                break;
                            }
                        }
                    }
                }
            }
        }
    }
    
    private boolean hasRuntimeVisibleAnnotation(final HasAttribute element) {
        final Attribute att = element.getAttributes().findFirst("RuntimeVisibleAnnotations");
        return att != null && ((AttRuntimeVisibleAnnotations)att).getAnnotations().size() > 0;
    }
    
    static {
        EOL = System.getProperty("line.separator");
        MainDexListBuilder.USAGE_MESSAGE = "Usage:" + MainDexListBuilder.EOL + MainDexListBuilder.EOL + "Short version: Don't use this." + MainDexListBuilder.EOL + MainDexListBuilder.EOL + "Slightly longer version: This tool is used by mainDexClasses script to build" + MainDexListBuilder.EOL + "the main dex list." + MainDexListBuilder.EOL;
    }
}

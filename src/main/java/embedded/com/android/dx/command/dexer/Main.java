package embedded.com.android.dx.command.dexer;

import java.util.concurrent.atomic.*;

import embedded.com.android.dx.rop.annotation.Annotation;
import embedded.com.android.dx.rop.code.*;
import embedded.com.android.dx.rop.type.*;
import embedded.com.android.dex.*;
import embedded.com.android.dx.merge.*;
import java.util.concurrent.*;
import embedded.com.android.dx.cf.iface.*;
import embedded.com.android.dx.cf.direct.*;
import java.util.zip.*;
import java.util.jar.*;
import embedded.com.android.dx.dex.file.*;
import java.io.*;
import embedded.com.android.dx.rop.cst.*;
import embedded.com.android.dx.rop.annotation.*;
import java.util.*;
import embedded.com.android.dx.dex.cf.*;
import embedded.com.android.dx.dex.*;
import embedded.com.android.dx.command.*;
import embedded.com.android.dex.util.*;
import embedded.com.android.dx.cf.code.*;

public class Main
{
    public static final int CONCURRENCY_LEVEL = 4;
    private static final String DEX_EXTENSION = ".dex";
    private static final String DEX_PREFIX = "classes";
    private static final String IN_RE_CORE_CLASSES = "Ill-advised or mistaken usage of a core class (java.* or javax.*)\nwhen not building a core library.\n\nThis is often due to inadvertently including a core library file\nin your application's project, when using an IDE (such as\nEclipse). If you are sure you're not intentionally defining a\ncore class, then this is the most likely explanation of what's\ngoing on.\n\nHowever, you might actually be trying to define a class in a core\nnamespace, the source of which you may have taken, for example,\nfrom a non-Android virtual machine project. This will most\nassuredly not work. At a minimum, it jeopardizes the\ncompatibility of your app with future versions of the platform.\nIt is also often of questionable legality.\n\nIf you really intend to build a core library -- which is only\nappropriate as part of creating a full virtual machine\ndistribution, as opposed to compiling an application -- then use\nthe \"--core-library\" option to suppress this error message.\n\nIf you go ahead and use \"--core-library\" but are in fact\nbuilding an application, then be forewarned that your application\nwill still fail to build or run, at some point. Please be\nprepared for angry customers who find, for example, that your\napplication ceases to function once they upgrade their operating\nsystem. You will be to blame for this problem.\n\nIf you are legitimately using some code that happens to be in a\ncore package, then the easiest safe alternative you have is to\nrepackage that code. That is, move the classes in question into\nyour own package namespace. This means that they will never be in\nconflict with core system classes. JarJar is a tool that may help\nyou in this endeavor. If you find that you cannot do this, then\nthat is an indication that the path you are on will ultimately\nlead to pain, suffering, grief, and lamentation.\n";
    private static final String MANIFEST_NAME = "META-INF/MANIFEST.MF";
    private static final Attributes.Name CREATED_BY;
    private static final String[] JAVAX_CORE;
    private static final int MAX_METHOD_ADDED_DURING_DEX_CREATION = 2;
    private static final int MAX_FIELD_ADDED_DURING_DEX_CREATION = 9;
    private AtomicInteger errors;
    private Arguments args;
    private DexFile outputDex;
    private TreeMap<String, byte[]> outputResources;
    private final List<byte[]> libraryDexBuffers;
    private ExecutorService classTranslatorPool;
    private ExecutorService classDefItemConsumer;
    private List<Future<Boolean>> addToDexFutures;
    private ExecutorService dexOutPool;
    private List<Future<byte[]>> dexOutputFutures;
    private Object dexRotationLock;
    private int maxMethodIdsInProcess;
    private int maxFieldIdsInProcess;
    private volatile boolean anyFilesProcessed;
    private long minimumFileAge;
    private Set<String> classesInMainDex;
    private List<byte[]> dexOutputArrays;
    private OutputStreamWriter humanOutWriter;
    private final DxContext context;
    
    public Main(final DxContext context) {
        this.errors = new AtomicInteger(0);
        this.libraryDexBuffers = new ArrayList<byte[]>();
        this.addToDexFutures = new ArrayList<Future<Boolean>>();
        this.dexOutputFutures = new ArrayList<Future<byte[]>>();
        this.dexRotationLock = new Object();
        this.maxMethodIdsInProcess = 0;
        this.maxFieldIdsInProcess = 0;
        this.minimumFileAge = 0L;
        this.classesInMainDex = null;
        this.dexOutputArrays = new ArrayList<byte[]>();
        this.humanOutWriter = null;
        this.context = context;
    }
    
    public static void main(final String[] argArray) throws IOException {
        final DxContext context = new DxContext();
        final Arguments arguments = new Arguments(context);
        arguments.parse(argArray);
        final int result = new Main(context).runDx(arguments);
        if (result != 0) {
            System.exit(result);
        }
    }
    
    public static void clearInternTables() {
        Prototype.clearInternTable();
        RegisterSpec.clearInternTable();
        CstType.clearInternTable();
        Type.clearInternTable();
    }
    
    public static int run(final Arguments arguments) throws IOException {
        return new Main(new DxContext()).runDx(arguments);
    }
    
    public int runDx(final Arguments arguments) throws IOException {
        this.errors.set(0);
        this.libraryDexBuffers.clear();
        (this.args = arguments).makeOptionsObjects();
        OutputStream humanOutRaw = null;
        if (this.args.humanOutName != null) {
            humanOutRaw = this.openOutput(this.args.humanOutName);
            this.humanOutWriter = new OutputStreamWriter(humanOutRaw);
        }
        try {
            if (this.args.multiDex) {
                return this.runMultiDex();
            }
            return this.runMonoDex();
        }
        finally {
            this.closeOutput(humanOutRaw);
        }
    }
    
    private int runMonoDex() throws IOException {
        File incrementalOutFile = null;
        if (this.args.incremental) {
            if (this.args.outName == null) {
                this.context.err.println("error: no incremental output name specified");
                return -1;
            }
            incrementalOutFile = new File(this.args.outName);
            if (incrementalOutFile.exists()) {
                this.minimumFileAge = incrementalOutFile.lastModified();
            }
        }
        if (!this.processAllFiles()) {
            return 1;
        }
        if (this.args.incremental && !this.anyFilesProcessed) {
            return 0;
        }
        byte[] outArray = null;
        if (!this.outputDex.isEmpty() || this.args.humanOutName != null) {
            outArray = this.writeDex(this.outputDex);
            if (outArray == null) {
                return 2;
            }
        }
        if (this.args.incremental) {
            outArray = this.mergeIncremental(outArray, incrementalOutFile);
        }
        outArray = this.mergeLibraryDexBuffers(outArray);
        if (this.args.jarOutput) {
            this.outputDex = null;
            if (outArray != null) {
                this.outputResources.put("classes.dex", outArray);
            }
            if (!this.createJar(this.args.outName)) {
                return 3;
            }
        }
        else if (outArray != null && this.args.outName != null) {
            final OutputStream out = this.openOutput(this.args.outName);
            out.write(outArray);
            this.closeOutput(out);
        }
        return 0;
    }
    
    private int runMultiDex() throws IOException {
        assert !this.args.incremental;
        if (this.args.mainDexListFile != null) {
            this.classesInMainDex = new HashSet<String>();
            readPathsFromFile(this.args.mainDexListFile, this.classesInMainDex);
        }
        this.dexOutPool = Executors.newFixedThreadPool(this.args.numThreads);
        if (!this.processAllFiles()) {
            return 1;
        }
        if (!this.libraryDexBuffers.isEmpty()) {
            throw new DexException("Library dex files are not supported in multi-dex mode");
        }
        if (this.outputDex != null) {
            this.dexOutputFutures.add(this.dexOutPool.submit((Callable<byte[]>)new DexWriter(this.outputDex)));
            this.outputDex = null;
        }
        try {
            this.dexOutPool.shutdown();
            if (!this.dexOutPool.awaitTermination(600L, TimeUnit.SECONDS)) {
                throw new RuntimeException("Timed out waiting for dex writer threads.");
            }
            for (final Future<byte[]> f : this.dexOutputFutures) {
                this.dexOutputArrays.add(f.get());
            }
        }
        catch (InterruptedException ex) {
            this.dexOutPool.shutdownNow();
            throw new RuntimeException("A dex writer thread has been interrupted.");
        }
        catch (Exception e) {
            this.dexOutPool.shutdownNow();
            throw new RuntimeException("Unexpected exception in dex writer thread");
        }
        if (this.args.jarOutput) {
            for (int i = 0; i < this.dexOutputArrays.size(); ++i) {
                this.outputResources.put(getDexFileName(i), this.dexOutputArrays.get(i));
            }
            if (!this.createJar(this.args.outName)) {
                return 3;
            }
        }
        else if (this.args.outName != null) {
            final File outDir = new File(this.args.outName);
            assert outDir.isDirectory();
            for (int j = 0; j < this.dexOutputArrays.size(); ++j) {
                final OutputStream out = new FileOutputStream(new File(outDir, getDexFileName(j)));
                try {
                    out.write(this.dexOutputArrays.get(j));
                }
                finally {
                    this.closeOutput(out);
                }
            }
        }
        return 0;
    }
    
    private static String getDexFileName(final int i) {
        if (i == 0) {
            return "classes.dex";
        }
        return "classes" + (i + 1) + ".dex";
    }
    
    private static void readPathsFromFile(final String fileName, final Collection<String> paths) throws IOException {
        BufferedReader bfr = null;
        try {
            final FileReader fr = new FileReader(fileName);
            bfr = new BufferedReader(fr);
            String line;
            while (null != (line = bfr.readLine())) {
                paths.add(fixPath(line));
            }
        }
        finally {
            if (bfr != null) {
                bfr.close();
            }
        }
    }
    
    private byte[] mergeIncremental(final byte[] update, final File base) throws IOException {
        Dex dexA = null;
        Dex dexB = null;
        if (update != null) {
            dexA = new Dex(update);
        }
        if (base.exists()) {
            dexB = new Dex(base);
        }
        if (dexA == null && dexB == null) {
            return null;
        }
        Dex result;
        if (dexA == null) {
            result = dexB;
        }
        else if (dexB == null) {
            result = dexA;
        }
        else {
            result = new DexMerger(new Dex[] { dexA, dexB }, CollisionPolicy.KEEP_FIRST, this.context).merge();
        }
        final ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
        result.writeTo(bytesOut);
        return bytesOut.toByteArray();
    }
    
    private byte[] mergeLibraryDexBuffers(final byte[] outArray) throws IOException {
        final ArrayList<Dex> dexes = new ArrayList<Dex>();
        if (outArray != null) {
            dexes.add(new Dex(outArray));
        }
        for (final byte[] libraryDex : this.libraryDexBuffers) {
            dexes.add(new Dex(libraryDex));
        }
        if (dexes.isEmpty()) {
            return null;
        }
        final Dex merged = new DexMerger(dexes.toArray(new Dex[dexes.size()]), CollisionPolicy.FAIL, this.context).merge();
        return merged.getBytes();
    }
    
    private boolean processAllFiles() {
        this.createDexFile();
        if (this.args.jarOutput) {
            this.outputResources = new TreeMap<String, byte[]>();
        }
        this.anyFilesProcessed = false;
        final String[] fileNames = this.args.fileNames;
        Arrays.sort(fileNames);
        this.classTranslatorPool = new ThreadPoolExecutor(this.args.numThreads, this.args.numThreads, 0L, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(2 * this.args.numThreads, true), new ThreadPoolExecutor.CallerRunsPolicy());
        this.classDefItemConsumer = Executors.newSingleThreadExecutor();
        try {
            if (this.args.mainDexListFile != null) {
                final ClassPathOpener.FileNameFilter mainPassFilter = this.args.strictNameCheck ? new MainDexListFilter() : new BestEffortMainDexListFilter();
                for (int i = 0; i < fileNames.length; ++i) {
                    this.processOne(fileNames[i], mainPassFilter);
                }
                if (this.dexOutputFutures.size() > 0) {
                    throw new DexException("Too many classes in --main-dex-list, main dex capacity exceeded");
                }
                if (this.args.minimalMainDex) {
                    synchronized (this.dexRotationLock) {
                        while (true) {
                            if (this.maxMethodIdsInProcess <= 0) {
                                if (this.maxFieldIdsInProcess <= 0) {
                                    break;
                                }
                            }
                            try {
                                this.dexRotationLock.wait();
                            }
                            catch (InterruptedException ex2) {}
                        }
                    }
                    this.rotateDexFile();
                }
                for (int i = 0; i < fileNames.length; ++i) {
                    this.processOne(fileNames[i], new NotFilter(mainPassFilter));
                }
            }
            else {
                for (int j = 0; j < fileNames.length; ++j) {
                    this.processOne(fileNames[j], ClassPathOpener.acceptAll);
                }
            }
        }
        catch (StopProcessing stopProcessing) {}
        try {
            this.classTranslatorPool.shutdown();
            this.classTranslatorPool.awaitTermination(600L, TimeUnit.SECONDS);
            this.classDefItemConsumer.shutdown();
            this.classDefItemConsumer.awaitTermination(600L, TimeUnit.SECONDS);
            for (final Future<Boolean> f : this.addToDexFutures) {
                try {
                    f.get();
                }
                catch (ExecutionException ex) {
                    final int count = this.errors.incrementAndGet();
                    if (count >= 10) {
                        throw new InterruptedException("Too many errors");
                    }
                    if (this.args.debug) {
                        this.context.err.println("Uncaught translation error:");
                        ex.getCause().printStackTrace(this.context.err);
                    }
                    else {
                        this.context.err.println("Uncaught translation error: " + ex.getCause());
                    }
                }
            }
        }
        catch (InterruptedException ie) {
            this.classTranslatorPool.shutdownNow();
            this.classDefItemConsumer.shutdownNow();
            throw new RuntimeException("Translation has been interrupted", ie);
        }
        catch (Exception e) {
            this.classTranslatorPool.shutdownNow();
            this.classDefItemConsumer.shutdownNow();
            e.printStackTrace(this.context.out);
            throw new RuntimeException("Unexpected exception in translator thread.", e);
        }
        final int errorNum = this.errors.get();
        if (errorNum != 0) {
            this.context.err.println(errorNum + " error" + ((errorNum == 1) ? "" : "s") + "; aborting");
            return false;
        }
        if (this.args.incremental && !this.anyFilesProcessed) {
            return true;
        }
        if (!this.anyFilesProcessed && !this.args.emptyOk) {
            this.context.err.println("no classfiles specified");
            return false;
        }
        if (this.args.optimize && this.args.statistics) {
            this.context.codeStatistics.dumpStatistics(this.context.out);
        }
        return true;
    }
    
    private void createDexFile() {
        this.outputDex = new DexFile(this.args.dexOptions);
        if (this.args.dumpWidth != 0) {
            this.outputDex.setDumpWidth(this.args.dumpWidth);
        }
    }
    
    private void rotateDexFile() {
        if (this.outputDex != null) {
            if (this.dexOutPool != null) {
                this.dexOutputFutures.add(this.dexOutPool.submit((Callable<byte[]>)new DexWriter(this.outputDex)));
            }
            else {
                this.dexOutputArrays.add(this.writeDex(this.outputDex));
            }
        }
        this.createDexFile();
    }
    
    private void processOne(final String pathname, final ClassPathOpener.FileNameFilter filter) {
        final ClassPathOpener opener = new ClassPathOpener(pathname, true, filter, new FileBytesConsumer());
        if (opener.process()) {
            this.updateStatus(true);
        }
    }
    
    private void updateStatus(final boolean res) {
        this.anyFilesProcessed |= res;
    }
    
    private boolean processFileBytes(final String name, final long lastModified, final byte[] bytes) {
        final boolean isClass = name.endsWith(".class");
        final boolean isClassesDex = name.equals("classes.dex");
        final boolean keepResources = this.outputResources != null;
        if (!isClass && !isClassesDex && !keepResources) {
            if (this.args.verbose) {
                this.context.out.println("ignored resource " + name);
            }
            return false;
        }
        if (this.args.verbose) {
            this.context.out.println("processing " + name + "...");
        }
        final String fixedName = fixPath(name);
        if (isClass) {
            if (keepResources && this.args.keepClassesInJar) {
                synchronized (this.outputResources) {
                    this.outputResources.put(fixedName, bytes);
                }
            }
            if (lastModified < this.minimumFileAge) {
                return true;
            }
            this.processClass(fixedName, bytes);
            return false;
        }
        else {
            if (isClassesDex) {
                synchronized (this.libraryDexBuffers) {
                    this.libraryDexBuffers.add(bytes);
                }
                return true;
            }
            synchronized (this.outputResources) {
                this.outputResources.put(fixedName, bytes);
            }
            return true;
        }
    }
    
    private boolean processClass(final String name, final byte[] bytes) {
        if (!this.args.coreLibrary) {
            this.checkClassName(name);
        }
        try {
            new DirectClassFileConsumer(name, bytes, (Future)null).call(new ClassParserTask(name, bytes).call());
        }
        catch (ParseException ex) {
            throw ex;
        }
        catch (Exception ex2) {
            throw new RuntimeException("Exception parsing classes", ex2);
        }
        return true;
    }
    
    private DirectClassFile parseClass(final String name, final byte[] bytes) {
        final DirectClassFile cf = new DirectClassFile(bytes, name, this.args.cfOptions.strictNameCheck);
        cf.setAttributeFactory(StdAttributeFactory.THE_ONE);
        cf.getMagic();
        return cf;
    }
    
    private ClassDefItem translateClass(final byte[] bytes, final DirectClassFile cf) {
        try {
            return CfTranslator.translate(this.context, cf, bytes, this.args.cfOptions, this.args.dexOptions, this.outputDex);
        }
        catch (ParseException ex) {
            this.context.err.println("\ntrouble processing:");
            if (this.args.debug) {
                ex.printStackTrace(this.context.err);
            }
            else {
                ex.printContext(this.context.err);
            }
            this.errors.incrementAndGet();
            return null;
        }
    }
    
    private boolean addClassToDex(final ClassDefItem clazz) {
        synchronized (this.outputDex) {
            this.outputDex.add(clazz);
        }
        return true;
    }
    
    private void checkClassName(final String name) {
        boolean bogus = false;
        if (name.startsWith("java/")) {
            bogus = true;
        }
        else if (name.startsWith("javax/")) {
            final int slashAt = name.indexOf(47, 6);
            if (slashAt == -1) {
                bogus = true;
            }
            else {
                final String pkg = name.substring(6, slashAt);
                bogus = (Arrays.binarySearch(Main.JAVAX_CORE, pkg) >= 0);
            }
        }
        if (!bogus) {
            return;
        }
        this.context.err.println("\ntrouble processing \"" + name + "\":\n\n" + "Ill-advised or mistaken usage of a core class (java.* or javax.*)\nwhen not building a core library.\n\nThis is often due to inadvertently including a core library file\nin your application's project, when using an IDE (such as\nEclipse). If you are sure you're not intentionally defining a\ncore class, then this is the most likely explanation of what's\ngoing on.\n\nHowever, you might actually be trying to define a class in a core\nnamespace, the source of which you may have taken, for example,\nfrom a non-Android virtual machine project. This will most\nassuredly not work. At a minimum, it jeopardizes the\ncompatibility of your app with future versions of the platform.\nIt is also often of questionable legality.\n\nIf you really intend to build a core library -- which is only\nappropriate as part of creating a full virtual machine\ndistribution, as opposed to compiling an application -- then use\nthe \"--core-library\" option to suppress this error message.\n\nIf you go ahead and use \"--core-library\" but are in fact\nbuilding an application, then be forewarned that your application\nwill still fail to build or run, at some point. Please be\nprepared for angry customers who find, for example, that your\napplication ceases to function once they upgrade their operating\nsystem. You will be to blame for this problem.\n\nIf you are legitimately using some code that happens to be in a\ncore package, then the easiest safe alternative you have is to\nrepackage that code. That is, move the classes in question into\nyour own package namespace. This means that they will never be in\nconflict with core system classes. JarJar is a tool that may help\nyou in this endeavor. If you find that you cannot do this, then\nthat is an indication that the path you are on will ultimately\nlead to pain, suffering, grief, and lamentation.\n");
        this.errors.incrementAndGet();
        throw new StopProcessing();
    }
    
    private byte[] writeDex(final DexFile outputDex) {
        byte[] outArray = null;
        try {
            try {
                if (this.args.methodToDump != null) {
                    outputDex.toDex(null, false);
                    this.dumpMethod(outputDex, this.args.methodToDump, this.humanOutWriter);
                }
                else {
                    outArray = outputDex.toDex(this.humanOutWriter, this.args.verboseDump);
                }
                if (this.args.statistics) {
                    this.context.out.println(outputDex.getStatistics().toHuman());
                }
            }
            finally {
                if (this.humanOutWriter != null) {
                    this.humanOutWriter.flush();
                }
            }
        }
        catch (Exception ex) {
            if (this.args.debug) {
                this.context.err.println("\ntrouble writing output:");
                ex.printStackTrace(this.context.err);
            }
            else {
                this.context.err.println("\ntrouble writing output: " + ex.getMessage());
            }
            return null;
        }
        return outArray;
    }
    
    private boolean createJar(final String fileName) {
        try {
            final Manifest manifest = this.makeManifest();
            final OutputStream out = this.openOutput(fileName);
            final JarOutputStream jarOut = new JarOutputStream(out, manifest);
            try {
                for (final Map.Entry<String, byte[]> e : this.outputResources.entrySet()) {
                    final String name = e.getKey();
                    final byte[] contents = e.getValue();
                    final JarEntry entry = new JarEntry(name);
                    final int length = contents.length;
                    if (this.args.verbose) {
                        this.context.out.println("writing " + name + "; size " + length + "...");
                    }
                    entry.setSize(length);
                    jarOut.putNextEntry(entry);
                    jarOut.write(contents);
                    jarOut.closeEntry();
                }
            }
            finally {
                jarOut.finish();
                jarOut.flush();
                this.closeOutput(out);
            }
        }
        catch (Exception ex) {
            if (this.args.debug) {
                this.context.err.println("\ntrouble writing output:");
                ex.printStackTrace(this.context.err);
            }
            else {
                this.context.err.println("\ntrouble writing output: " + ex.getMessage());
            }
            return false;
        }
        return true;
    }
    
    private Manifest makeManifest() throws IOException {
        final byte[] manifestBytes = this.outputResources.get("META-INF/MANIFEST.MF");
        Manifest manifest;
        Attributes attribs;
        if (manifestBytes == null) {
            manifest = new Manifest();
            attribs = manifest.getMainAttributes();
            attribs.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        }
        else {
            manifest = new Manifest(new ByteArrayInputStream(manifestBytes));
            attribs = manifest.getMainAttributes();
            this.outputResources.remove("META-INF/MANIFEST.MF");
        }
        String createdBy = attribs.getValue(Main.CREATED_BY);
        if (createdBy == null) {
            createdBy = "";
        }
        else {
            createdBy += " + ";
        }
        createdBy += "dx 1.14";
        attribs.put(Main.CREATED_BY, createdBy);
        attribs.putValue("Dex-Location", "classes.dex");
        return manifest;
    }
    
    private OutputStream openOutput(final String name) throws IOException {
        if (name.equals("-") || name.startsWith("-.")) {
            return this.context.out;
        }
        return new FileOutputStream(name);
    }
    
    private void closeOutput(final OutputStream stream) throws IOException {
        if (stream == null) {
            return;
        }
        stream.flush();
        if (stream != this.context.out) {
            stream.close();
        }
    }
    
    private static String fixPath(String path) {
        if (File.separatorChar == '\\') {
            path = path.replace('\\', '/');
        }
        final int index = path.lastIndexOf("/./");
        if (index != -1) {
            return path.substring(index + 3);
        }
        if (path.startsWith("./")) {
            return path.substring(2);
        }
        return path;
    }
    
    private void dumpMethod(final DexFile dex, final String fqName, final OutputStreamWriter out) {
        final boolean wildcard = fqName.endsWith("*");
        final int lastDot = fqName.lastIndexOf(46);
        if (lastDot <= 0 || lastDot == fqName.length() - 1) {
            this.context.err.println("bogus fully-qualified method name: " + fqName);
            return;
        }
        final String className = fqName.substring(0, lastDot).replace('.', '/');
        String methodName = fqName.substring(lastDot + 1);
        final ClassDefItem clazz = dex.getClassOrNull(className);
        if (clazz == null) {
            this.context.err.println("no such class: " + className);
            return;
        }
        if (wildcard) {
            methodName = methodName.substring(0, methodName.length() - 1);
        }
        final ArrayList<EncodedMethod> allMeths = clazz.getMethods();
        final TreeMap<CstNat, EncodedMethod> meths = new TreeMap<CstNat, EncodedMethod>();
        for (final EncodedMethod meth : allMeths) {
            final String methName = meth.getName().getString();
            if ((wildcard && methName.startsWith(methodName)) || (!wildcard && methName.equals(methodName))) {
                meths.put(meth.getRef().getNat(), meth);
            }
        }
        if (meths.size() == 0) {
            this.context.err.println("no such method: " + fqName);
            return;
        }
        final PrintWriter pw = new PrintWriter(out);
        for (final EncodedMethod meth2 : meths.values()) {
            meth2.debugPrint(pw, this.args.verboseDump);
            final CstString sourceFile = clazz.getSourceFile();
            if (sourceFile != null) {
                pw.println("  source file: " + sourceFile.toQuoted());
            }
            final Annotations methodAnnotations = clazz.getMethodAnnotations(meth2.getRef());
            final AnnotationsList parameterAnnotations = clazz.getParameterAnnotations(meth2.getRef());
            if (methodAnnotations != null) {
                pw.println("  method annotations:");
                for (final Annotation a : methodAnnotations.getAnnotations()) {
                    pw.println("    " + a);
                }
            }
            if (parameterAnnotations != null) {
                pw.println("  parameter annotations:");
                for (int sz = parameterAnnotations.size(), i = 0; i < sz; ++i) {
                    pw.println("    parameter " + i);
                    final Annotations annotations = parameterAnnotations.get(i);
                    for (final Annotation a2 : annotations.getAnnotations()) {
                        pw.println("      " + a2);
                    }
                }
            }
        }
        pw.flush();
    }
    
    static {
        CREATED_BY = new Attributes.Name("Created-By");
        JAVAX_CORE = new String[] { "accessibility", "crypto", "imageio", "management", "naming", "net", "print", "rmi", "security", "sip", "sound", "sql", "swing", "transaction", "xml" };
    }
    
    private static class NotFilter implements ClassPathOpener.FileNameFilter
    {
        private final ClassPathOpener.FileNameFilter filter;
        
        private NotFilter(final ClassPathOpener.FileNameFilter filter) {
            this.filter = filter;
        }
        
        @Override
        public boolean accept(final String path) {
            return !this.filter.accept(path);
        }
    }
    
    private class MainDexListFilter implements ClassPathOpener.FileNameFilter
    {
        @Override
        public boolean accept(final String fullPath) {
            if (fullPath.endsWith(".class")) {
                final String path = fixPath(fullPath);
                return Main.this.classesInMainDex.contains(path);
            }
            return true;
        }
    }
    
    private class BestEffortMainDexListFilter implements ClassPathOpener.FileNameFilter
    {
        Map<String, List<String>> map;
        
        public BestEffortMainDexListFilter() {
            this.map = new HashMap<String, List<String>>();
            for (final String pathOfClass : Main.this.classesInMainDex) {
                final String normalized = fixPath(pathOfClass);
                final String simple = this.getSimpleName(normalized);
                List<String> fullPath = this.map.get(simple);
                if (fullPath == null) {
                    fullPath = new ArrayList<String>(1);
                    this.map.put(simple, fullPath);
                }
                fullPath.add(normalized);
            }
        }
        
        @Override
        public boolean accept(final String path) {
            if (path.endsWith(".class")) {
                final String normalized = fixPath(path);
                final String simple = this.getSimpleName(normalized);
                final List<String> fullPaths = this.map.get(simple);
                if (fullPaths != null) {
                    for (final String fullPath : fullPaths) {
                        if (normalized.endsWith(fullPath)) {
                            return true;
                        }
                    }
                }
                return false;
            }
            return true;
        }
        
        private String getSimpleName(final String path) {
            final int index = path.lastIndexOf(47);
            if (index >= 0) {
                return path.substring(index + 1);
            }
            return path;
        }
    }
    
    private static class StopProcessing extends RuntimeException
    {
    }
    
    public static class Arguments
    {
        private static final String MINIMAL_MAIN_DEX_OPTION = "--minimal-main-dex";
        private static final String MAIN_DEX_LIST_OPTION = "--main-dex-list";
        private static final String MULTI_DEX_OPTION = "--multi-dex";
        private static final String NUM_THREADS_OPTION = "--num-threads";
        private static final String INCREMENTAL_OPTION = "--incremental";
        private static final String INPUT_LIST_OPTION = "--input-list";
        public final DxContext context;
        public boolean debug;
        public boolean warnings;
        public boolean verbose;
        public boolean verboseDump;
        public boolean coreLibrary;
        public String methodToDump;
        public int dumpWidth;
        public String outName;
        public String humanOutName;
        public boolean strictNameCheck;
        public boolean emptyOk;
        public boolean jarOutput;
        public boolean keepClassesInJar;
        public int minSdkVersion;
        public int positionInfo;
        public boolean localInfo;
        public boolean incremental;
        public boolean forceJumbo;
        public String[] fileNames;
        public boolean optimize;
        public String optimizeListFile;
        public String dontOptimizeListFile;
        public boolean statistics;
        public CfOptions cfOptions;
        public DexOptions dexOptions;
        public int numThreads;
        public boolean multiDex;
        public String mainDexListFile;
        public boolean minimalMainDex;
        public int maxNumberOfIdxPerDex;
        private List<String> inputList;
        private boolean outputIsDirectory;
        private boolean outputIsDirectDex;
        
        public Arguments(final DxContext context) {
            this.debug = false;
            this.warnings = true;
            this.verbose = false;
            this.verboseDump = false;
            this.coreLibrary = false;
            this.methodToDump = null;
            this.dumpWidth = 0;
            this.outName = null;
            this.humanOutName = null;
            this.strictNameCheck = true;
            this.emptyOk = false;
            this.jarOutput = false;
            this.keepClassesInJar = false;
            this.minSdkVersion = 13;
            this.positionInfo = 2;
            this.localInfo = true;
            this.incremental = false;
            this.forceJumbo = false;
            this.optimize = true;
            this.optimizeListFile = null;
            this.dontOptimizeListFile = null;
            this.numThreads = 1;
            this.multiDex = false;
            this.mainDexListFile = null;
            this.minimalMainDex = false;
            this.maxNumberOfIdxPerDex = 65536;
            this.inputList = null;
            this.outputIsDirectory = false;
            this.outputIsDirectDex = false;
            this.context = context;
        }
        
        public Arguments() {
            this(new DxContext());
        }
        
        private void parseFlags(final ArgumentsParser parser) {
            while (parser.getNext()) {
                if (parser.isArg("--debug")) {
                    this.debug = true;
                }
                else if (parser.isArg("--no-warning")) {
                    this.warnings = false;
                }
                else if (parser.isArg("--verbose")) {
                    this.verbose = true;
                }
                else if (parser.isArg("--verbose-dump")) {
                    this.verboseDump = true;
                }
                else if (parser.isArg("--no-files")) {
                    this.emptyOk = true;
                }
                else if (parser.isArg("--no-optimize")) {
                    this.optimize = false;
                }
                else if (parser.isArg("--no-strict")) {
                    this.strictNameCheck = false;
                }
                else if (parser.isArg("--core-library")) {
                    this.coreLibrary = true;
                }
                else if (parser.isArg("--statistics")) {
                    this.statistics = true;
                }
                else if (parser.isArg("--optimize-list=")) {
                    if (this.dontOptimizeListFile != null) {
                        this.context.err.println("--optimize-list and --no-optimize-list are incompatible.");
                        throw new UsageException();
                    }
                    this.optimize = true;
                    this.optimizeListFile = parser.getLastValue();
                }
                else if (parser.isArg("--no-optimize-list=")) {
                    if (this.dontOptimizeListFile != null) {
                        this.context.err.println("--optimize-list and --no-optimize-list are incompatible.");
                        throw new UsageException();
                    }
                    this.optimize = true;
                    this.dontOptimizeListFile = parser.getLastValue();
                }
                else if (parser.isArg("--keep-classes")) {
                    this.keepClassesInJar = true;
                }
                else if (parser.isArg("--output=")) {
                    this.outName = parser.getLastValue();
                    if (new File(this.outName).isDirectory()) {
                        this.jarOutput = false;
                        this.outputIsDirectory = true;
                    }
                    else if (FileUtils.hasArchiveSuffix(this.outName)) {
                        this.jarOutput = true;
                    }
                    else {
                        if (!this.outName.endsWith(".dex") && !this.outName.equals("-")) {
                            this.context.err.println("unknown output extension: " + this.outName);
                            throw new UsageException();
                        }
                        this.jarOutput = false;
                        this.outputIsDirectDex = true;
                    }
                }
                else if (parser.isArg("--dump-to=")) {
                    this.humanOutName = parser.getLastValue();
                }
                else if (parser.isArg("--dump-width=")) {
                    this.dumpWidth = Integer.parseInt(parser.getLastValue());
                }
                else if (parser.isArg("--dump-method=")) {
                    this.methodToDump = parser.getLastValue();
                    this.jarOutput = false;
                }
                else if (parser.isArg("--positions=")) {
                    final String pstr = parser.getLastValue().intern();
                    if (pstr == "none") {
                        this.positionInfo = 1;
                    }
                    else if (pstr == "important") {
                        this.positionInfo = 3;
                    }
                    else {
                        if (pstr != "lines") {
                            this.context.err.println("unknown positions option: " + pstr);
                            throw new UsageException();
                        }
                        this.positionInfo = 2;
                    }
                }
                else if (parser.isArg("--no-locals")) {
                    this.localInfo = false;
                }
                else if (parser.isArg("--num-threads=")) {
                    this.numThreads = Integer.parseInt(parser.getLastValue());
                }
                else if (parser.isArg("--incremental")) {
                    this.incremental = true;
                }
                else if (parser.isArg("--force-jumbo")) {
                    this.forceJumbo = true;
                }
                else if (parser.isArg("--multi-dex")) {
                    this.multiDex = true;
                }
                else if (parser.isArg("--main-dex-list=")) {
                    this.mainDexListFile = parser.getLastValue();
                }
                else if (parser.isArg("--minimal-main-dex")) {
                    this.minimalMainDex = true;
                }
                else if (parser.isArg("--set-max-idx-number=")) {
                    this.maxNumberOfIdxPerDex = Integer.parseInt(parser.getLastValue());
                }
                else if (parser.isArg("--input-list=")) {
                    final File inputListFile = new File(parser.getLastValue());
                    try {
                        this.inputList = new ArrayList<String>();
                        readPathsFromFile(inputListFile.getAbsolutePath(), this.inputList);
                    }
                    catch (IOException e) {
                        this.context.err.println("Unable to read input list file: " + inputListFile.getName());
                        throw new UsageException();
                    }
                }
                else {
                    if (!parser.isArg("--min-sdk-version=")) {
                        this.context.err.println("unknown option: " + parser.getCurrent());
                        throw new UsageException();
                    }
                    final String arg = parser.getLastValue();
                    int value;
                    try {
                        value = Integer.parseInt(arg);
                    }
                    catch (NumberFormatException ex) {
                        value = -1;
                    }
                    if (value < 1) {
                        System.err.println("improper min-sdk-version option: " + arg);
                        throw new UsageException();
                    }
                    this.minSdkVersion = value;
                }
            }
        }
        
        private void parse(final String[] args) {
            final ArgumentsParser parser = new ArgumentsParser(args);
            this.parseFlags(parser);
            this.fileNames = parser.getRemaining();
            if (this.inputList != null && !this.inputList.isEmpty()) {
                this.inputList.addAll(Arrays.asList(this.fileNames));
                this.fileNames = this.inputList.toArray(new String[this.inputList.size()]);
            }
            if (this.fileNames.length == 0) {
                if (!this.emptyOk) {
                    this.context.err.println("no input files specified");
                    throw new UsageException();
                }
            }
            else if (this.emptyOk) {
                this.context.out.println("ignoring input files");
            }
            if (this.humanOutName == null && this.methodToDump != null) {
                this.humanOutName = "-";
            }
            if (this.mainDexListFile != null && !this.multiDex) {
                this.context.err.println("--main-dex-list is only supported in combination with --multi-dex");
                throw new UsageException();
            }
            if (this.minimalMainDex && (this.mainDexListFile == null || !this.multiDex)) {
                this.context.err.println("--minimal-main-dex is only supported in combination with --multi-dex and --main-dex-list");
                throw new UsageException();
            }
            if (this.multiDex && this.incremental) {
                this.context.err.println("--incremental is not supported with --multi-dex");
                throw new UsageException();
            }
            if (this.multiDex && this.outputIsDirectDex) {
                this.context.err.println("Unsupported output \"" + this.outName + "\". " + "--multi-dex" + " supports only archive or directory output");
                throw new UsageException();
            }
            if (this.outputIsDirectory && !this.multiDex) {
                this.outName = new File(this.outName, "classes.dex").getPath();
            }
            this.makeOptionsObjects();
        }
        
        public void parseFlags(final String[] flags) {
            this.parseFlags(new ArgumentsParser(flags));
        }
        
        public void makeOptionsObjects() {
            this.cfOptions = new CfOptions();
            this.cfOptions.positionInfo = this.positionInfo;
            this.cfOptions.localInfo = this.localInfo;
            this.cfOptions.strictNameCheck = this.strictNameCheck;
            this.cfOptions.optimize = this.optimize;
            this.cfOptions.optimizeListFile = this.optimizeListFile;
            this.cfOptions.dontOptimizeListFile = this.dontOptimizeListFile;
            this.cfOptions.statistics = this.statistics;
            if (this.warnings) {
                this.cfOptions.warn = this.context.err;
            }
            else {
                this.cfOptions.warn = this.context.noop;
            }
            this.dexOptions = new DexOptions();
            this.dexOptions.minSdkVersion = this.minSdkVersion;
            this.dexOptions.forceJumbo = this.forceJumbo;
        }
        
        private static class ArgumentsParser
        {
            private final String[] arguments;
            private int index;
            private String current;
            private String lastValue;
            
            public ArgumentsParser(final String[] arguments) {
                this.arguments = arguments;
                this.index = 0;
            }
            
            public String getCurrent() {
                return this.current;
            }
            
            public String getLastValue() {
                return this.lastValue;
            }
            
            public boolean getNext() {
                if (this.index >= this.arguments.length) {
                    return false;
                }
                this.current = this.arguments[this.index];
                if (this.current.equals("--") || !this.current.startsWith("--")) {
                    return false;
                }
                ++this.index;
                return true;
            }
            
            private boolean getNextValue() {
                if (this.index >= this.arguments.length) {
                    return false;
                }
                this.current = this.arguments[this.index];
                ++this.index;
                return true;
            }
            
            public String[] getRemaining() {
                final int n = this.arguments.length - this.index;
                final String[] remaining = new String[n];
                if (n > 0) {
                    System.arraycopy(this.arguments, this.index, remaining, 0, n);
                }
                return remaining;
            }
            
            public boolean isArg(String prefix) {
                final int n = prefix.length();
                if (n <= 0 || prefix.charAt(n - 1) != '=') {
                    return this.current.equals(prefix);
                }
                if (this.current.startsWith(prefix)) {
                    this.lastValue = this.current.substring(n);
                    return true;
                }
                prefix = prefix.substring(0, n - 1);
                if (!this.current.equals(prefix)) {
                    return false;
                }
                if (this.getNextValue()) {
                    this.lastValue = this.current;
                    return true;
                }
                System.err.println("Missing value after parameter " + prefix);
                throw new UsageException();
            }
        }
    }
    
    private class FileBytesConsumer implements ClassPathOpener.Consumer
    {
        @Override
        public boolean processFileBytes(final String name, final long lastModified, final byte[] bytes) {
            return Main.this.processFileBytes(name, lastModified, bytes);
        }
        
        @Override
        public void onException(final Exception ex) {
            if (ex instanceof StopProcessing) {
                throw (StopProcessing)ex;
            }
            if (ex instanceof SimException) {
                Main.this.context.err.println("\nEXCEPTION FROM SIMULATION:");
                Main.this.context.err.println(ex.getMessage() + "\n");
                Main.this.context.err.println(((SimException)ex).getContext());
            }
            else if (ex instanceof ParseException) {
                Main.this.context.err.println("\nPARSE ERROR:");
                final ParseException parseException = (ParseException)ex;
                if (Main.this.args.debug) {
                    parseException.printStackTrace(Main.this.context.err);
                }
                else {
                    parseException.printContext(Main.this.context.err);
                }
            }
            else {
                Main.this.context.err.println("\nUNEXPECTED TOP-LEVEL EXCEPTION:");
                ex.printStackTrace(Main.this.context.err);
            }
            Main.this.errors.incrementAndGet();
        }
        
        @Override
        public void onProcessArchiveStart(final File file) {
            if (Main.this.args.verbose) {
                Main.this.context.out.println("processing archive " + file + "...");
            }
        }
    }
    
    private class ClassParserTask implements Callable<DirectClassFile>
    {
        String name;
        byte[] bytes;
        
        private ClassParserTask(final String name, final byte[] bytes) {
            this.name = name;
            this.bytes = bytes;
        }
        
        @Override
        public DirectClassFile call() throws Exception {
            final DirectClassFile cf = Main.this.parseClass(this.name, this.bytes);
            return cf;
        }
    }
    
    private class DirectClassFileConsumer implements Callable<Boolean>
    {
        String name;
        byte[] bytes;
        Future<DirectClassFile> dcff;
        
        private DirectClassFileConsumer(final String name, final byte[] bytes, final Future<DirectClassFile> dcff) {
            this.name = name;
            this.bytes = bytes;
            this.dcff = dcff;
        }
        
        @Override
        public Boolean call() throws Exception {
            final DirectClassFile cf = this.dcff.get();
            return this.call(cf);
        }
        
        private Boolean call(final DirectClassFile cf) {
            int maxMethodIdsInClass = 0;
            int maxFieldIdsInClass = 0;
            if (Main.this.args.multiDex) {
                final int constantPoolSize = cf.getConstantPool().size();
                maxMethodIdsInClass = constantPoolSize + cf.getMethods().size() + 2;
                maxFieldIdsInClass = constantPoolSize + cf.getFields().size() + 9;
                synchronized (Main.this.dexRotationLock) {
                    int numMethodIds;
                    int numFieldIds;
                    synchronized (Main.this.outputDex) {
                        numMethodIds = Main.this.outputDex.getMethodIds().items().size();
                        numFieldIds = Main.this.outputDex.getFieldIds().items().size();
                    }
                    while (numMethodIds + maxMethodIdsInClass + Main.this.maxMethodIdsInProcess > Main.this.args.maxNumberOfIdxPerDex || numFieldIds + maxFieldIdsInClass + Main.this.maxFieldIdsInProcess > Main.this.args.maxNumberOfIdxPerDex) {
                        Label_0251: {
                            if (Main.this.maxMethodIdsInProcess <= 0) {
                                if (Main.this.maxFieldIdsInProcess <= 0) {
                                    if (Main.this.outputDex.getClassDefs().items().size() > 0) {
                                        Main.this.rotateDexFile();
                                        break Label_0251;
                                    }
                                    break;
                                }
                            }
                            try {
                                Main.this.dexRotationLock.wait();
                            }
                            catch (InterruptedException ex) {}
                        }
                        synchronized (Main.this.outputDex) {
                            numMethodIds = Main.this.outputDex.getMethodIds().items().size();
                            numFieldIds = Main.this.outputDex.getFieldIds().items().size();
                        }
                    }
                    Main.this.maxMethodIdsInProcess += maxMethodIdsInClass;
                    Main.this.maxFieldIdsInProcess += maxFieldIdsInClass;
                }
            }
            final Future<ClassDefItem> cdif = Main.this.classTranslatorPool.submit((Callable<ClassDefItem>)new ClassTranslatorTask(this.name, this.bytes, cf));
            final Future<Boolean> res = Main.this.classDefItemConsumer.submit((Callable<Boolean>)new ClassDefItemConsumer(this.name, (Future)cdif, maxMethodIdsInClass, maxFieldIdsInClass));
            Main.this.addToDexFutures.add(res);
            return true;
        }
    }
    
    private class ClassTranslatorTask implements Callable<ClassDefItem>
    {
        String name;
        byte[] bytes;
        DirectClassFile classFile;
        
        private ClassTranslatorTask(final String name, final byte[] bytes, final DirectClassFile classFile) {
            this.name = name;
            this.bytes = bytes;
            this.classFile = classFile;
        }
        
        @Override
        public ClassDefItem call() {
            final ClassDefItem clazz = Main.this.translateClass(this.bytes, this.classFile);
            return clazz;
        }
    }
    
    private class ClassDefItemConsumer implements Callable<Boolean>
    {
        String name;
        Future<ClassDefItem> futureClazz;
        int maxMethodIdsInClass;
        int maxFieldIdsInClass;
        
        private ClassDefItemConsumer(final String name, final Future<ClassDefItem> futureClazz, final int maxMethodIdsInClass, final int maxFieldIdsInClass) {
            this.name = name;
            this.futureClazz = futureClazz;
            this.maxMethodIdsInClass = maxMethodIdsInClass;
            this.maxFieldIdsInClass = maxFieldIdsInClass;
        }
        
        @Override
        public Boolean call() throws Exception {
            try {
                final ClassDefItem clazz = this.futureClazz.get();
                if (clazz != null) {
                    Main.this.addClassToDex(clazz);
                    Main.this.updateStatus(true);
                }
                return true;
            }
            catch (ExecutionException ex) {
                final Throwable t = ex.getCause();
                throw (t instanceof Exception) ? ((Exception)t) : ex;
            }
            finally {
                if (Main.this.args.multiDex) {
                    synchronized (Main.this.dexRotationLock) {
                        Main.this.maxMethodIdsInProcess -= this.maxMethodIdsInClass;
                        Main.this.maxFieldIdsInProcess -= this.maxFieldIdsInClass;
                        Main.this.dexRotationLock.notifyAll();
                    }
                }
            }
        }
    }
    
    private class DexWriter implements Callable<byte[]>
    {
        private DexFile dexFile;
        
        private DexWriter(final DexFile dexFile) {
            this.dexFile = dexFile;
        }
        
        @Override
        public byte[] call() throws IOException {
            return Main.this.writeDex(this.dexFile);
        }
    }
}

package embedded.com.android.multidex;

import java.util.zip.*;
import java.io.*;
import java.util.*;
import embedded.com.android.dx.cf.direct.*;
import embedded.com.android.dx.rop.cst.*;
import embedded.com.android.dx.cf.iface.*;
import embedded.com.android.dx.rop.type.*;

public class ClassReferenceListBuilder
{
    private static final String CLASS_EXTENSION = ".class";
    private final Path path;
    private final Set<String> classNames;
    
    public ClassReferenceListBuilder(final Path path) {
        this.classNames = new HashSet<String>();
        this.path = path;
    }
    
    @Deprecated
    public static void main(final String[] args) {
        MainDexListBuilder.main(args);
    }
    
    public void addRoots(final ZipFile jarOfRoots) throws IOException {
        Enumeration<? extends ZipEntry> entries = jarOfRoots.entries();
        while (entries.hasMoreElements()) {
            final ZipEntry entry = (ZipEntry)entries.nextElement();
            final String name = entry.getName();
            if (name.endsWith(".class")) {
                this.classNames.add(name.substring(0, name.length() - ".class".length()));
            }
        }
        entries = jarOfRoots.entries();
        while (entries.hasMoreElements()) {
            final ZipEntry entry = (ZipEntry)entries.nextElement();
            final String name = entry.getName();
            if (name.endsWith(".class")) {
                DirectClassFile classFile;
                try {
                    classFile = this.path.getClass(name);
                }
                catch (FileNotFoundException e) {
                    throw new IOException("Class " + name + " is missing form original class path " + this.path, e);
                }
                this.addDependencies(classFile);
            }
        }
    }
    
    Set<String> getClassNames() {
        return this.classNames;
    }
    
    private void addDependencies(final DirectClassFile classFile) {
        for (final Constant constant : classFile.getConstantPool().getEntries()) {
            if (constant instanceof CstType) {
                this.checkDescriptor(((CstType)constant).getClassType().getDescriptor());
            }
            else if (constant instanceof CstFieldRef) {
                this.checkDescriptor(((CstFieldRef)constant).getType().getDescriptor());
            }
            else if (constant instanceof CstBaseMethodRef) {
                this.checkPrototype(((CstBaseMethodRef)constant).getPrototype());
            }
        }
        final FieldList fields = classFile.getFields();
        for (int nbField = fields.size(), i = 0; i < nbField; ++i) {
            this.checkDescriptor(fields.get(i).getDescriptor().getString());
        }
        final MethodList methods = classFile.getMethods();
        for (int nbMethods = methods.size(), j = 0; j < nbMethods; ++j) {
            this.checkPrototype(Prototype.intern(methods.get(j).getDescriptor().getString()));
        }
    }
    
    private void checkPrototype(final Prototype proto) {
        this.checkDescriptor(proto.getReturnType().getDescriptor());
        final StdTypeList args = proto.getParameterTypes();
        for (int i = 0; i < args.size(); ++i) {
            this.checkDescriptor(args.get(i).getDescriptor());
        }
    }
    
    private void checkDescriptor(final String typeDescriptor) {
        if (typeDescriptor.endsWith(";")) {
            final int lastBrace = typeDescriptor.lastIndexOf(91);
            if (lastBrace < 0) {
                this.addClassWithHierachy(typeDescriptor.substring(1, typeDescriptor.length() - 1));
            }
            else {
                assert typeDescriptor.length() > lastBrace + 3 && typeDescriptor.charAt(lastBrace + 1) == 'L';
                this.addClassWithHierachy(typeDescriptor.substring(lastBrace + 2, typeDescriptor.length() - 1));
            }
        }
    }
    
    private void addClassWithHierachy(final String classBinaryName) {
        if (this.classNames.contains(classBinaryName)) {
            return;
        }
        try {
            final DirectClassFile classFile = this.path.getClass(classBinaryName + ".class");
            this.classNames.add(classBinaryName);
            final CstType superClass = classFile.getSuperclass();
            if (superClass != null) {
                this.addClassWithHierachy(superClass.getClassType().getClassName());
            }
            final TypeList interfaceList = classFile.getInterfaces();
            for (int interfaceNumber = interfaceList.size(), i = 0; i < interfaceNumber; ++i) {
                this.addClassWithHierachy(interfaceList.getType(i).getClassName());
            }
        }
        catch (FileNotFoundException ex) {}
    }
}

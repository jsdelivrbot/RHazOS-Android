package embedded.com.android.dx.command.annotool;

import embedded.com.android.dx.rop.annotation.Annotation;
import embedded.com.android.dx.util.*;
import embedded.com.android.dx.cf.direct.*;
import embedded.com.android.dx.cf.attrib.*;
import embedded.com.android.dx.cf.iface.*;
import java.io.*;
import java.lang.annotation.*;
import embedded.com.android.dx.rop.annotation.*;
import java.util.*;

class AnnotationLister
{
    private static final String PACKAGE_INFO = "package-info";
    private final Main.Arguments args;
    HashSet<String> matchInnerClassesOf;
    HashSet<String> matchPackages;
    
    AnnotationLister(final Main.Arguments args) {
        this.matchInnerClassesOf = new HashSet<String>();
        this.matchPackages = new HashSet<String>();
        this.args = args;
    }
    
    void process() {
        for (final String path : this.args.files) {
            final ClassPathOpener opener = new ClassPathOpener(path, true, new ClassPathOpener.Consumer() {
                @Override
                public boolean processFileBytes(final String name, final long lastModified, final byte[] bytes) {
                    if (!name.endsWith(".class")) {
                        return true;
                    }
                    final ByteArray ba = new ByteArray(bytes);
                    final DirectClassFile cf = new DirectClassFile(ba, name, true);
                    cf.setAttributeFactory(StdAttributeFactory.THE_ONE);
                    final AttributeList attributes = cf.getAttributes();
                    final String cfClassName = cf.getThisClass().getClassType().getClassName();
                    if (cfClassName.endsWith("package-info")) {
                        for (Attribute att = attributes.findFirst("RuntimeInvisibleAnnotations"); att != null; att = attributes.findNext(att)) {
                            final BaseAnnotations ann = (BaseAnnotations)att;
                            AnnotationLister.this.visitPackageAnnotation(cf, ann);
                        }
                        for (Attribute att = attributes.findFirst("RuntimeVisibleAnnotations"); att != null; att = attributes.findNext(att)) {
                            final BaseAnnotations ann = (BaseAnnotations)att;
                            AnnotationLister.this.visitPackageAnnotation(cf, ann);
                        }
                    }
                    else if (AnnotationLister.this.isMatchingInnerClass(cfClassName) || AnnotationLister.this.isMatchingPackage(cfClassName)) {
                        AnnotationLister.this.printMatch(cf);
                    }
                    else {
                        for (Attribute att = attributes.findFirst("RuntimeInvisibleAnnotations"); att != null; att = attributes.findNext(att)) {
                            final BaseAnnotations ann = (BaseAnnotations)att;
                            AnnotationLister.this.visitClassAnnotation(cf, ann);
                        }
                        for (Attribute att = attributes.findFirst("RuntimeVisibleAnnotations"); att != null; att = attributes.findNext(att)) {
                            final BaseAnnotations ann = (BaseAnnotations)att;
                            AnnotationLister.this.visitClassAnnotation(cf, ann);
                        }
                    }
                    return true;
                }
                
                @Override
                public void onException(final Exception ex) {
                    throw new RuntimeException(ex);
                }
                
                @Override
                public void onProcessArchiveStart(final File file) {
                }
            });
            opener.process();
        }
    }
    
    private void visitClassAnnotation(final DirectClassFile cf, final BaseAnnotations ann) {
        if (!this.args.eTypes.contains(ElementType.TYPE)) {
            return;
        }
        for (final Annotation anAnn : ann.getAnnotations().getAnnotations()) {
            final String annClassName = anAnn.getType().getClassType().getClassName();
            if (this.args.aclass.equals(annClassName)) {
                this.printMatch(cf);
            }
        }
    }
    
    private void visitPackageAnnotation(final DirectClassFile cf, final BaseAnnotations ann) {
        if (!this.args.eTypes.contains(ElementType.PACKAGE)) {
            return;
        }
        String packageName = cf.getThisClass().getClassType().getClassName();
        final int slashIndex = packageName.lastIndexOf(47);
        if (slashIndex == -1) {
            packageName = "";
        }
        else {
            packageName = packageName.substring(0, slashIndex);
        }
        for (final Annotation anAnn : ann.getAnnotations().getAnnotations()) {
            final String annClassName = anAnn.getType().getClassType().getClassName();
            if (this.args.aclass.equals(annClassName)) {
                this.printMatchPackage(packageName);
            }
        }
    }
    
    private void printMatchPackage(final String packageName) {
        for (final Main.PrintType pt : this.args.printTypes) {
            switch (pt) {
                case CLASS:
                case INNERCLASS:
                case METHOD: {
                    this.matchPackages.add(packageName);
                    continue;
                }
                case PACKAGE: {
                    System.out.println(packageName.replace('/', '.'));
                    continue;
                }
            }
        }
    }
    
    private void printMatch(final DirectClassFile cf) {
        for (final Main.PrintType pt : this.args.printTypes) {
            switch (pt) {
                case CLASS: {
                    String classname = cf.getThisClass().getClassType().getClassName();
                    classname = classname.replace('/', '.');
                    System.out.println(classname);
                    continue;
                }
                case INNERCLASS: {
                    this.matchInnerClassesOf.add(cf.getThisClass().getClassType().getClassName());
                }
                case METHOD: {
                    continue;
                }
            }
        }
    }
    
    private boolean isMatchingInnerClass(String s) {
        int i;
        while (0 < (i = s.lastIndexOf(36))) {
            s = s.substring(0, i);
            if (this.matchInnerClassesOf.contains(s)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean isMatchingPackage(final String s) {
        final int slashIndex = s.lastIndexOf(47);
        String packageName;
        if (slashIndex == -1) {
            packageName = "";
        }
        else {
            packageName = s.substring(0, slashIndex);
        }
        return this.matchPackages.contains(packageName);
    }
}

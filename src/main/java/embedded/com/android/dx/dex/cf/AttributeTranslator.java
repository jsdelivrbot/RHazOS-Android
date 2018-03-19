package embedded.com.android.dx.dex.cf;

import embedded.com.android.dx.cf.direct.*;
import embedded.com.android.dx.util.*;
import embedded.com.android.dx.rop.code.*;
import embedded.com.android.dx.dex.file.*;
import embedded.com.android.dx.rop.cst.*;
import java.util.*;
import embedded.com.android.dx.rop.type.*;
import embedded.com.android.dx.cf.attrib.*;
import embedded.com.android.dx.rop.annotation.*;
import embedded.com.android.dx.cf.iface.*;

class AttributeTranslator
{
    public static TypeList getExceptions(final Method method) {
        final AttributeList attribs = method.getAttributes();
        final AttExceptions exceptions = (AttExceptions)attribs.findFirst("Exceptions");
        if (exceptions == null) {
            return StdTypeList.EMPTY;
        }
        return exceptions.getExceptions();
    }
    
    public static Annotations getAnnotations(final AttributeList attribs) {
        Annotations result = getAnnotations0(attribs);
        final Annotation signature = getSignature(attribs);
        final Annotation sourceDebugExtension = getSourceDebugExtension(attribs);
        if (signature != null) {
            result = Annotations.combine(result, signature);
        }
        if (sourceDebugExtension != null) {
            result = Annotations.combine(result, sourceDebugExtension);
        }
        return result;
    }
    
    public static Annotations getClassAnnotations(final DirectClassFile cf, final CfOptions args) {
        final CstType thisClass = cf.getThisClass();
        final AttributeList attribs = cf.getAttributes();
        Annotations result = getAnnotations(attribs);
        final Annotation enclosingMethod = translateEnclosingMethod(attribs);
        try {
            final Annotations innerClassAnnotations = translateInnerClasses(thisClass, attribs, enclosingMethod == null);
            if (innerClassAnnotations != null) {
                result = Annotations.combine(result, innerClassAnnotations);
            }
        }
        catch (Warning warn) {
            args.warn.println("warning: " + warn.getMessage());
        }
        if (enclosingMethod != null) {
            result = Annotations.combine(result, enclosingMethod);
        }
        if (AccessFlags.isAnnotation(cf.getAccessFlags())) {
            final Annotation annotationDefault = translateAnnotationDefaults(cf);
            if (annotationDefault != null) {
                result = Annotations.combine(result, annotationDefault);
            }
        }
        return result;
    }
    
    public static Annotations getMethodAnnotations(final Method method) {
        Annotations result = getAnnotations(method.getAttributes());
        final TypeList exceptions = getExceptions(method);
        if (exceptions.size() != 0) {
            final Annotation throwsAnnotation = AnnotationUtils.makeThrows(exceptions);
            result = Annotations.combine(result, throwsAnnotation);
        }
        return result;
    }
    
    private static Annotations getAnnotations0(final AttributeList attribs) {
        final AttRuntimeVisibleAnnotations visible = (AttRuntimeVisibleAnnotations)attribs.findFirst("RuntimeVisibleAnnotations");
        final AttRuntimeInvisibleAnnotations invisible = (AttRuntimeInvisibleAnnotations)attribs.findFirst("RuntimeInvisibleAnnotations");
        if (visible == null) {
            if (invisible == null) {
                return Annotations.EMPTY;
            }
            return invisible.getAnnotations();
        }
        else {
            if (invisible == null) {
                return visible.getAnnotations();
            }
            return Annotations.combine(visible.getAnnotations(), invisible.getAnnotations());
        }
    }
    
    private static Annotation getSignature(final AttributeList attribs) {
        final AttSignature signature = (AttSignature)attribs.findFirst("Signature");
        if (signature == null) {
            return null;
        }
        return AnnotationUtils.makeSignature(signature.getSignature());
    }
    
    private static Annotation getSourceDebugExtension(final AttributeList attribs) {
        final AttSourceDebugExtension extension = (AttSourceDebugExtension)attribs.findFirst("SourceDebugExtension");
        if (extension == null) {
            return null;
        }
        return AnnotationUtils.makeSourceDebugExtension(extension.getSmapString());
    }
    
    private static Annotation translateEnclosingMethod(final AttributeList attribs) {
        final AttEnclosingMethod enclosingMethod = (AttEnclosingMethod)attribs.findFirst("EnclosingMethod");
        if (enclosingMethod == null) {
            return null;
        }
        final CstType enclosingClass = enclosingMethod.getEnclosingClass();
        final CstNat nat = enclosingMethod.getMethod();
        if (nat == null) {
            return AnnotationUtils.makeEnclosingClass(enclosingClass);
        }
        return AnnotationUtils.makeEnclosingMethod(new CstMethodRef(enclosingClass, nat));
    }
    
    private static Annotations translateInnerClasses(final CstType thisClass, final AttributeList attribs, final boolean needEnclosingClass) {
        final AttInnerClasses innerClasses = (AttInnerClasses)attribs.findFirst("InnerClasses");
        if (innerClasses == null) {
            return null;
        }
        final InnerClassList list = innerClasses.getInnerClasses();
        final int size = list.size();
        InnerClassList.Item foundThisClass = null;
        final ArrayList<Type> membersList = new ArrayList<Type>();
        for (int i = 0; i < size; ++i) {
            final InnerClassList.Item item = list.get(i);
            final CstType innerClass = item.getInnerClass();
            if (innerClass.equals(thisClass)) {
                foundThisClass = item;
            }
            else if (thisClass.equals(item.getOuterClass())) {
                membersList.add(innerClass.getClassType());
            }
        }
        final int membersSize = membersList.size();
        if (foundThisClass == null && membersSize == 0) {
            return null;
        }
        final Annotations result = new Annotations();
        if (foundThisClass != null) {
            result.add(AnnotationUtils.makeInnerClass(foundThisClass.getInnerName(), foundThisClass.getAccessFlags()));
            if (needEnclosingClass) {
                final CstType outer = foundThisClass.getOuterClass();
                if (outer == null) {
                    throw new Warning("Ignoring InnerClasses attribute for an anonymous inner class\n(" + thisClass.toHuman() + ") that doesn't come with an\n" + "associated EnclosingMethod attribute. " + "This class was probably produced by a\n" + "compiler that did not target the modern " + ".class file format. The recommended\n" + "solution is to recompile the class from " + "source, using an up-to-date compiler\n" + "and without specifying any \"-target\" type " + "options. The consequence of ignoring\n" + "this warning is that reflective operations " + "on this class will incorrectly\n" + "indicate that it is *not* an inner class.");
                }
                result.add(AnnotationUtils.makeEnclosingClass(foundThisClass.getOuterClass()));
            }
        }
        if (membersSize != 0) {
            final StdTypeList typeList = new StdTypeList(membersSize);
            for (int j = 0; j < membersSize; ++j) {
                typeList.set(j, membersList.get(j));
            }
            typeList.setImmutable();
            result.add(AnnotationUtils.makeMemberClasses(typeList));
        }
        result.setImmutable();
        return result;
    }
    
    public static AnnotationsList getParameterAnnotations(final Method method) {
        final AttributeList attribs = method.getAttributes();
        final AttRuntimeVisibleParameterAnnotations visible = (AttRuntimeVisibleParameterAnnotations)attribs.findFirst("RuntimeVisibleParameterAnnotations");
        final AttRuntimeInvisibleParameterAnnotations invisible = (AttRuntimeInvisibleParameterAnnotations)attribs.findFirst("RuntimeInvisibleParameterAnnotations");
        if (visible == null) {
            if (invisible == null) {
                return AnnotationsList.EMPTY;
            }
            return invisible.getParameterAnnotations();
        }
        else {
            if (invisible == null) {
                return visible.getParameterAnnotations();
            }
            return AnnotationsList.combine(visible.getParameterAnnotations(), invisible.getParameterAnnotations());
        }
    }
    
    private static Annotation translateAnnotationDefaults(final DirectClassFile cf) {
        final CstType thisClass = cf.getThisClass();
        final MethodList methods = cf.getMethods();
        final int sz = methods.size();
        final Annotation result = new Annotation(thisClass, AnnotationVisibility.EMBEDDED);
        boolean any = false;
        for (int i = 0; i < sz; ++i) {
            final Method one = methods.get(i);
            final AttributeList attribs = one.getAttributes();
            final AttAnnotationDefault oneDefault = (AttAnnotationDefault)attribs.findFirst("AnnotationDefault");
            if (oneDefault != null) {
                final NameValuePair pair = new NameValuePair(one.getNat().getName(), oneDefault.getValue());
                result.add(pair);
                any = true;
            }
        }
        if (!any) {
            return null;
        }
        result.setImmutable();
        return AnnotationUtils.makeAnnotationDefault(result);
    }
}

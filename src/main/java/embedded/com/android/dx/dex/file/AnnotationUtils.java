package embedded.com.android.dx.dex.file;

import embedded.com.android.dx.rop.annotation.*;
import embedded.com.android.dx.rop.cst.*;
import java.util.*;
import embedded.com.android.dx.rop.type.*;

public final class AnnotationUtils
{
    private static final CstType ANNOTATION_DEFAULT_TYPE;
    private static final CstType ENCLOSING_CLASS_TYPE;
    private static final CstType ENCLOSING_METHOD_TYPE;
    private static final CstType INNER_CLASS_TYPE;
    private static final CstType MEMBER_CLASSES_TYPE;
    private static final CstType SIGNATURE_TYPE;
    private static final CstType SOURCE_DEBUG_EXTENSION_TYPE;
    private static final CstType THROWS_TYPE;
    private static final CstString ACCESS_FLAGS_STRING;
    private static final CstString NAME_STRING;
    private static final CstString VALUE_STRING;
    
    public static Annotation makeAnnotationDefault(final Annotation defaults) {
        final Annotation result = new Annotation(AnnotationUtils.ANNOTATION_DEFAULT_TYPE, AnnotationVisibility.SYSTEM);
        result.put(new NameValuePair(AnnotationUtils.VALUE_STRING, new CstAnnotation(defaults)));
        result.setImmutable();
        return result;
    }
    
    public static Annotation makeEnclosingClass(final CstType clazz) {
        final Annotation result = new Annotation(AnnotationUtils.ENCLOSING_CLASS_TYPE, AnnotationVisibility.SYSTEM);
        result.put(new NameValuePair(AnnotationUtils.VALUE_STRING, clazz));
        result.setImmutable();
        return result;
    }
    
    public static Annotation makeEnclosingMethod(final CstMethodRef method) {
        final Annotation result = new Annotation(AnnotationUtils.ENCLOSING_METHOD_TYPE, AnnotationVisibility.SYSTEM);
        result.put(new NameValuePair(AnnotationUtils.VALUE_STRING, method));
        result.setImmutable();
        return result;
    }
    
    public static Annotation makeInnerClass(final CstString name, final int accessFlags) {
        final Annotation result = new Annotation(AnnotationUtils.INNER_CLASS_TYPE, AnnotationVisibility.SYSTEM);
        final Constant nameCst = (name != null) ? name : CstKnownNull.THE_ONE;
        result.put(new NameValuePair(AnnotationUtils.NAME_STRING, nameCst));
        result.put(new NameValuePair(AnnotationUtils.ACCESS_FLAGS_STRING, CstInteger.make(accessFlags)));
        result.setImmutable();
        return result;
    }
    
    public static Annotation makeMemberClasses(final TypeList types) {
        final CstArray array = makeCstArray(types);
        final Annotation result = new Annotation(AnnotationUtils.MEMBER_CLASSES_TYPE, AnnotationVisibility.SYSTEM);
        result.put(new NameValuePair(AnnotationUtils.VALUE_STRING, array));
        result.setImmutable();
        return result;
    }
    
    public static Annotation makeSignature(final CstString signature) {
        final Annotation result = new Annotation(AnnotationUtils.SIGNATURE_TYPE, AnnotationVisibility.SYSTEM);
        final String raw = signature.getString();
        final int rawLength = raw.length();
        final ArrayList<String> pieces = new ArrayList<String>(20);
        int endAt;
        for (int at = 0; at < rawLength; at = endAt) {
            char c = raw.charAt(at);
            endAt = at + 1;
            if (c == 'L') {
                while (endAt < rawLength) {
                    c = raw.charAt(endAt);
                    if (c == ';') {
                        ++endAt;
                        break;
                    }
                    if (c == '<') {
                        break;
                    }
                    ++endAt;
                }
            }
            else {
                while (endAt < rawLength) {
                    c = raw.charAt(endAt);
                    if (c == 'L') {
                        break;
                    }
                    ++endAt;
                }
            }
            pieces.add(raw.substring(at, endAt));
        }
        final int size = pieces.size();
        final CstArray.List list = new CstArray.List(size);
        for (int i = 0; i < size; ++i) {
            list.set(i, new CstString(pieces.get(i)));
        }
        list.setImmutable();
        result.put(new NameValuePair(AnnotationUtils.VALUE_STRING, new CstArray(list)));
        result.setImmutable();
        return result;
    }
    
    public static Annotation makeSourceDebugExtension(final CstString smapString) {
        final Annotation result = new Annotation(AnnotationUtils.SOURCE_DEBUG_EXTENSION_TYPE, AnnotationVisibility.SYSTEM);
        result.put(new NameValuePair(AnnotationUtils.VALUE_STRING, smapString));
        result.setImmutable();
        return result;
    }
    
    public static Annotation makeThrows(final TypeList types) {
        final CstArray array = makeCstArray(types);
        final Annotation result = new Annotation(AnnotationUtils.THROWS_TYPE, AnnotationVisibility.SYSTEM);
        result.put(new NameValuePair(AnnotationUtils.VALUE_STRING, array));
        result.setImmutable();
        return result;
    }
    
    private static CstArray makeCstArray(final TypeList types) {
        final int size = types.size();
        final CstArray.List list = new CstArray.List(size);
        for (int i = 0; i < size; ++i) {
            list.set(i, CstType.intern(types.getType(i)));
        }
        list.setImmutable();
        return new CstArray(list);
    }
    
    static {
        ANNOTATION_DEFAULT_TYPE = CstType.intern(Type.intern("Ldalvik/annotation/AnnotationDefault;"));
        ENCLOSING_CLASS_TYPE = CstType.intern(Type.intern("Ldalvik/annotation/EnclosingClass;"));
        ENCLOSING_METHOD_TYPE = CstType.intern(Type.intern("Ldalvik/annotation/EnclosingMethod;"));
        INNER_CLASS_TYPE = CstType.intern(Type.intern("Ldalvik/annotation/InnerClass;"));
        MEMBER_CLASSES_TYPE = CstType.intern(Type.intern("Ldalvik/annotation/MemberClasses;"));
        SIGNATURE_TYPE = CstType.intern(Type.intern("Ldalvik/annotation/Signature;"));
        SOURCE_DEBUG_EXTENSION_TYPE = CstType.intern(Type.intern("Ldalvik/annotation/SourceDebugExtension;"));
        THROWS_TYPE = CstType.intern(Type.intern("Ldalvik/annotation/Throws;"));
        ACCESS_FLAGS_STRING = new CstString("accessFlags");
        NAME_STRING = new CstString("name");
        VALUE_STRING = new CstString("value");
    }
}

package embedded.com.android.dx.rop.code;

import embedded.com.android.dx.util.*;

public final class AccessFlags
{
    public static final int ACC_PUBLIC = 1;
    public static final int ACC_PRIVATE = 2;
    public static final int ACC_PROTECTED = 4;
    public static final int ACC_STATIC = 8;
    public static final int ACC_FINAL = 16;
    public static final int ACC_SYNCHRONIZED = 32;
    public static final int ACC_SUPER = 32;
    public static final int ACC_VOLATILE = 64;
    public static final int ACC_BRIDGE = 64;
    public static final int ACC_TRANSIENT = 128;
    public static final int ACC_VARARGS = 128;
    public static final int ACC_NATIVE = 256;
    public static final int ACC_INTERFACE = 512;
    public static final int ACC_ABSTRACT = 1024;
    public static final int ACC_STRICT = 2048;
    public static final int ACC_SYNTHETIC = 4096;
    public static final int ACC_ANNOTATION = 8192;
    public static final int ACC_ENUM = 16384;
    public static final int ACC_CONSTRUCTOR = 65536;
    public static final int ACC_DECLARED_SYNCHRONIZED = 131072;
    public static final int CLASS_FLAGS = 30257;
    public static final int INNER_CLASS_FLAGS = 30239;
    public static final int FIELD_FLAGS = 20703;
    public static final int METHOD_FLAGS = 204287;
    private static final int CONV_CLASS = 1;
    private static final int CONV_FIELD = 2;
    private static final int CONV_METHOD = 3;
    
    public static String classString(final int flags) {
        return humanHelper(flags, 30257, 1);
    }
    
    public static String innerClassString(final int flags) {
        return humanHelper(flags, 30239, 1);
    }
    
    public static String fieldString(final int flags) {
        return humanHelper(flags, 20703, 2);
    }
    
    public static String methodString(final int flags) {
        return humanHelper(flags, 204287, 3);
    }
    
    public static boolean isPublic(final int flags) {
        return (flags & 0x1) != 0x0;
    }
    
    public static boolean isProtected(final int flags) {
        return (flags & 0x4) != 0x0;
    }
    
    public static boolean isPrivate(final int flags) {
        return (flags & 0x2) != 0x0;
    }
    
    public static boolean isStatic(final int flags) {
        return (flags & 0x8) != 0x0;
    }
    
    public static boolean isConstructor(final int flags) {
        return (flags & 0x10000) != 0x0;
    }
    
    public static boolean isInterface(final int flags) {
        return (flags & 0x200) != 0x0;
    }
    
    public static boolean isSynchronized(final int flags) {
        return (flags & 0x20) != 0x0;
    }
    
    public static boolean isAbstract(final int flags) {
        return (flags & 0x400) != 0x0;
    }
    
    public static boolean isNative(final int flags) {
        return (flags & 0x100) != 0x0;
    }
    
    public static boolean isAnnotation(final int flags) {
        return (flags & 0x2000) != 0x0;
    }
    
    public static boolean isDeclaredSynchronized(final int flags) {
        return (flags & 0x20000) != 0x0;
    }
    
    public static boolean isEnum(final int flags) {
        return (flags & 0x4000) != 0x0;
    }
    
    private static String humanHelper(int flags, final int mask, final int what) {
        final StringBuffer sb = new StringBuffer(80);
        final int extra = flags & ~mask;
        flags &= mask;
        if ((flags & 0x1) != 0x0) {
            sb.append("|public");
        }
        if ((flags & 0x2) != 0x0) {
            sb.append("|private");
        }
        if ((flags & 0x4) != 0x0) {
            sb.append("|protected");
        }
        if ((flags & 0x8) != 0x0) {
            sb.append("|static");
        }
        if ((flags & 0x10) != 0x0) {
            sb.append("|final");
        }
        if ((flags & 0x20) != 0x0) {
            if (what == 1) {
                sb.append("|super");
            }
            else {
                sb.append("|synchronized");
            }
        }
        if ((flags & 0x40) != 0x0) {
            if (what == 3) {
                sb.append("|bridge");
            }
            else {
                sb.append("|volatile");
            }
        }
        if ((flags & 0x80) != 0x0) {
            if (what == 3) {
                sb.append("|varargs");
            }
            else {
                sb.append("|transient");
            }
        }
        if ((flags & 0x100) != 0x0) {
            sb.append("|native");
        }
        if ((flags & 0x200) != 0x0) {
            sb.append("|interface");
        }
        if ((flags & 0x400) != 0x0) {
            sb.append("|abstract");
        }
        if ((flags & 0x800) != 0x0) {
            sb.append("|strictfp");
        }
        if ((flags & 0x1000) != 0x0) {
            sb.append("|synthetic");
        }
        if ((flags & 0x2000) != 0x0) {
            sb.append("|annotation");
        }
        if ((flags & 0x4000) != 0x0) {
            sb.append("|enum");
        }
        if ((flags & 0x10000) != 0x0) {
            sb.append("|constructor");
        }
        if ((flags & 0x20000) != 0x0) {
            sb.append("|declared_synchronized");
        }
        if (extra != 0 || sb.length() == 0) {
            sb.append('|');
            sb.append(Hex.u2(extra));
        }
        return sb.substring(1);
    }
}

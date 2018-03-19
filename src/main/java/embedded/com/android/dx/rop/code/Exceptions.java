package embedded.com.android.dx.rop.code;

import embedded.com.android.dx.rop.type.*;

public final class Exceptions
{
    public static final Type TYPE_ArithmeticException;
    public static final Type TYPE_ArrayIndexOutOfBoundsException;
    public static final Type TYPE_ArrayStoreException;
    public static final Type TYPE_ClassCastException;
    public static final Type TYPE_Error;
    public static final Type TYPE_IllegalMonitorStateException;
    public static final Type TYPE_NegativeArraySizeException;
    public static final Type TYPE_NullPointerException;
    public static final StdTypeList LIST_Error;
    public static final StdTypeList LIST_Error_ArithmeticException;
    public static final StdTypeList LIST_Error_ClassCastException;
    public static final StdTypeList LIST_Error_NegativeArraySizeException;
    public static final StdTypeList LIST_Error_NullPointerException;
    public static final StdTypeList LIST_Error_Null_ArrayIndexOutOfBounds;
    public static final StdTypeList LIST_Error_Null_ArrayIndex_ArrayStore;
    public static final StdTypeList LIST_Error_Null_IllegalMonitorStateException;
    
    static {
        TYPE_ArithmeticException = Type.intern("Ljava/lang/ArithmeticException;");
        TYPE_ArrayIndexOutOfBoundsException = Type.intern("Ljava/lang/ArrayIndexOutOfBoundsException;");
        TYPE_ArrayStoreException = Type.intern("Ljava/lang/ArrayStoreException;");
        TYPE_ClassCastException = Type.intern("Ljava/lang/ClassCastException;");
        TYPE_Error = Type.intern("Ljava/lang/Error;");
        TYPE_IllegalMonitorStateException = Type.intern("Ljava/lang/IllegalMonitorStateException;");
        TYPE_NegativeArraySizeException = Type.intern("Ljava/lang/NegativeArraySizeException;");
        TYPE_NullPointerException = Type.intern("Ljava/lang/NullPointerException;");
        LIST_Error = StdTypeList.make(Exceptions.TYPE_Error);
        LIST_Error_ArithmeticException = StdTypeList.make(Exceptions.TYPE_Error, Exceptions.TYPE_ArithmeticException);
        LIST_Error_ClassCastException = StdTypeList.make(Exceptions.TYPE_Error, Exceptions.TYPE_ClassCastException);
        LIST_Error_NegativeArraySizeException = StdTypeList.make(Exceptions.TYPE_Error, Exceptions.TYPE_NegativeArraySizeException);
        LIST_Error_NullPointerException = StdTypeList.make(Exceptions.TYPE_Error, Exceptions.TYPE_NullPointerException);
        LIST_Error_Null_ArrayIndexOutOfBounds = StdTypeList.make(Exceptions.TYPE_Error, Exceptions.TYPE_NullPointerException, Exceptions.TYPE_ArrayIndexOutOfBoundsException);
        LIST_Error_Null_ArrayIndex_ArrayStore = StdTypeList.make(Exceptions.TYPE_Error, Exceptions.TYPE_NullPointerException, Exceptions.TYPE_ArrayIndexOutOfBoundsException, Exceptions.TYPE_ArrayStoreException);
        LIST_Error_Null_IllegalMonitorStateException = StdTypeList.make(Exceptions.TYPE_Error, Exceptions.TYPE_NullPointerException, Exceptions.TYPE_IllegalMonitorStateException);
    }
}

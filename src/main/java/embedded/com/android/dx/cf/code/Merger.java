package embedded.com.android.dx.cf.code;

import embedded.com.android.dx.util.*;
import embedded.com.android.dx.rop.type.*;

public final class Merger
{
    public static OneLocalsArray mergeLocals(final OneLocalsArray locals1, final OneLocalsArray locals2) {
        if (locals1 == locals2) {
            return locals1;
        }
        final int sz = locals1.getMaxLocals();
        OneLocalsArray result = null;
        if (locals2.getMaxLocals() != sz) {
            throw new SimException("mismatched maxLocals values");
        }
        for (int i = 0; i < sz; ++i) {
            final TypeBearer tb1 = locals1.getOrNull(i);
            final TypeBearer tb2 = locals2.getOrNull(i);
            final TypeBearer resultType = mergeType(tb1, tb2);
            if (resultType != tb1) {
                if (result == null) {
                    result = locals1.copy();
                }
                if (resultType == null) {
                    result.invalidate(i);
                }
                else {
                    result.set(i, resultType);
                }
            }
        }
        if (result == null) {
            return locals1;
        }
        result.setImmutable();
        return result;
    }
    
    public static ExecutionStack mergeStack(final ExecutionStack stack1, final ExecutionStack stack2) {
        if (stack1 == stack2) {
            return stack1;
        }
        final int sz = stack1.size();
        ExecutionStack result = null;
        if (stack2.size() != sz) {
            throw new SimException("mismatched stack depths");
        }
        for (int i = 0; i < sz; ++i) {
            final TypeBearer tb1 = stack1.peek(i);
            final TypeBearer tb2 = stack2.peek(i);
            final TypeBearer resultType = mergeType(tb1, tb2);
            if (resultType != tb1) {
                if (result == null) {
                    result = stack1.copy();
                }
                try {
                    if (resultType == null) {
                        throw new SimException("incompatible: " + tb1 + ", " + tb2);
                    }
                    result.change(i, resultType);
                }
                catch (SimException ex) {
                    ex.addContext("...while merging stack[" + Hex.u2(i) + "]");
                    throw ex;
                }
            }
        }
        if (result == null) {
            return stack1;
        }
        result.setImmutable();
        return result;
    }
    
    public static TypeBearer mergeType(final TypeBearer ft1, final TypeBearer ft2) {
        if (ft1 == null || ft1.equals(ft2)) {
            return ft1;
        }
        if (ft2 == null) {
            return null;
        }
        final Type type1 = ft1.getType();
        final Type type2 = ft2.getType();
        if (type1 == type2) {
            return type1;
        }
        if (type1.isReference() && type2.isReference()) {
            if (type1 == Type.KNOWN_NULL) {
                return type2;
            }
            if (type2 == Type.KNOWN_NULL) {
                return type1;
            }
            if (!type1.isArray() || !type2.isArray()) {
                return Type.OBJECT;
            }
            final TypeBearer componentUnion = mergeType(type1.getComponentType(), type2.getComponentType());
            if (componentUnion == null) {
                return Type.OBJECT;
            }
            return ((Type)componentUnion).getArrayType();
        }
        else {
            if (type1.isIntlike() && type2.isIntlike()) {
                return Type.INT;
            }
            return null;
        }
    }
    
    public static boolean isPossiblyAssignableFrom(final TypeBearer supertypeBearer, final TypeBearer subtypeBearer) {
        Type supertype = supertypeBearer.getType();
        Type subtype = subtypeBearer.getType();
        if (supertype.equals(subtype)) {
            return true;
        }
        int superBt = supertype.getBasicType();
        int subBt = subtype.getBasicType();
        if (superBt == 10) {
            supertype = Type.OBJECT;
            superBt = 9;
        }
        if (subBt == 10) {
            subtype = Type.OBJECT;
            subBt = 9;
        }
        if (superBt != 9 || subBt != 9) {
            return supertype.isIntlike() && subtype.isIntlike();
        }
        if (supertype == Type.KNOWN_NULL) {
            return false;
        }
        if (subtype == Type.KNOWN_NULL) {
            return true;
        }
        if (supertype == Type.OBJECT) {
            return true;
        }
        if (!supertype.isArray()) {
            return !subtype.isArray() || supertype == Type.SERIALIZABLE || supertype == Type.CLONEABLE;
        }
        if (!subtype.isArray()) {
            return false;
        }
        do {
            supertype = supertype.getComponentType();
            subtype = subtype.getComponentType();
        } while (supertype.isArray() && subtype.isArray());
        return isPossiblyAssignableFrom(supertype, subtype);
    }
}

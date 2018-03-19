package embedded.com.android.dx.dex.code;

import java.util.*;
import embedded.com.android.dx.rop.cst.*;
import embedded.com.android.dx.rop.code.*;

public final class RopToDop
{
    private static final HashMap<Rop, Dop> MAP;
    
    public static Dop dopFor(final Insn insn) {
        final Rop rop = insn.getOpcode();
        final Dop result = RopToDop.MAP.get(rop);
        if (result != null) {
            return result;
        }
        Label_0781: {
            switch (rop.getOpcode()) {
                case 4: {
                    return Dops.MOVE_EXCEPTION;
                }
                case 49: {
                    return Dops.INVOKE_STATIC;
                }
                case 50: {
                    return Dops.INVOKE_VIRTUAL;
                }
                case 51: {
                    return Dops.INVOKE_SUPER;
                }
                case 52: {
                    return Dops.INVOKE_DIRECT;
                }
                case 53: {
                    return Dops.INVOKE_INTERFACE;
                }
                case 58: {
                    return Dops.INVOKE_POLYMORPHIC;
                }
                case 59: {
                    return Dops.INVOKE_CUSTOM;
                }
                case 41: {
                    return Dops.NEW_ARRAY;
                }
                case 42: {
                    return Dops.FILLED_NEW_ARRAY;
                }
                case 57: {
                    return Dops.FILL_ARRAY_DATA;
                }
                case 55: {
                    final RegisterSpec resultReg = insn.getResult();
                    if (resultReg == null) {
                        return Dops.NOP;
                    }
                    switch (resultReg.getBasicType()) {
                        case 1:
                        case 2:
                        case 3:
                        case 5:
                        case 6:
                        case 8: {
                            return Dops.MOVE_RESULT;
                        }
                        case 4:
                        case 7: {
                            return Dops.MOVE_RESULT_WIDE;
                        }
                        case 9: {
                            return Dops.MOVE_RESULT_OBJECT;
                        }
                        default: {
                            throw new RuntimeException("Unexpected basic type");
                        }
                    }

                }
                case 45: {
                    final CstFieldRef ref = (CstFieldRef)((ThrowingCstInsn)insn).getConstant();
                    final int basicType = ref.getBasicType();
                    switch (basicType) {
                        case 1: {
                            return Dops.IGET_BOOLEAN;
                        }
                        case 2: {
                            return Dops.IGET_BYTE;
                        }
                        case 3: {
                            return Dops.IGET_CHAR;
                        }
                        case 8: {
                            return Dops.IGET_SHORT;
                        }
                        case 6: {
                            return Dops.IGET;
                        }
                        default: {
                            break Label_0781;
                        }
                    }

                }
                case 47: {
                    final CstFieldRef ref = (CstFieldRef)((ThrowingCstInsn)insn).getConstant();
                    final int basicType = ref.getBasicType();
                    switch (basicType) {
                        case 1: {
                            return Dops.IPUT_BOOLEAN;
                        }
                        case 2: {
                            return Dops.IPUT_BYTE;
                        }
                        case 3: {
                            return Dops.IPUT_CHAR;
                        }
                        case 8: {
                            return Dops.IPUT_SHORT;
                        }
                        case 6: {
                            return Dops.IPUT;
                        }
                        default: {
                            break Label_0781;
                        }
                    }

                }
                case 46: {
                    final CstFieldRef ref = (CstFieldRef)((ThrowingCstInsn)insn).getConstant();
                    final int basicType = ref.getBasicType();
                    switch (basicType) {
                        case 1: {
                            return Dops.SGET_BOOLEAN;
                        }
                        case 2: {
                            return Dops.SGET_BYTE;
                        }
                        case 3: {
                            return Dops.SGET_CHAR;
                        }
                        case 8: {
                            return Dops.SGET_SHORT;
                        }
                        case 6: {
                            return Dops.SGET;
                        }
                        default: {
                            break Label_0781;
                        }
                    }

                }
                case 48: {
                    final CstFieldRef ref = (CstFieldRef)((ThrowingCstInsn)insn).getConstant();
                    final int basicType = ref.getBasicType();
                    switch (basicType) {
                        case 1: {
                            return Dops.SPUT_BOOLEAN;
                        }
                        case 2: {
                            return Dops.SPUT_BYTE;
                        }
                        case 3: {
                            return Dops.SPUT_CHAR;
                        }
                        case 8: {
                            return Dops.SPUT_SHORT;
                        }
                        case 6: {
                            return Dops.SPUT;
                        }
                        default: {
                            break Label_0781;
                        }
                    }
                }
                case 5: {
                    final Constant cst = ((ThrowingCstInsn)insn).getConstant();
                    if (cst instanceof CstType) {
                        return Dops.CONST_CLASS;
                    }
                    if (cst instanceof CstString) {
                        return Dops.CONST_STRING;
                    }
                    break;
                }
            }
        }
        throw new RuntimeException("unknown rop: " + rop);
    }
    
    static {
        (MAP = new HashMap<Rop, Dop>(400)).put(Rops.NOP, Dops.NOP);
        RopToDop.MAP.put(Rops.MOVE_INT, Dops.MOVE);
        RopToDop.MAP.put(Rops.MOVE_LONG, Dops.MOVE_WIDE);
        RopToDop.MAP.put(Rops.MOVE_FLOAT, Dops.MOVE);
        RopToDop.MAP.put(Rops.MOVE_DOUBLE, Dops.MOVE_WIDE);
        RopToDop.MAP.put(Rops.MOVE_OBJECT, Dops.MOVE_OBJECT);
        RopToDop.MAP.put(Rops.MOVE_PARAM_INT, Dops.MOVE);
        RopToDop.MAP.put(Rops.MOVE_PARAM_LONG, Dops.MOVE_WIDE);
        RopToDop.MAP.put(Rops.MOVE_PARAM_FLOAT, Dops.MOVE);
        RopToDop.MAP.put(Rops.MOVE_PARAM_DOUBLE, Dops.MOVE_WIDE);
        RopToDop.MAP.put(Rops.MOVE_PARAM_OBJECT, Dops.MOVE_OBJECT);
        RopToDop.MAP.put(Rops.CONST_INT, Dops.CONST_4);
        RopToDop.MAP.put(Rops.CONST_LONG, Dops.CONST_WIDE_16);
        RopToDop.MAP.put(Rops.CONST_FLOAT, Dops.CONST_4);
        RopToDop.MAP.put(Rops.CONST_DOUBLE, Dops.CONST_WIDE_16);
        RopToDop.MAP.put(Rops.CONST_OBJECT_NOTHROW, Dops.CONST_4);
        RopToDop.MAP.put(Rops.GOTO, Dops.GOTO);
        RopToDop.MAP.put(Rops.IF_EQZ_INT, Dops.IF_EQZ);
        RopToDop.MAP.put(Rops.IF_NEZ_INT, Dops.IF_NEZ);
        RopToDop.MAP.put(Rops.IF_LTZ_INT, Dops.IF_LTZ);
        RopToDop.MAP.put(Rops.IF_GEZ_INT, Dops.IF_GEZ);
        RopToDop.MAP.put(Rops.IF_LEZ_INT, Dops.IF_LEZ);
        RopToDop.MAP.put(Rops.IF_GTZ_INT, Dops.IF_GTZ);
        RopToDop.MAP.put(Rops.IF_EQZ_OBJECT, Dops.IF_EQZ);
        RopToDop.MAP.put(Rops.IF_NEZ_OBJECT, Dops.IF_NEZ);
        RopToDop.MAP.put(Rops.IF_EQ_INT, Dops.IF_EQ);
        RopToDop.MAP.put(Rops.IF_NE_INT, Dops.IF_NE);
        RopToDop.MAP.put(Rops.IF_LT_INT, Dops.IF_LT);
        RopToDop.MAP.put(Rops.IF_GE_INT, Dops.IF_GE);
        RopToDop.MAP.put(Rops.IF_LE_INT, Dops.IF_LE);
        RopToDop.MAP.put(Rops.IF_GT_INT, Dops.IF_GT);
        RopToDop.MAP.put(Rops.IF_EQ_OBJECT, Dops.IF_EQ);
        RopToDop.MAP.put(Rops.IF_NE_OBJECT, Dops.IF_NE);
        RopToDop.MAP.put(Rops.SWITCH, Dops.SPARSE_SWITCH);
        RopToDop.MAP.put(Rops.ADD_INT, Dops.ADD_INT_2ADDR);
        RopToDop.MAP.put(Rops.ADD_LONG, Dops.ADD_LONG_2ADDR);
        RopToDop.MAP.put(Rops.ADD_FLOAT, Dops.ADD_FLOAT_2ADDR);
        RopToDop.MAP.put(Rops.ADD_DOUBLE, Dops.ADD_DOUBLE_2ADDR);
        RopToDop.MAP.put(Rops.SUB_INT, Dops.SUB_INT_2ADDR);
        RopToDop.MAP.put(Rops.SUB_LONG, Dops.SUB_LONG_2ADDR);
        RopToDop.MAP.put(Rops.SUB_FLOAT, Dops.SUB_FLOAT_2ADDR);
        RopToDop.MAP.put(Rops.SUB_DOUBLE, Dops.SUB_DOUBLE_2ADDR);
        RopToDop.MAP.put(Rops.MUL_INT, Dops.MUL_INT_2ADDR);
        RopToDop.MAP.put(Rops.MUL_LONG, Dops.MUL_LONG_2ADDR);
        RopToDop.MAP.put(Rops.MUL_FLOAT, Dops.MUL_FLOAT_2ADDR);
        RopToDop.MAP.put(Rops.MUL_DOUBLE, Dops.MUL_DOUBLE_2ADDR);
        RopToDop.MAP.put(Rops.DIV_INT, Dops.DIV_INT_2ADDR);
        RopToDop.MAP.put(Rops.DIV_LONG, Dops.DIV_LONG_2ADDR);
        RopToDop.MAP.put(Rops.DIV_FLOAT, Dops.DIV_FLOAT_2ADDR);
        RopToDop.MAP.put(Rops.DIV_DOUBLE, Dops.DIV_DOUBLE_2ADDR);
        RopToDop.MAP.put(Rops.REM_INT, Dops.REM_INT_2ADDR);
        RopToDop.MAP.put(Rops.REM_LONG, Dops.REM_LONG_2ADDR);
        RopToDop.MAP.put(Rops.REM_FLOAT, Dops.REM_FLOAT_2ADDR);
        RopToDop.MAP.put(Rops.REM_DOUBLE, Dops.REM_DOUBLE_2ADDR);
        RopToDop.MAP.put(Rops.NEG_INT, Dops.NEG_INT);
        RopToDop.MAP.put(Rops.NEG_LONG, Dops.NEG_LONG);
        RopToDop.MAP.put(Rops.NEG_FLOAT, Dops.NEG_FLOAT);
        RopToDop.MAP.put(Rops.NEG_DOUBLE, Dops.NEG_DOUBLE);
        RopToDop.MAP.put(Rops.AND_INT, Dops.AND_INT_2ADDR);
        RopToDop.MAP.put(Rops.AND_LONG, Dops.AND_LONG_2ADDR);
        RopToDop.MAP.put(Rops.OR_INT, Dops.OR_INT_2ADDR);
        RopToDop.MAP.put(Rops.OR_LONG, Dops.OR_LONG_2ADDR);
        RopToDop.MAP.put(Rops.XOR_INT, Dops.XOR_INT_2ADDR);
        RopToDop.MAP.put(Rops.XOR_LONG, Dops.XOR_LONG_2ADDR);
        RopToDop.MAP.put(Rops.SHL_INT, Dops.SHL_INT_2ADDR);
        RopToDop.MAP.put(Rops.SHL_LONG, Dops.SHL_LONG_2ADDR);
        RopToDop.MAP.put(Rops.SHR_INT, Dops.SHR_INT_2ADDR);
        RopToDop.MAP.put(Rops.SHR_LONG, Dops.SHR_LONG_2ADDR);
        RopToDop.MAP.put(Rops.USHR_INT, Dops.USHR_INT_2ADDR);
        RopToDop.MAP.put(Rops.USHR_LONG, Dops.USHR_LONG_2ADDR);
        RopToDop.MAP.put(Rops.NOT_INT, Dops.NOT_INT);
        RopToDop.MAP.put(Rops.NOT_LONG, Dops.NOT_LONG);
        RopToDop.MAP.put(Rops.ADD_CONST_INT, Dops.ADD_INT_LIT8);
        RopToDop.MAP.put(Rops.SUB_CONST_INT, Dops.RSUB_INT_LIT8);
        RopToDop.MAP.put(Rops.MUL_CONST_INT, Dops.MUL_INT_LIT8);
        RopToDop.MAP.put(Rops.DIV_CONST_INT, Dops.DIV_INT_LIT8);
        RopToDop.MAP.put(Rops.REM_CONST_INT, Dops.REM_INT_LIT8);
        RopToDop.MAP.put(Rops.AND_CONST_INT, Dops.AND_INT_LIT8);
        RopToDop.MAP.put(Rops.OR_CONST_INT, Dops.OR_INT_LIT8);
        RopToDop.MAP.put(Rops.XOR_CONST_INT, Dops.XOR_INT_LIT8);
        RopToDop.MAP.put(Rops.SHL_CONST_INT, Dops.SHL_INT_LIT8);
        RopToDop.MAP.put(Rops.SHR_CONST_INT, Dops.SHR_INT_LIT8);
        RopToDop.MAP.put(Rops.USHR_CONST_INT, Dops.USHR_INT_LIT8);
        RopToDop.MAP.put(Rops.CMPL_LONG, Dops.CMP_LONG);
        RopToDop.MAP.put(Rops.CMPL_FLOAT, Dops.CMPL_FLOAT);
        RopToDop.MAP.put(Rops.CMPL_DOUBLE, Dops.CMPL_DOUBLE);
        RopToDop.MAP.put(Rops.CMPG_FLOAT, Dops.CMPG_FLOAT);
        RopToDop.MAP.put(Rops.CMPG_DOUBLE, Dops.CMPG_DOUBLE);
        RopToDop.MAP.put(Rops.CONV_L2I, Dops.LONG_TO_INT);
        RopToDop.MAP.put(Rops.CONV_F2I, Dops.FLOAT_TO_INT);
        RopToDop.MAP.put(Rops.CONV_D2I, Dops.DOUBLE_TO_INT);
        RopToDop.MAP.put(Rops.CONV_I2L, Dops.INT_TO_LONG);
        RopToDop.MAP.put(Rops.CONV_F2L, Dops.FLOAT_TO_LONG);
        RopToDop.MAP.put(Rops.CONV_D2L, Dops.DOUBLE_TO_LONG);
        RopToDop.MAP.put(Rops.CONV_I2F, Dops.INT_TO_FLOAT);
        RopToDop.MAP.put(Rops.CONV_L2F, Dops.LONG_TO_FLOAT);
        RopToDop.MAP.put(Rops.CONV_D2F, Dops.DOUBLE_TO_FLOAT);
        RopToDop.MAP.put(Rops.CONV_I2D, Dops.INT_TO_DOUBLE);
        RopToDop.MAP.put(Rops.CONV_L2D, Dops.LONG_TO_DOUBLE);
        RopToDop.MAP.put(Rops.CONV_F2D, Dops.FLOAT_TO_DOUBLE);
        RopToDop.MAP.put(Rops.TO_BYTE, Dops.INT_TO_BYTE);
        RopToDop.MAP.put(Rops.TO_CHAR, Dops.INT_TO_CHAR);
        RopToDop.MAP.put(Rops.TO_SHORT, Dops.INT_TO_SHORT);
        RopToDop.MAP.put(Rops.RETURN_VOID, Dops.RETURN_VOID);
        RopToDop.MAP.put(Rops.RETURN_INT, Dops.RETURN);
        RopToDop.MAP.put(Rops.RETURN_LONG, Dops.RETURN_WIDE);
        RopToDop.MAP.put(Rops.RETURN_FLOAT, Dops.RETURN);
        RopToDop.MAP.put(Rops.RETURN_DOUBLE, Dops.RETURN_WIDE);
        RopToDop.MAP.put(Rops.RETURN_OBJECT, Dops.RETURN_OBJECT);
        RopToDop.MAP.put(Rops.ARRAY_LENGTH, Dops.ARRAY_LENGTH);
        RopToDop.MAP.put(Rops.THROW, Dops.THROW);
        RopToDop.MAP.put(Rops.MONITOR_ENTER, Dops.MONITOR_ENTER);
        RopToDop.MAP.put(Rops.MONITOR_EXIT, Dops.MONITOR_EXIT);
        RopToDop.MAP.put(Rops.AGET_INT, Dops.AGET);
        RopToDop.MAP.put(Rops.AGET_LONG, Dops.AGET_WIDE);
        RopToDop.MAP.put(Rops.AGET_FLOAT, Dops.AGET);
        RopToDop.MAP.put(Rops.AGET_DOUBLE, Dops.AGET_WIDE);
        RopToDop.MAP.put(Rops.AGET_OBJECT, Dops.AGET_OBJECT);
        RopToDop.MAP.put(Rops.AGET_BOOLEAN, Dops.AGET_BOOLEAN);
        RopToDop.MAP.put(Rops.AGET_BYTE, Dops.AGET_BYTE);
        RopToDop.MAP.put(Rops.AGET_CHAR, Dops.AGET_CHAR);
        RopToDop.MAP.put(Rops.AGET_SHORT, Dops.AGET_SHORT);
        RopToDop.MAP.put(Rops.APUT_INT, Dops.APUT);
        RopToDop.MAP.put(Rops.APUT_LONG, Dops.APUT_WIDE);
        RopToDop.MAP.put(Rops.APUT_FLOAT, Dops.APUT);
        RopToDop.MAP.put(Rops.APUT_DOUBLE, Dops.APUT_WIDE);
        RopToDop.MAP.put(Rops.APUT_OBJECT, Dops.APUT_OBJECT);
        RopToDop.MAP.put(Rops.APUT_BOOLEAN, Dops.APUT_BOOLEAN);
        RopToDop.MAP.put(Rops.APUT_BYTE, Dops.APUT_BYTE);
        RopToDop.MAP.put(Rops.APUT_CHAR, Dops.APUT_CHAR);
        RopToDop.MAP.put(Rops.APUT_SHORT, Dops.APUT_SHORT);
        RopToDop.MAP.put(Rops.NEW_INSTANCE, Dops.NEW_INSTANCE);
        RopToDop.MAP.put(Rops.CHECK_CAST, Dops.CHECK_CAST);
        RopToDop.MAP.put(Rops.INSTANCE_OF, Dops.INSTANCE_OF);
        RopToDop.MAP.put(Rops.GET_FIELD_LONG, Dops.IGET_WIDE);
        RopToDop.MAP.put(Rops.GET_FIELD_FLOAT, Dops.IGET);
        RopToDop.MAP.put(Rops.GET_FIELD_DOUBLE, Dops.IGET_WIDE);
        RopToDop.MAP.put(Rops.GET_FIELD_OBJECT, Dops.IGET_OBJECT);
        RopToDop.MAP.put(Rops.GET_STATIC_LONG, Dops.SGET_WIDE);
        RopToDop.MAP.put(Rops.GET_STATIC_FLOAT, Dops.SGET);
        RopToDop.MAP.put(Rops.GET_STATIC_DOUBLE, Dops.SGET_WIDE);
        RopToDop.MAP.put(Rops.GET_STATIC_OBJECT, Dops.SGET_OBJECT);
        RopToDop.MAP.put(Rops.PUT_FIELD_LONG, Dops.IPUT_WIDE);
        RopToDop.MAP.put(Rops.PUT_FIELD_FLOAT, Dops.IPUT);
        RopToDop.MAP.put(Rops.PUT_FIELD_DOUBLE, Dops.IPUT_WIDE);
        RopToDop.MAP.put(Rops.PUT_FIELD_OBJECT, Dops.IPUT_OBJECT);
        RopToDop.MAP.put(Rops.PUT_STATIC_LONG, Dops.SPUT_WIDE);
        RopToDop.MAP.put(Rops.PUT_STATIC_FLOAT, Dops.SPUT);
        RopToDop.MAP.put(Rops.PUT_STATIC_DOUBLE, Dops.SPUT_WIDE);
        RopToDop.MAP.put(Rops.PUT_STATIC_OBJECT, Dops.SPUT_OBJECT);
    }
}

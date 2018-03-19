package embedded.com.android.dx.cf.code;

import embedded.com.android.dx.rop.type.*;
import embedded.com.android.dx.util.*;
import java.util.*;
import embedded.com.android.dx.rop.cst.*;

public final class BytecodeArray
{
    public static final Visitor EMPTY_VISITOR;
    private final ByteArray bytes;
    private final ConstantPool pool;
    
    public BytecodeArray(final ByteArray bytes, final ConstantPool pool) {
        if (bytes == null) {
            throw new NullPointerException("bytes == null");
        }
        if (pool == null) {
            throw new NullPointerException("pool == null");
        }
        this.bytes = bytes;
        this.pool = pool;
    }
    
    public ByteArray getBytes() {
        return this.bytes;
    }
    
    public int size() {
        return this.bytes.size();
    }
    
    public int byteLength() {
        return 4 + this.bytes.size();
    }
    
    public void forEach(final Visitor visitor) {
        for (int sz = this.bytes.size(), at = 0; at < sz; at += this.parseInstruction(at, visitor)) {}
    }
    
    public int[] getInstructionOffsets() {
        final int sz = this.bytes.size();
        final int[] result = Bits.makeBitSet(sz);
        int length;
        for (int at = 0; at < sz; at += length) {
            Bits.set(result, at, true);
            length = this.parseInstruction(at, null);
        }
        return result;
    }
    
    public void processWorkSet(final int[] workSet, final Visitor visitor) {
        if (visitor == null) {
            throw new NullPointerException("visitor == null");
        }
        while (true) {
            final int offset = Bits.findFirst(workSet, 0);
            if (offset < 0) {
                break;
            }
            Bits.clear(workSet, offset);
            this.parseInstruction(offset, visitor);
            visitor.setPreviousOffset(offset);
        }
    }
    
    public int parseInstruction(final int offset, Visitor visitor) {
        if (visitor == null) {
            visitor = BytecodeArray.EMPTY_VISITOR;
        }
        try {
            final int opcode = this.bytes.getUnsignedByte(offset);
            final int info = ByteOps.opInfo(opcode);
            final int fmt = info & 0x1F;
            switch (opcode) {
                case 0: {
                    visitor.visitNoArgs(opcode, offset, 1, Type.VOID);
                    return 1;
                }
                case 1: {
                    visitor.visitConstant(18, offset, 1, CstKnownNull.THE_ONE, 0);
                    return 1;
                }
                case 2: {
                    visitor.visitConstant(18, offset, 1, CstInteger.VALUE_M1, -1);
                    return 1;
                }
                case 3: {
                    visitor.visitConstant(18, offset, 1, CstInteger.VALUE_0, 0);
                    return 1;
                }
                case 4: {
                    visitor.visitConstant(18, offset, 1, CstInteger.VALUE_1, 1);
                    return 1;
                }
                case 5: {
                    visitor.visitConstant(18, offset, 1, CstInteger.VALUE_2, 2);
                    return 1;
                }
                case 6: {
                    visitor.visitConstant(18, offset, 1, CstInteger.VALUE_3, 3);
                    return 1;
                }
                case 7: {
                    visitor.visitConstant(18, offset, 1, CstInteger.VALUE_4, 4);
                    return 1;
                }
                case 8: {
                    visitor.visitConstant(18, offset, 1, CstInteger.VALUE_5, 5);
                    return 1;
                }
                case 9: {
                    visitor.visitConstant(18, offset, 1, CstLong.VALUE_0, 0);
                    return 1;
                }
                case 10: {
                    visitor.visitConstant(18, offset, 1, CstLong.VALUE_1, 0);
                    return 1;
                }
                case 11: {
                    visitor.visitConstant(18, offset, 1, CstFloat.VALUE_0, 0);
                    return 1;
                }
                case 12: {
                    visitor.visitConstant(18, offset, 1, CstFloat.VALUE_1, 0);
                    return 1;
                }
                case 13: {
                    visitor.visitConstant(18, offset, 1, CstFloat.VALUE_2, 0);
                    return 1;
                }
                case 14: {
                    visitor.visitConstant(18, offset, 1, CstDouble.VALUE_0, 0);
                    return 1;
                }
                case 15: {
                    visitor.visitConstant(18, offset, 1, CstDouble.VALUE_1, 0);
                    return 1;
                }
                case 16: {
                    final int value = this.bytes.getByte(offset + 1);
                    visitor.visitConstant(18, offset, 2, CstInteger.make(value), value);
                    return 2;
                }
                case 17: {
                    final int value = this.bytes.getShort(offset + 1);
                    visitor.visitConstant(18, offset, 3, CstInteger.make(value), value);
                    return 3;
                }
                case 18: {
                    final int idx = this.bytes.getUnsignedByte(offset + 1);
                    final Constant cst = this.pool.get(idx);
                    final int value2 = (cst instanceof CstInteger) ? ((CstInteger)cst).getValue() : 0;
                    visitor.visitConstant(18, offset, 2, cst, value2);
                    return 2;
                }
                case 19: {
                    final int idx = this.bytes.getUnsignedShort(offset + 1);
                    final Constant cst = this.pool.get(idx);
                    final int value2 = (cst instanceof CstInteger) ? ((CstInteger)cst).getValue() : 0;
                    visitor.visitConstant(18, offset, 3, cst, value2);
                    return 3;
                }
                case 20: {
                    final int idx = this.bytes.getUnsignedShort(offset + 1);
                    final Constant cst = this.pool.get(idx);
                    visitor.visitConstant(20, offset, 3, cst, 0);
                    return 3;
                }
                case 21: {
                    final int idx = this.bytes.getUnsignedByte(offset + 1);
                    visitor.visitLocal(21, offset, 2, idx, Type.INT, 0);
                    return 2;
                }
                case 22: {
                    final int idx = this.bytes.getUnsignedByte(offset + 1);
                    visitor.visitLocal(21, offset, 2, idx, Type.LONG, 0);
                    return 2;
                }
                case 23: {
                    final int idx = this.bytes.getUnsignedByte(offset + 1);
                    visitor.visitLocal(21, offset, 2, idx, Type.FLOAT, 0);
                    return 2;
                }
                case 24: {
                    final int idx = this.bytes.getUnsignedByte(offset + 1);
                    visitor.visitLocal(21, offset, 2, idx, Type.DOUBLE, 0);
                    return 2;
                }
                case 25: {
                    final int idx = this.bytes.getUnsignedByte(offset + 1);
                    visitor.visitLocal(21, offset, 2, idx, Type.OBJECT, 0);
                    return 2;
                }
                case 26:
                case 27:
                case 28:
                case 29: {
                    final int idx = opcode - 26;
                    visitor.visitLocal(21, offset, 1, idx, Type.INT, 0);
                    return 1;
                }
                case 30:
                case 31:
                case 32:
                case 33: {
                    final int idx = opcode - 30;
                    visitor.visitLocal(21, offset, 1, idx, Type.LONG, 0);
                    return 1;
                }
                case 34:
                case 35:
                case 36:
                case 37: {
                    final int idx = opcode - 34;
                    visitor.visitLocal(21, offset, 1, idx, Type.FLOAT, 0);
                    return 1;
                }
                case 38:
                case 39:
                case 40:
                case 41: {
                    final int idx = opcode - 38;
                    visitor.visitLocal(21, offset, 1, idx, Type.DOUBLE, 0);
                    return 1;
                }
                case 42:
                case 43:
                case 44:
                case 45: {
                    final int idx = opcode - 42;
                    visitor.visitLocal(21, offset, 1, idx, Type.OBJECT, 0);
                    return 1;
                }
                case 46: {
                    visitor.visitNoArgs(46, offset, 1, Type.INT);
                    return 1;
                }
                case 47: {
                    visitor.visitNoArgs(46, offset, 1, Type.LONG);
                    return 1;
                }
                case 48: {
                    visitor.visitNoArgs(46, offset, 1, Type.FLOAT);
                    return 1;
                }
                case 49: {
                    visitor.visitNoArgs(46, offset, 1, Type.DOUBLE);
                    return 1;
                }
                case 50: {
                    visitor.visitNoArgs(46, offset, 1, Type.OBJECT);
                    return 1;
                }
                case 51: {
                    visitor.visitNoArgs(46, offset, 1, Type.BYTE);
                    return 1;
                }
                case 52: {
                    visitor.visitNoArgs(46, offset, 1, Type.CHAR);
                    return 1;
                }
                case 53: {
                    visitor.visitNoArgs(46, offset, 1, Type.SHORT);
                    return 1;
                }
                case 54: {
                    final int idx = this.bytes.getUnsignedByte(offset + 1);
                    visitor.visitLocal(54, offset, 2, idx, Type.INT, 0);
                    return 2;
                }
                case 55: {
                    final int idx = this.bytes.getUnsignedByte(offset + 1);
                    visitor.visitLocal(54, offset, 2, idx, Type.LONG, 0);
                    return 2;
                }
                case 56: {
                    final int idx = this.bytes.getUnsignedByte(offset + 1);
                    visitor.visitLocal(54, offset, 2, idx, Type.FLOAT, 0);
                    return 2;
                }
                case 57: {
                    final int idx = this.bytes.getUnsignedByte(offset + 1);
                    visitor.visitLocal(54, offset, 2, idx, Type.DOUBLE, 0);
                    return 2;
                }
                case 58: {
                    final int idx = this.bytes.getUnsignedByte(offset + 1);
                    visitor.visitLocal(54, offset, 2, idx, Type.OBJECT, 0);
                    return 2;
                }
                case 59:
                case 60:
                case 61:
                case 62: {
                    final int idx = opcode - 59;
                    visitor.visitLocal(54, offset, 1, idx, Type.INT, 0);
                    return 1;
                }
                case 63:
                case 64:
                case 65:
                case 66: {
                    final int idx = opcode - 63;
                    visitor.visitLocal(54, offset, 1, idx, Type.LONG, 0);
                    return 1;
                }
                case 67:
                case 68:
                case 69:
                case 70: {
                    final int idx = opcode - 67;
                    visitor.visitLocal(54, offset, 1, idx, Type.FLOAT, 0);
                    return 1;
                }
                case 71:
                case 72:
                case 73:
                case 74: {
                    final int idx = opcode - 71;
                    visitor.visitLocal(54, offset, 1, idx, Type.DOUBLE, 0);
                    return 1;
                }
                case 75:
                case 76:
                case 77:
                case 78: {
                    final int idx = opcode - 75;
                    visitor.visitLocal(54, offset, 1, idx, Type.OBJECT, 0);
                    return 1;
                }
                case 79: {
                    visitor.visitNoArgs(79, offset, 1, Type.INT);
                    return 1;
                }
                case 80: {
                    visitor.visitNoArgs(79, offset, 1, Type.LONG);
                    return 1;
                }
                case 81: {
                    visitor.visitNoArgs(79, offset, 1, Type.FLOAT);
                    return 1;
                }
                case 82: {
                    visitor.visitNoArgs(79, offset, 1, Type.DOUBLE);
                    return 1;
                }
                case 83: {
                    visitor.visitNoArgs(79, offset, 1, Type.OBJECT);
                    return 1;
                }
                case 84: {
                    visitor.visitNoArgs(79, offset, 1, Type.BYTE);
                    return 1;
                }
                case 85: {
                    visitor.visitNoArgs(79, offset, 1, Type.CHAR);
                    return 1;
                }
                case 86: {
                    visitor.visitNoArgs(79, offset, 1, Type.SHORT);
                    return 1;
                }
                case 87:
                case 88:
                case 89:
                case 90:
                case 91:
                case 92:
                case 93:
                case 94:
                case 95: {
                    visitor.visitNoArgs(opcode, offset, 1, Type.VOID);
                    return 1;
                }
                case 96:
                case 100:
                case 104:
                case 108:
                case 112:
                case 116:
                case 120:
                case 122:
                case 124:
                case 126:
                case 128:
                case 130: {
                    visitor.visitNoArgs(opcode, offset, 1, Type.INT);
                    return 1;
                }
                case 97:
                case 101:
                case 105:
                case 109:
                case 113:
                case 117:
                case 121:
                case 123:
                case 125:
                case 127:
                case 129:
                case 131: {
                    visitor.visitNoArgs(opcode - 1, offset, 1, Type.LONG);
                    return 1;
                }
                case 98:
                case 102:
                case 106:
                case 110:
                case 114:
                case 118: {
                    visitor.visitNoArgs(opcode - 2, offset, 1, Type.FLOAT);
                    return 1;
                }
                case 99:
                case 103:
                case 107:
                case 111:
                case 115:
                case 119: {
                    visitor.visitNoArgs(opcode - 3, offset, 1, Type.DOUBLE);
                    return 1;
                }
                case 132: {
                    final int idx = this.bytes.getUnsignedByte(offset + 1);
                    final int value3 = this.bytes.getByte(offset + 2);
                    visitor.visitLocal(opcode, offset, 3, idx, Type.INT, value3);
                    return 3;
                }
                case 133:
                case 140:
                case 143: {
                    visitor.visitNoArgs(opcode, offset, 1, Type.LONG);
                    return 1;
                }
                case 134:
                case 137:
                case 144: {
                    visitor.visitNoArgs(opcode, offset, 1, Type.FLOAT);
                    return 1;
                }
                case 135:
                case 138:
                case 141: {
                    visitor.visitNoArgs(opcode, offset, 1, Type.DOUBLE);
                    return 1;
                }
                case 136:
                case 139:
                case 142:
                case 145:
                case 146:
                case 147:
                case 148:
                case 149:
                case 150:
                case 151:
                case 152:
                case 190: {
                    visitor.visitNoArgs(opcode, offset, 1, Type.INT);
                    return 1;
                }
                case 153:
                case 154:
                case 155:
                case 156:
                case 157:
                case 158:
                case 159:
                case 160:
                case 161:
                case 162:
                case 163:
                case 164:
                case 165:
                case 166:
                case 167:
                case 168:
                case 198:
                case 199: {
                    final int target = offset + this.bytes.getShort(offset + 1);
                    visitor.visitBranch(opcode, offset, 3, target);
                    return 3;
                }
                case 169: {
                    final int idx = this.bytes.getUnsignedByte(offset + 1);
                    visitor.visitLocal(opcode, offset, 2, idx, Type.RETURN_ADDRESS, 0);
                    return 2;
                }
                case 170: {
                    return this.parseTableswitch(offset, visitor);
                }
                case 171: {
                    return this.parseLookupswitch(offset, visitor);
                }
                case 172: {
                    visitor.visitNoArgs(172, offset, 1, Type.INT);
                    return 1;
                }
                case 173: {
                    visitor.visitNoArgs(172, offset, 1, Type.LONG);
                    return 1;
                }
                case 174: {
                    visitor.visitNoArgs(172, offset, 1, Type.FLOAT);
                    return 1;
                }
                case 175: {
                    visitor.visitNoArgs(172, offset, 1, Type.DOUBLE);
                    return 1;
                }
                case 176: {
                    visitor.visitNoArgs(172, offset, 1, Type.OBJECT);
                    return 1;
                }
                case 177:
                case 191:
                case 194:
                case 195: {
                    visitor.visitNoArgs(opcode, offset, 1, Type.VOID);
                    return 1;
                }
                case 178:
                case 179:
                case 180:
                case 181:
                case 182:
                case 183:
                case 184:
                case 187:
                case 189:
                case 192:
                case 193: {
                    final int idx = this.bytes.getUnsignedShort(offset + 1);
                    final Constant cst = this.pool.get(idx);
                    visitor.visitConstant(opcode, offset, 3, cst, 0);
                    return 3;
                }
                case 185: {
                    final int idx = this.bytes.getUnsignedShort(offset + 1);
                    final int count = this.bytes.getUnsignedByte(offset + 3);
                    final int expectZero = this.bytes.getUnsignedByte(offset + 4);
                    final Constant cst2 = this.pool.get(idx);
                    visitor.visitConstant(opcode, offset, 5, cst2, count | expectZero << 8);
                    return 5;
                }
                case 186: {
                    final int idx = this.bytes.getUnsignedShort(offset + 1);
                    final CstInvokeDynamic cstInvokeDynamic = (CstInvokeDynamic)this.pool.get(idx);
                    visitor.visitConstant(opcode, offset, 5, cstInvokeDynamic, 0);
                    return 5;
                }
                case 188: {
                    return this.parseNewarray(offset, visitor);
                }
                case 196: {
                    return this.parseWide(offset, visitor);
                }
                case 197: {
                    final int idx = this.bytes.getUnsignedShort(offset + 1);
                    final int dimensions = this.bytes.getUnsignedByte(offset + 3);
                    final Constant cst3 = this.pool.get(idx);
                    visitor.visitConstant(opcode, offset, 4, cst3, dimensions);
                    return 4;
                }
                case 200:
                case 201: {
                    final int target = offset + this.bytes.getInt(offset + 1);
                    final int newop = (opcode == 200) ? 167 : 168;
                    visitor.visitBranch(newop, offset, 5, target);
                    return 5;
                }
                default: {
                    visitor.visitInvalid(opcode, offset, 1);
                    return 1;
                }
            }
        }
        catch (SimException ex) {
            ex.addContext("...at bytecode offset " + Hex.u4(offset));
            throw ex;
        }
        catch (RuntimeException ex2) {
            final SimException se = new SimException(ex2);
            se.addContext("...at bytecode offset " + Hex.u4(offset));
            throw se;
        }
    }
    
    private int parseTableswitch(final int offset, final Visitor visitor) {
        int at = offset + 4 & 0xFFFFFFFC;
        int padding = 0;
        for (int i = offset + 1; i < at; ++i) {
            padding = (padding << 8 | this.bytes.getUnsignedByte(i));
        }
        final int defaultTarget = offset + this.bytes.getInt(at);
        final int low = this.bytes.getInt(at + 4);
        final int high = this.bytes.getInt(at + 8);
        final int count = high - low + 1;
        at += 12;
        if (low > high) {
            throw new SimException("low / high inversion");
        }
        final SwitchList cases = new SwitchList(count);
        for (int j = 0; j < count; ++j) {
            final int target = offset + this.bytes.getInt(at);
            at += 4;
            cases.add(low + j, target);
        }
        cases.setDefaultTarget(defaultTarget);
        cases.removeSuperfluousDefaults();
        cases.setImmutable();
        final int length = at - offset;
        visitor.visitSwitch(171, offset, length, cases, padding);
        return length;
    }
    
    private int parseLookupswitch(final int offset, final Visitor visitor) {
        int at = offset + 4 & 0xFFFFFFFC;
        int padding = 0;
        for (int i = offset + 1; i < at; ++i) {
            padding = (padding << 8 | this.bytes.getUnsignedByte(i));
        }
        final int defaultTarget = offset + this.bytes.getInt(at);
        final int npairs = this.bytes.getInt(at + 4);
        at += 8;
        final SwitchList cases = new SwitchList(npairs);
        for (int j = 0; j < npairs; ++j) {
            final int match = this.bytes.getInt(at);
            final int target = offset + this.bytes.getInt(at + 4);
            at += 8;
            cases.add(match, target);
        }
        cases.setDefaultTarget(defaultTarget);
        cases.removeSuperfluousDefaults();
        cases.setImmutable();
        final int length = at - offset;
        visitor.visitSwitch(171, offset, length, cases, padding);
        return length;
    }
    
    private int parseNewarray(final int offset, final Visitor visitor) {
        final int value = this.bytes.getUnsignedByte(offset + 1);
        CstType type = null;
        switch (value) {
            case 4: {
                type = CstType.BOOLEAN_ARRAY;
                break;
            }
            case 5: {
                type = CstType.CHAR_ARRAY;
                break;
            }
            case 7: {
                type = CstType.DOUBLE_ARRAY;
                break;
            }
            case 6: {
                type = CstType.FLOAT_ARRAY;
                break;
            }
            case 8: {
                type = CstType.BYTE_ARRAY;
                break;
            }
            case 9: {
                type = CstType.SHORT_ARRAY;
                break;
            }
            case 10: {
                type = CstType.INT_ARRAY;
                break;
            }
            case 11: {
                type = CstType.LONG_ARRAY;
                break;
            }
            default: {
                throw new SimException("bad newarray code " + Hex.u1(value));
            }
        }
        final int previousOffset = visitor.getPreviousOffset();
        final ConstantParserVisitor constantVisitor = new ConstantParserVisitor();
        int arrayLength = 0;
        if (previousOffset >= 0) {
            this.parseInstruction(previousOffset, constantVisitor);
            if (constantVisitor.cst instanceof CstInteger && constantVisitor.length + previousOffset == offset) {
                arrayLength = constantVisitor.value;
            }
        }
        int nInit = 0;
        int lastOffset;
        int curOffset = lastOffset = offset + 2;
        final ArrayList<Constant> initVals = new ArrayList<Constant>();
        if (arrayLength != 0) {
            while (true) {
                boolean punt = false;
                int nextByte = this.bytes.getUnsignedByte(curOffset++);
                if (nextByte != 89) {
                    break;
                }
                this.parseInstruction(curOffset, constantVisitor);
                if (constantVisitor.length == 0 || !(constantVisitor.cst instanceof CstInteger)) {
                    break;
                }
                if (constantVisitor.value != nInit) {
                    break;
                }
                curOffset += constantVisitor.length;
                this.parseInstruction(curOffset, constantVisitor);
                if (constantVisitor.length == 0) {
                    break;
                }
                if (!(constantVisitor.cst instanceof CstLiteralBits)) {
                    break;
                }
                curOffset += constantVisitor.length;
                initVals.add(constantVisitor.cst);
                nextByte = this.bytes.getUnsignedByte(curOffset++);
                switch (value) {
                    case 4:
                    case 8: {
                        if (nextByte != 84) {
                            punt = true;
                            break;
                        }
                        break;
                    }
                    case 5: {
                        if (nextByte != 85) {
                            punt = true;
                            break;
                        }
                        break;
                    }
                    case 7: {
                        if (nextByte != 82) {
                            punt = true;
                            break;
                        }
                        break;
                    }
                    case 6: {
                        if (nextByte != 81) {
                            punt = true;
                            break;
                        }
                        break;
                    }
                    case 9: {
                        if (nextByte != 86) {
                            punt = true;
                            break;
                        }
                        break;
                    }
                    case 10: {
                        if (nextByte != 79) {
                            punt = true;
                            break;
                        }
                        break;
                    }
                    case 11: {
                        if (nextByte != 80) {
                            punt = true;
                            break;
                        }
                        break;
                    }
                    default: {
                        punt = true;
                        break;
                    }
                }
                if (punt) {
                    break;
                }
                lastOffset = curOffset;
                ++nInit;
            }
        }
        if (nInit < 2 || nInit != arrayLength) {
            visitor.visitNewarray(offset, 2, type, null);
            return 2;
        }
        visitor.visitNewarray(offset, lastOffset - offset, type, initVals);
        return lastOffset - offset;
    }
    
    private int parseWide(final int offset, final Visitor visitor) {
        final int opcode = this.bytes.getUnsignedByte(offset + 1);
        final int idx = this.bytes.getUnsignedShort(offset + 2);
        switch (opcode) {
            case 21: {
                visitor.visitLocal(21, offset, 4, idx, Type.INT, 0);
                return 4;
            }
            case 22: {
                visitor.visitLocal(21, offset, 4, idx, Type.LONG, 0);
                return 4;
            }
            case 23: {
                visitor.visitLocal(21, offset, 4, idx, Type.FLOAT, 0);
                return 4;
            }
            case 24: {
                visitor.visitLocal(21, offset, 4, idx, Type.DOUBLE, 0);
                return 4;
            }
            case 25: {
                visitor.visitLocal(21, offset, 4, idx, Type.OBJECT, 0);
                return 4;
            }
            case 54: {
                visitor.visitLocal(54, offset, 4, idx, Type.INT, 0);
                return 4;
            }
            case 55: {
                visitor.visitLocal(54, offset, 4, idx, Type.LONG, 0);
                return 4;
            }
            case 56: {
                visitor.visitLocal(54, offset, 4, idx, Type.FLOAT, 0);
                return 4;
            }
            case 57: {
                visitor.visitLocal(54, offset, 4, idx, Type.DOUBLE, 0);
                return 4;
            }
            case 58: {
                visitor.visitLocal(54, offset, 4, idx, Type.OBJECT, 0);
                return 4;
            }
            case 169: {
                visitor.visitLocal(opcode, offset, 4, idx, Type.RETURN_ADDRESS, 0);
                return 4;
            }
            case 132: {
                final int value = this.bytes.getShort(offset + 4);
                visitor.visitLocal(opcode, offset, 6, idx, Type.INT, value);
                return 6;
            }
            default: {
                visitor.visitInvalid(196, offset, 1);
                return 1;
            }
        }
    }
    
    static {
        EMPTY_VISITOR = new BaseVisitor();
    }
    
    public static class BaseVisitor implements Visitor
    {
        private int previousOffset;
        
        BaseVisitor() {
            this.previousOffset = -1;
        }
        
        @Override
        public void visitInvalid(final int opcode, final int offset, final int length) {
        }
        
        @Override
        public void visitNoArgs(final int opcode, final int offset, final int length, final Type type) {
        }
        
        @Override
        public void visitLocal(final int opcode, final int offset, final int length, final int idx, final Type type, final int value) {
        }
        
        @Override
        public void visitConstant(final int opcode, final int offset, final int length, final Constant cst, final int value) {
        }
        
        @Override
        public void visitBranch(final int opcode, final int offset, final int length, final int target) {
        }
        
        @Override
        public void visitSwitch(final int opcode, final int offset, final int length, final SwitchList cases, final int padding) {
        }
        
        @Override
        public void visitNewarray(final int offset, final int length, final CstType type, final ArrayList<Constant> initValues) {
        }
        
        @Override
        public void setPreviousOffset(final int offset) {
            this.previousOffset = offset;
        }
        
        @Override
        public int getPreviousOffset() {
            return this.previousOffset;
        }
    }
    
    class ConstantParserVisitor extends BaseVisitor
    {
        Constant cst;
        int length;
        int value;
        
        private void clear() {
            this.length = 0;
        }
        
        @Override
        public void visitInvalid(final int opcode, final int offset, final int length) {
            this.clear();
        }
        
        @Override
        public void visitNoArgs(final int opcode, final int offset, final int length, final Type type) {
            this.clear();
        }
        
        @Override
        public void visitLocal(final int opcode, final int offset, final int length, final int idx, final Type type, final int value) {
            this.clear();
        }
        
        @Override
        public void visitConstant(final int opcode, final int offset, final int length, final Constant cst, final int value) {
            this.cst = cst;
            this.length = length;
            this.value = value;
        }
        
        @Override
        public void visitBranch(final int opcode, final int offset, final int length, final int target) {
            this.clear();
        }
        
        @Override
        public void visitSwitch(final int opcode, final int offset, final int length, final SwitchList cases, final int padding) {
            this.clear();
        }
        
        @Override
        public void visitNewarray(final int offset, final int length, final CstType type, final ArrayList<Constant> initVals) {
            this.clear();
        }
        
        @Override
        public void setPreviousOffset(final int offset) {
        }
        
        @Override
        public int getPreviousOffset() {
            return -1;
        }
    }
    
    public interface Visitor
    {
        void visitInvalid(final int p0, final int p1, final int p2);
        
        void visitNoArgs(final int p0, final int p1, final int p2, final Type p3);
        
        void visitLocal(final int p0, final int p1, final int p2, final int p3, final Type p4, final int p5);
        
        void visitConstant(final int p0, final int p1, final int p2, final Constant p3, final int p4);
        
        void visitBranch(final int p0, final int p1, final int p2, final int p3);
        
        void visitSwitch(final int p0, final int p1, final int p2, final SwitchList p3, final int p4);
        
        void visitNewarray(final int p0, final int p1, final CstType p2, final ArrayList<Constant> p3);
        
        void setPreviousOffset(final int p0);
        
        int getPreviousOffset();
    }
}

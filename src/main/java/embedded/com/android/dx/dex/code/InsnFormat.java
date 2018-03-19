package embedded.com.android.dx.dex.code;

import java.util.*;
import embedded.com.android.dx.rop.code.*;
import embedded.com.android.dx.util.*;
import embedded.com.android.dx.rop.cst.*;

public abstract class InsnFormat
{
    public static final boolean ALLOW_EXTENDED_OPCODES = true;
    
    public final String listingString(final DalvInsn insn, final boolean noteIndices) {
        final String op = insn.getOpcode().getName();
        final String arg = this.insnArgString(insn);
        final String comment = this.insnCommentString(insn, noteIndices);
        final StringBuilder sb = new StringBuilder(100);
        sb.append(op);
        if (arg.length() != 0) {
            sb.append(' ');
            sb.append(arg);
        }
        if (comment.length() != 0) {
            sb.append(" // ");
            sb.append(comment);
        }
        return sb.toString();
    }
    
    public abstract String insnArgString(final DalvInsn p0);
    
    public abstract String insnCommentString(final DalvInsn p0, final boolean p1);
    
    public abstract int codeSize();
    
    public abstract boolean isCompatible(final DalvInsn p0);
    
    public BitSet compatibleRegs(final DalvInsn insn) {
        return new BitSet();
    }
    
    public boolean branchFits(final TargetInsn insn) {
        return false;
    }
    
    public abstract void writeTo(final AnnotatedOutput p0, final DalvInsn p1);
    
    protected static String regListString(final RegisterSpecList list) {
        final int sz = list.size();
        final StringBuffer sb = new StringBuffer(sz * 5 + 2);
        sb.append('{');
        for (int i = 0; i < sz; ++i) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append(list.get(i).regString());
        }
        sb.append('}');
        return sb.toString();
    }
    
    protected static String regRangeString(final RegisterSpecList list) {
        final int size = list.size();
        final StringBuilder sb = new StringBuilder(30);
        sb.append("{");
        switch (size) {
            case 0: {
                break;
            }
            case 1: {
                sb.append(list.get(0).regString());
                break;
            }
            default: {
                RegisterSpec lastReg = list.get(size - 1);
                if (lastReg.getCategory() == 2) {
                    lastReg = lastReg.withOffset(1);
                }
                sb.append(list.get(0).regString());
                sb.append("..");
                sb.append(lastReg.regString());
                break;
            }
        }
        sb.append("}");
        return sb.toString();
    }
    
    protected static String literalBitsString(final CstLiteralBits value) {
        final StringBuffer sb = new StringBuffer(100);
        sb.append('#');
        if (value instanceof CstKnownNull) {
            sb.append("null");
        }
        else {
            sb.append(value.typeName());
            sb.append(' ');
            sb.append(value.toHuman());
        }
        return sb.toString();
    }
    
    protected static String literalBitsComment(final CstLiteralBits value, final int width) {
        final StringBuffer sb = new StringBuffer(20);
        sb.append("#");
        long bits;
        if (value instanceof CstLiteral64) {
            bits = ((CstLiteral64)value).getLongBits();
        }
        else {
            bits = value.getIntBits();
        }
        switch (width) {
            case 4: {
                sb.append(Hex.uNibble((int)bits));
                break;
            }
            case 8: {
                sb.append(Hex.u1((int)bits));
                break;
            }
            case 16: {
                sb.append(Hex.u2((int)bits));
                break;
            }
            case 32: {
                sb.append(Hex.u4((int)bits));
                break;
            }
            case 64: {
                sb.append(Hex.u8(bits));
                break;
            }
            default: {
                throw new RuntimeException("shouldn't happen");
            }
        }
        return sb.toString();
    }
    
    protected static String branchString(final DalvInsn insn) {
        final TargetInsn ti = (TargetInsn)insn;
        final int address = ti.getTargetAddress();
        return (address == (char)address) ? Hex.u2(address) : Hex.u4(address);
    }
    
    protected static String branchComment(final DalvInsn insn) {
        final TargetInsn ti = (TargetInsn)insn;
        final int offset = ti.getTargetOffset();
        return (offset == (short)offset) ? Hex.s2(offset) : Hex.s4(offset);
    }
    
    protected static boolean signedFitsInNibble(final int value) {
        return value >= -8 && value <= 7;
    }
    
    protected static boolean unsignedFitsInNibble(final int value) {
        return value == (value & 0xF);
    }
    
    protected static boolean signedFitsInByte(final int value) {
        return (byte)value == value;
    }
    
    protected static boolean unsignedFitsInByte(final int value) {
        return value == (value & 0xFF);
    }
    
    protected static boolean signedFitsInShort(final int value) {
        return (short)value == value;
    }
    
    protected static boolean unsignedFitsInShort(final int value) {
        return value == (value & 0xFFFF);
    }
    
    protected static boolean isRegListSequential(final RegisterSpecList list) {
        final int sz = list.size();
        if (sz < 2) {
            return true;
        }
        int next;
        final int first = next = list.get(0).getReg();
        for (int i = 0; i < sz; ++i) {
            final RegisterSpec one = list.get(i);
            if (one.getReg() != next) {
                return false;
            }
            next += one.getCategory();
        }
        return true;
    }
    
    protected static int argIndex(final DalvInsn insn) {
        final int arg = ((CstInteger)((CstInsn)insn).getConstant()).getValue();
        if (arg < 0) {
            throw new IllegalArgumentException("bogus insn");
        }
        return arg;
    }
    
    protected static short opcodeUnit(final DalvInsn insn, final int arg) {
        if ((arg & 0xFF) != arg) {
            throw new IllegalArgumentException("arg out of range 0..255");
        }
        final int opcode = insn.getOpcode().getOpcode();
        if ((opcode & 0xFF) != opcode) {
            throw new IllegalArgumentException("opcode out of range 0..255");
        }
        return (short)(opcode | arg << 8);
    }
    
    protected static short opcodeUnit(final DalvInsn insn) {
        final int opcode = insn.getOpcode().getOpcode();
        if (opcode < 256 || opcode > 65535) {
            throw new IllegalArgumentException("opcode out of range 0..65535");
        }
        return (short)opcode;
    }
    
    protected static short codeUnit(final int low, final int high) {
        if ((low & 0xFF) != low) {
            throw new IllegalArgumentException("low out of range 0..255");
        }
        if ((high & 0xFF) != high) {
            throw new IllegalArgumentException("high out of range 0..255");
        }
        return (short)(low | high << 8);
    }
    
    protected static short codeUnit(final int n0, final int n1, final int n2, final int n3) {
        if ((n0 & 0xF) != n0) {
            throw new IllegalArgumentException("n0 out of range 0..15");
        }
        if ((n1 & 0xF) != n1) {
            throw new IllegalArgumentException("n1 out of range 0..15");
        }
        if ((n2 & 0xF) != n2) {
            throw new IllegalArgumentException("n2 out of range 0..15");
        }
        if ((n3 & 0xF) != n3) {
            throw new IllegalArgumentException("n3 out of range 0..15");
        }
        return (short)(n0 | n1 << 4 | n2 << 8 | n3 << 12);
    }
    
    protected static int makeByte(final int low, final int high) {
        if ((low & 0xF) != low) {
            throw new IllegalArgumentException("low out of range 0..15");
        }
        if ((high & 0xF) != high) {
            throw new IllegalArgumentException("high out of range 0..15");
        }
        return low | high << 4;
    }
    
    protected static void write(final AnnotatedOutput out, final short c0) {
        out.writeShort(c0);
    }
    
    protected static void write(final AnnotatedOutput out, final short c0, final short c1) {
        out.writeShort(c0);
        out.writeShort(c1);
    }
    
    protected static void write(final AnnotatedOutput out, final short c0, final short c1, final short c2) {
        out.writeShort(c0);
        out.writeShort(c1);
        out.writeShort(c2);
    }
    
    protected static void write(final AnnotatedOutput out, final short c0, final short c1, final short c2, final short c3) {
        out.writeShort(c0);
        out.writeShort(c1);
        out.writeShort(c2);
        out.writeShort(c3);
    }
    
    protected static void write(final AnnotatedOutput out, final short c0, final short c1, final short c2, final short c3, final short c4) {
        out.writeShort(c0);
        out.writeShort(c1);
        out.writeShort(c2);
        out.writeShort(c3);
        out.writeShort(c4);
    }
    
    protected static void write(final AnnotatedOutput out, final short c0, final int c1c2) {
        write(out, c0, (short)c1c2, (short)(c1c2 >> 16));
    }
    
    protected static void write(final AnnotatedOutput out, final short c0, final int c1c2, final short c3) {
        write(out, c0, (short)c1c2, (short)(c1c2 >> 16), c3);
    }
    
    protected static void write(final AnnotatedOutput out, final short c0, final int c1c2, final short c3, final short c4) {
        write(out, c0, (short)c1c2, (short)(c1c2 >> 16), c3, c4);
    }
    
    protected static void write(final AnnotatedOutput out, final short c0, final long c1c2c3c4) {
        write(out, c0, (short)c1c2c3c4, (short)(c1c2c3c4 >> 16), (short)(c1c2c3c4 >> 32), (short)(c1c2c3c4 >> 48));
    }
}

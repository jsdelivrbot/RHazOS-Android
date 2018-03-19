package embedded.com.android.dx.cf.direct;

import embedded.com.android.dx.cf.iface.*;
import embedded.com.android.dx.rop.type.*;
import embedded.com.android.dx.util.*;
import embedded.com.android.dx.rop.cst.*;
import java.util.*;
import embedded.com.android.dx.cf.code.*;

public class CodeObserver implements BytecodeArray.Visitor
{
    private final ByteArray bytes;
    private final ParseObserver observer;
    
    public CodeObserver(final ByteArray bytes, final ParseObserver observer) {
        if (bytes == null) {
            throw new NullPointerException("bytes == null");
        }
        if (observer == null) {
            throw new NullPointerException("observer == null");
        }
        this.bytes = bytes;
        this.observer = observer;
    }
    
    @Override
    public void visitInvalid(final int opcode, final int offset, final int length) {
        this.observer.parsed(this.bytes, offset, length, this.header(offset));
    }
    
    @Override
    public void visitNoArgs(final int opcode, final int offset, final int length, final Type type) {
        this.observer.parsed(this.bytes, offset, length, this.header(offset));
    }
    
    @Override
    public void visitLocal(final int opcode, final int offset, final int length, final int idx, final Type type, final int value) {
        final String idxStr = (length <= 3) ? Hex.u1(idx) : Hex.u2(idx);
        final boolean argComment = length == 1;
        String valueStr = "";
        if (opcode == 132) {
            valueStr = ", #" + ((length <= 3) ? Hex.s1(value) : Hex.s2(value));
        }
        String catStr = "";
        if (type.isCategory2()) {
            catStr = (argComment ? "," : " //") + " category-2";
        }
        this.observer.parsed(this.bytes, offset, length, this.header(offset) + (argComment ? " // " : " ") + idxStr + valueStr + catStr);
    }
    
    @Override
    public void visitConstant(final int opcode, final int offset, final int length, final Constant cst, final int value) {
        if (cst instanceof CstKnownNull) {
            this.visitNoArgs(opcode, offset, length, null);
            return;
        }
        if (cst instanceof CstInteger) {
            this.visitLiteralInt(opcode, offset, length, value);
            return;
        }
        if (cst instanceof CstLong) {
            this.visitLiteralLong(opcode, offset, length, ((CstLong)cst).getValue());
            return;
        }
        if (cst instanceof CstFloat) {
            this.visitLiteralFloat(opcode, offset, length, ((CstFloat)cst).getIntBits());
            return;
        }
        if (cst instanceof CstDouble) {
            this.visitLiteralDouble(opcode, offset, length, ((CstDouble)cst).getLongBits());
            return;
        }
        String valueStr = "";
        if (value != 0) {
            valueStr = ", ";
            if (opcode == 197) {
                valueStr += Hex.u1(value);
            }
            else {
                valueStr += Hex.u2(value);
            }
        }
        this.observer.parsed(this.bytes, offset, length, this.header(offset) + " " + cst + valueStr);
    }
    
    @Override
    public void visitBranch(final int opcode, final int offset, final int length, final int target) {
        final String targetStr = (length <= 3) ? Hex.u2(target) : Hex.u4(target);
        this.observer.parsed(this.bytes, offset, length, this.header(offset) + " " + targetStr);
    }
    
    @Override
    public void visitSwitch(final int opcode, final int offset, final int length, final SwitchList cases, final int padding) {
        final int sz = cases.size();
        final StringBuffer sb = new StringBuffer(sz * 20 + 100);
        sb.append(this.header(offset));
        if (padding != 0) {
            sb.append(" // padding: " + Hex.u4(padding));
        }
        sb.append('\n');
        for (int i = 0; i < sz; ++i) {
            sb.append("  ");
            sb.append(Hex.s4(cases.getValue(i)));
            sb.append(": ");
            sb.append(Hex.u2(cases.getTarget(i)));
            sb.append('\n');
        }
        sb.append("  default: ");
        sb.append(Hex.u2(cases.getDefaultTarget()));
        this.observer.parsed(this.bytes, offset, length, sb.toString());
    }
    
    @Override
    public void visitNewarray(final int offset, final int length, final CstType cst, final ArrayList<Constant> intVals) {
        final String commentOrSpace = (length == 1) ? " // " : " ";
        final String typeName = cst.getClassType().getComponentType().toHuman();
        this.observer.parsed(this.bytes, offset, length, this.header(offset) + commentOrSpace + typeName);
    }
    
    @Override
    public void setPreviousOffset(final int offset) {
    }
    
    @Override
    public int getPreviousOffset() {
        return -1;
    }
    
    private String header(final int offset) {
        int opcode = this.bytes.getUnsignedByte(offset);
        String name = ByteOps.opName(opcode);
        if (opcode == 196) {
            opcode = this.bytes.getUnsignedByte(offset + 1);
            name = name + " " + ByteOps.opName(opcode);
        }
        return Hex.u2(offset) + ": " + name;
    }
    
    private void visitLiteralInt(int opcode, final int offset, final int length, final int value) {
        final String commentOrSpace = (length == 1) ? " // " : " ";
        opcode = this.bytes.getUnsignedByte(offset);
        String valueStr;
        if (length == 1 || opcode == 16) {
            valueStr = "#" + Hex.s1(value);
        }
        else if (opcode == 17) {
            valueStr = "#" + Hex.s2(value);
        }
        else {
            valueStr = "#" + Hex.s4(value);
        }
        this.observer.parsed(this.bytes, offset, length, this.header(offset) + commentOrSpace + valueStr);
    }
    
    private void visitLiteralLong(final int opcode, final int offset, final int length, final long value) {
        final String commentOrLit = (length == 1) ? " // " : " #";
        String valueStr;
        if (length == 1) {
            valueStr = Hex.s1((int)value);
        }
        else {
            valueStr = Hex.s8(value);
        }
        this.observer.parsed(this.bytes, offset, length, this.header(offset) + commentOrLit + valueStr);
    }
    
    private void visitLiteralFloat(final int opcode, final int offset, final int length, final int bits) {
        final String optArg = (length != 1) ? (" #" + Hex.u4(bits)) : "";
        this.observer.parsed(this.bytes, offset, length, this.header(offset) + optArg + " // " + Float.intBitsToFloat(bits));
    }
    
    private void visitLiteralDouble(final int opcode, final int offset, final int length, final long bits) {
        final String optArg = (length != 1) ? (" #" + Hex.u8(bits)) : "";
        this.observer.parsed(this.bytes, offset, length, this.header(offset) + optArg + " // " + Double.longBitsToDouble(bits));
    }
}

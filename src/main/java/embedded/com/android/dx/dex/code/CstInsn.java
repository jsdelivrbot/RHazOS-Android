package embedded.com.android.dx.dex.code;

import embedded.com.android.dx.rop.code.*;
import embedded.com.android.dx.rop.cst.*;
import embedded.com.android.dx.util.*;

public final class CstInsn extends FixedSizeInsn
{
    private final Constant constant;
    private int index;
    private int classIndex;
    
    public CstInsn(final Dop opcode, final SourcePosition position, final RegisterSpecList registers, final Constant constant) {
        super(opcode, position, registers);
        if (constant == null) {
            throw new NullPointerException("constant == null");
        }
        this.constant = constant;
        this.index = -1;
        this.classIndex = -1;
    }
    
    @Override
    public DalvInsn withOpcode(final Dop opcode) {
        final CstInsn result = new CstInsn(opcode, this.getPosition(), this.getRegisters(), this.constant);
        if (this.index >= 0) {
            result.setIndex(this.index);
        }
        if (this.classIndex >= 0) {
            result.setClassIndex(this.classIndex);
        }
        return result;
    }
    
    @Override
    public DalvInsn withRegisters(final RegisterSpecList registers) {
        final CstInsn result = new CstInsn(this.getOpcode(), this.getPosition(), registers, this.constant);
        if (this.index >= 0) {
            result.setIndex(this.index);
        }
        if (this.classIndex >= 0) {
            result.setClassIndex(this.classIndex);
        }
        return result;
    }
    
    public Constant getConstant() {
        return this.constant;
    }
    
    public int getIndex() {
        if (this.index < 0) {
            throw new IllegalStateException("index not yet set for " + this.constant);
        }
        return this.index;
    }
    
    public boolean hasIndex() {
        return this.index >= 0;
    }
    
    public void setIndex(final int index) {
        if (index < 0) {
            throw new IllegalArgumentException("index < 0");
        }
        if (this.index >= 0) {
            throw new IllegalStateException("index already set");
        }
        this.index = index;
    }
    
    public int getClassIndex() {
        if (this.classIndex < 0) {
            throw new IllegalStateException("class index not yet set");
        }
        return this.classIndex;
    }
    
    public boolean hasClassIndex() {
        return this.classIndex >= 0;
    }
    
    public void setClassIndex(final int index) {
        if (index < 0) {
            throw new IllegalArgumentException("index < 0");
        }
        if (this.classIndex >= 0) {
            throw new IllegalStateException("class index already set");
        }
        this.classIndex = index;
    }
    
    @Override
    protected String argString() {
        return this.constant.toHuman();
    }
    
    @Override
    public String cstString() {
        if (this.constant instanceof CstString) {
            return ((CstString)this.constant).toQuoted();
        }
        return this.constant.toHuman();
    }
    
    @Override
    public String cstComment() {
        if (!this.hasIndex()) {
            return "";
        }
        final StringBuilder sb = new StringBuilder(20);
        sb.append(this.getConstant().typeName());
        sb.append('@');
        if (this.index < 65536) {
            sb.append(Hex.u2(this.index));
        }
        else {
            sb.append(Hex.u4(this.index));
        }
        return sb.toString();
    }
}

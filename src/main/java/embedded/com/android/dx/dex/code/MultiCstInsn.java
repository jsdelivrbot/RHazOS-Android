package embedded.com.android.dx.dex.code;

import embedded.com.android.dx.rop.cst.*;
import embedded.com.android.dx.rop.code.*;
import embedded.com.android.dx.util.*;
import java.io.*;

public final class MultiCstInsn extends FixedSizeInsn
{
    private static final int NOT_SET = -1;
    private final Constant[] constants;
    private final int[] index;
    private int classIndex;
    
    public MultiCstInsn(final Dop opcode, final SourcePosition position, final RegisterSpecList registers, final Constant[] constants) {
        super(opcode, position, registers);
        if (constants == null) {
            throw new NullPointerException("constants == null");
        }
        this.constants = constants;
        this.index = new int[constants.length];
        for (int i = 0; i < this.index.length; ++i) {
            if (constants[i] == null) {
                throw new NullPointerException("constants[i] == null");
            }
            this.index[i] = -1;
        }
        this.classIndex = -1;
    }
    
    private MultiCstInsn(final Dop opcode, final SourcePosition position, final RegisterSpecList registers, final Constant[] constants, final int[] index, final int classIndex) {
        super(opcode, position, registers);
        this.constants = constants;
        this.index = index;
        this.classIndex = classIndex;
    }
    
    @Override
    public DalvInsn withOpcode(final Dop opcode) {
        return new MultiCstInsn(opcode, this.getPosition(), this.getRegisters(), this.constants, this.index, this.classIndex);
    }
    
    @Override
    public DalvInsn withRegisters(final RegisterSpecList registers) {
        return new MultiCstInsn(this.getOpcode(), this.getPosition(), registers, this.constants, this.index, this.classIndex);
    }
    
    public int getNumberOfConstants() {
        return this.constants.length;
    }
    
    public Constant getConstant(final int position) {
        return this.constants[position];
    }
    
    public int getIndex(final int position) {
        if (!this.hasIndex(position)) {
            throw new IllegalStateException("index not yet set for constant " + position + " value = " + this.constants[position]);
        }
        return this.index[position];
    }
    
    public boolean hasIndex(final int position) {
        return this.index[position] != -1;
    }
    
    public void setIndex(final int position, final int index) {
        if (index < 0) {
            throw new IllegalArgumentException("index < 0");
        }
        if (this.hasIndex(position)) {
            throw new IllegalStateException("index already set");
        }
        this.index[position] = index;
    }
    
    public int getClassIndex() {
        if (!this.hasClassIndex()) {
            throw new IllegalStateException("class index not yet set");
        }
        return this.classIndex;
    }
    
    public boolean hasClassIndex() {
        return this.classIndex != -1;
    }
    
    public void setClassIndex(final int index) {
        if (index < 0) {
            throw new IllegalArgumentException("index < 0");
        }
        if (this.hasClassIndex()) {
            throw new IllegalStateException("class index already set");
        }
        this.classIndex = index;
    }
    
    @Override
    protected String argString() {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < this.constants.length; ++i) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(this.constants[i].toHuman());
        }
        return sb.toString();
    }
    
    @Override
    public String cstString() {
        return this.argString();
    }
    
    @Override
    public String cstComment() {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < this.constants.length; ++i) {
            if (!this.hasIndex(i)) {
                return "";
            }
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(this.getConstant(i).typeName());
            sb.append('@');
            final int currentIndex = this.getIndex(i);
            if (currentIndex < 65536) {
                sb.append(Hex.u2(currentIndex));
            }
            else {
                sb.append(Hex.u4(currentIndex));
            }
        }
        return sb.toString();
    }
}

package embedded.com.android.dx.rop.code;

import embedded.com.android.dx.rop.type.*;
import embedded.com.android.dx.util.*;

public final class Rop
{
    public static final int BRANCH_MIN = 1;
    public static final int BRANCH_NONE = 1;
    public static final int BRANCH_RETURN = 2;
    public static final int BRANCH_GOTO = 3;
    public static final int BRANCH_IF = 4;
    public static final int BRANCH_SWITCH = 5;
    public static final int BRANCH_THROW = 6;
    public static final int BRANCH_MAX = 6;
    private final int opcode;
    private final Type result;
    private final TypeList sources;
    private final TypeList exceptions;
    private final int branchingness;
    private final boolean isCallLike;
    private final String nickname;
    
    public Rop(final int opcode, final Type result, final TypeList sources, final TypeList exceptions, final int branchingness, final boolean isCallLike, final String nickname) {
        if (result == null) {
            throw new NullPointerException("result == null");
        }
        if (sources == null) {
            throw new NullPointerException("sources == null");
        }
        if (exceptions == null) {
            throw new NullPointerException("exceptions == null");
        }
        if (branchingness < 1 || branchingness > 6) {
            throw new IllegalArgumentException("invalid branchingness: " + branchingness);
        }
        if (exceptions.size() != 0 && branchingness != 6) {
            throw new IllegalArgumentException("exceptions / branchingness mismatch");
        }
        this.opcode = opcode;
        this.result = result;
        this.sources = sources;
        this.exceptions = exceptions;
        this.branchingness = branchingness;
        this.isCallLike = isCallLike;
        this.nickname = nickname;
    }
    
    public Rop(final int opcode, final Type result, final TypeList sources, final TypeList exceptions, final int branchingness, final String nickname) {
        this(opcode, result, sources, exceptions, branchingness, false, nickname);
    }
    
    public Rop(final int opcode, final Type result, final TypeList sources, final int branchingness, final String nickname) {
        this(opcode, result, sources, StdTypeList.EMPTY, branchingness, false, nickname);
    }
    
    public Rop(final int opcode, final Type result, final TypeList sources, final String nickname) {
        this(opcode, result, sources, StdTypeList.EMPTY, 1, false, nickname);
    }
    
    public Rop(final int opcode, final Type result, final TypeList sources, final TypeList exceptions, final String nickname) {
        this(opcode, result, sources, exceptions, 6, false, nickname);
    }
    
    public Rop(final int opcode, final TypeList sources, final TypeList exceptions) {
        this(opcode, Type.VOID, sources, exceptions, 6, true, null);
    }
    
    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Rop)) {
            return false;
        }
        final Rop rop = (Rop)other;
        return this.opcode == rop.opcode && this.branchingness == rop.branchingness && this.result == rop.result && this.sources.equals(rop.sources) && this.exceptions.equals(rop.exceptions);
    }
    
    @Override
    public int hashCode() {
        int h = this.opcode * 31 + this.branchingness;
        h = h * 31 + this.result.hashCode();
        h = h * 31 + this.sources.hashCode();
        h = h * 31 + this.exceptions.hashCode();
        return h;
    }
    
    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer(40);
        sb.append("Rop{");
        sb.append(RegOps.opName(this.opcode));
        if (this.result != Type.VOID) {
            sb.append(" ");
            sb.append(this.result);
        }
        else {
            sb.append(" .");
        }
        sb.append(" <-");
        int sz = this.sources.size();
        if (sz == 0) {
            sb.append(" .");
        }
        else {
            for (int i = 0; i < sz; ++i) {
                sb.append(' ');
                sb.append(this.sources.getType(i));
            }
        }
        if (this.isCallLike) {
            sb.append(" call");
        }
        sz = this.exceptions.size();
        if (sz != 0) {
            sb.append(" throws");
            for (int i = 0; i < sz; ++i) {
                sb.append(' ');
                final Type one = this.exceptions.getType(i);
                if (one == Type.THROWABLE) {
                    sb.append("<any>");
                }
                else {
                    sb.append(this.exceptions.getType(i));
                }
            }
        }
        else {
            switch (this.branchingness) {
                case 1: {
                    sb.append(" flows");
                    break;
                }
                case 2: {
                    sb.append(" returns");
                    break;
                }
                case 3: {
                    sb.append(" gotos");
                    break;
                }
                case 4: {
                    sb.append(" ifs");
                    break;
                }
                case 5: {
                    sb.append(" switches");
                    break;
                }
                default: {
                    sb.append(" " + Hex.u1(this.branchingness));
                    break;
                }
            }
        }
        sb.append('}');
        return sb.toString();
    }
    
    public int getOpcode() {
        return this.opcode;
    }
    
    public Type getResult() {
        return this.result;
    }
    
    public TypeList getSources() {
        return this.sources;
    }
    
    public TypeList getExceptions() {
        return this.exceptions;
    }
    
    public int getBranchingness() {
        return this.branchingness;
    }
    
    public boolean isCallLike() {
        return this.isCallLike;
    }
    
    public boolean isCommutative() {
        switch (this.opcode) {
            case 14:
            case 16:
            case 20:
            case 21:
            case 22: {
                return true;
            }
            default: {
                return false;
            }
        }
    }
    
    public String getNickname() {
        if (this.nickname != null) {
            return this.nickname;
        }
        return this.toString();
    }
    
    public final boolean canThrow() {
        return this.exceptions.size() != 0;
    }
}

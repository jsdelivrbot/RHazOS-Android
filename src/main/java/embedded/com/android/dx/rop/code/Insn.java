package embedded.com.android.dx.rop.code;

import embedded.com.android.dx.util.*;
import embedded.com.android.dx.rop.type.*;

public abstract class Insn implements ToHuman
{
    private final Rop opcode;
    private final SourcePosition position;
    private final RegisterSpec result;
    private final RegisterSpecList sources;
    
    public Insn(final Rop opcode, final SourcePosition position, final RegisterSpec result, final RegisterSpecList sources) {
        if (opcode == null) {
            throw new NullPointerException("opcode == null");
        }
        if (position == null) {
            throw new NullPointerException("position == null");
        }
        if (sources == null) {
            throw new NullPointerException("sources == null");
        }
        this.opcode = opcode;
        this.position = position;
        this.result = result;
        this.sources = sources;
    }
    
    @Override
    public final boolean equals(final Object other) {
        return this == other;
    }
    
    @Override
    public final int hashCode() {
        return System.identityHashCode(this);
    }
    
    @Override
    public String toString() {
        return this.toStringWithInline(this.getInlineString());
    }
    
    @Override
    public String toHuman() {
        return this.toHumanWithInline(this.getInlineString());
    }
    
    public String getInlineString() {
        return null;
    }
    
    public final Rop getOpcode() {
        return this.opcode;
    }
    
    public final SourcePosition getPosition() {
        return this.position;
    }
    
    public final RegisterSpec getResult() {
        return this.result;
    }
    
    public final RegisterSpec getLocalAssignment() {
        RegisterSpec assignment;
        if (this.opcode.getOpcode() == 54) {
            assignment = this.sources.get(0);
        }
        else {
            assignment = this.result;
        }
        if (assignment == null) {
            return null;
        }
        final LocalItem localItem = assignment.getLocalItem();
        if (localItem == null) {
            return null;
        }
        return assignment;
    }
    
    public final RegisterSpecList getSources() {
        return this.sources;
    }
    
    public final boolean canThrow() {
        return this.opcode.canThrow();
    }
    
    public abstract TypeList getCatches();
    
    public abstract void accept(final Visitor p0);
    
    public abstract Insn withAddedCatch(final Type p0);
    
    public abstract Insn withRegisterOffset(final int p0);
    
    public Insn withSourceLiteral() {
        return this;
    }
    
    public Insn copy() {
        return this.withRegisterOffset(0);
    }
    
    private static boolean equalsHandleNulls(final Object a, final Object b) {
        return a == b || (a != null && a.equals(b));
    }
    
    public boolean contentEquals(final Insn b) {
        return this.opcode == b.getOpcode() && this.position.equals(b.getPosition()) && this.getClass() == b.getClass() && equalsHandleNulls(this.result, b.getResult()) && equalsHandleNulls(this.sources, b.getSources()) && StdTypeList.equalContents(this.getCatches(), b.getCatches());
    }
    
    public abstract Insn withNewRegisters(final RegisterSpec p0, final RegisterSpecList p1);
    
    protected final String toStringWithInline(final String extra) {
        final StringBuffer sb = new StringBuffer(80);
        sb.append("Insn{");
        sb.append(this.position);
        sb.append(' ');
        sb.append(this.opcode);
        if (extra != null) {
            sb.append(' ');
            sb.append(extra);
        }
        sb.append(" :: ");
        if (this.result != null) {
            sb.append(this.result);
            sb.append(" <- ");
        }
        sb.append(this.sources);
        sb.append('}');
        return sb.toString();
    }
    
    protected final String toHumanWithInline(final String extra) {
        final StringBuffer sb = new StringBuffer(80);
        sb.append(this.position);
        sb.append(": ");
        sb.append(this.opcode.getNickname());
        if (extra != null) {
            sb.append("(");
            sb.append(extra);
            sb.append(")");
        }
        if (this.result == null) {
            sb.append(" .");
        }
        else {
            sb.append(" ");
            sb.append(this.result.toHuman());
        }
        sb.append(" <-");
        final int sz = this.sources.size();
        if (sz == 0) {
            sb.append(" .");
        }
        else {
            for (int i = 0; i < sz; ++i) {
                sb.append(" ");
                sb.append(this.sources.get(i).toHuman());
            }
        }
        return sb.toString();
    }
    
    public static class BaseVisitor implements Visitor
    {
        @Override
        public void visitPlainInsn(final PlainInsn insn) {
        }
        
        @Override
        public void visitPlainCstInsn(final PlainCstInsn insn) {
        }
        
        @Override
        public void visitSwitchInsn(final SwitchInsn insn) {
        }
        
        @Override
        public void visitThrowingCstInsn(final ThrowingCstInsn insn) {
        }
        
        @Override
        public void visitThrowingInsn(final ThrowingInsn insn) {
        }
        
        @Override
        public void visitFillArrayDataInsn(final FillArrayDataInsn insn) {
        }
        
        @Override
        public void visitInvokePolymorphicInsn(final InvokePolymorphicInsn insn) {
        }
    }
    
    public interface Visitor
    {
        void visitPlainInsn(final PlainInsn p0);
        
        void visitPlainCstInsn(final PlainCstInsn p0);
        
        void visitSwitchInsn(final SwitchInsn p0);
        
        void visitThrowingCstInsn(final ThrowingCstInsn p0);
        
        void visitThrowingInsn(final ThrowingInsn p0);
        
        void visitFillArrayDataInsn(final FillArrayDataInsn p0);
        
        void visitInvokePolymorphicInsn(final InvokePolymorphicInsn p0);
    }
}

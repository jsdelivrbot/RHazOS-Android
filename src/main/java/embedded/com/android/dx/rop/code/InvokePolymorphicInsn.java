package embedded.com.android.dx.rop.code;

import embedded.com.android.dx.rop.type.*;
import embedded.com.android.dx.rop.cst.*;

public class InvokePolymorphicInsn extends Insn
{
    private static final CstString INVOKE_DESCRIPTOR;
    private final TypeList catches;
    private final CstMethodRef callSiteMethod;
    private final CstMethodRef invokeMethod;
    private final CstProtoRef callSiteProto;
    
    public InvokePolymorphicInsn(final Rop opcode, final SourcePosition position, final RegisterSpecList sources, final TypeList catches, final CstMethodRef callSiteMethod) {
        super(opcode, position, null, sources);
        if (opcode.getBranchingness() != 6) {
            throw new IllegalArgumentException("opcode with invalid branchingness: " + opcode.getBranchingness());
        }
        if (catches == null) {
            throw new NullPointerException("catches == null");
        }
        this.catches = catches;
        if (callSiteMethod == null) {
            throw new NullPointerException("callSiteMethod == null");
        }
        if (!callSiteMethod.isSignaturePolymorphic()) {
            throw new IllegalArgumentException("callSiteMethod is not signature polymorphic");
        }
        this.callSiteMethod = callSiteMethod;
        this.invokeMethod = makeInvokeMethod(callSiteMethod);
        this.callSiteProto = makeCallSiteProto(callSiteMethod);
    }
    
    @Override
    public TypeList getCatches() {
        return this.catches;
    }
    
    @Override
    public void accept(final Visitor visitor) {
        visitor.visitInvokePolymorphicInsn(this);
    }
    
    @Override
    public Insn withAddedCatch(final Type type) {
        return new InvokePolymorphicInsn(this.getOpcode(), this.getPosition(), this.getSources(), this.catches.withAddedType(type), this.getCallSiteMethod());
    }
    
    @Override
    public Insn withRegisterOffset(final int delta) {
        return new InvokePolymorphicInsn(this.getOpcode(), this.getPosition(), this.getSources().withOffset(delta), this.catches, this.getCallSiteMethod());
    }
    
    @Override
    public Insn withNewRegisters(final RegisterSpec result, final RegisterSpecList sources) {
        return new InvokePolymorphicInsn(this.getOpcode(), this.getPosition(), sources, this.catches, this.getCallSiteMethod());
    }
    
    public CstMethodRef getCallSiteMethod() {
        return this.callSiteMethod;
    }
    
    public CstMethodRef getInvokeMethod() {
        return this.invokeMethod;
    }
    
    public CstProtoRef getCallSiteProto() {
        return this.callSiteProto;
    }
    
    @Override
    public String getInlineString() {
        return this.getInvokeMethod().toString() + " " + this.getCallSiteProto().toString() + " " + ThrowingInsn.toCatchString(this.catches);
    }
    
    private static CstMethodRef makeInvokeMethod(final CstMethodRef callSiteMethod) {
        final CstNat cstNat = new CstNat(callSiteMethod.getNat().getName(), InvokePolymorphicInsn.INVOKE_DESCRIPTOR);
        return new CstMethodRef(CstType.METHOD_HANDLE, cstNat);
    }
    
    private static CstProtoRef makeCallSiteProto(final CstMethodRef callSiteMethod) {
        return new CstProtoRef(callSiteMethod.getPrototype(true));
    }
    
    static {
        INVOKE_DESCRIPTOR = new CstString("([Ljava/lang/Object;)Ljava/lang/Object;");
    }
}

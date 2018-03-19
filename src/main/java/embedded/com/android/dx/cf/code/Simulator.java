package embedded.com.android.dx.cf.code;

import embedded.com.android.dx.dex.*;
import embedded.com.android.dex.util.*;
import embedded.com.android.dx.util.*;
import embedded.com.android.dx.rop.code.*;
import embedded.com.android.dx.rop.type.*;
import embedded.com.android.dx.rop.cst.*;
import java.util.*;

public class Simulator
{
    private static final String LOCAL_MISMATCH_ERROR = "This is symptomatic of .class transformation tools that ignore local variable information.";
    private final Machine machine;
    private final BytecodeArray code;
    private final LocalVariableList localVariables;
    private final SimVisitor visitor;
    private final DexOptions dexOptions;
    
    public Simulator(final Machine machine, final ConcreteMethod method, final DexOptions dexOptions) {
        if (machine == null) {
            throw new NullPointerException("machine == null");
        }
        if (method == null) {
            throw new NullPointerException("method == null");
        }
        this.machine = machine;
        this.code = method.getCode();
        this.localVariables = method.getLocalVariables();
        this.visitor = new SimVisitor();
        this.dexOptions = dexOptions;
    }
    
    public void simulate(final ByteBlock bb, final Frame frame) {
        final int end = bb.getEnd();
        this.visitor.setFrame(frame);
        try {
            int length;
            for (int off = bb.getStart(); off < end; off += length) {
                length = this.code.parseInstruction(off, this.visitor);
                this.visitor.setPreviousOffset(off);
            }
        }
        catch (SimException ex) {
            frame.annotate(ex);
            throw ex;
        }
    }
    
    public int simulate(final int offset, final Frame frame) {
        this.visitor.setFrame(frame);
        return this.code.parseInstruction(offset, this.visitor);
    }
    
    private static SimException illegalTos() {
        return new SimException("stack mismatch: illegal top-of-stack for opcode");
    }
    
    private static Type requiredArrayTypeFor(final Type impliedType, final Type foundArrayType) {
        if (foundArrayType == Type.KNOWN_NULL) {
            return impliedType.isReference() ? Type.KNOWN_NULL : impliedType.getArrayType();
        }
        if (impliedType == Type.OBJECT && foundArrayType.isArray() && foundArrayType.getComponentType().isReference()) {
            return foundArrayType;
        }
        if (impliedType == Type.BYTE && foundArrayType == Type.BOOLEAN_ARRAY) {
            return Type.BOOLEAN_ARRAY;
        }
        return impliedType.getArrayType();
    }
    
    private class SimVisitor implements BytecodeArray.Visitor
    {
        private final Machine machine;
        private Frame frame;
        private int previousOffset;
        
        public SimVisitor() {
            this.machine = Simulator.this.machine;
            this.frame = null;
        }
        
        public void setFrame(final Frame frame) {
            if (frame == null) {
                throw new NullPointerException("frame == null");
            }
            this.frame = frame;
        }
        
        @Override
        public void visitInvalid(final int opcode, final int offset, final int length) {
            throw new SimException("invalid opcode " + Hex.u1(opcode));
        }
        
        @Override
        public void visitNoArgs(final int opcode, final int offset, final int length, Type type) {
            switch (opcode) {
                case 0: {
                    this.machine.clearArgs();
                    break;
                }
                case 116: {
                    this.machine.popArgs(this.frame, type);
                    break;
                }
                case 133:
                case 134:
                case 135:
                case 145:
                case 146:
                case 147: {
                    this.machine.popArgs(this.frame, Type.INT);
                    break;
                }
                case 136:
                case 137:
                case 138: {
                    this.machine.popArgs(this.frame, Type.LONG);
                    break;
                }
                case 139:
                case 140:
                case 141: {
                    this.machine.popArgs(this.frame, Type.FLOAT);
                    break;
                }
                case 142:
                case 143:
                case 144: {
                    this.machine.popArgs(this.frame, Type.DOUBLE);
                    break;
                }
                case 177: {
                    this.machine.clearArgs();
                    this.checkReturnType(Type.VOID);
                    break;
                }
                case 172: {
                    Type checkType = type;
                    if (type == Type.OBJECT) {
                        checkType = this.frame.getStack().peekType(0);
                    }
                    this.machine.popArgs(this.frame, type);
                    this.checkReturnType(checkType);
                    break;
                }
                case 87: {
                    final Type peekType = this.frame.getStack().peekType(0);
                    if (peekType.isCategory2()) {
                        throw illegalTos();
                    }
                    this.machine.popArgs(this.frame, 1);
                    break;
                }
                case 190: {
                    final Type arrayType = this.frame.getStack().peekType(0);
                    if (!arrayType.isArrayOrKnownNull()) {
                        throw new SimException("type mismatch: expected array type but encountered " + arrayType.toHuman());
                    }
                    this.machine.popArgs(this.frame, Type.OBJECT);
                    break;
                }
                case 191:
                case 194:
                case 195: {
                    this.machine.popArgs(this.frame, Type.OBJECT);
                    break;
                }
                case 46: {
                    final Type foundArrayType = this.frame.getStack().peekType(1);
                    final Type requiredArrayType = requiredArrayTypeFor(type, foundArrayType);
                    type = ((requiredArrayType == Type.KNOWN_NULL) ? Type.KNOWN_NULL : requiredArrayType.getComponentType());
                    this.machine.popArgs(this.frame, requiredArrayType, Type.INT);
                    break;
                }
                case 96:
                case 100:
                case 104:
                case 108:
                case 112:
                case 126:
                case 128:
                case 130: {
                    this.machine.popArgs(this.frame, type, type);
                    break;
                }
                case 120:
                case 122:
                case 124: {
                    this.machine.popArgs(this.frame, type, Type.INT);
                    break;
                }
                case 148: {
                    this.machine.popArgs(this.frame, Type.LONG, Type.LONG);
                    break;
                }
                case 149:
                case 150: {
                    this.machine.popArgs(this.frame, Type.FLOAT, Type.FLOAT);
                    break;
                }
                case 151:
                case 152: {
                    this.machine.popArgs(this.frame, Type.DOUBLE, Type.DOUBLE);
                    break;
                }
                case 79: {
                    final ExecutionStack stack = this.frame.getStack();
                    final int peekDepth = type.isCategory1() ? 2 : 3;
                    final Type foundArrayType2 = stack.peekType(peekDepth);
                    final boolean foundArrayLocal = stack.peekLocal(peekDepth);
                    final Type requiredArrayType2 = requiredArrayTypeFor(type, foundArrayType2);
                    if (foundArrayLocal) {
                        type = ((requiredArrayType2 == Type.KNOWN_NULL) ? Type.KNOWN_NULL : requiredArrayType2.getComponentType());
                    }
                    this.machine.popArgs(this.frame, requiredArrayType2, Type.INT, type);
                    break;
                }
                case 88:
                case 92: {
                    final ExecutionStack stack = this.frame.getStack();
                    int pattern;
                    if (stack.peekType(0).isCategory2()) {
                        this.machine.popArgs(this.frame, 1);
                        pattern = 17;
                    }
                    else {
                        if (!stack.peekType(1).isCategory1()) {
                            throw illegalTos();
                        }
                        this.machine.popArgs(this.frame, 2);
                        pattern = 8481;
                    }
                    if (opcode == 92) {
                        this.machine.auxIntArg(pattern);
                        break;
                    }
                    break;
                }
                case 89: {
                    final Type peekType = this.frame.getStack().peekType(0);
                    if (peekType.isCategory2()) {
                        throw illegalTos();
                    }
                    this.machine.popArgs(this.frame, 1);
                    this.machine.auxIntArg(17);
                    break;
                }
                case 90: {
                    final ExecutionStack stack = this.frame.getStack();
                    if (!stack.peekType(0).isCategory1() || !stack.peekType(1).isCategory1()) {
                        throw illegalTos();
                    }
                    this.machine.popArgs(this.frame, 2);
                    this.machine.auxIntArg(530);
                    break;
                }
                case 91: {
                    final ExecutionStack stack = this.frame.getStack();
                    if (stack.peekType(0).isCategory2()) {
                        throw illegalTos();
                    }
                    if (stack.peekType(1).isCategory2()) {
                        this.machine.popArgs(this.frame, 2);
                        this.machine.auxIntArg(530);
                        break;
                    }
                    if (stack.peekType(2).isCategory1()) {
                        this.machine.popArgs(this.frame, 3);
                        this.machine.auxIntArg(12819);
                        break;
                    }
                    throw illegalTos();
                }
                case 93: {
                    final ExecutionStack stack = this.frame.getStack();
                    if (stack.peekType(0).isCategory2()) {
                        if (stack.peekType(2).isCategory2()) {
                            throw illegalTos();
                        }
                        this.machine.popArgs(this.frame, 2);
                        this.machine.auxIntArg(530);
                        break;
                    }
                    else {
                        if (stack.peekType(1).isCategory2() || stack.peekType(2).isCategory2()) {
                            throw illegalTos();
                        }
                        this.machine.popArgs(this.frame, 3);
                        this.machine.auxIntArg(205106);
                        break;
                    }

                }
                case 94: {
                    final ExecutionStack stack = this.frame.getStack();
                    if (stack.peekType(0).isCategory2()) {
                        if (stack.peekType(2).isCategory2()) {
                            this.machine.popArgs(this.frame, 2);
                            this.machine.auxIntArg(530);
                            break;
                        }
                        if (stack.peekType(3).isCategory1()) {
                            this.machine.popArgs(this.frame, 3);
                            this.machine.auxIntArg(12819);
                            break;
                        }
                        throw illegalTos();
                    }
                    else {
                        if (!stack.peekType(1).isCategory1()) {
                            throw illegalTos();
                        }
                        if (stack.peekType(2).isCategory2()) {
                            this.machine.popArgs(this.frame, 3);
                            this.machine.auxIntArg(205106);
                            break;
                        }
                        if (stack.peekType(3).isCategory1()) {
                            this.machine.popArgs(this.frame, 4);
                            this.machine.auxIntArg(4399427);
                            break;
                        }
                        throw illegalTos();
                    }

                }
                case 95: {
                    final ExecutionStack stack = this.frame.getStack();
                    if (!stack.peekType(0).isCategory1() || !stack.peekType(1).isCategory1()) {
                        throw illegalTos();
                    }
                    this.machine.popArgs(this.frame, 2);
                    this.machine.auxIntArg(18);
                    break;
                }
                default: {
                    this.visitInvalid(opcode, offset, length);
                    return;
                }
            }
            this.machine.auxType(type);
            this.machine.run(this.frame, offset, opcode);
        }
        
        private void checkReturnType(final Type encountered) {
            final Type returnType = this.machine.getPrototype().getReturnType();
            if (!Merger.isPossiblyAssignableFrom(returnType, encountered)) {
                throw new SimException("return type mismatch: prototype indicates " + returnType.toHuman() + ", but encountered type " + encountered.toHuman());
            }
        }
        
        @Override
        public void visitLocal(final int opcode, final int offset, final int length, final int idx, final Type type, final int value) {
            final int localOffset = (opcode == 54) ? (offset + length) : offset;
            LocalVariableList.Item local = Simulator.this.localVariables.pcAndIndexToLocal(localOffset, idx);
            Type localType;
            if (local != null) {
                localType = local.getType();
                if (localType.getBasicFrameType() != type.getBasicFrameType()) {
                    local = null;
                    localType = type;
                }
            }
            else {
                localType = type;
            }
            switch (opcode) {
                case 21:
                case 169: {
                    this.machine.localArg(this.frame, idx);
                    this.machine.localInfo(local != null);
                    this.machine.auxType(type);
                    break;
                }
                case 54: {
                    final LocalItem item = (local == null) ? null : local.getLocalItem();
                    this.machine.popArgs(this.frame, type);
                    this.machine.auxType(type);
                    this.machine.localTarget(idx, localType, item);
                    break;
                }
                case 132: {
                    final LocalItem item = (local == null) ? null : local.getLocalItem();
                    this.machine.localArg(this.frame, idx);
                    this.machine.localTarget(idx, localType, item);
                    this.machine.auxType(type);
                    this.machine.auxIntArg(value);
                    this.machine.auxCstArg(CstInteger.make(value));
                    break;
                }
                default: {
                    this.visitInvalid(opcode, offset, length);
                    return;
                }
            }
            this.machine.run(this.frame, offset, opcode);
        }
        
        @Override
        public void visitConstant(final int opcode, final int offset, final int length, Constant cst, final int value) {
            switch (opcode) {
                case 189: {
                    this.machine.popArgs(this.frame, Type.INT);
                    break;
                }
                case 179: {
                    final Type fieldType = ((CstFieldRef)cst).getType();
                    this.machine.popArgs(this.frame, fieldType);
                    break;
                }
                case 180:
                case 192:
                case 193: {
                    this.machine.popArgs(this.frame, Type.OBJECT);
                    break;
                }
                case 181: {
                    final Type fieldType = ((CstFieldRef)cst).getType();
                    this.machine.popArgs(this.frame, Type.OBJECT, fieldType);
                    break;
                }
                case 182:
                case 183:
                case 184:
                case 185: {
                    if (cst instanceof CstInterfaceMethodRef) {
                        if (opcode != 185 && !Simulator.this.dexOptions.canUseDefaultInterfaceMethods()) {
                            throw new SimException("default or static interface method used without --min-sdk-version >= 24");
                        }
                        cst = ((CstInterfaceMethodRef)cst).toMethodRef();
                    }
                    if (cst instanceof CstMethodRef) {
                        final CstMethodRef methodRef = (CstMethodRef)cst;
                        if (methodRef.isSignaturePolymorphic()) {
                            if (!Simulator.this.dexOptions.canUseInvokePolymorphic()) {
                                throw new SimException("signature-polymorphic method called without --min-sdk-version >= 26");
                            }
                            if (opcode != 182) {
                                throw new SimException("Unsupported signature polymorphic invocation (" + ByteOps.opName(opcode) + ")");
                            }
                        }
                    }
                    final boolean staticMethod = opcode == 184;
                    final Prototype prototype = ((CstMethodRef)cst).getPrototype(staticMethod);
                    this.machine.popArgs(this.frame, prototype);
                    break;
                }
                case 186: {
                    if (!Simulator.this.dexOptions.canUseInvokeCustom()) {
                        throw new SimException("invalid opcode " + Hex.u1(opcode) + " (invokedynamic requires --min-sdk-version >= " + 26 + ")");
                    }
                    final CstInvokeDynamic invokeDynamicRef = (CstInvokeDynamic)cst;
                    final Prototype prototype = invokeDynamicRef.getPrototype();
                    this.machine.popArgs(this.frame, prototype);
                    cst = invokeDynamicRef.addReference();
                    break;
                }
                case 197: {
                    final Prototype prototype2 = Prototype.internInts(Type.VOID, value);
                    this.machine.popArgs(this.frame, prototype2);
                    break;
                }
                default: {
                    this.machine.clearArgs();
                    break;
                }
            }
            this.machine.auxIntArg(value);
            this.machine.auxCstArg(cst);
            this.machine.run(this.frame, offset, opcode);
        }
        
        @Override
        public void visitBranch(final int opcode, final int offset, final int length, final int target) {
            switch (opcode) {
                case 153:
                case 154:
                case 155:
                case 156:
                case 157:
                case 158: {
                    this.machine.popArgs(this.frame, Type.INT);
                    break;
                }
                case 198:
                case 199: {
                    this.machine.popArgs(this.frame, Type.OBJECT);
                    break;
                }
                case 159:
                case 160:
                case 161:
                case 162:
                case 163:
                case 164: {
                    this.machine.popArgs(this.frame, Type.INT, Type.INT);
                    break;
                }
                case 165:
                case 166: {
                    this.machine.popArgs(this.frame, Type.OBJECT, Type.OBJECT);
                    break;
                }
                case 167:
                case 168:
                case 200:
                case 201: {
                    this.machine.clearArgs();
                    break;
                }
                default: {
                    this.visitInvalid(opcode, offset, length);
                    return;
                }
            }
            this.machine.auxTargetArg(target);
            this.machine.run(this.frame, offset, opcode);
        }
        
        @Override
        public void visitSwitch(final int opcode, final int offset, final int length, final SwitchList cases, final int padding) {
            this.machine.popArgs(this.frame, Type.INT);
            this.machine.auxIntArg(padding);
            this.machine.auxSwitchArg(cases);
            this.machine.run(this.frame, offset, opcode);
        }
        
        @Override
        public void visitNewarray(final int offset, final int length, final CstType type, final ArrayList<Constant> initValues) {
            this.machine.popArgs(this.frame, Type.INT);
            this.machine.auxInitValues(initValues);
            this.machine.auxCstArg(type);
            this.machine.run(this.frame, offset, 188);
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
}

package embedded.com.android.dx.cf.code;

import java.util.*;
import embedded.com.android.dx.rop.type.*;
import embedded.com.android.dx.util.*;
import embedded.com.android.dx.cf.iface.*;
import embedded.com.android.dx.rop.code.*;
import embedded.com.android.dx.rop.cst.*;

final class RopperMachine extends ValueAwareMachine
{
    private static final CstType ARRAY_REFLECT_TYPE;
    private static final CstMethodRef MULTIANEWARRAY_METHOD;
    private final Ropper ropper;
    private final ConcreteMethod method;
    private final MethodList methods;
    private final TranslationAdvice advice;
    private final int maxLocals;
    private final ArrayList<Insn> insns;
    private TypeList catches;
    private boolean catchesUsed;
    private boolean returns;
    private int primarySuccessorIndex;
    private int extraBlockCount;
    private boolean hasJsr;
    private boolean blockCanThrow;
    private ReturnAddress returnAddress;
    private Rop returnOp;
    private SourcePosition returnPosition;
    
    public RopperMachine(final Ropper ropper, final ConcreteMethod method, final TranslationAdvice advice, final MethodList methods) {
        super(method.getEffectiveDescriptor());
        if (methods == null) {
            throw new NullPointerException("methods == null");
        }
        if (ropper == null) {
            throw new NullPointerException("ropper == null");
        }
        if (advice == null) {
            throw new NullPointerException("advice == null");
        }
        this.ropper = ropper;
        this.method = method;
        this.methods = methods;
        this.advice = advice;
        this.maxLocals = method.getMaxLocals();
        this.insns = new ArrayList<Insn>(25);
        this.catches = null;
        this.catchesUsed = false;
        this.returns = false;
        this.primarySuccessorIndex = -1;
        this.extraBlockCount = 0;
        this.blockCanThrow = false;
        this.returnOp = null;
        this.returnPosition = null;
    }
    
    public ArrayList<Insn> getInsns() {
        return this.insns;
    }
    
    public Rop getReturnOp() {
        return this.returnOp;
    }
    
    public SourcePosition getReturnPosition() {
        return this.returnPosition;
    }
    
    public void startBlock(final TypeList catches) {
        this.catches = catches;
        this.insns.clear();
        this.catchesUsed = false;
        this.returns = false;
        this.primarySuccessorIndex = 0;
        this.extraBlockCount = 0;
        this.blockCanThrow = false;
        this.hasJsr = false;
        this.returnAddress = null;
    }
    
    public boolean wereCatchesUsed() {
        return this.catchesUsed;
    }
    
    public boolean returns() {
        return this.returns;
    }
    
    public int getPrimarySuccessorIndex() {
        return this.primarySuccessorIndex;
    }
    
    public int getExtraBlockCount() {
        return this.extraBlockCount;
    }
    
    public boolean canThrow() {
        return this.blockCanThrow;
    }
    
    public boolean hasJsr() {
        return this.hasJsr;
    }
    
    public boolean hasRet() {
        return this.returnAddress != null;
    }
    
    public ReturnAddress getReturnAddress() {
        return this.returnAddress;
    }
    
    @Override
    public void run(final Frame frame, final int offset, int opcode) {
        int stackPointer = this.maxLocals + frame.getStack().size();
        RegisterSpecList sources = this.getSources(opcode, stackPointer);
        final int sourceCount = sources.size();
        super.run(frame, offset, opcode);
        final SourcePosition pos = this.method.makeSourcePosistion(offset);
        final RegisterSpec localTarget = this.getLocalTarget(opcode == 54);
        final int destCount = this.resultCount();
        RegisterSpec dest;
        if (destCount == 0) {
            dest = null;
            switch (opcode) {
                case 87:
                case 88: {
                    return;
                }
            }
        }
        else if (localTarget != null) {
            dest = localTarget;
        }
        else {
            if (destCount != 1) {
                int scratchAt = this.ropper.getFirstTempStackReg();
                final RegisterSpec[] scratchRegs = new RegisterSpec[sourceCount];
                for (int i = 0; i < sourceCount; ++i) {
                    final RegisterSpec src = sources.get(i);
                    final TypeBearer type = src.getTypeBearer();
                    final RegisterSpec scratch = src.withReg(scratchAt);
                    this.insns.add(new PlainInsn(Rops.opMove(type), pos, scratch, src));
                    scratchRegs[i] = scratch;
                    scratchAt += src.getCategory();
                }
                for (int pattern = this.getAuxInt(); pattern != 0; pattern >>= 4) {
                    final int which = (pattern & 0xF) - 1;
                    final RegisterSpec scratch2 = scratchRegs[which];
                    final TypeBearer type2 = scratch2.getTypeBearer();
                    this.insns.add(new PlainInsn(Rops.opMove(type2), pos, scratch2.withReg(stackPointer), scratch2));
                    stackPointer += type2.getType().getCategory();
                }
                return;
            }
            dest = RegisterSpec.make(stackPointer, this.result(0));
        }
        final TypeBearer destType = (dest != null) ? dest : Type.VOID;
        Constant cst = this.getAuxCst();
        if (opcode == 197) {
            this.blockCanThrow = true;
            this.extraBlockCount = 6;
            final RegisterSpec dimsReg = RegisterSpec.make(dest.getNextReg(), Type.INT_ARRAY);
            Rop rop = Rops.opFilledNewArray(Type.INT_ARRAY, sourceCount);
            Insn insn = new ThrowingCstInsn(rop, pos, sources, this.catches, CstType.INT_ARRAY);
            this.insns.add(insn);
            rop = Rops.opMoveResult(Type.INT_ARRAY);
            insn = new PlainInsn(rop, pos, dimsReg, RegisterSpecList.EMPTY);
            this.insns.add(insn);
            Type componentType = ((CstType)cst).getClassType();
            for (int j = 0; j < sourceCount; ++j) {
                componentType = componentType.getComponentType();
            }
            final RegisterSpec classReg = RegisterSpec.make(dest.getReg(), Type.CLASS);
            if (componentType.isPrimitive()) {
                final CstFieldRef typeField = CstFieldRef.forPrimitiveType(componentType);
                insn = new ThrowingCstInsn(Rops.GET_STATIC_OBJECT, pos, RegisterSpecList.EMPTY, this.catches, typeField);
            }
            else {
                insn = new ThrowingCstInsn(Rops.CONST_OBJECT, pos, RegisterSpecList.EMPTY, this.catches, new CstType(componentType));
            }
            this.insns.add(insn);
            rop = Rops.opMoveResultPseudo(classReg.getType());
            insn = new PlainInsn(rop, pos, classReg, RegisterSpecList.EMPTY);
            this.insns.add(insn);
            final RegisterSpec objectReg = RegisterSpec.make(dest.getReg(), Type.OBJECT);
            insn = new ThrowingCstInsn(Rops.opInvokeStatic(RopperMachine.MULTIANEWARRAY_METHOD.getPrototype()), pos, RegisterSpecList.make(classReg, dimsReg), this.catches, RopperMachine.MULTIANEWARRAY_METHOD);
            this.insns.add(insn);
            rop = Rops.opMoveResult(RopperMachine.MULTIANEWARRAY_METHOD.getPrototype().getReturnType());
            insn = new PlainInsn(rop, pos, objectReg, RegisterSpecList.EMPTY);
            this.insns.add(insn);
            opcode = 192;
            sources = RegisterSpecList.make(objectReg);
        }
        else {
            if (opcode == 168) {
                this.hasJsr = true;
                return;
            }
            if (opcode == 169) {
                try {
                    this.returnAddress = (ReturnAddress)this.arg(0);
                }
                catch (ClassCastException ex) {
                    throw new RuntimeException("Argument to RET was not a ReturnAddress", ex);
                }
                return;
            }
        }
        int ropOpcode = this.jopToRopOpcode(opcode, cst);
        Rop rop = Rops.ropFor(ropOpcode, destType, sources, cst);
        Insn moveResult = null;
        if (dest != null && rop.isCallLike()) {
            ++this.extraBlockCount;
            Type returnType;
            if (rop.getOpcode() == 59) {
                returnType = ((CstCallSiteRef)cst).getReturnType();
            }
            else {
                returnType = ((CstMethodRef)cst).getPrototype().getReturnType();
            }
            moveResult = new PlainInsn(Rops.opMoveResult(returnType), pos, dest, RegisterSpecList.EMPTY);
            dest = null;
        }
        else if (dest != null && rop.canThrow()) {
            ++this.extraBlockCount;
            moveResult = new PlainInsn(Rops.opMoveResultPseudo(dest.getTypeBearer()), pos, dest, RegisterSpecList.EMPTY);
            dest = null;
        }
        if (ropOpcode == 41) {
            cst = CstType.intern(rop.getResult());
        }
        else if (cst == null && sourceCount == 2) {
            final TypeBearer firstType = sources.get(0).getTypeBearer();
            final TypeBearer lastType = sources.get(1).getTypeBearer();
            if ((lastType.isConstant() || firstType.isConstant()) && this.advice.hasConstantOperation(rop, sources.get(0), sources.get(1))) {
                if (lastType.isConstant()) {
                    cst = (Constant)lastType;
                    sources = sources.withoutLast();
                    if (rop.getOpcode() == 15) {
                        ropOpcode = 14;
                        final CstInteger cstInt = (CstInteger)lastType;
                        cst = CstInteger.make(-cstInt.getValue());
                    }
                }
                else {
                    cst = (Constant)firstType;
                    sources = sources.withoutFirst();
                }
                rop = Rops.ropFor(ropOpcode, destType, sources, cst);
            }
        }
        final SwitchList cases = this.getAuxCases();
        final ArrayList<Constant> initValues = this.getInitValues();
        final boolean canThrow = rop.canThrow();
        this.blockCanThrow |= canThrow;
        Insn insn;
        if (cases != null) {
            if (cases.size() == 0) {
                insn = new PlainInsn(Rops.GOTO, pos, null, RegisterSpecList.EMPTY);
                this.primarySuccessorIndex = 0;
            }
            else {
                final IntList values = cases.getValues();
                insn = new SwitchInsn(rop, pos, dest, sources, values);
                this.primarySuccessorIndex = values.size();
            }
        }
        else if (ropOpcode == 33) {
            if (sources.size() != 0) {
                final RegisterSpec source = sources.get(0);
                final TypeBearer type3 = source.getTypeBearer();
                if (source.getReg() != 0) {
                    this.insns.add(new PlainInsn(Rops.opMove(type3), pos, RegisterSpec.make(0, type3), source));
                }
            }
            insn = new PlainInsn(Rops.GOTO, pos, null, RegisterSpecList.EMPTY);
            this.primarySuccessorIndex = 0;
            this.updateReturnOp(rop, pos);
            this.returns = true;
        }
        else if (cst != null) {
            if (canThrow) {
                if (rop.getOpcode() == 58) {
                    insn = this.makeInvokePolymorphicInsn(rop, pos, sources, this.catches, cst);
                }
                else {
                    insn = new ThrowingCstInsn(rop, pos, sources, this.catches, cst);
                }
                this.catchesUsed = true;
                this.primarySuccessorIndex = this.catches.size();
            }
            else {
                insn = new PlainCstInsn(rop, pos, dest, sources, cst);
            }
        }
        else if (canThrow) {
            insn = new ThrowingInsn(rop, pos, sources, this.catches);
            this.catchesUsed = true;
            if (opcode == 191) {
                this.primarySuccessorIndex = -1;
            }
            else {
                this.primarySuccessorIndex = this.catches.size();
            }
        }
        else {
            insn = new PlainInsn(rop, pos, dest, sources);
        }
        this.insns.add(insn);
        if (moveResult != null) {
            this.insns.add(moveResult);
        }
        if (initValues != null) {
            ++this.extraBlockCount;
            insn = new FillArrayDataInsn(Rops.FILL_ARRAY_DATA, pos, RegisterSpecList.make(moveResult.getResult()), initValues, cst);
            this.insns.add(insn);
        }
    }
    
    private RegisterSpecList getSources(final int opcode, final int stackPointer) {
        final int count = this.argCount();
        if (count == 0) {
            return RegisterSpecList.EMPTY;
        }
        final int localIndex = this.getLocalIndex();
        RegisterSpecList sources;
        if (localIndex >= 0) {
            sources = new RegisterSpecList(1);
            sources.set(0, RegisterSpec.make(localIndex, this.arg(0)));
        }
        else {
            sources = new RegisterSpecList(count);
            int regAt = stackPointer;
            for (int i = 0; i < count; ++i) {
                final RegisterSpec spec = RegisterSpec.make(regAt, this.arg(i));
                sources.set(i, spec);
                regAt += spec.getCategory();
            }
            switch (opcode) {
                case 79: {
                    if (count != 3) {
                        throw new RuntimeException("shouldn't happen");
                    }
                    final RegisterSpec array = sources.get(0);
                    final RegisterSpec index = sources.get(1);
                    final RegisterSpec value = sources.get(2);
                    sources.set(0, value);
                    sources.set(1, array);
                    sources.set(2, index);
                    break;
                }
                case 181: {
                    if (count != 2) {
                        throw new RuntimeException("shouldn't happen");
                    }
                    final RegisterSpec obj = sources.get(0);
                    final RegisterSpec value2 = sources.get(1);
                    sources.set(0, value2);
                    sources.set(1, obj);
                    break;
                }
            }
        }
        sources.setImmutable();
        return sources;
    }
    
    private void updateReturnOp(final Rop op, final SourcePosition pos) {
        if (op == null) {
            throw new NullPointerException("op == null");
        }
        if (pos == null) {
            throw new NullPointerException("pos == null");
        }
        if (this.returnOp == null) {
            this.returnOp = op;
            this.returnPosition = pos;
        }
        else {
            if (this.returnOp != op) {
                throw new SimException("return op mismatch: " + op + ", " + this.returnOp);
            }
            if (pos.getLine() > this.returnPosition.getLine()) {
                this.returnPosition = pos;
            }
        }
    }
    
    private int jopToRopOpcode(final int jop, final Constant cst) {
        switch (jop) {
            case 0: {
                return 1;
            }
            case 18:
            case 20: {
                return 5;
            }
            case 21:
            case 54: {
                return 2;
            }
            case 46: {
                return 38;
            }
            case 79: {
                return 39;
            }
            case 96:
            case 132: {
                return 14;
            }
            case 100: {
                return 15;
            }
            case 104: {
                return 16;
            }
            case 108: {
                return 17;
            }
            case 112: {
                return 18;
            }
            case 116: {
                return 19;
            }
            case 120: {
                return 23;
            }
            case 122: {
                return 24;
            }
            case 124: {
                return 25;
            }
            case 126: {
                return 20;
            }
            case 128: {
                return 21;
            }
            case 130: {
                return 22;
            }
            case 133:
            case 134:
            case 135:
            case 136:
            case 137:
            case 138:
            case 139:
            case 140:
            case 141:
            case 142:
            case 143:
            case 144: {
                return 29;
            }
            case 145: {
                return 30;
            }
            case 146: {
                return 31;
            }
            case 147: {
                return 32;
            }
            case 148:
            case 149:
            case 151: {
                return 27;
            }
            case 150:
            case 152: {
                return 28;
            }
            case 153:
            case 159:
            case 165:
            case 198: {
                return 7;
            }
            case 154:
            case 160:
            case 166:
            case 199: {
                return 8;
            }
            case 155:
            case 161: {
                return 9;
            }
            case 156:
            case 162: {
                return 10;
            }
            case 157:
            case 163: {
                return 12;
            }
            case 158:
            case 164: {
                return 11;
            }
            case 167: {
                return 6;
            }
            case 171: {
                return 13;
            }
            case 172:
            case 177: {
                return 33;
            }
            case 178: {
                return 46;
            }
            case 179: {
                return 48;
            }
            case 180: {
                return 45;
            }
            case 181: {
                return 47;
            }
            case 182: {
                final CstMethodRef ref = (CstMethodRef)cst;
                if (ref.getDefiningClass().equals(this.method.getDefiningClass())) {
                    for (int i = 0; i < this.methods.size(); ++i) {
                        final Method m = this.methods.get(i);
                        if (AccessFlags.isPrivate(m.getAccessFlags()) && ref.getNat().equals(m.getNat())) {
                            return 52;
                        }
                    }
                }
                if (ref.isSignaturePolymorphic()) {
                    return 58;
                }
                return 50;
            }
            case 183: {
                final CstMethodRef ref = (CstMethodRef)cst;
                if (ref.isInstanceInit() || ref.getDefiningClass().equals(this.method.getDefiningClass())) {
                    return 52;
                }
                return 51;
            }
            case 184: {
                return 49;
            }
            case 185: {
                return 53;
            }
            case 186: {
                return 59;
            }
            case 187: {
                return 40;
            }
            case 188:
            case 189: {
                return 41;
            }
            case 190: {
                return 34;
            }
            case 191: {
                return 35;
            }
            case 192: {
                return 43;
            }
            case 193: {
                return 44;
            }
            case 194: {
                return 36;
            }
            case 195: {
                return 37;
            }
        }
        throw new RuntimeException("shouldn't happen");
    }
    
    private Insn makeInvokePolymorphicInsn(final Rop rop, final SourcePosition pos, final RegisterSpecList sources, final TypeList catches, final Constant cst) {
        final CstMethodRef cstMethodRef = (CstMethodRef)cst;
        return new InvokePolymorphicInsn(rop, pos, sources, catches, cstMethodRef);
    }
    
    static {
        ARRAY_REFLECT_TYPE = new CstType(Type.internClassName("java/lang/reflect/Array"));
        MULTIANEWARRAY_METHOD = new CstMethodRef(RopperMachine.ARRAY_REFLECT_TYPE, new CstNat(new CstString("newInstance"), new CstString("(Ljava/lang/Class;[I)Ljava/lang/Object;")));
    }
}

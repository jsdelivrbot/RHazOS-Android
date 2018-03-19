package embedded.com.android.dx.dex.code;

import embedded.com.android.dx.dex.*;
import embedded.com.android.dx.rop.type.*;
import embedded.com.android.dx.rop.cst.*;
import java.util.*;
import embedded.com.android.dex.*;
import embedded.com.android.dx.rop.code.*;
import embedded.com.android.dx.ssa.*;

public final class OutputFinisher
{
    private final DexOptions dexOptions;
    private final int unreservedRegCount;
    private ArrayList<DalvInsn> insns;
    private boolean hasAnyPositionInfo;
    private boolean hasAnyLocalInfo;
    private int reservedCount;
    private int reservedParameterCount;
    private final int paramSize;
    
    public OutputFinisher(final DexOptions dexOptions, final int initialCapacity, final int regCount, final int paramSize) {
        this.dexOptions = dexOptions;
        this.unreservedRegCount = regCount;
        this.insns = new ArrayList<DalvInsn>(initialCapacity);
        this.reservedCount = -1;
        this.hasAnyPositionInfo = false;
        this.hasAnyLocalInfo = false;
        this.paramSize = paramSize;
    }
    
    public boolean hasAnyPositionInfo() {
        return this.hasAnyPositionInfo;
    }
    
    public boolean hasAnyLocalInfo() {
        return this.hasAnyLocalInfo;
    }
    
    private static boolean hasLocalInfo(final DalvInsn insn) {
        if (insn instanceof LocalSnapshot) {
            final RegisterSpecSet specs = ((LocalSnapshot)insn).getLocals();
            for (int size = specs.size(), i = 0; i < size; ++i) {
                if (hasLocalInfo(specs.get(i))) {
                    return true;
                }
            }
        }
        else if (insn instanceof LocalStart) {
            final RegisterSpec spec = ((LocalStart)insn).getLocal();
            if (hasLocalInfo(spec)) {
                return true;
            }
        }
        return false;
    }
    
    private static boolean hasLocalInfo(final RegisterSpec spec) {
        return spec != null && spec.getLocalItem().getName() != null;
    }
    
    public HashSet<Constant> getAllConstants() {
        final HashSet<Constant> result = new HashSet<Constant>(20);
        for (final DalvInsn insn : this.insns) {
            addConstants(result, insn);
        }
        return result;
    }
    
    private static void addConstants(final HashSet<Constant> result, final DalvInsn insn) {
        if (insn instanceof CstInsn) {
            final Constant cst = ((CstInsn)insn).getConstant();
            result.add(cst);
        }
        else if (insn instanceof MultiCstInsn) {
            final MultiCstInsn m = (MultiCstInsn)insn;
            for (int i = 0; i < m.getNumberOfConstants(); ++i) {
                result.add(m.getConstant(i));
            }
        }
        else if (insn instanceof LocalSnapshot) {
            final RegisterSpecSet specs = ((LocalSnapshot)insn).getLocals();
            for (int size = specs.size(), j = 0; j < size; ++j) {
                addConstants(result, specs.get(j));
            }
        }
        else if (insn instanceof LocalStart) {
            final RegisterSpec spec = ((LocalStart)insn).getLocal();
            addConstants(result, spec);
        }
    }
    
    private static void addConstants(final HashSet<Constant> result, final RegisterSpec spec) {
        if (spec == null) {
            return;
        }
        final LocalItem local = spec.getLocalItem();
        final CstString name = local.getName();
        final CstString signature = local.getSignature();
        final Type type = spec.getType();
        if (type != Type.KNOWN_NULL) {
            result.add(CstType.intern(type));
        }
        else {
            result.add(CstType.intern(Type.OBJECT));
        }
        if (name != null) {
            result.add(name);
        }
        if (signature != null) {
            result.add(signature);
        }
    }
    
    public void add(final DalvInsn insn) {
        this.insns.add(insn);
        this.updateInfo(insn);
    }
    
    public void insert(final int at, final DalvInsn insn) {
        this.insns.add(at, insn);
        this.updateInfo(insn);
    }
    
    private void updateInfo(final DalvInsn insn) {
        if (!this.hasAnyPositionInfo) {
            final SourcePosition pos = insn.getPosition();
            if (pos.getLine() >= 0) {
                this.hasAnyPositionInfo = true;
            }
        }
        if (!this.hasAnyLocalInfo && hasLocalInfo(insn)) {
            this.hasAnyLocalInfo = true;
        }
    }
    
    public void reverseBranch(final int which, final CodeAddress newTarget) {
        final int size = this.insns.size();
        final int index = size - which - 1;
        TargetInsn targetInsn;
        try {
            targetInsn = (TargetInsn)  this.insns.get(index);
        }
        catch (IndexOutOfBoundsException ex) {
            throw new IllegalArgumentException("too few instructions");
        }
        catch (ClassCastException ex2) {
            throw new IllegalArgumentException("non-reversible instruction");
        }
        this.insns.set(index, targetInsn.withNewTargetAndReversed(newTarget));
    }
    
    public void assignIndices(final DalvCode.AssignIndicesCallback callback) {
        for (final DalvInsn insn : this.insns) {
            if (insn instanceof CstInsn) {
                assignIndices((CstInsn)insn, callback);
            }
            else {
                if (!(insn instanceof MultiCstInsn)) {
                    continue;
                }
                assignIndices((MultiCstInsn)insn, callback);
            }
        }
    }
    
    private static void assignIndices(final CstInsn insn, final DalvCode.AssignIndicesCallback callback) {
        final Constant cst = insn.getConstant();
        int index = callback.getIndex(cst);
        if (index >= 0) {
            insn.setIndex(index);
        }
        if (cst instanceof CstMemberRef) {
            final CstMemberRef member = (CstMemberRef)cst;
            final CstType definer = member.getDefiningClass();
            index = callback.getIndex(definer);
            if (index >= 0) {
                insn.setClassIndex(index);
            }
        }
    }
    
    private static void assignIndices(final MultiCstInsn insn, final DalvCode.AssignIndicesCallback callback) {
        for (int i = 0; i < insn.getNumberOfConstants(); ++i) {
            final Constant cst = insn.getConstant(i);
            int index = callback.getIndex(cst);
            insn.setIndex(i, index);
            if (cst instanceof CstMemberRef) {
                final CstMemberRef member = (CstMemberRef)cst;
                final CstType definer = member.getDefiningClass();
                index = callback.getIndex(definer);
                insn.setClassIndex(index);
            }
        }
    }
    
    public DalvInsnList finishProcessingAndGetList() {
        if (this.reservedCount >= 0) {
            throw new UnsupportedOperationException("already processed");
        }
        final Dop[] opcodes = this.makeOpcodesArray();
        this.reserveRegisters(opcodes);
        if (this.dexOptions.ALIGN_64BIT_REGS_IN_OUTPUT_FINISHER) {
            this.align64bits(opcodes);
        }
        this.massageInstructions(opcodes);
        this.assignAddressesAndFixBranches();
        return DalvInsnList.makeImmutable(this.insns, this.reservedCount + this.unreservedRegCount + this.reservedParameterCount);
    }
    
    private Dop[] makeOpcodesArray() {
        final int size = this.insns.size();
        final Dop[] result = new Dop[size];
        for (int i = 0; i < size; ++i) {
            final DalvInsn insn = this.insns.get(i);
            result[i] = insn.getOpcode();
        }
        return result;
    }
    
    private boolean reserveRegisters(final Dop[] opcodes) {
        boolean reservedCountExpanded = false;
        int oldReservedCount = (this.reservedCount < 0) ? 0 : this.reservedCount;
        while (true) {
            final int newReservedCount = this.calculateReservedCount(opcodes);
            if (oldReservedCount >= newReservedCount) {
                break;
            }
            reservedCountExpanded = true;
            final int reservedDifference = newReservedCount - oldReservedCount;
            for (int size = this.insns.size(), i = 0; i < size; ++i) {
                final DalvInsn insn = this.insns.get(i);
                if (!(insn instanceof CodeAddress)) {
                    this.insns.set(i, insn.withRegisterOffset(reservedDifference));
                }
            }
            oldReservedCount = newReservedCount;
        }
        this.reservedCount = oldReservedCount;
        return reservedCountExpanded;
    }
    
    private int calculateReservedCount(final Dop[] opcodes) {
        final int size = this.insns.size();
        int newReservedCount = this.reservedCount;
        for (int i = 0; i < size; ++i) {
            final DalvInsn insn = this.insns.get(i);
            final Dop originalOpcode = opcodes[i];
            final Dop newOpcode = this.findOpcodeForInsn(insn, originalOpcode);
            if (newOpcode == null) {
                final Dop expandedOp = this.findExpandedOpcodeForInsn(insn);
                final BitSet compatRegs = expandedOp.getFormat().compatibleRegs(insn);
                final int reserve = insn.getMinimumRegisterRequirement(compatRegs);
                if (reserve > newReservedCount) {
                    newReservedCount = reserve;
                }
            }
            else if (originalOpcode == newOpcode) {
                continue;
            }
            opcodes[i] = newOpcode;
        }
        return newReservedCount;
    }
    
    private Dop findOpcodeForInsn(final DalvInsn insn, Dop guess) {
        while (guess != null) {
            if (guess.getFormat().isCompatible(insn)) {
                if (!this.dexOptions.forceJumbo) {
                    break;
                }
                if (guess.getOpcode() != 26) {
                    break;
                }
            }
            guess = Dops.getNextOrNull(guess, this.dexOptions);
        }
        return guess;
    }
    
    private Dop findExpandedOpcodeForInsn(final DalvInsn insn) {
        final Dop result = this.findOpcodeForInsn(insn.getLowRegVersion(), insn.getOpcode());
        if (result == null) {
            throw new DexException("No expanded opcode for " + insn);
        }
        return result;
    }
    
    private void massageInstructions(final Dop[] opcodes) {
        if (this.reservedCount == 0) {
            for (int size = this.insns.size(), i = 0; i < size; ++i) {
                final DalvInsn insn = this.insns.get(i);
                final Dop originalOpcode = insn.getOpcode();
                final Dop currentOpcode = opcodes[i];
                if (originalOpcode != currentOpcode) {
                    this.insns.set(i, insn.withOpcode(currentOpcode));
                }
            }
        }
        else {
            this.insns = this.performExpansion(opcodes);
        }
    }
    
    private ArrayList<DalvInsn> performExpansion(final Dop[] opcodes) {
        final int size = this.insns.size();
        final ArrayList<DalvInsn> result = new ArrayList<DalvInsn>(size * 2);
        final ArrayList<CodeAddress> closelyBoundAddresses = new ArrayList<CodeAddress>();
        for (int i = 0; i < size; ++i) {
            DalvInsn insn = this.insns.get(i);
            final Dop originalOpcode = insn.getOpcode();
            Dop currentOpcode = opcodes[i];
            DalvInsn prefix;
            DalvInsn suffix;
            if (currentOpcode != null) {
                prefix = null;
                suffix = null;
            }
            else {
                currentOpcode = this.findExpandedOpcodeForInsn(insn);
                final BitSet compatRegs = currentOpcode.getFormat().compatibleRegs(insn);
                prefix = insn.expandedPrefix(compatRegs);
                suffix = insn.expandedSuffix(compatRegs);
                insn = insn.expandedVersion(compatRegs);
            }
            if (insn instanceof CodeAddress && ((CodeAddress)insn).getBindsClosely()) {
                closelyBoundAddresses.add((CodeAddress)insn);
            }
            else {
                if (prefix != null) {
                    result.add(prefix);
                }
                if (!(insn instanceof ZeroSizeInsn) && closelyBoundAddresses.size() > 0) {
                    for (final CodeAddress codeAddress : closelyBoundAddresses) {
                        result.add(codeAddress);
                    }
                    closelyBoundAddresses.clear();
                }
                if (currentOpcode != originalOpcode) {
                    insn = insn.withOpcode(currentOpcode);
                }
                result.add(insn);
                if (suffix != null) {
                    result.add(suffix);
                }
            }
        }
        return result;
    }
    
    private void assignAddressesAndFixBranches() {
        do {
            this.assignAddresses();
        } while (this.fixBranches());
    }
    
    private void assignAddresses() {
        int address = 0;
        for (int size = this.insns.size(), i = 0; i < size; ++i) {
            final DalvInsn insn = this.insns.get(i);
            insn.setAddress(address);
            address += insn.codeSize();
        }
    }
    
    private boolean fixBranches() {
        int size = this.insns.size();
        boolean anyFixed = false;
        for (int i = 0; i < size; ++i) {
            final DalvInsn insn = this.insns.get(i);
            if (insn instanceof TargetInsn) {
                Dop opcode = insn.getOpcode();
                final TargetInsn target = (TargetInsn)insn;
                if (!opcode.getFormat().branchFits(target)) {
                    if (opcode.getFamily() == 40) {
                        opcode = this.findOpcodeForInsn(insn, opcode);
                        if (opcode == null) {
                            throw new UnsupportedOperationException("method too long");
                        }
                        this.insns.set(i, insn.withOpcode(opcode));
                    }
                    else {
                        CodeAddress newTarget;
                        try {
                            newTarget = (CodeAddress) this.insns.get(i + 1);
                        }
                        catch (IndexOutOfBoundsException ex) {
                            throw new IllegalStateException("unpaired TargetInsn (dangling)");
                        }
                        catch (ClassCastException ex2) {
                            throw new IllegalStateException("unpaired TargetInsn");
                        }
                        final TargetInsn gotoInsn = new TargetInsn(Dops.GOTO, target.getPosition(), RegisterSpecList.EMPTY, target.getTarget());
                        this.insns.set(i, gotoInsn);
                        this.insns.add(i, target.withNewTargetAndReversed(newTarget));
                        ++size;
                        ++i;
                    }
                    anyFixed = true;
                }
            }
        }
        return anyFixed;
    }
    
    private void align64bits(final Dop[] opcodes) {
        do {
            int notAligned64bitRegAccess = 0;
            int aligned64bitRegAccess = 0;
            int notAligned64bitParamAccess = 0;
            int aligned64bitParamAccess = 0;
            final int lastParameter = this.unreservedRegCount + this.reservedCount + this.reservedParameterCount;
            final int firstParameter = lastParameter - this.paramSize;
            for (final DalvInsn insn : this.insns) {
                final RegisterSpecList regs = insn.getRegisters();
                for (int usedRegIdx = 0; usedRegIdx < regs.size(); ++usedRegIdx) {
                    final RegisterSpec reg = regs.get(usedRegIdx);
                    if (reg.isCategory2()) {
                        final boolean isParameter = reg.getReg() >= firstParameter;
                        if (reg.isEvenRegister()) {
                            if (isParameter) {
                                ++aligned64bitParamAccess;
                            }
                            else {
                                ++aligned64bitRegAccess;
                            }
                        }
                        else if (isParameter) {
                            ++notAligned64bitParamAccess;
                        }
                        else {
                            ++notAligned64bitRegAccess;
                        }
                    }
                }
            }
            if (notAligned64bitParamAccess > aligned64bitParamAccess && notAligned64bitRegAccess > aligned64bitRegAccess) {
                this.addReservedRegisters(1);
            }
            else if (notAligned64bitParamAccess > aligned64bitParamAccess) {
                this.addReservedParameters(1);
            }
            else {
                if (notAligned64bitRegAccess <= aligned64bitRegAccess) {
                    break;
                }
                this.addReservedRegisters(1);
                if (this.paramSize != 0 && aligned64bitParamAccess > notAligned64bitParamAccess) {
                    this.addReservedParameters(1);
                }
            }
        } while (this.reserveRegisters(opcodes));
    }
    
    private void addReservedParameters(final int delta) {
        this.shiftParameters(delta);
        this.reservedParameterCount += delta;
    }
    
    private void addReservedRegisters(final int delta) {
        this.shiftAllRegisters(delta);
        this.reservedCount += delta;
    }
    
    private void shiftAllRegisters(final int delta) {
        for (int insnSize = this.insns.size(), i = 0; i < insnSize; ++i) {
            final DalvInsn insn = this.insns.get(i);
            if (!(insn instanceof CodeAddress)) {
                this.insns.set(i, insn.withRegisterOffset(delta));
            }
        }
    }
    
    private void shiftParameters(final int delta) {
        final int insnSize = this.insns.size();
        final int lastParameter = this.unreservedRegCount + this.reservedCount + this.reservedParameterCount;
        final int firstParameter = lastParameter - this.paramSize;
        final BasicRegisterMapper mapper = new BasicRegisterMapper(lastParameter);
        for (int i = 0; i < lastParameter; ++i) {
            if (i >= firstParameter) {
                mapper.addMapping(i, i + delta, 1);
            }
            else {
                mapper.addMapping(i, i, 1);
            }
        }
        for (int i = 0; i < insnSize; ++i) {
            final DalvInsn insn = this.insns.get(i);
            if (!(insn instanceof CodeAddress)) {
                this.insns.set(i, insn.withMapper(mapper));
            }
        }
    }
}

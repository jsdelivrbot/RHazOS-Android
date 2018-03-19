package embedded.com.android.dx.ssa.back;

import java.util.*;
import embedded.com.android.dx.rop.cst.*;
import embedded.com.android.dx.ssa.*;
import embedded.com.android.dx.rop.code.*;
import embedded.com.android.dx.util.*;

public class FirstFitLocalCombiningAllocator extends RegisterAllocator
{
    private static final boolean DEBUG = false;
    private final Map<LocalItem, ArrayList<RegisterSpec>> localVariables;
    private final ArrayList<NormalSsaInsn> moveResultPseudoInsns;
    private final ArrayList<NormalSsaInsn> invokeRangeInsns;
    private final ArrayList<PhiInsn> phiInsns;
    private final BitSet ssaRegsMapped;
    private final InterferenceRegisterMapper mapper;
    private final int paramRangeEnd;
    private final BitSet reservedRopRegs;
    private final BitSet usedRopRegs;
    private final boolean minimizeRegisters;
    
    public FirstFitLocalCombiningAllocator(final SsaMethod ssaMeth, final InterferenceGraph interference, final boolean minimizeRegisters) {
        super(ssaMeth, interference);
        this.ssaRegsMapped = new BitSet(ssaMeth.getRegCount());
        this.mapper = new InterferenceRegisterMapper(interference, ssaMeth.getRegCount());
        this.minimizeRegisters = minimizeRegisters;
        this.paramRangeEnd = ssaMeth.getParamWidth();
        (this.reservedRopRegs = new BitSet(this.paramRangeEnd * 2)).set(0, this.paramRangeEnd);
        this.usedRopRegs = new BitSet(this.paramRangeEnd * 2);
        this.localVariables = new TreeMap<LocalItem, ArrayList<RegisterSpec>>();
        this.moveResultPseudoInsns = new ArrayList<NormalSsaInsn>();
        this.invokeRangeInsns = new ArrayList<NormalSsaInsn>();
        this.phiInsns = new ArrayList<PhiInsn>();
    }
    
    @Override
    public boolean wantsParamsMovedHigh() {
        return true;
    }
    
    @Override
    public RegisterMapper allocateRegisters() {
        this.analyzeInstructions();
        this.handleLocalAssociatedParams();
        this.handleUnassociatedParameters();
        this.handleInvokeRangeInsns();
        this.handleLocalAssociatedOther();
        this.handleCheckCastResults();
        this.handlePhiInsns();
        this.handleNormalUnassociated();
        return this.mapper;
    }
    
    private void printLocalVars() {
        System.out.println("Printing local vars");
        for (final Map.Entry<LocalItem, ArrayList<RegisterSpec>> e : this.localVariables.entrySet()) {
            final StringBuilder regs = new StringBuilder();
            regs.append('{');
            regs.append(' ');
            for (final RegisterSpec reg : e.getValue()) {
                regs.append('v');
                regs.append(reg.getReg());
                regs.append(' ');
            }
            regs.append('}');
            System.out.printf("Local: %s Registers: %s\n", e.getKey(), regs);
        }
    }
    
    private void handleLocalAssociatedParams() {
        for (final ArrayList<RegisterSpec> ssaRegs : this.localVariables.values()) {
            final int sz = ssaRegs.size();
            int paramIndex = -1;
            int paramCategory = 0;
            for (int i = 0; i < sz; ++i) {
                final RegisterSpec ssaSpec = ssaRegs.get(i);
                final int ssaReg = ssaSpec.getReg();
                paramIndex = this.getParameterIndexForReg(ssaReg);
                if (paramIndex >= 0) {
                    paramCategory = ssaSpec.getCategory();
                    this.addMapping(ssaSpec, paramIndex);
                    break;
                }
            }
            if (paramIndex < 0) {
                continue;
            }
            this.tryMapRegs(ssaRegs, paramIndex, paramCategory, true);
        }
    }
    
    private int getParameterIndexForReg(final int ssaReg) {
        final SsaInsn defInsn = this.ssaMeth.getDefinitionForRegister(ssaReg);
        if (defInsn == null) {
            return -1;
        }
        final Rop opcode = defInsn.getOpcode();
        if (opcode != null && opcode.getOpcode() == 3) {
            final CstInsn origInsn = (CstInsn)defInsn.getOriginalRopInsn();
            return ((CstInteger)origInsn.getConstant()).getValue();
        }
        return -1;
    }
    
    private void handleLocalAssociatedOther() {
        for (final ArrayList<RegisterSpec> specs : this.localVariables.values()) {
            int ropReg = this.paramRangeEnd;
            boolean done = false;
            do {
                int maxCategory = 1;
                for (int sz = specs.size(), i = 0; i < sz; ++i) {
                    final RegisterSpec ssaSpec = specs.get(i);
                    final int category = ssaSpec.getCategory();
                    if (!this.ssaRegsMapped.get(ssaSpec.getReg()) && category > maxCategory) {
                        maxCategory = category;
                    }
                }
                ropReg = this.findRopRegForLocal(ropReg, maxCategory);
                if (this.canMapRegs(specs, ropReg)) {
                    done = this.tryMapRegs(specs, ropReg, maxCategory, true);
                }
                ++ropReg;
            } while (!done);
        }
    }
    
    private boolean tryMapRegs(final ArrayList<RegisterSpec> specs, final int ropReg, final int maxAllowedCategory, final boolean markReserved) {
        boolean remaining = false;
        for (final RegisterSpec spec : specs) {
            if (this.ssaRegsMapped.get(spec.getReg())) {
                continue;
            }
            final boolean succeeded = this.tryMapReg(spec, ropReg, maxAllowedCategory);
            remaining = (!succeeded || remaining);
            if (!succeeded || !markReserved) {
                continue;
            }
            this.markReserved(ropReg, spec.getCategory());
        }
        return !remaining;
    }
    
    private boolean tryMapReg(final RegisterSpec ssaSpec, final int ropReg, final int maxAllowedCategory) {
        if (ssaSpec.getCategory() <= maxAllowedCategory && !this.ssaRegsMapped.get(ssaSpec.getReg()) && this.canMapReg(ssaSpec, ropReg)) {
            this.addMapping(ssaSpec, ropReg);
            return true;
        }
        return false;
    }
    
    private void markReserved(final int ropReg, final int category) {
        this.reservedRopRegs.set(ropReg, ropReg + category, true);
    }
    
    private boolean rangeContainsReserved(final int ropRangeStart, final int width) {
        for (int i = ropRangeStart; i < ropRangeStart + width; ++i) {
            if (this.reservedRopRegs.get(i)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean isThisPointerReg(final int startReg) {
        return startReg == 0 && !this.ssaMeth.isStatic();
    }
    
    private Alignment getAlignment(final int regCategory) {
        Alignment alignment = Alignment.UNSPECIFIED;
        if (regCategory == 2) {
            if (isEven(this.paramRangeEnd)) {
                alignment = Alignment.EVEN;
            }
            else {
                alignment = Alignment.ODD;
            }
        }
        return alignment;
    }
    
    private int findNextUnreservedRopReg(final int startReg, final int regCategory) {
        return this.findNextUnreservedRopReg(startReg, regCategory, this.getAlignment(regCategory));
    }
    
    private int findNextUnreservedRopReg(final int startReg, final int width, final Alignment alignment) {
        int reg = alignment.nextClearBit(this.reservedRopRegs, startReg);
        while (true) {
            int i;
            for (i = 1; i < width && !this.reservedRopRegs.get(reg + i); ++i) {}
            if (i == width) {
                break;
            }
            reg = alignment.nextClearBit(this.reservedRopRegs, reg + i);
        }
        return reg;
    }
    
    private int findRopRegForLocal(final int startReg, final int category) {
        final Alignment alignment = this.getAlignment(category);
        int reg = alignment.nextClearBit(this.usedRopRegs, startReg);
        while (true) {
            int i;
            for (i = 1; i < category && !this.usedRopRegs.get(reg + i); ++i) {}
            if (i == category) {
                break;
            }
            reg = alignment.nextClearBit(this.usedRopRegs, reg + i);
        }
        return reg;
    }
    
    private void handleUnassociatedParameters() {
        for (int szSsaRegs = this.ssaMeth.getRegCount(), ssaReg = 0; ssaReg < szSsaRegs; ++ssaReg) {
            if (!this.ssaRegsMapped.get(ssaReg)) {
                final int paramIndex = this.getParameterIndexForReg(ssaReg);
                final RegisterSpec ssaSpec = this.getDefinitionSpecForSsaReg(ssaReg);
                if (paramIndex >= 0) {
                    this.addMapping(ssaSpec, paramIndex);
                }
            }
        }
    }
    
    private void handleInvokeRangeInsns() {
        for (final NormalSsaInsn insn : this.invokeRangeInsns) {
            this.adjustAndMapSourceRangeRange(insn);
        }
    }
    
    private void handleCheckCastResults() {
        for (final NormalSsaInsn insn : this.moveResultPseudoInsns) {
            final RegisterSpec moveRegSpec = insn.getResult();
            final int moveReg = moveRegSpec.getReg();
            final BitSet predBlocks = insn.getBlock().getPredecessors();
            if (predBlocks.cardinality() != 1) {
                continue;
            }
            final SsaBasicBlock predBlock = this.ssaMeth.getBlocks().get(predBlocks.nextSetBit(0));
            final ArrayList<SsaInsn> insnList = predBlock.getInsns();
            final SsaInsn checkCastInsn = insnList.get(insnList.size() - 1);
            if (checkCastInsn.getOpcode().getOpcode() != 43) {
                continue;
            }
            final RegisterSpec checkRegSpec = checkCastInsn.getSources().get(0);
            final int checkReg = checkRegSpec.getReg();
            final int category = checkRegSpec.getCategory();
            boolean moveMapped = this.ssaRegsMapped.get(moveReg);
            boolean checkMapped = this.ssaRegsMapped.get(checkReg);
            if (moveMapped & !checkMapped) {
                final int moveRopReg = this.mapper.oldToNew(moveReg);
                checkMapped = this.tryMapReg(checkRegSpec, moveRopReg, category);
            }
            if (checkMapped & !moveMapped) {
                final int checkRopReg = this.mapper.oldToNew(checkReg);
                moveMapped = this.tryMapReg(moveRegSpec, checkRopReg, category);
            }
            if (!moveMapped || !checkMapped) {
                int ropReg = this.findNextUnreservedRopReg(this.paramRangeEnd, category);
                final ArrayList<RegisterSpec> ssaRegs = new ArrayList<RegisterSpec>(2);
                ssaRegs.add(moveRegSpec);
                ssaRegs.add(checkRegSpec);
                while (!this.tryMapRegs(ssaRegs, ropReg, category, false)) {
                    ropReg = this.findNextUnreservedRopReg(ropReg + 1, category);
                }
            }
            final boolean hasExceptionHandlers = checkCastInsn.getOriginalRopInsn().getCatches().size() != 0;
            final int moveRopReg2 = this.mapper.oldToNew(moveReg);
            final int checkRopReg2 = this.mapper.oldToNew(checkReg);
            if (moveRopReg2 == checkRopReg2 || hasExceptionHandlers) {
                continue;
            }
            ((NormalSsaInsn)checkCastInsn).changeOneSource(0, this.insertMoveBefore(checkCastInsn, checkRegSpec));
            this.addMapping(checkCastInsn.getSources().get(0), moveRopReg2);
        }
    }
    
    private void handlePhiInsns() {
        for (final PhiInsn insn : this.phiInsns) {
            this.processPhiInsn(insn);
        }
    }
    
    private void handleNormalUnassociated() {
        for (int szSsaRegs = this.ssaMeth.getRegCount(), ssaReg = 0; ssaReg < szSsaRegs; ++ssaReg) {
            if (!this.ssaRegsMapped.get(ssaReg)) {
                final RegisterSpec ssaSpec = this.getDefinitionSpecForSsaReg(ssaReg);
                if (ssaSpec != null) {
                    int category;
                    int ropReg;
                    for (category = ssaSpec.getCategory(), ropReg = this.findNextUnreservedRopReg(this.paramRangeEnd, category); !this.canMapReg(ssaSpec, ropReg); ropReg = this.findNextUnreservedRopReg(ropReg + 1, category)) {}
                    this.addMapping(ssaSpec, ropReg);
                }
            }
        }
    }
    
    private boolean canMapRegs(final ArrayList<RegisterSpec> specs, final int ropReg) {
        for (final RegisterSpec spec : specs) {
            if (this.ssaRegsMapped.get(spec.getReg())) {
                continue;
            }
            if (!this.canMapReg(spec, ropReg)) {
                return false;
            }
        }
        return true;
    }
    
    private boolean canMapReg(final RegisterSpec ssaSpec, final int ropReg) {
        final int category = ssaSpec.getCategory();
        return !this.spansParamRange(ropReg, category) && !this.mapper.interferes(ssaSpec, ropReg);
    }
    
    private boolean spansParamRange(final int ssaReg, final int category) {
        return ssaReg < this.paramRangeEnd && ssaReg + category > this.paramRangeEnd;
    }
    
    private void analyzeInstructions() {
        this.ssaMeth.forEachInsn(new SsaInsn.Visitor() {
            @Override
            public void visitMoveInsn(final NormalSsaInsn insn) {
                this.processInsn(insn);
            }
            
            @Override
            public void visitPhiInsn(final PhiInsn insn) {
                this.processInsn(insn);
            }
            
            @Override
            public void visitNonMoveInsn(final NormalSsaInsn insn) {
                this.processInsn(insn);
            }
            
            private void processInsn(final SsaInsn insn) {
                final RegisterSpec assignment = insn.getLocalAssignment();
                if (assignment != null) {
                    final LocalItem local = assignment.getLocalItem();
                    ArrayList<RegisterSpec> regList = FirstFitLocalCombiningAllocator.this.localVariables.get(local);
                    if (regList == null) {
                        regList = new ArrayList<RegisterSpec>();
                        FirstFitLocalCombiningAllocator.this.localVariables.put(local, regList);
                    }
                    regList.add(assignment);
                }
                if (insn instanceof NormalSsaInsn) {
                    if (insn.getOpcode().getOpcode() == 56) {
                        FirstFitLocalCombiningAllocator.this.moveResultPseudoInsns.add((NormalSsaInsn) insn);
                    }
                    else if (Optimizer.getAdvice().requiresSourcesInOrder(insn.getOriginalRopInsn().getOpcode(), insn.getSources())) {
                        FirstFitLocalCombiningAllocator.this.invokeRangeInsns.add((NormalSsaInsn)  insn);
                    }
                }
                else if (insn instanceof PhiInsn) {
                    FirstFitLocalCombiningAllocator.this.phiInsns.add((PhiInsn) insn);
                }
            }
        });
    }
    
    private void addMapping(final RegisterSpec ssaSpec, final int ropReg) {
        final int ssaReg = ssaSpec.getReg();
        if (this.ssaRegsMapped.get(ssaReg) || !this.canMapReg(ssaSpec, ropReg)) {
            throw new RuntimeException("attempt to add invalid register mapping");
        }
        final int category = ssaSpec.getCategory();
        this.mapper.addMapping(ssaSpec.getReg(), ropReg, category);
        this.ssaRegsMapped.set(ssaReg);
        this.usedRopRegs.set(ropReg, ropReg + category);
    }
    
    private void adjustAndMapSourceRangeRange(final NormalSsaInsn insn) {
        final int newRegStart = this.findRangeAndAdjust(insn);
        final RegisterSpecList sources = insn.getSources();
        final int szSources = sources.size();
        int nextRopReg = newRegStart;
        for (int i = 0; i < szSources; ++i) {
            final RegisterSpec source = sources.get(i);
            final int sourceReg = source.getReg();
            final int category = source.getCategory();
            final int curRopReg = nextRopReg;
            nextRopReg += category;
            if (!this.ssaRegsMapped.get(sourceReg)) {
                final LocalItem localItem = this.getLocalItemForReg(sourceReg);
                this.addMapping(source, curRopReg);
                if (localItem != null) {
                    this.markReserved(curRopReg, category);
                    final ArrayList<RegisterSpec> similarRegisters = this.localVariables.get(localItem);
                    for (int szSimilar = similarRegisters.size(), j = 0; j < szSimilar; ++j) {
                        final RegisterSpec similarSpec = similarRegisters.get(j);
                        final int similarReg = similarSpec.getReg();
                        if (-1 == sources.indexOfRegister(similarReg)) {
                            this.tryMapReg(similarSpec, curRopReg, category);
                        }
                    }
                }
            }
        }
    }
    
    private int findRangeAndAdjust(final NormalSsaInsn insn) {
        final RegisterSpecList sources = insn.getSources();
        final int szSources = sources.size();
        final int[] categoriesForIndex = new int[szSources];
        int rangeLength = 0;
        for (int i = 0; i < szSources; ++i) {
            final int category = sources.get(i).getCategory();
            categoriesForIndex[i] = category;
            rangeLength += categoriesForIndex[i];
        }
        int maxScore = Integer.MIN_VALUE;
        int resultRangeStart = -1;
        BitSet resultMovesRequired = null;
        int rangeStartOffset = 0;
        for (int j = 0; j < szSources; ++j) {
            final int ssaCenterReg = sources.get(j).getReg();
            if (j != 0) {
                rangeStartOffset -= categoriesForIndex[j - 1];
            }
            if (this.ssaRegsMapped.get(ssaCenterReg)) {
                final int rangeStart = this.mapper.oldToNew(ssaCenterReg) + rangeStartOffset;
                if (rangeStart >= 0) {
                    if (!this.spansParamRange(rangeStart, rangeLength)) {
                        final BitSet curMovesRequired = new BitSet(szSources);
                        final int fitWidth = this.fitPlanForRange(rangeStart, insn, categoriesForIndex, curMovesRequired);
                        if (fitWidth >= 0) {
                            final int score = fitWidth - curMovesRequired.cardinality();
                            if (score > maxScore) {
                                maxScore = score;
                                resultRangeStart = rangeStart;
                                resultMovesRequired = curMovesRequired;
                            }
                            if (fitWidth == rangeLength) {
                                break;
                            }
                        }
                    }
                }
            }
        }
        if (resultRangeStart == -1) {
            resultMovesRequired = new BitSet(szSources);
            resultRangeStart = this.findAnyFittingRange(insn, rangeLength, categoriesForIndex, resultMovesRequired);
        }
        for (int j = resultMovesRequired.nextSetBit(0); j >= 0; j = resultMovesRequired.nextSetBit(j + 1)) {
            insn.changeOneSource(j, this.insertMoveBefore(insn, sources.get(j)));
        }
        return resultRangeStart;
    }
    
    private int findAnyFittingRange(final NormalSsaInsn insn, final int rangeLength, final int[] categoriesForIndex, final BitSet outMovesRequired) {
        Alignment alignment = Alignment.UNSPECIFIED;
        int regNumber = 0;
        int p64bitsAligned = 0;
        int p64bitsNotAligned = 0;
        for (final int category : categoriesForIndex) {
            if (category == 2) {
                if (isEven(regNumber)) {
                    ++p64bitsAligned;
                }
                else {
                    ++p64bitsNotAligned;
                }
                regNumber += 2;
            }
            else {
                ++regNumber;
            }
        }
        if (p64bitsNotAligned > p64bitsAligned) {
            if (isEven(this.paramRangeEnd)) {
                alignment = Alignment.ODD;
            }
            else {
                alignment = Alignment.EVEN;
            }
        }
        else if (p64bitsAligned > 0) {
            if (isEven(this.paramRangeEnd)) {
                alignment = Alignment.EVEN;
            }
            else {
                alignment = Alignment.ODD;
            }
        }
        int rangeStart = this.paramRangeEnd;
        while (true) {
            rangeStart = this.findNextUnreservedRopReg(rangeStart, rangeLength, alignment);
            final int fitWidth = this.fitPlanForRange(rangeStart, insn, categoriesForIndex, outMovesRequired);
            if (fitWidth >= 0) {
                break;
            }
            ++rangeStart;
            outMovesRequired.clear();
        }
        return rangeStart;
    }
    
    private int fitPlanForRange(int ropReg, final NormalSsaInsn insn, final int[] categoriesForIndex, final BitSet outMovesRequired) {
        final RegisterSpecList sources = insn.getSources();
        final int szSources = sources.size();
        int fitWidth = 0;
        final IntSet liveOut = insn.getBlock().getLiveOutRegs();
        final RegisterSpecList liveOutSpecs = this.ssaSetToSpecs(liveOut);
        final BitSet seen = new BitSet(this.ssaMeth.getRegCount());
        for (int i = 0; i < szSources; ++i) {
            final RegisterSpec ssaSpec = sources.get(i);
            final int ssaReg = ssaSpec.getReg();
            final int category = categoriesForIndex[i];
            if (i != 0) {
                ropReg += categoriesForIndex[i - 1];
            }
            if (this.ssaRegsMapped.get(ssaReg) && this.mapper.oldToNew(ssaReg) == ropReg) {
                fitWidth += category;
            }
            else {
                if (this.rangeContainsReserved(ropReg, category)) {
                    fitWidth = -1;
                    break;
                }
                if (!this.ssaRegsMapped.get(ssaReg) && this.canMapReg(ssaSpec, ropReg) && !seen.get(ssaReg)) {
                    fitWidth += category;
                }
                else {
                    if (this.mapper.areAnyPinned(liveOutSpecs, ropReg, category) || this.mapper.areAnyPinned(sources, ropReg, category)) {
                        fitWidth = -1;
                        break;
                    }
                    outMovesRequired.set(i);
                }
            }
            seen.set(ssaReg);
        }
        return fitWidth;
    }
    
    RegisterSpecList ssaSetToSpecs(final IntSet ssaSet) {
        final RegisterSpecList result = new RegisterSpecList(ssaSet.elements());
        final IntIterator iter = ssaSet.iterator();
        int i = 0;
        while (iter.hasNext()) {
            result.set(i++, this.getDefinitionSpecForSsaReg(iter.next()));
        }
        return result;
    }
    
    private LocalItem getLocalItemForReg(final int ssaReg) {
        for (final Map.Entry<LocalItem, ArrayList<RegisterSpec>> entry : this.localVariables.entrySet()) {
            for (final RegisterSpec spec : entry.getValue()) {
                if (spec.getReg() == ssaReg) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }
    
    private void processPhiInsn(final PhiInsn insn) {
        final RegisterSpec result = insn.getResult();
        final int resultReg = result.getReg();
        final int category = result.getCategory();
        final RegisterSpecList sources = insn.getSources();
        final int sourcesSize = sources.size();
        final ArrayList<RegisterSpec> ssaRegs = new ArrayList<RegisterSpec>();
        final Multiset mapSet = new Multiset(sourcesSize + 1);
        if (this.ssaRegsMapped.get(resultReg)) {
            mapSet.add(this.mapper.oldToNew(resultReg));
        }
        else {
            ssaRegs.add(result);
        }
        for (int i = 0; i < sourcesSize; ++i) {
            final RegisterSpec source = sources.get(i);
            final SsaInsn def = this.ssaMeth.getDefinitionForRegister(source.getReg());
            final RegisterSpec sourceDef = def.getResult();
            final int sourceReg = sourceDef.getReg();
            if (this.ssaRegsMapped.get(sourceReg)) {
                mapSet.add(this.mapper.oldToNew(sourceReg));
            }
            else {
                ssaRegs.add(sourceDef);
            }
        }
        for (int i = 0; i < mapSet.getSize(); ++i) {
            final int maxReg = mapSet.getAndRemoveHighestCount();
            this.tryMapRegs(ssaRegs, maxReg, category, false);
        }
        for (int mapReg = this.findNextUnreservedRopReg(this.paramRangeEnd, category); !this.tryMapRegs(ssaRegs, mapReg, category, false); mapReg = this.findNextUnreservedRopReg(mapReg + 1, category)) {}
    }
    
    private static boolean isEven(final int regNumger) {
        return (regNumger & 0x1) == 0x0;
    }
    
    private enum Alignment
    {
        EVEN {
            @Override
            int nextClearBit(final BitSet bitSet, final int startIdx) {
                int bitNumber;
                for (bitNumber = bitSet.nextClearBit(startIdx); !isEven(bitNumber); bitNumber = bitSet.nextClearBit(bitNumber + 1)) {}
                return bitNumber;
            }
        }, 
        ODD {
            @Override
            int nextClearBit(final BitSet bitSet, final int startIdx) {
                int bitNumber;
                for (bitNumber = bitSet.nextClearBit(startIdx); isEven(bitNumber); bitNumber = bitSet.nextClearBit(bitNumber + 1)) {}
                return bitNumber;
            }
        }, 
        UNSPECIFIED {
            @Override
            int nextClearBit(final BitSet bitSet, final int startIdx) {
                return bitSet.nextClearBit(startIdx);
            }
        };
        
        abstract int nextClearBit(final BitSet p0, final int p1);
    }
    
    private static class Multiset
    {
        private final int[] reg;
        private final int[] count;
        private int size;
        
        public Multiset(final int maxSize) {
            this.reg = new int[maxSize];
            this.count = new int[maxSize];
            this.size = 0;
        }
        
        public void add(final int element) {
            for (int i = 0; i < this.size; ++i) {
                if (this.reg[i] == element) {
                    final int[] count = this.count;
                    final int n = i;
                    ++count[n];
                    return;
                }
            }
            this.reg[this.size] = element;
            this.count[this.size] = 1;
            ++this.size;
        }
        
        public int getAndRemoveHighestCount() {
            int maxIndex = -1;
            int maxReg = -1;
            int maxCount = 0;
            for (int i = 0; i < this.size; ++i) {
                if (maxCount < this.count[i]) {
                    maxIndex = i;
                    maxReg = this.reg[i];
                    maxCount = this.count[i];
                }
            }
            this.count[maxIndex] = 0;
            return maxReg;
        }
        
        public int getSize() {
            return this.size;
        }
    }
}

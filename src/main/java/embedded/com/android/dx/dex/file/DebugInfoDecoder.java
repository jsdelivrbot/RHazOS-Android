package embedded.com.android.dx.dex.file;

import embedded.com.android.dx.rop.cst.*;
import embedded.com.android.dex.*;
import embedded.com.android.dex.util.*;
import embedded.com.android.dx.rop.type.*;
import java.io.*;
import embedded.com.android.dx.dex.code.*;
import java.util.*;

public class DebugInfoDecoder
{
    private final byte[] encoded;
    private final ArrayList<PositionEntry> positions;
    private final ArrayList<LocalEntry> locals;
    private final int codesize;
    private final LocalEntry[] lastEntryForReg;
    private final Prototype desc;
    private final boolean isStatic;
    private final DexFile file;
    private final int regSize;
    private int line;
    private int address;
    private final int thisStringIdx;
    
    DebugInfoDecoder(final byte[] encoded, final int codesize, final int regSize, final boolean isStatic, final CstMethodRef ref, final DexFile file) {
        this.line = 1;
        this.address = 0;
        if (encoded == null) {
            throw new NullPointerException("encoded == null");
        }
        this.encoded = encoded;
        this.isStatic = isStatic;
        this.desc = ref.getPrototype();
        this.file = file;
        this.regSize = regSize;
        this.positions = new ArrayList<PositionEntry>();
        this.locals = new ArrayList<LocalEntry>();
        this.codesize = codesize;
        this.lastEntryForReg = new LocalEntry[regSize];
        int idx = -1;
        try {
            idx = file.getStringIds().indexOf(new CstString("this"));
        }
        catch (IllegalArgumentException ex) {}
        this.thisStringIdx = idx;
    }
    
    public List<PositionEntry> getPositionList() {
        return this.positions;
    }
    
    public List<LocalEntry> getLocals() {
        return this.locals;
    }
    
    public void decode() {
        try {
            this.decode0();
        }
        catch (Exception ex) {
            throw ExceptionWithContext.withContext(ex, "...while decoding debug info");
        }
    }
    
    private int readStringIndex(final ByteInput bs) throws IOException {
        final int offsetIndex = Leb128.readUnsignedLeb128(bs);
        return offsetIndex - 1;
    }
    
    private int getParamBase() {
        return this.regSize - this.desc.getParameterTypes().getWordCount() - (this.isStatic ? 0 : 1);
    }
    
    private void decode0() throws IOException {
        final ByteInput bs = new ByteArrayByteInput(this.encoded);
        this.line = Leb128.readUnsignedLeb128(bs);
        final int szParams = Leb128.readUnsignedLeb128(bs);
        final StdTypeList params = this.desc.getParameterTypes();
        int curReg = this.getParamBase();
        if (szParams != params.size()) {
            throw new RuntimeException("Mismatch between parameters_size and prototype");
        }
        if (!this.isStatic) {
            final LocalEntry thisEntry = new LocalEntry(0, true, curReg, this.thisStringIdx, 0, 0);
            this.locals.add(thisEntry);
            this.lastEntryForReg[curReg] = thisEntry;
            ++curReg;
        }
        for (int i = 0; i < szParams; ++i) {
            final Type paramType = params.getType(i);
            final int nameIdx = this.readStringIndex(bs);
            LocalEntry le;
            if (nameIdx == -1) {
                le = new LocalEntry(0, true, curReg, -1, 0, 0);
            }
            else {
                le = new LocalEntry(0, true, curReg, nameIdx, 0, 0);
            }
            this.locals.add(le);
            this.lastEntryForReg[curReg] = le;
            curReg += paramType.getCategory();
        }
        while (true) {
            final int opcode = bs.readByte() & 0xFF;
            switch (opcode) {
                case 3: {
                    final int reg = Leb128.readUnsignedLeb128(bs);
                    final int nameIdx2 = this.readStringIndex(bs);
                    final int typeIdx = this.readStringIndex(bs);
                    final LocalEntry le2 = new LocalEntry(this.address, true, reg, nameIdx2, typeIdx, 0);
                    this.locals.add(le2);
                    this.lastEntryForReg[reg] = le2;
                    continue;
                }
                case 4: {
                    final int reg = Leb128.readUnsignedLeb128(bs);
                    final int nameIdx2 = this.readStringIndex(bs);
                    final int typeIdx = this.readStringIndex(bs);
                    final int sigIdx = this.readStringIndex(bs);
                    final LocalEntry le3 = new LocalEntry(this.address, true, reg, nameIdx2, typeIdx, sigIdx);
                    this.locals.add(le3);
                    this.lastEntryForReg[reg] = le3;
                    continue;
                }
                case 6: {
                    final int reg = Leb128.readUnsignedLeb128(bs);
                    LocalEntry le4;
                    try {
                        final LocalEntry prevle = this.lastEntryForReg[reg];
                        if (prevle.isStart) {
                            throw new RuntimeException("nonsensical RESTART_LOCAL on live register v" + reg);
                        }
                        le4 = new LocalEntry(this.address, true, reg, prevle.nameIndex, prevle.typeIndex, 0);
                    }
                    catch (NullPointerException ex) {
                        throw new RuntimeException("Encountered RESTART_LOCAL on new v" + reg);
                    }
                    this.locals.add(le4);
                    this.lastEntryForReg[reg] = le4;
                    continue;
                }
                case 5: {
                    final int reg = Leb128.readUnsignedLeb128(bs);
                    LocalEntry le4;
                    try {
                        final LocalEntry prevle = this.lastEntryForReg[reg];
                        if (!prevle.isStart) {
                            throw new RuntimeException("nonsensical END_LOCAL on dead register v" + reg);
                        }
                        le4 = new LocalEntry(this.address, false, reg, prevle.nameIndex, prevle.typeIndex, prevle.signatureIndex);
                    }
                    catch (NullPointerException ex) {
                        throw new RuntimeException("Encountered END_LOCAL on new v" + reg);
                    }
                    this.locals.add(le4);
                    this.lastEntryForReg[reg] = le4;
                    continue;
                }
                case 0: {}
                case 1: {
                    this.address += Leb128.readUnsignedLeb128(bs);
                    continue;
                }
                case 2: {
                    this.line += Leb128.readSignedLeb128(bs);
                    continue;
                }
                case 7: {
                    continue;
                }
                case 8: {
                    continue;
                }
                case 9: {
                    continue;
                }
                default: {
                    if (opcode < 10) {
                        throw new RuntimeException("Invalid extended opcode encountered " + opcode);
                    }
                    final int adjopcode = opcode - 10;
                    this.address += adjopcode / 15;
                    this.line += -4 + adjopcode % 15;
                    this.positions.add(new PositionEntry(this.address, this.line));
                    continue;
                }
            }
        }
    }
    
    public static void validateEncode(final byte[] info, final DexFile file, final CstMethodRef ref, final DalvCode code, final boolean isStatic) {
        final PositionList pl = code.getPositions();
        final LocalList ll = code.getLocals();
        final DalvInsnList insns = code.getInsns();
        final int codeSize = insns.codeSize();
        final int countRegisters = insns.getRegistersSize();
        try {
            validateEncode0(info, codeSize, countRegisters, isStatic, ref, file, pl, ll);
        }
        catch (RuntimeException ex) {
            System.err.println("instructions:");
            insns.debugPrint(System.err, "  ", true);
            System.err.println("local list:");
            ll.debugPrint(System.err, "  ");
            throw ExceptionWithContext.withContext(ex, "while processing " + ref.toHuman());
        }
    }
    
    private static void validateEncode0(final byte[] info, final int codeSize, final int countRegisters, final boolean isStatic, final CstMethodRef ref, final DexFile file, final PositionList pl, final LocalList ll) {
        final DebugInfoDecoder decoder = new DebugInfoDecoder(info, codeSize, countRegisters, isStatic, ref, file);
        decoder.decode();
        final List<PositionEntry> decodedEntries = decoder.getPositionList();
        if (decodedEntries.size() != pl.size()) {
            throw new RuntimeException("Decoded positions table not same size was " + decodedEntries.size() + " expected " + pl.size());
        }
        for (final PositionEntry entry : decodedEntries) {
            boolean found = false;
            for (int i = pl.size() - 1; i >= 0; --i) {
                final PositionList.Entry ple = pl.get(i);
                if (entry.line == ple.getPosition().getLine() && entry.address == ple.getAddress()) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new RuntimeException("Could not match position entry: " + entry.address + ", " + entry.line);
            }
        }
        final List<LocalEntry> decodedLocals = decoder.getLocals();
        final int thisStringIdx = decoder.thisStringIdx;
        int decodedSz = decodedLocals.size();
        final int paramBase = decoder.getParamBase();
        for (int j = 0; j < decodedSz; ++j) {
            final LocalEntry entry2 = decodedLocals.get(j);
            final int idx = entry2.nameIndex;
            if (idx < 0 || idx == thisStringIdx) {
                for (int k = j + 1; k < decodedSz; ++k) {
                    final LocalEntry e2 = decodedLocals.get(k);
                    if (e2.address != 0) {
                        break;
                    }
                    if (entry2.reg == e2.reg && e2.isStart) {
                        decodedLocals.set(j, e2);
                        decodedLocals.remove(k);
                        --decodedSz;
                        break;
                    }
                }
            }
        }
        final int origSz = ll.size();
        int decodeAt = 0;
        boolean problem = false;
        for (int l = 0; l < origSz; ++l) {
            final LocalList.Entry origEntry = ll.get(l);
            if (origEntry.getDisposition() != LocalList.Disposition.END_REPLACED) {
                LocalEntry decodedEntry;
                do {
                    decodedEntry = decodedLocals.get(decodeAt);
                    if (decodedEntry.nameIndex >= 0) {
                        break;
                    }
                } while (++decodeAt < decodedSz);
                final int decodedAddress = decodedEntry.address;
                if (decodedEntry.reg != origEntry.getRegister()) {
                    System.err.println("local register mismatch at orig " + l + " / decoded " + decodeAt);
                    problem = true;
                    break;
                }
                if (decodedEntry.isStart != origEntry.isStart()) {
                    System.err.println("local start/end mismatch at orig " + l + " / decoded " + decodeAt);
                    problem = true;
                    break;
                }
                if (decodedAddress != origEntry.getAddress() && (decodedAddress != 0 || decodedEntry.reg < paramBase)) {
                    System.err.println("local address mismatch at orig " + l + " / decoded " + decodeAt);
                    problem = true;
                    break;
                }
                ++decodeAt;
            }
        }
        if (problem) {
            System.err.println("decoded locals:");
            for (final LocalEntry e3 : decodedLocals) {
                System.err.println("  " + e3);
            }
            throw new RuntimeException("local table problem");
        }
    }
    
    private static class PositionEntry
    {
        public int address;
        public int line;
        
        public PositionEntry(final int address, final int line) {
            this.address = address;
            this.line = line;
        }
    }
    
    private static class LocalEntry
    {
        public int address;
        public boolean isStart;
        public int reg;
        public int nameIndex;
        public int typeIndex;
        public int signatureIndex;
        
        public LocalEntry(final int address, final boolean isStart, final int reg, final int nameIndex, final int typeIndex, final int signatureIndex) {
            this.address = address;
            this.isStart = isStart;
            this.reg = reg;
            this.nameIndex = nameIndex;
            this.typeIndex = typeIndex;
            this.signatureIndex = signatureIndex;
        }
        
        @Override
        public String toString() {
            return String.format("[%x %s v%d %04x %04x %04x]", this.address, this.isStart ? "start" : "end", this.reg, this.nameIndex, this.typeIndex, this.signatureIndex);
        }
    }
}

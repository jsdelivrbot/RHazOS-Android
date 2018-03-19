package embedded.com.android.dx.dex.file;

import embedded.com.android.dx.dex.code.*;
import embedded.com.android.dx.util.*;
import embedded.com.android.dex.util.*;
import java.io.*;
import embedded.com.android.dx.rop.type.*;
import java.util.*;
import embedded.com.android.dx.rop.cst.*;
import embedded.com.android.dx.rop.code.*;

public final class DebugInfoEncoder
{
    private static final boolean DEBUG = false;
    private final PositionList positions;
    private final LocalList locals;
    private final ByteArrayAnnotatedOutput output;
    private final DexFile file;
    private final int codeSize;
    private final int regSize;
    private final Prototype desc;
    private final boolean isStatic;
    private int address;
    private int line;
    private AnnotatedOutput annotateTo;
    private PrintWriter debugPrint;
    private String prefix;
    private boolean shouldConsume;
    private final LocalList.Entry[] lastEntryForReg;
    
    public DebugInfoEncoder(final PositionList positions, final LocalList locals, final DexFile file, final int codeSize, final int regSize, final boolean isStatic, final CstMethodRef ref) {
        this.address = 0;
        this.line = 1;
        this.positions = positions;
        this.locals = locals;
        this.file = file;
        this.desc = ref.getPrototype();
        this.isStatic = isStatic;
        this.codeSize = codeSize;
        this.regSize = regSize;
        this.output = new ByteArrayAnnotatedOutput();
        this.lastEntryForReg = new LocalList.Entry[regSize];
    }
    
    private void annotate(final int length, String message) {
        if (this.prefix != null) {
            message = this.prefix + message;
        }
        if (this.annotateTo != null) {
            this.annotateTo.annotate(this.shouldConsume ? length : 0, message);
        }
        if (this.debugPrint != null) {
            this.debugPrint.println(message);
        }
    }
    
    public byte[] convert() {
        try {
            final byte[] ret = this.convert0();
            return ret;
        }
        catch (IOException ex) {
            throw ExceptionWithContext.withContext(ex, "...while encoding debug info");
        }
    }
    
    public byte[] convertAndAnnotate(final String prefix, final PrintWriter debugPrint, final AnnotatedOutput out, final boolean consume) {
        this.prefix = prefix;
        this.debugPrint = debugPrint;
        this.annotateTo = out;
        this.shouldConsume = consume;
        final byte[] result = this.convert();
        return result;
    }
    
    private byte[] convert0() throws IOException {
        final ArrayList<PositionList.Entry> sortedPositions = this.buildSortedPositions();
        final ArrayList<LocalList.Entry> methodArgs = this.extractMethodArguments();
        this.emitHeader(sortedPositions, methodArgs);
        this.output.writeByte(7);
        if (this.annotateTo != null || this.debugPrint != null) {
            this.annotate(1, String.format("%04x: prologue end", this.address));
        }
        final int positionsSz = sortedPositions.size();
        final int localsSz = this.locals.size();
        int curPositionIdx = 0;
        int curLocalIdx = 0;
        while (true) {
            curLocalIdx = this.emitLocalsAtAddress(curLocalIdx);
            curPositionIdx = this.emitPositionsAtAddress(curPositionIdx, sortedPositions);
            int nextAddrL = Integer.MAX_VALUE;
            int nextAddrP = Integer.MAX_VALUE;
            if (curLocalIdx < localsSz) {
                nextAddrL = this.locals.get(curLocalIdx).getAddress();
            }
            if (curPositionIdx < positionsSz) {
                nextAddrP = sortedPositions.get(curPositionIdx).getAddress();
            }
            final int next = Math.min(nextAddrP, nextAddrL);
            if (next == Integer.MAX_VALUE) {
                break;
            }
            if (next == this.codeSize && nextAddrL == Integer.MAX_VALUE && nextAddrP == Integer.MAX_VALUE) {
                break;
            }
            if (next == nextAddrP) {
                this.emitPosition(sortedPositions.get(curPositionIdx++));
            }
            else {
                this.emitAdvancePc(next - this.address);
            }
        }
        this.emitEndSequence();
        return this.output.toByteArray();
    }
    
    private int emitLocalsAtAddress(int curLocalIdx) throws IOException {
        final int sz = this.locals.size();
        while (curLocalIdx < sz && this.locals.get(curLocalIdx).getAddress() == this.address) {
            final LocalList.Entry entry = this.locals.get(curLocalIdx++);
            final int reg = entry.getRegister();
            final LocalList.Entry prevEntry = this.lastEntryForReg[reg];
            if (entry == prevEntry) {
                continue;
            }
            this.lastEntryForReg[reg] = entry;
            if (entry.isStart()) {
                if (prevEntry != null && entry.matches(prevEntry)) {
                    if (prevEntry.isStart()) {
                        throw new RuntimeException("shouldn't happen");
                    }
                    this.emitLocalRestart(entry);
                }
                else {
                    this.emitLocalStart(entry);
                }
            }
            else {
                if (entry.getDisposition() == LocalList.Disposition.END_REPLACED) {
                    continue;
                }
                this.emitLocalEnd(entry);
            }
        }
        return curLocalIdx;
    }
    
    private int emitPositionsAtAddress(int curPositionIdx, final ArrayList<PositionList.Entry> sortedPositions) throws IOException {
        final int positionsSz = sortedPositions.size();
        while (curPositionIdx < positionsSz && sortedPositions.get(curPositionIdx).getAddress() == this.address) {
            this.emitPosition(sortedPositions.get(curPositionIdx++));
        }
        return curPositionIdx;
    }
    
    private void emitHeader(final ArrayList<PositionList.Entry> sortedPositions, final ArrayList<LocalList.Entry> methodArgs) throws IOException {
        final boolean annotate = this.annotateTo != null || this.debugPrint != null;
        int mark = this.output.getCursor();
        if (sortedPositions.size() > 0) {
            final PositionList.Entry entry = sortedPositions.get(0);
            this.line = entry.getPosition().getLine();
        }
        this.output.writeUleb128(this.line);
        if (annotate) {
            this.annotate(this.output.getCursor() - mark, "line_start: " + this.line);
        }
        int curParam = this.getParamBase();
        final StdTypeList paramTypes = this.desc.getParameterTypes();
        final int szParamTypes = paramTypes.size();
        if (!this.isStatic) {
            for (final LocalList.Entry arg : methodArgs) {
                if (curParam == arg.getRegister()) {
                    this.lastEntryForReg[curParam] = arg;
                    break;
                }
            }
            ++curParam;
        }
        mark = this.output.getCursor();
        this.output.writeUleb128(szParamTypes);
        if (annotate) {
            this.annotate(this.output.getCursor() - mark, String.format("parameters_size: %04x", szParamTypes));
        }
        for (int i = 0; i < szParamTypes; ++i) {
            final Type pt = paramTypes.get(i);
            LocalList.Entry found = null;
            mark = this.output.getCursor();
            for (final LocalList.Entry arg2 : methodArgs) {
                if (curParam == arg2.getRegister()) {
                    found = arg2;
                    if (arg2.getSignature() != null) {
                        this.emitStringIndex(null);
                    }
                    else {
                        this.emitStringIndex(arg2.getName());
                    }
                    this.lastEntryForReg[curParam] = arg2;
                    break;
                }
            }
            if (found == null) {
                this.emitStringIndex(null);
            }
            if (annotate) {
                final String parameterName = (found == null || found.getSignature() != null) ? "<unnamed>" : found.getName().toHuman();
                this.annotate(this.output.getCursor() - mark, "parameter " + parameterName + " " + "v" + curParam);
            }
            curParam += pt.getCategory();
        }
        for (final LocalList.Entry arg3 : this.lastEntryForReg) {
            if (arg3 != null) {
                final CstString signature = arg3.getSignature();
                if (signature != null) {
                    this.emitLocalStartExtended(arg3);
                }
            }
        }
    }
    
    private ArrayList<PositionList.Entry> buildSortedPositions() {
        final int sz = (this.positions == null) ? 0 : this.positions.size();
        final ArrayList<PositionList.Entry> result = new ArrayList<PositionList.Entry>(sz);
        for (int i = 0; i < sz; ++i) {
            result.add(this.positions.get(i));
        }
        Collections.sort(result, new Comparator<PositionList.Entry>() {
            @Override
            public int compare(final PositionList.Entry a, final PositionList.Entry b) {
                return a.getAddress() - b.getAddress();
            }
            
            @Override
            public boolean equals(final Object obj) {
                return obj == this;
            }
        });
        return result;
    }
    
    private int getParamBase() {
        return this.regSize - this.desc.getParameterTypes().getWordCount() - (this.isStatic ? 0 : 1);
    }
    
    private ArrayList<LocalList.Entry> extractMethodArguments() {
        final ArrayList<LocalList.Entry> result = new ArrayList<LocalList.Entry>(this.desc.getParameterTypes().size());
        final int argBase = this.getParamBase();
        final BitSet seen = new BitSet(this.regSize - argBase);
        for (int sz = this.locals.size(), i = 0; i < sz; ++i) {
            final LocalList.Entry e = this.locals.get(i);
            final int reg = e.getRegister();
            if (reg >= argBase) {
                if (!seen.get(reg - argBase)) {
                    seen.set(reg - argBase);
                    result.add(e);
                }
            }
        }
        Collections.sort(result, new Comparator<LocalList.Entry>() {
            @Override
            public int compare(final LocalList.Entry a, final LocalList.Entry b) {
                return a.getRegister() - b.getRegister();
            }
            
            @Override
            public boolean equals(final Object obj) {
                return obj == this;
            }
        });
        return result;
    }
    
    private String entryAnnotationString(final LocalList.Entry e) {
        final StringBuilder sb = new StringBuilder();
        sb.append("v");
        sb.append(e.getRegister());
        sb.append(' ');
        final CstString name = e.getName();
        if (name == null) {
            sb.append("null");
        }
        else {
            sb.append(name.toHuman());
        }
        sb.append(' ');
        final CstType type = e.getType();
        if (type == null) {
            sb.append("null");
        }
        else {
            sb.append(type.toHuman());
        }
        final CstString signature = e.getSignature();
        if (signature != null) {
            sb.append(' ');
            sb.append(signature.toHuman());
        }
        return sb.toString();
    }
    
    private void emitLocalRestart(final LocalList.Entry entry) throws IOException {
        final int mark = this.output.getCursor();
        this.output.writeByte(6);
        this.emitUnsignedLeb128(entry.getRegister());
        if (this.annotateTo != null || this.debugPrint != null) {
            this.annotate(this.output.getCursor() - mark, String.format("%04x: +local restart %s", this.address, this.entryAnnotationString(entry)));
        }
    }
    
    private void emitStringIndex(final CstString string) throws IOException {
        if (string == null || this.file == null) {
            this.output.writeUleb128(0);
        }
        else {
            this.output.writeUleb128(1 + this.file.getStringIds().indexOf(string));
        }
    }
    
    private void emitTypeIndex(final CstType type) throws IOException {
        if (type == null || this.file == null) {
            this.output.writeUleb128(0);
        }
        else {
            this.output.writeUleb128(1 + this.file.getTypeIds().indexOf(type));
        }
    }
    
    private void emitLocalStart(final LocalList.Entry entry) throws IOException {
        if (entry.getSignature() != null) {
            this.emitLocalStartExtended(entry);
            return;
        }
        final int mark = this.output.getCursor();
        this.output.writeByte(3);
        this.emitUnsignedLeb128(entry.getRegister());
        this.emitStringIndex(entry.getName());
        this.emitTypeIndex(entry.getType());
        if (this.annotateTo != null || this.debugPrint != null) {
            this.annotate(this.output.getCursor() - mark, String.format("%04x: +local %s", this.address, this.entryAnnotationString(entry)));
        }
    }
    
    private void emitLocalStartExtended(final LocalList.Entry entry) throws IOException {
        final int mark = this.output.getCursor();
        this.output.writeByte(4);
        this.emitUnsignedLeb128(entry.getRegister());
        this.emitStringIndex(entry.getName());
        this.emitTypeIndex(entry.getType());
        this.emitStringIndex(entry.getSignature());
        if (this.annotateTo != null || this.debugPrint != null) {
            this.annotate(this.output.getCursor() - mark, String.format("%04x: +localx %s", this.address, this.entryAnnotationString(entry)));
        }
    }
    
    private void emitLocalEnd(final LocalList.Entry entry) throws IOException {
        final int mark = this.output.getCursor();
        this.output.writeByte(5);
        this.output.writeUleb128(entry.getRegister());
        if (this.annotateTo != null || this.debugPrint != null) {
            this.annotate(this.output.getCursor() - mark, String.format("%04x: -local %s", this.address, this.entryAnnotationString(entry)));
        }
    }
    
    private void emitPosition(final PositionList.Entry entry) throws IOException {
        final SourcePosition pos = entry.getPosition();
        final int newLine = pos.getLine();
        final int newAddress = entry.getAddress();
        int deltaLines = newLine - this.line;
        int deltaAddress = newAddress - this.address;
        if (deltaAddress < 0) {
            throw new RuntimeException("Position entries must be in ascending address order");
        }
        if (deltaLines < -4 || deltaLines > 10) {
            this.emitAdvanceLine(deltaLines);
            deltaLines = 0;
        }
        int opcode = computeOpcode(deltaLines, deltaAddress);
        if ((opcode & 0xFFFFFF00) > 0) {
            this.emitAdvancePc(deltaAddress);
            deltaAddress = 0;
            opcode = computeOpcode(deltaLines, deltaAddress);
            if ((opcode & 0xFFFFFF00) > 0) {
                this.emitAdvanceLine(deltaLines);
                deltaLines = 0;
                opcode = computeOpcode(deltaLines, deltaAddress);
            }
        }
        this.output.writeByte(opcode);
        this.line += deltaLines;
        this.address += deltaAddress;
        if (this.annotateTo != null || this.debugPrint != null) {
            this.annotate(1, String.format("%04x: line %d", this.address, this.line));
        }
    }
    
    private static int computeOpcode(final int deltaLines, final int deltaAddress) {
        if (deltaLines < -4 || deltaLines > 10) {
            throw new RuntimeException("Parameter out of range");
        }
        return deltaLines + 4 + 15 * deltaAddress + 10;
    }
    
    private void emitAdvanceLine(final int deltaLines) throws IOException {
        final int mark = this.output.getCursor();
        this.output.writeByte(2);
        this.output.writeSleb128(deltaLines);
        this.line += deltaLines;
        if (this.annotateTo != null || this.debugPrint != null) {
            this.annotate(this.output.getCursor() - mark, String.format("line = %d", this.line));
        }
    }
    
    private void emitAdvancePc(final int deltaAddress) throws IOException {
        final int mark = this.output.getCursor();
        this.output.writeByte(1);
        this.output.writeUleb128(deltaAddress);
        this.address += deltaAddress;
        if (this.annotateTo != null || this.debugPrint != null) {
            this.annotate(this.output.getCursor() - mark, String.format("%04x: advance pc", this.address));
        }
    }
    
    private void emitUnsignedLeb128(final int n) throws IOException {
        if (n < 0) {
            throw new RuntimeException("Signed value where unsigned required: " + n);
        }
        this.output.writeUleb128(n);
    }
    
    private void emitEndSequence() {
        this.output.writeByte(0);
        if (this.annotateTo != null || this.debugPrint != null) {
            this.annotate(1, "end sequence");
        }
    }
}

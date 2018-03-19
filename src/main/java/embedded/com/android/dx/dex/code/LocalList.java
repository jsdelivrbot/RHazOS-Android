package embedded.com.android.dx.dex.code;

import embedded.com.android.dx.util.*;
import java.io.*;
import embedded.com.android.dx.rop.code.*;
import embedded.com.android.dx.rop.cst.*;
import embedded.com.android.dx.rop.type.*;
import java.util.*;

public final class LocalList extends FixedSizeList
{
    public static final LocalList EMPTY;
    private static final boolean DEBUG = false;
    
    public LocalList(final int size) {
        super(size);
    }
    
    public Entry get(final int n) {
        return (Entry)this.get0(n);
    }
    
    public void set(final int n, final Entry entry) {
        this.set0(n, entry);
    }
    
    public void debugPrint(final PrintStream out, final String prefix) {
        for (int sz = this.size(), i = 0; i < sz; ++i) {
            out.print(prefix);
            out.println(this.get(i));
        }
    }
    
    public static LocalList make(final DalvInsnList insns) {
        final int sz = insns.size();
        final MakeState state = new MakeState(sz);
        for (int i = 0; i < sz; ++i) {
            final DalvInsn insn = insns.get(i);
            if (insn instanceof LocalSnapshot) {
                final RegisterSpecSet snapshot = ((LocalSnapshot)insn).getLocals();
                state.snapshot(insn.getAddress(), snapshot);
            }
            else if (insn instanceof LocalStart) {
                final RegisterSpec local = ((LocalStart)insn).getLocal();
                state.startLocal(insn.getAddress(), local);
            }
        }
        final LocalList result = state.finish();
        return result;
    }
    
    private static void debugVerify(final LocalList locals) {
        try {
            debugVerify0(locals);
        }
        catch (RuntimeException ex) {
            for (int sz = locals.size(), i = 0; i < sz; ++i) {
                System.err.println(locals.get(i));
            }
            throw ex;
        }
    }
    
    private static void debugVerify0(final LocalList locals) {
        final int sz = locals.size();
        final Entry[] active = new Entry[65536];
        for (int i = 0; i < sz; ++i) {
            final Entry e = locals.get(i);
            final int reg = e.getRegister();
            if (e.isStart()) {
                final Entry already = active[reg];
                if (already != null && e.matches(already)) {
                    throw new RuntimeException("redundant start at " + Integer.toHexString(e.getAddress()) + ": got " + e + "; had " + already);
                }
                active[reg] = e;
            }
            else {
                if (active[reg] == null) {
                    throw new RuntimeException("redundant end at " + Integer.toHexString(e.getAddress()));
                }
                final int addr = e.getAddress();
                boolean foundStart = false;
                for (int j = i + 1; j < sz; ++j) {
                    final Entry test = locals.get(j);
                    if (test.getAddress() != addr) {
                        break;
                    }
                    if (test.getRegisterSpec().getReg() == reg) {
                        if (!test.isStart()) {
                            throw new RuntimeException("redundant end at " + Integer.toHexString(addr));
                        }
                        if (e.getDisposition() != Disposition.END_REPLACED) {
                            throw new RuntimeException("improperly marked end at " + Integer.toHexString(addr));
                        }
                        foundStart = true;
                    }
                }
                if (!foundStart && e.getDisposition() == Disposition.END_REPLACED) {
                    throw new RuntimeException("improper end replacement claim at " + Integer.toHexString(addr));
                }
                active[reg] = null;
            }
        }
    }
    
    static {
        EMPTY = new LocalList(0);
    }
    
    public enum Disposition
    {
        START, 
        END_SIMPLY, 
        END_REPLACED, 
        END_MOVED, 
        END_CLOBBERED_BY_PREV, 
        END_CLOBBERED_BY_NEXT;
    }
    
    public static class Entry implements Comparable<Entry>
    {
        private final int address;
        private final Disposition disposition;
        private final RegisterSpec spec;
        private final CstType type;
        
        public Entry(final int address, final Disposition disposition, final RegisterSpec spec) {
            if (address < 0) {
                throw new IllegalArgumentException("address < 0");
            }
            if (disposition == null) {
                throw new NullPointerException("disposition == null");
            }
            try {
                if (spec.getLocalItem() == null) {
                    throw new NullPointerException("spec.getLocalItem() == null");
                }
            }
            catch (NullPointerException ex) {
                throw new NullPointerException("spec == null");
            }
            this.address = address;
            this.disposition = disposition;
            this.spec = spec;
            this.type = CstType.intern(spec.getType());
        }
        
        @Override
        public String toString() {
            return Integer.toHexString(this.address) + " " + this.disposition + " " + this.spec;
        }
        
        @Override
        public boolean equals(final Object other) {
            return other instanceof Entry && this.compareTo((Entry)other) == 0;
        }
        
        @Override
        public int compareTo(final Entry other) {
            if (this.address < other.address) {
                return -1;
            }
            if (this.address > other.address) {
                return 1;
            }
            final boolean thisIsStart = this.isStart();
            final boolean otherIsStart = other.isStart();
            if (thisIsStart != otherIsStart) {
                return thisIsStart ? 1 : -1;
            }
            return this.spec.compareTo(other.spec);
        }
        
        public int getAddress() {
            return this.address;
        }
        
        public Disposition getDisposition() {
            return this.disposition;
        }
        
        public boolean isStart() {
            return this.disposition == Disposition.START;
        }
        
        public CstString getName() {
            return this.spec.getLocalItem().getName();
        }
        
        public CstString getSignature() {
            return this.spec.getLocalItem().getSignature();
        }
        
        public CstType getType() {
            return this.type;
        }
        
        public int getRegister() {
            return this.spec.getReg();
        }
        
        public RegisterSpec getRegisterSpec() {
            return this.spec;
        }
        
        public boolean matches(final RegisterSpec otherSpec) {
            return this.spec.equalsUsingSimpleType(otherSpec);
        }
        
        public boolean matches(final Entry other) {
            return this.matches(other.spec);
        }
        
        public Entry withDisposition(final Disposition disposition) {
            if (disposition == this.disposition) {
                return this;
            }
            return new Entry(this.address, disposition, this.spec);
        }
    }
    
    public static class MakeState
    {
        private final ArrayList<Entry> result;
        private int nullResultCount;
        private RegisterSpecSet regs;
        private int[] endIndices;
        private int lastAddress;
        
        public MakeState(final int initialSize) {
            this.result = new ArrayList<Entry>(initialSize);
            this.nullResultCount = 0;
            this.regs = null;
            this.endIndices = null;
            this.lastAddress = 0;
        }
        
        private void aboutToProcess(final int address, final int reg) {
            final boolean first = this.endIndices == null;
            if (address == this.lastAddress && !first) {
                return;
            }
            if (address < this.lastAddress) {
                throw new RuntimeException("shouldn't happen");
            }
            if (first || reg >= this.endIndices.length) {
                final int newSz = reg + 1;
                final RegisterSpecSet newRegs = new RegisterSpecSet(newSz);
                final int[] newEnds = new int[newSz];
                Arrays.fill(newEnds, -1);
                if (!first) {
                    newRegs.putAll(this.regs);
                    System.arraycopy(this.endIndices, 0, newEnds, 0, this.endIndices.length);
                }
                this.regs = newRegs;
                this.endIndices = newEnds;
            }
        }
        
        public void snapshot(final int address, final RegisterSpecSet specs) {
            final int sz = specs.getMaxSize();
            this.aboutToProcess(address, sz - 1);
            for (int i = 0; i < sz; ++i) {
                final RegisterSpec oldSpec = this.regs.get(i);
                final RegisterSpec newSpec = filterSpec(specs.get(i));
                if (oldSpec == null) {
                    if (newSpec != null) {
                        this.startLocal(address, newSpec);
                    }
                }
                else if (newSpec == null) {
                    this.endLocal(address, oldSpec);
                }
                else if (!newSpec.equalsUsingSimpleType(oldSpec)) {
                    this.endLocal(address, oldSpec);
                    this.startLocal(address, newSpec);
                }
            }
        }
        
        public void startLocal(final int address, RegisterSpec startedLocal) {
            final int regNum = startedLocal.getReg();
            startedLocal = filterSpec(startedLocal);
            this.aboutToProcess(address, regNum);
            final RegisterSpec existingLocal = this.regs.get(regNum);
            if (startedLocal.equalsUsingSimpleType(existingLocal)) {
                return;
            }
            final RegisterSpec movedLocal = this.regs.findMatchingLocal(startedLocal);
            if (movedLocal != null) {
                this.addOrUpdateEnd(address, Disposition.END_MOVED, movedLocal);
            }
            final int endAt = this.endIndices[regNum];
            if (existingLocal != null) {
                this.add(address, Disposition.END_REPLACED, existingLocal);
            }
            else if (endAt >= 0) {
                Entry endEntry = this.result.get(endAt);
                if (endEntry.getAddress() == address) {
                    if (endEntry.matches(startedLocal)) {
                        this.result.set(endAt, null);
                        ++this.nullResultCount;
                        this.regs.put(startedLocal);
                        this.endIndices[regNum] = -1;
                        return;
                    }
                    endEntry = endEntry.withDisposition(Disposition.END_REPLACED);
                    this.result.set(endAt, endEntry);
                }
            }
            if (regNum > 0) {
                final RegisterSpec justBelow = this.regs.get(regNum - 1);
                if (justBelow != null && justBelow.isCategory2()) {
                    this.addOrUpdateEnd(address, Disposition.END_CLOBBERED_BY_NEXT, justBelow);
                }
            }
            if (startedLocal.isCategory2()) {
                final RegisterSpec justAbove = this.regs.get(regNum + 1);
                if (justAbove != null) {
                    this.addOrUpdateEnd(address, Disposition.END_CLOBBERED_BY_PREV, justAbove);
                }
            }
            this.add(address, Disposition.START, startedLocal);
        }
        
        public void endLocal(final int address, final RegisterSpec endedLocal) {
            this.endLocal(address, endedLocal, Disposition.END_SIMPLY);
        }
        
        public void endLocal(final int address, RegisterSpec endedLocal, final Disposition disposition) {
            final int regNum = endedLocal.getReg();
            endedLocal = filterSpec(endedLocal);
            this.aboutToProcess(address, regNum);
            final int endAt = this.endIndices[regNum];
            if (endAt >= 0) {
                return;
            }
            if (this.checkForEmptyRange(address, endedLocal)) {
                return;
            }
            this.add(address, disposition, endedLocal);
        }
        
        private boolean checkForEmptyRange(final int address, final RegisterSpec endedLocal) {
            int at;
            for (at = this.result.size() - 1; at >= 0; --at) {
                final Entry entry = this.result.get(at);
                if (entry != null) {
                    if (entry.getAddress() != address) {
                        return false;
                    }
                    if (entry.matches(endedLocal)) {
                        break;
                    }
                }
            }
            this.regs.remove(endedLocal);
            this.result.set(at, null);
            ++this.nullResultCount;
            final int regNum = endedLocal.getReg();
            boolean found = false;
            Entry entry = null;
            --at;
            while (at >= 0) {
                entry = this.result.get(at);
                if (entry != null) {
                    if (entry.getRegisterSpec().getReg() == regNum) {
                        found = true;
                        break;
                    }
                }
                --at;
            }
            if (found) {
                this.endIndices[regNum] = at;
                if (entry.getAddress() == address) {
                    this.result.set(at, entry.withDisposition(Disposition.END_SIMPLY));
                }
            }
            return true;
        }
        
        private static RegisterSpec filterSpec(final RegisterSpec orig) {
            if (orig != null && orig.getType() == Type.KNOWN_NULL) {
                return orig.withType(Type.OBJECT);
            }
            return orig;
        }
        
        private void add(final int address, final Disposition disposition, final RegisterSpec spec) {
            final int regNum = spec.getReg();
            this.result.add(new Entry(address, disposition, spec));
            if (disposition == Disposition.START) {
                this.regs.put(spec);
                this.endIndices[regNum] = -1;
            }
            else {
                this.regs.remove(spec);
                this.endIndices[regNum] = this.result.size() - 1;
            }
        }
        
        private void addOrUpdateEnd(final int address, final Disposition disposition, final RegisterSpec spec) {
            if (disposition == Disposition.START) {
                throw new RuntimeException("shouldn't happen");
            }
            final int regNum = spec.getReg();
            final int endAt = this.endIndices[regNum];
            if (endAt >= 0) {
                final Entry endEntry = this.result.get(endAt);
                if (endEntry.getAddress() == address && endEntry.getRegisterSpec().equals(spec)) {
                    this.result.set(endAt, endEntry.withDisposition(disposition));
                    this.regs.remove(spec);
                    return;
                }
            }
            this.endLocal(address, spec, disposition);
        }
        
        public LocalList finish() {
            this.aboutToProcess(Integer.MAX_VALUE, 0);
            final int resultSz = this.result.size();
            final int finalSz = resultSz - this.nullResultCount;
            if (finalSz == 0) {
                return LocalList.EMPTY;
            }
            final Entry[] resultArr = new Entry[finalSz];
            if (resultSz == finalSz) {
                this.result.toArray(resultArr);
            }
            else {
                int at = 0;
                for (final Entry e : this.result) {
                    if (e != null) {
                        resultArr[at++] = e;
                    }
                }
            }
            Arrays.sort(resultArr);
            final LocalList resultList = new LocalList(finalSz);
            for (int i = 0; i < finalSz; ++i) {
                resultList.set(i, resultArr[i]);
            }
            resultList.setImmutable();
            return resultList;
        }
    }
}

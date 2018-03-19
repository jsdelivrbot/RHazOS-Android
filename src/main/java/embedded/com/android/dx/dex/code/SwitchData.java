package embedded.com.android.dx.dex.code;

import embedded.com.android.dx.rop.code.*;
import embedded.com.android.dx.util.*;

public final class SwitchData extends VariableSizeInsn
{
    private final CodeAddress user;
    private final IntList cases;
    private final CodeAddress[] targets;
    private final boolean packed;
    
    public SwitchData(final SourcePosition position, final CodeAddress user, final IntList cases, final CodeAddress[] targets) {
        super(position, RegisterSpecList.EMPTY);
        if (user == null) {
            throw new NullPointerException("user == null");
        }
        if (cases == null) {
            throw new NullPointerException("cases == null");
        }
        if (targets == null) {
            throw new NullPointerException("targets == null");
        }
        final int sz = cases.size();
        if (sz != targets.length) {
            throw new IllegalArgumentException("cases / targets mismatch");
        }
        if (sz > 65535) {
            throw new IllegalArgumentException("too many cases");
        }
        this.user = user;
        this.cases = cases;
        this.targets = targets;
        this.packed = shouldPack(cases);
    }
    
    @Override
    public int codeSize() {
        return this.packed ? ((int)packedCodeSize(this.cases)) : ((int)sparseCodeSize(this.cases));
    }
    
    @Override
    public void writeTo(final AnnotatedOutput out) {
        final int baseAddress = this.user.getAddress();
        final int defaultTarget = Dops.PACKED_SWITCH.getFormat().codeSize();
        final int sz = this.targets.length;
        if (this.packed) {
            final int firstCase = (sz == 0) ? 0 : this.cases.get(0);
            final int lastCase = (sz == 0) ? 0 : this.cases.get(sz - 1);
            final int outSz = lastCase - firstCase + 1;
            out.writeShort(256);
            out.writeShort(outSz);
            out.writeInt(firstCase);
            int caseAt = 0;
            for (int i = 0; i < outSz; ++i) {
                final int outCase = firstCase + i;
                final int oneCase = this.cases.get(caseAt);
                int relTarget;
                if (oneCase > outCase) {
                    relTarget = defaultTarget;
                }
                else {
                    relTarget = this.targets[caseAt].getAddress() - baseAddress;
                    ++caseAt;
                }
                out.writeInt(relTarget);
            }
        }
        else {
            out.writeShort(512);
            out.writeShort(sz);
            for (int j = 0; j < sz; ++j) {
                out.writeInt(this.cases.get(j));
            }
            for (int j = 0; j < sz; ++j) {
                final int relTarget2 = this.targets[j].getAddress() - baseAddress;
                out.writeInt(relTarget2);
            }
        }
    }
    
    @Override
    public DalvInsn withRegisters(final RegisterSpecList registers) {
        return new SwitchData(this.getPosition(), this.user, this.cases, this.targets);
    }
    
    public boolean isPacked() {
        return this.packed;
    }
    
    @Override
    protected String argString() {
        final StringBuffer sb = new StringBuffer(100);
        for (int sz = this.targets.length, i = 0; i < sz; ++i) {
            sb.append("\n    ");
            sb.append(this.cases.get(i));
            sb.append(": ");
            sb.append(this.targets[i]);
        }
        return sb.toString();
    }
    
    @Override
    protected String listingString0(final boolean noteIndices) {
        final int baseAddress = this.user.getAddress();
        final StringBuffer sb = new StringBuffer(100);
        final int sz = this.targets.length;
        sb.append(this.packed ? "packed" : "sparse");
        sb.append("-switch-payload // for switch @ ");
        sb.append(Hex.u2(baseAddress));
        for (int i = 0; i < sz; ++i) {
            final int absTarget = this.targets[i].getAddress();
            final int relTarget = absTarget - baseAddress;
            sb.append("\n  ");
            sb.append(this.cases.get(i));
            sb.append(": ");
            sb.append(Hex.u4(absTarget));
            sb.append(" // ");
            sb.append(Hex.s4(relTarget));
        }
        return sb.toString();
    }
    
    private static long packedCodeSize(final IntList cases) {
        final int sz = cases.size();
        final long low = cases.get(0);
        final long high = cases.get(sz - 1);
        final long result = (high - low + 1L) * 2L + 4L;
        return (result <= 2147483647L) ? result : -1L;
    }
    
    private static long sparseCodeSize(final IntList cases) {
        final int sz = cases.size();
        return sz * 4L + 2L;
    }
    
    private static boolean shouldPack(final IntList cases) {
        final int sz = cases.size();
        if (sz < 2) {
            return true;
        }
        final long packedSize = packedCodeSize(cases);
        final long sparseSize = sparseCodeSize(cases);
        return packedSize >= 0L && packedSize <= sparseSize * 5L / 4L;
    }
}

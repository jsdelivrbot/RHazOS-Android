package embedded.com.android.dx.rop.code;

import embedded.com.android.dx.util.*;

public final class InsnList extends FixedSizeList
{
    public InsnList(final int size) {
        super(size);
    }
    
    public Insn get(final int n) {
        return (Insn)this.get0(n);
    }
    
    public void set(final int n, final Insn insn) {
        this.set0(n, insn);
    }
    
    public Insn getLast() {
        return this.get(this.size() - 1);
    }
    
    public void forEach(final Insn.Visitor visitor) {
        for (int sz = this.size(), i = 0; i < sz; ++i) {
            this.get(i).accept(visitor);
        }
    }
    
    public boolean contentEquals(final InsnList b) {
        if (b == null) {
            return false;
        }
        final int sz = this.size();
        if (sz != b.size()) {
            return false;
        }
        for (int i = 0; i < sz; ++i) {
            if (!this.get(i).contentEquals(b.get(i))) {
                return false;
            }
        }
        return true;
    }
    
    public InsnList withRegisterOffset(final int delta) {
        final int sz = this.size();
        final InsnList result = new InsnList(sz);
        for (int i = 0; i < sz; ++i) {
            final Insn one = (Insn)this.get0(i);
            if (one != null) {
                result.set0(i, one.withRegisterOffset(delta));
            }
        }
        if (this.isImmutable()) {
            result.setImmutable();
        }
        return result;
    }
}

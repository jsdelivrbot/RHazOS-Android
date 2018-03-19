package embedded.com.android.dx.rop.code;

import embedded.com.android.dx.util.*;
import embedded.com.android.dx.rop.type.*;
import java.util.*;

public final class RegisterSpecList extends FixedSizeList implements TypeList
{
    public static final RegisterSpecList EMPTY;
    
    public static RegisterSpecList make(final RegisterSpec spec) {
        final RegisterSpecList result = new RegisterSpecList(1);
        result.set(0, spec);
        return result;
    }
    
    public static RegisterSpecList make(final RegisterSpec spec0, final RegisterSpec spec1) {
        final RegisterSpecList result = new RegisterSpecList(2);
        result.set(0, spec0);
        result.set(1, spec1);
        return result;
    }
    
    public static RegisterSpecList make(final RegisterSpec spec0, final RegisterSpec spec1, final RegisterSpec spec2) {
        final RegisterSpecList result = new RegisterSpecList(3);
        result.set(0, spec0);
        result.set(1, spec1);
        result.set(2, spec2);
        return result;
    }
    
    public static RegisterSpecList make(final RegisterSpec spec0, final RegisterSpec spec1, final RegisterSpec spec2, final RegisterSpec spec3) {
        final RegisterSpecList result = new RegisterSpecList(4);
        result.set(0, spec0);
        result.set(1, spec1);
        result.set(2, spec2);
        result.set(3, spec3);
        return result;
    }
    
    public RegisterSpecList(final int size) {
        super(size);
    }
    
    @Override
    public Type getType(final int n) {
        return this.get(n).getType().getType();
    }
    
    @Override
    public int getWordCount() {
        final int sz = this.size();
        int result = 0;
        for (int i = 0; i < sz; ++i) {
            result += this.getType(i).getCategory();
        }
        return result;
    }
    
    @Override
    public TypeList withAddedType(final Type type) {
        throw new UnsupportedOperationException("unsupported");
    }
    
    public RegisterSpec get(final int n) {
        return (RegisterSpec)this.get0(n);
    }
    
    public RegisterSpec specForRegister(final int reg) {
        for (int sz = this.size(), i = 0; i < sz; ++i) {
            final RegisterSpec rs = this.get(i);
            if (rs.getReg() == reg) {
                return rs;
            }
        }
        return null;
    }
    
    public int indexOfRegister(final int reg) {
        for (int sz = this.size(), i = 0; i < sz; ++i) {
            final RegisterSpec rs = this.get(i);
            if (rs.getReg() == reg) {
                return i;
            }
        }
        return -1;
    }
    
    public void set(final int n, final RegisterSpec spec) {
        this.set0(n, spec);
    }
    
    public int getRegistersSize() {
        final int sz = this.size();
        int result = 0;
        for (int i = 0; i < sz; ++i) {
            final RegisterSpec spec = (RegisterSpec)this.get0(i);
            if (spec != null) {
                final int min = spec.getNextReg();
                if (min > result) {
                    result = min;
                }
            }
        }
        return result;
    }
    
    public RegisterSpecList withFirst(final RegisterSpec spec) {
        final int sz = this.size();
        final RegisterSpecList result = new RegisterSpecList(sz + 1);
        for (int i = 0; i < sz; ++i) {
            result.set0(i + 1, this.get0(i));
        }
        result.set0(0, spec);
        if (this.isImmutable()) {
            result.setImmutable();
        }
        return result;
    }
    
    public RegisterSpecList withoutFirst() {
        final int newSize = this.size() - 1;
        if (newSize == 0) {
            return RegisterSpecList.EMPTY;
        }
        final RegisterSpecList result = new RegisterSpecList(newSize);
        for (int i = 0; i < newSize; ++i) {
            result.set0(i, this.get0(i + 1));
        }
        if (this.isImmutable()) {
            result.setImmutable();
        }
        return result;
    }
    
    public RegisterSpecList withoutLast() {
        final int newSize = this.size() - 1;
        if (newSize == 0) {
            return RegisterSpecList.EMPTY;
        }
        final RegisterSpecList result = new RegisterSpecList(newSize);
        for (int i = 0; i < newSize; ++i) {
            result.set0(i, this.get0(i));
        }
        if (this.isImmutable()) {
            result.setImmutable();
        }
        return result;
    }
    
    public RegisterSpecList subset(final BitSet exclusionSet) {
        final int newSize = this.size() - exclusionSet.cardinality();
        if (newSize == 0) {
            return RegisterSpecList.EMPTY;
        }
        final RegisterSpecList result = new RegisterSpecList(newSize);
        int newIndex = 0;
        for (int oldIndex = 0; oldIndex < this.size(); ++oldIndex) {
            if (!exclusionSet.get(oldIndex)) {
                result.set0(newIndex, this.get0(oldIndex));
                ++newIndex;
            }
        }
        if (this.isImmutable()) {
            result.setImmutable();
        }
        return result;
    }
    
    public RegisterSpecList withOffset(final int delta) {
        final int sz = this.size();
        if (sz == 0) {
            return this;
        }
        final RegisterSpecList result = new RegisterSpecList(sz);
        for (int i = 0; i < sz; ++i) {
            final RegisterSpec one = (RegisterSpec)this.get0(i);
            if (one != null) {
                result.set0(i, one.withOffset(delta));
            }
        }
        if (this.isImmutable()) {
            result.setImmutable();
        }
        return result;
    }
    
    public RegisterSpecList withExpandedRegisters(final int base, final boolean duplicateFirst, final BitSet compatRegs) {
        final int sz = this.size();
        if (sz == 0) {
            return this;
        }
        final Expander expander = new Expander(this, compatRegs, base, duplicateFirst);
        for (int regIdx = 0; regIdx < sz; ++regIdx) {
            expander.expandRegister(regIdx);
        }
        return expander.getResult();
    }
    
    static {
        EMPTY = new RegisterSpecList(0);
    }
    
    private static class Expander
    {
        private BitSet compatRegs;
        private RegisterSpecList regSpecList;
        private int base;
        private RegisterSpecList result;
        private boolean duplicateFirst;
        
        private Expander(final RegisterSpecList regSpecList, final BitSet compatRegs, final int base, final boolean duplicateFirst) {
            this.regSpecList = regSpecList;
            this.compatRegs = compatRegs;
            this.base = base;
            this.result = new RegisterSpecList(regSpecList.size());
            this.duplicateFirst = duplicateFirst;
        }
        
        private void expandRegister(final int regIdx) {
            this.expandRegister(regIdx, (RegisterSpec)this.regSpecList.get0(regIdx));
        }
        
        private void expandRegister(final int regIdx, final RegisterSpec registerToExpand) {
            final boolean replace = this.compatRegs == null || !this.compatRegs.get(regIdx);
            RegisterSpec expandedReg;
            if (replace) {
                expandedReg = registerToExpand.withReg(this.base);
                if (!this.duplicateFirst) {
                    this.base += expandedReg.getCategory();
                }
            }
            else {
                expandedReg = registerToExpand;
            }
            this.duplicateFirst = false;
            this.result.set0(regIdx, expandedReg);
        }
        
        private RegisterSpecList getResult() {
            if (this.regSpecList.isImmutable()) {
                this.result.setImmutable();
            }
            return this.result;
        }
    }
}

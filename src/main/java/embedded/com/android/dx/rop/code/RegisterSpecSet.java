package embedded.com.android.dx.rop.code;

import embedded.com.android.dx.util.*;

public final class RegisterSpecSet extends MutabilityControl
{
    public static final RegisterSpecSet EMPTY;
    private final RegisterSpec[] specs;
    private int size;
    
    public RegisterSpecSet(final int maxSize) {
        super(maxSize != 0);
        this.specs = new RegisterSpec[maxSize];
        this.size = 0;
    }
    
    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof RegisterSpecSet)) {
            return false;
        }
        final RegisterSpecSet otherSet = (RegisterSpecSet)other;
        final RegisterSpec[] otherSpecs = otherSet.specs;
        final int len = this.specs.length;
        if (len != otherSpecs.length || this.size() != otherSet.size()) {
            return false;
        }
        for (int i = 0; i < len; ++i) {
            final RegisterSpec s1 = this.specs[i];
            final RegisterSpec s2 = otherSpecs[i];
            if (s1 != s2) {
                if (s1 == null || !s1.equals(s2)) {
                    return false;
                }
            }
        }
        return true;
    }
    
    @Override
    public int hashCode() {
        final int len = this.specs.length;
        int hash = 0;
        for (int i = 0; i < len; ++i) {
            final RegisterSpec spec = this.specs[i];
            final int oneHash = (spec == null) ? 0 : spec.hashCode();
            hash = hash * 31 + oneHash;
        }
        return hash;
    }
    
    @Override
    public String toString() {
        final int len = this.specs.length;
        final StringBuffer sb = new StringBuffer(len * 25);
        sb.append('{');
        boolean any = false;
        for (int i = 0; i < len; ++i) {
            final RegisterSpec spec = this.specs[i];
            if (spec != null) {
                if (any) {
                    sb.append(", ");
                }
                else {
                    any = true;
                }
                sb.append(spec);
            }
        }
        sb.append('}');
        return sb.toString();
    }
    
    public int getMaxSize() {
        return this.specs.length;
    }
    
    public int size() {
        int result = this.size;
        if (result < 0) {
            final int len = this.specs.length;
            result = 0;
            for (int i = 0; i < len; ++i) {
                if (this.specs[i] != null) {
                    ++result;
                }
            }
            this.size = result;
        }
        return result;
    }
    
    public RegisterSpec get(final int reg) {
        try {
            return this.specs[reg];
        }
        catch (ArrayIndexOutOfBoundsException ex) {
            throw new IllegalArgumentException("bogus reg");
        }
    }
    
    public RegisterSpec get(final RegisterSpec spec) {
        return this.get(spec.getReg());
    }
    
    public RegisterSpec findMatchingLocal(final RegisterSpec spec) {
        for (int length = this.specs.length, reg = 0; reg < length; ++reg) {
            final RegisterSpec s = this.specs[reg];
            if (s != null) {
                if (spec.matchesVariable(s)) {
                    return s;
                }
            }
        }
        return null;
    }
    
    public RegisterSpec localItemToSpec(final LocalItem local) {
        for (int length = this.specs.length, reg = 0; reg < length; ++reg) {
            final RegisterSpec spec = this.specs[reg];
            if (spec != null && local.equals(spec.getLocalItem())) {
                return spec;
            }
        }
        return null;
    }
    
    public void remove(final RegisterSpec toRemove) {
        try {
            this.specs[toRemove.getReg()] = null;
            this.size = -1;
        }
        catch (ArrayIndexOutOfBoundsException ex) {
            throw new IllegalArgumentException("bogus reg");
        }
    }
    
    public void put(final RegisterSpec spec) {
        this.throwIfImmutable();
        if (spec == null) {
            throw new NullPointerException("spec == null");
        }
        this.size = -1;
        try {
            final int reg = spec.getReg();
            this.specs[reg] = spec;
            if (reg > 0) {
                final int prevReg = reg - 1;
                final RegisterSpec prevSpec = this.specs[prevReg];
                if (prevSpec != null && prevSpec.getCategory() == 2) {
                    this.specs[prevReg] = null;
                }
            }
            if (spec.getCategory() == 2) {
                this.specs[reg + 1] = null;
            }
        }
        catch (ArrayIndexOutOfBoundsException ex) {
            throw new IllegalArgumentException("spec.getReg() out of range");
        }
    }
    
    public void putAll(final RegisterSpecSet set) {
        for (int max = set.getMaxSize(), i = 0; i < max; ++i) {
            final RegisterSpec spec = set.get(i);
            if (spec != null) {
                this.put(spec);
            }
        }
    }
    
    public void intersect(final RegisterSpecSet other, final boolean localPrimary) {
        this.throwIfImmutable();
        final RegisterSpec[] otherSpecs = other.specs;
        final int thisLen = this.specs.length;
        final int len = Math.min(thisLen, otherSpecs.length);
        this.size = -1;
        for (int i = 0; i < len; ++i) {
            final RegisterSpec spec = this.specs[i];
            if (spec != null) {
                final RegisterSpec intersection = spec.intersect(otherSpecs[i], localPrimary);
                if (intersection != spec) {
                    this.specs[i] = intersection;
                }
            }
        }
        for (int i = len; i < thisLen; ++i) {
            this.specs[i] = null;
        }
    }
    
    public RegisterSpecSet withOffset(final int delta) {
        final int len = this.specs.length;
        final RegisterSpecSet result = new RegisterSpecSet(len + delta);
        for (int i = 0; i < len; ++i) {
            final RegisterSpec spec = this.specs[i];
            if (spec != null) {
                result.put(spec.withOffset(delta));
            }
        }
        result.size = this.size;
        if (this.isImmutable()) {
            result.setImmutable();
        }
        return result;
    }
    
    public RegisterSpecSet mutableCopy() {
        final int len = this.specs.length;
        final RegisterSpecSet copy = new RegisterSpecSet(len);
        for (int i = 0; i < len; ++i) {
            final RegisterSpec spec = this.specs[i];
            if (spec != null) {
                copy.put(spec);
            }
        }
        copy.size = this.size;
        return copy;
    }
    
    static {
        EMPTY = new RegisterSpecSet(0);
    }
}

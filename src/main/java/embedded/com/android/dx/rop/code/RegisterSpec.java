package embedded.com.android.dx.rop.code;

import embedded.com.android.dx.util.*;
import java.util.*;
import embedded.com.android.dx.rop.type.*;
import embedded.com.android.dx.rop.cst.*;

public final class RegisterSpec implements TypeBearer, ToHuman, Comparable<RegisterSpec>
{
    public static final String PREFIX = "v";
    private static final HashMap<Object, RegisterSpec> theInterns;
    private static final ForComparison theInterningItem;
    private final int reg;
    private final TypeBearer type;
    private final LocalItem local;
    
    private static RegisterSpec intern(final int reg, final TypeBearer type, final LocalItem local) {
        synchronized (RegisterSpec.theInterns) {
            RegisterSpec.theInterningItem.set(reg, type, local);
            RegisterSpec found = RegisterSpec.theInterns.get(RegisterSpec.theInterningItem);
            if (found != null) {
                return found;
            }
            found = RegisterSpec.theInterningItem.toRegisterSpec();
            RegisterSpec.theInterns.put(found, found);
            return found;
        }
    }
    
    public static RegisterSpec make(final int reg, final TypeBearer type) {
        return intern(reg, type, null);
    }
    
    public static RegisterSpec make(final int reg, final TypeBearer type, final LocalItem local) {
        if (local == null) {
            throw new NullPointerException("local  == null");
        }
        return intern(reg, type, local);
    }
    
    public static RegisterSpec makeLocalOptional(final int reg, final TypeBearer type, final LocalItem local) {
        return intern(reg, type, local);
    }
    
    public static String regString(final int reg) {
        return "v" + reg;
    }
    
    private RegisterSpec(final int reg, final TypeBearer type, final LocalItem local) {
        if (reg < 0) {
            throw new IllegalArgumentException("reg < 0");
        }
        if (type == null) {
            throw new NullPointerException("type == null");
        }
        this.reg = reg;
        this.type = type;
        this.local = local;
    }
    
    @Override
    public boolean equals(final Object other) {
        if (other instanceof RegisterSpec) {
            final RegisterSpec spec = (RegisterSpec)other;
            return this.equals(spec.reg, spec.type, spec.local);
        }
        if (other instanceof ForComparison) {
            final ForComparison fc = (ForComparison)other;
            return this.equals(fc.reg, fc.type, fc.local);
        }
        return false;
    }
    
    public boolean equalsUsingSimpleType(final RegisterSpec other) {
        return this.matchesVariable(other) && this.reg == other.reg;
    }
    
    public boolean matchesVariable(final RegisterSpec other) {
        return other != null && this.type.getType().equals(other.type.getType()) && (this.local == other.local || (this.local != null && this.local.equals(other.local)));
    }
    
    private boolean equals(final int reg, final TypeBearer type, final LocalItem local) {
        return this.reg == reg && this.type.equals(type) && (this.local == local || (this.local != null && this.local.equals(local)));
    }
    
    @Override
    public int compareTo(final RegisterSpec other) {
        if (this.reg < other.reg) {
            return -1;
        }
        if (this.reg > other.reg) {
            return 1;
        }
        final int compare = this.type.getType().compareTo(other.type.getType());
        if (compare != 0) {
            return compare;
        }
        if (this.local == null) {
            return (other.local == null) ? 0 : -1;
        }
        if (other.local == null) {
            return 1;
        }
        return this.local.compareTo(other.local);
    }
    
    @Override
    public int hashCode() {
        return hashCodeOf(this.reg, this.type, this.local);
    }
    
    private static int hashCodeOf(final int reg, final TypeBearer type, final LocalItem local) {
        int hash = (local != null) ? local.hashCode() : 0;
        hash = (hash * 31 + type.hashCode()) * 31 + reg;
        return hash;
    }
    
    @Override
    public String toString() {
        return this.toString0(false);
    }
    
    @Override
    public String toHuman() {
        return this.toString0(true);
    }
    
    @Override
    public Type getType() {
        return this.type.getType();
    }
    
    @Override
    public TypeBearer getFrameType() {
        return this.type.getFrameType();
    }
    
    @Override
    public final int getBasicType() {
        return this.type.getBasicType();
    }
    
    @Override
    public final int getBasicFrameType() {
        return this.type.getBasicFrameType();
    }
    
    @Override
    public final boolean isConstant() {
        return false;
    }
    
    public int getReg() {
        return this.reg;
    }
    
    public TypeBearer getTypeBearer() {
        return this.type;
    }
    
    public LocalItem getLocalItem() {
        return this.local;
    }
    
    public int getNextReg() {
        return this.reg + this.getCategory();
    }
    
    public int getCategory() {
        return this.type.getType().getCategory();
    }
    
    public boolean isCategory1() {
        return this.type.getType().isCategory1();
    }
    
    public boolean isCategory2() {
        return this.type.getType().isCategory2();
    }
    
    public String regString() {
        return regString(this.reg);
    }
    
    public RegisterSpec intersect(final RegisterSpec other, final boolean localPrimary) {
        if (this == other) {
            return this;
        }
        if (other == null || this.reg != other.getReg()) {
            return null;
        }
        final LocalItem resultLocal = (this.local == null || !this.local.equals(other.getLocalItem())) ? null : this.local;
        final boolean sameName = resultLocal == this.local;
        if (localPrimary && !sameName) {
            return null;
        }
        final Type thisType = this.getType();
        final Type otherType = other.getType();
        if (thisType != otherType) {
            return null;
        }
        final TypeBearer resultTypeBearer = this.type.equals(other.getTypeBearer()) ? this.type : thisType;
        if (resultTypeBearer == this.type && sameName) {
            return this;
        }
        return (resultLocal == null) ? make(this.reg, resultTypeBearer) : make(this.reg, resultTypeBearer, resultLocal);
    }
    
    public RegisterSpec withReg(final int newReg) {
        if (this.reg == newReg) {
            return this;
        }
        return makeLocalOptional(newReg, this.type, this.local);
    }
    
    public RegisterSpec withType(final TypeBearer newType) {
        return makeLocalOptional(this.reg, newType, this.local);
    }
    
    public RegisterSpec withOffset(final int delta) {
        if (delta == 0) {
            return this;
        }
        return this.withReg(this.reg + delta);
    }
    
    public RegisterSpec withSimpleType() {
        final TypeBearer orig = this.type;
        Type newType;
        if (orig instanceof Type) {
            newType = (Type)orig;
        }
        else {
            newType = orig.getType();
        }
        if (newType.isUninitialized()) {
            newType = newType.getInitializedType();
        }
        if (newType == orig) {
            return this;
        }
        return makeLocalOptional(this.reg, newType, this.local);
    }
    
    public RegisterSpec withLocalItem(final LocalItem local) {
        if (this.local == local || (this.local != null && this.local.equals(local))) {
            return this;
        }
        return makeLocalOptional(this.reg, this.type, local);
    }
    
    public boolean isEvenRegister() {
        return (this.getReg() & 0x1) == 0x0;
    }
    
    private String toString0(final boolean human) {
        final StringBuffer sb = new StringBuffer(40);
        sb.append(this.regString());
        sb.append(":");
        if (this.local != null) {
            sb.append(this.local.toString());
        }
        final Type justType = this.type.getType();
        sb.append(justType);
        if (justType != this.type) {
            sb.append("=");
            if (human && this.type instanceof CstString) {
                sb.append(((CstString)this.type).toQuoted());
            }
            else if (human && this.type instanceof Constant) {
                sb.append(this.type.toHuman());
            }
            else {
                sb.append(this.type);
            }
        }
        return sb.toString();
    }
    
    public static void clearInternTable() {
        RegisterSpec.theInterns.clear();
    }
    
    static {
        theInterns = new HashMap<Object, RegisterSpec>(1000);
        theInterningItem = new ForComparison();
    }
    
    private static class ForComparison
    {
        private int reg;
        private TypeBearer type;
        private LocalItem local;
        
        public void set(final int reg, final TypeBearer type, final LocalItem local) {
            this.reg = reg;
            this.type = type;
            this.local = local;
        }
        
        public RegisterSpec toRegisterSpec() {
            return new RegisterSpec(this.reg, this.type, this.local);
        }
        
        @Override
        public boolean equals(final Object other) {
            if (!(other instanceof RegisterSpec)) {
                return false;
            }
            final RegisterSpec spec = (RegisterSpec)other;
            return spec.equals(this.reg, this.type, this.local);
        }
        
        @Override
        public int hashCode() {
            return hashCodeOf(this.reg, this.type, this.local);
        }
    }
}

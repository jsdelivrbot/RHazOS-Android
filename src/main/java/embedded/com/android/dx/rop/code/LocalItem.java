package embedded.com.android.dx.rop.code;

import embedded.com.android.dx.rop.cst.*;

public class LocalItem implements Comparable<LocalItem>
{
    private final CstString name;
    private final CstString signature;
    
    public static LocalItem make(final CstString name, final CstString signature) {
        if (name == null && signature == null) {
            return null;
        }
        return new LocalItem(name, signature);
    }
    
    private LocalItem(final CstString name, final CstString signature) {
        this.name = name;
        this.signature = signature;
    }
    
    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof LocalItem)) {
            return false;
        }
        final LocalItem local = (LocalItem)other;
        return 0 == this.compareTo(local);
    }
    
    private static int compareHandlesNulls(final CstString a, final CstString b) {
        if (a == b) {
            return 0;
        }
        if (a == null) {
            return -1;
        }
        if (b == null) {
            return 1;
        }
        return a.compareTo((Constant)b);
    }
    
    @Override
    public int compareTo(final LocalItem local) {
        int ret = compareHandlesNulls(this.name, local.name);
        if (ret != 0) {
            return ret;
        }
        ret = compareHandlesNulls(this.signature, local.signature);
        return ret;
    }
    
    @Override
    public int hashCode() {
        return ((this.name == null) ? 0 : this.name.hashCode()) * 31 + ((this.signature == null) ? 0 : this.signature.hashCode());
    }
    
    @Override
    public String toString() {
        if (this.name != null && this.signature == null) {
            return this.name.toQuoted();
        }
        if (this.name == null && this.signature == null) {
            return "";
        }
        return "[" + ((this.name == null) ? "" : this.name.toQuoted()) + "|" + ((this.signature == null) ? "" : this.signature.toQuoted());
    }
    
    public CstString getName() {
        return this.name;
    }
    
    public CstString getSignature() {
        return this.signature;
    }
}

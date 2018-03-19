package embedded.com.android.dx.rop.annotation;

import embedded.com.android.dx.rop.cst.*;

public final class NameValuePair implements Comparable<NameValuePair>
{
    private final CstString name;
    private final Constant value;
    
    public NameValuePair(final CstString name, final Constant value) {
        if (name == null) {
            throw new NullPointerException("name == null");
        }
        if (value == null) {
            throw new NullPointerException("value == null");
        }
        this.name = name;
        this.value = value;
    }
    
    @Override
    public String toString() {
        return this.name.toHuman() + ":" + this.value;
    }
    
    @Override
    public int hashCode() {
        return this.name.hashCode() * 31 + this.value.hashCode();
    }
    
    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof NameValuePair)) {
            return false;
        }
        final NameValuePair otherPair = (NameValuePair)other;
        return this.name.equals(otherPair.name) && this.value.equals(otherPair.value);
    }
    
    @Override
    public int compareTo(final NameValuePair other) {
        final int result = this.name.compareTo((Constant)other.name);
        if (result != 0) {
            return result;
        }
        return this.value.compareTo(other.value);
    }
    
    public CstString getName() {
        return this.name;
    }
    
    public Constant getValue() {
        return this.value;
    }
}

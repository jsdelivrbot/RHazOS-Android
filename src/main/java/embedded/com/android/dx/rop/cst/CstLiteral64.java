package embedded.com.android.dx.rop.cst;

public abstract class CstLiteral64 extends CstLiteralBits
{
    private final long bits;
    
    CstLiteral64(final long bits) {
        this.bits = bits;
    }
    
    @Override
    public final boolean equals(final Object other) {
        return other != null && this.getClass() == other.getClass() && this.bits == ((CstLiteral64)other).bits;
    }
    
    @Override
    public final int hashCode() {
        return (int)this.bits ^ (int)(this.bits >> 32);
    }
    
    @Override
    protected int compareTo0(final Constant other) {
        final long otherBits = ((CstLiteral64)other).bits;
        if (this.bits < otherBits) {
            return -1;
        }
        if (this.bits > otherBits) {
            return 1;
        }
        return 0;
    }
    
    @Override
    public final boolean isCategory2() {
        return true;
    }
    
    @Override
    public final boolean fitsInInt() {
        return (int)this.bits == this.bits;
    }
    
    @Override
    public final int getIntBits() {
        return (int)this.bits;
    }
    
    @Override
    public final long getLongBits() {
        return this.bits;
    }
}

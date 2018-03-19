package embedded.com.android.dx.rop.cst;

public abstract class CstLiteralBits extends TypedConstant
{
    public abstract boolean fitsInInt();
    
    public abstract int getIntBits();
    
    public abstract long getLongBits();
    
    public boolean fitsIn16Bits() {
        if (!this.fitsInInt()) {
            return false;
        }
        final int bits = this.getIntBits();
        return (short)bits == bits;
    }
    
    public boolean fitsIn8Bits() {
        if (!this.fitsInInt()) {
            return false;
        }
        final int bits = this.getIntBits();
        return (byte)bits == bits;
    }
}

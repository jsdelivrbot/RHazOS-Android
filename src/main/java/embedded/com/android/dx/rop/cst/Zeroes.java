package embedded.com.android.dx.rop.cst;

import embedded.com.android.dx.rop.type.*;

public final class Zeroes
{
    public static Constant zeroFor(final Type type) {
        switch (type.getBasicType()) {
            case 1: {
                return CstBoolean.VALUE_FALSE;
            }
            case 2: {
                return CstByte.VALUE_0;
            }
            case 3: {
                return CstChar.VALUE_0;
            }
            case 4: {
                return CstDouble.VALUE_0;
            }
            case 5: {
                return CstFloat.VALUE_0;
            }
            case 6: {
                return CstInteger.VALUE_0;
            }
            case 7: {
                return CstLong.VALUE_0;
            }
            case 8: {
                return CstShort.VALUE_0;
            }
            case 9: {
                return CstKnownNull.THE_ONE;
            }
            default: {
                throw new UnsupportedOperationException("no zero for type: " + type.toHuman());
            }
        }
    }
}

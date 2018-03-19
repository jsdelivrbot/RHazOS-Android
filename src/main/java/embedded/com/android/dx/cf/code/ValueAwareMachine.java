package embedded.com.android.dx.cf.code;

import embedded.com.android.dx.rop.type.*;
import embedded.com.android.dx.rop.cst.*;
import embedded.com.android.dx.util.*;

public class ValueAwareMachine extends BaseMachine
{
    public ValueAwareMachine(final Prototype prototype) {
        super(prototype);
    }
    
    @Override
    public void run(final Frame frame, final int offset, final int opcode) {
        switch (opcode) {
            case 0:
            case 79:
            case 87:
            case 88:
            case 153:
            case 154:
            case 155:
            case 156:
            case 157:
            case 158:
            case 159:
            case 160:
            case 161:
            case 162:
            case 163:
            case 164:
            case 165:
            case 166:
            case 167:
            case 169:
            case 171:
            case 172:
            case 177:
            case 179:
            case 181:
            case 191:
            case 194:
            case 195:
            case 198:
            case 199: {
                this.clearResult();
                break;
            }
            case 18:
            case 20: {
                this.setResult((TypeBearer)this.getAuxCst());
                break;
            }
            case 21:
            case 54: {
                this.setResult(this.arg(0));
                break;
            }
            case 46:
            case 96:
            case 100:
            case 104:
            case 108:
            case 112:
            case 116:
            case 120:
            case 122:
            case 124:
            case 126:
            case 128:
            case 130:
            case 132:
            case 133:
            case 134:
            case 135:
            case 136:
            case 137:
            case 138:
            case 139:
            case 140:
            case 141:
            case 142:
            case 143:
            case 144:
            case 145:
            case 146:
            case 147:
            case 148:
            case 149:
            case 150:
            case 151:
            case 152:
            case 190: {
                this.setResult(this.getAuxType());
                break;
            }
            case 89:
            case 90:
            case 91:
            case 92:
            case 93:
            case 94:
            case 95: {
                this.clearResult();
                for (int pattern = this.getAuxInt(); pattern != 0; pattern >>= 4) {
                    final int which = (pattern & 0xF) - 1;
                    this.addResult(this.arg(which));
                }
                break;
            }
            case 168: {
                this.setResult(new ReturnAddress(this.getAuxTarget()));
                break;
            }
            case 178:
            case 180:
            case 182:
            case 184:
            case 185: {
                final Type type = ((TypeBearer)this.getAuxCst()).getType();
                if (type == Type.VOID) {
                    this.clearResult();
                    break;
                }
                this.setResult(type);
                break;
            }
            case 183: {
                final Type thisType = this.arg(0).getType();
                if (thisType.isUninitialized()) {
                    frame.makeInitialized(thisType);
                }
                final Type type2 = ((TypeBearer)this.getAuxCst()).getType();
                if (type2 == Type.VOID) {
                    this.clearResult();
                    break;
                }
                this.setResult(type2);
                break;
            }
            case 186: {
                final Type type = ((CstCallSiteRef)this.getAuxCst()).getReturnType();
                if (type == Type.VOID) {
                    this.clearResult();
                    break;
                }
                this.setResult(type);
                break;
            }
            case 187: {
                final Type type = ((CstType)this.getAuxCst()).getClassType();
                this.setResult(type.asUninitialized(offset));
                break;
            }
            case 188:
            case 192:
            case 197: {
                final Type type = ((CstType)this.getAuxCst()).getClassType();
                this.setResult(type);
                break;
            }
            case 189: {
                final Type type = ((CstType)this.getAuxCst()).getClassType();
                this.setResult(type.getArrayType());
                break;
            }
            case 193: {
                this.setResult(Type.INT);
                break;
            }
            default: {
                throw new RuntimeException("shouldn't happen: " + Hex.u1(opcode));
            }
        }
        this.storeResults(frame);
    }
}

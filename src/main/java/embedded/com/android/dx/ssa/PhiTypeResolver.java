package embedded.com.android.dx.ssa;

import java.util.*;
import embedded.com.android.dx.cf.code.*;
import embedded.com.android.dx.rop.type.*;
import embedded.com.android.dx.rop.code.*;

public class PhiTypeResolver
{
    SsaMethod ssaMeth;
    private final BitSet worklist;
    
    public static void process(final SsaMethod ssaMeth) {
        new PhiTypeResolver(ssaMeth).run();
    }
    
    private PhiTypeResolver(final SsaMethod ssaMeth) {
        this.ssaMeth = ssaMeth;
        this.worklist = new BitSet(ssaMeth.getRegCount());
    }
    
    private void run() {
        for (int regCount = this.ssaMeth.getRegCount(), reg = 0; reg < regCount; ++reg) {
            final SsaInsn definsn = this.ssaMeth.getDefinitionForRegister(reg);
            if (definsn != null && definsn.getResult().getBasicType() == 0) {
                this.worklist.set(reg);
            }
        }
        int reg;
        while (0 <= (reg = this.worklist.nextSetBit(0))) {
            this.worklist.clear(reg);
            final PhiInsn definsn2 = (PhiInsn)this.ssaMeth.getDefinitionForRegister(reg);
            if (this.resolveResultType(definsn2)) {
                final List<SsaInsn> useList = this.ssaMeth.getUseListForRegister(reg);
                for (int sz = useList.size(), i = 0; i < sz; ++i) {
                    final SsaInsn useInsn = useList.get(i);
                    final RegisterSpec resultReg = useInsn.getResult();
                    if (resultReg != null && useInsn instanceof PhiInsn) {
                        this.worklist.set(resultReg.getReg());
                    }
                }
            }
        }
    }
    
    private static boolean equalsHandlesNulls(final LocalItem a, final LocalItem b) {
        return a == b || (a != null && a.equals(b));
    }
    
    boolean resolveResultType(final PhiInsn insn) {
        insn.updateSourcesToDefinitions(this.ssaMeth);
        final RegisterSpecList sources = insn.getSources();
        RegisterSpec first = null;
        int firstIndex = -1;
        final int szSources = sources.size();
        for (int i = 0; i < szSources; ++i) {
            final RegisterSpec rs = sources.get(i);
            if (rs.getBasicType() != 0) {
                first = rs;
                firstIndex = i;
            }
        }
        if (first == null) {
            return false;
        }
        final LocalItem firstLocal = first.getLocalItem();
        TypeBearer mergedType = first.getType();
        boolean sameLocals = true;
        for (int j = 0; j < szSources; ++j) {
            if (j != firstIndex) {
                final RegisterSpec rs2 = sources.get(j);
                if (rs2.getBasicType() != 0) {
                    sameLocals = (sameLocals && equalsHandlesNulls(firstLocal, rs2.getLocalItem()));
                    mergedType = Merger.mergeType(mergedType, rs2.getType());
                }
            }
        }
        if (mergedType == null) {
            final StringBuilder sb = new StringBuilder();
            for (int k = 0; k < szSources; ++k) {
                sb.append(sources.get(k).toString());
                sb.append(' ');
            }
            throw new RuntimeException("Couldn't map types in phi insn:" + (Object)sb);
        }
        final TypeBearer newResultType = mergedType;
        final LocalItem newLocal = sameLocals ? firstLocal : null;
        final RegisterSpec result = insn.getResult();
        if (result.getTypeBearer() == newResultType && equalsHandlesNulls(newLocal, result.getLocalItem())) {
            return false;
        }
        insn.changeResultType(newResultType, newLocal);
        return true;
    }
}

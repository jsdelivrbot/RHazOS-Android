package embedded.com.android.dx.ssa;

import java.util.*;
import embedded.com.android.dx.rop.code.*;
import embedded.com.android.dx.rop.cst.*;

public class MoveParamCombiner
{
    private final SsaMethod ssaMeth;
    
    public static void process(final SsaMethod ssaMethod) {
        new MoveParamCombiner(ssaMethod).run();
    }
    
    private MoveParamCombiner(final SsaMethod ssaMeth) {
        this.ssaMeth = ssaMeth;
    }
    
    private void run() {
        final RegisterSpec[] paramSpecs = new RegisterSpec[this.ssaMeth.getParamWidth()];
        final HashSet<SsaInsn> deletedInsns = new HashSet<SsaInsn>();
        this.ssaMeth.forEachInsn(new SsaInsn.Visitor() {
            @Override
            public void visitMoveInsn(final NormalSsaInsn insn) {
            }
            
            @Override
            public void visitPhiInsn(final PhiInsn phi) {
            }
            
            @Override
            public void visitNonMoveInsn(final NormalSsaInsn insn) {
                if (insn.getOpcode().getOpcode() != 3) {
                    return;
                }
                final int param = MoveParamCombiner.this.getParamIndex(insn);
                if (paramSpecs[param] == null) {
                    paramSpecs[param] = insn.getResult();
                }
                else {
                    final RegisterSpec specA = paramSpecs[param];
                    final RegisterSpec specB = insn.getResult();
                    final LocalItem localA = specA.getLocalItem();
                    final LocalItem localB = specB.getLocalItem();
                    LocalItem newLocal;
                    if (localA == null) {
                        newLocal = localB;
                    }
                    else if (localB == null) {
                        newLocal = localA;
                    }
                    else {
                        if (!localA.equals(localB)) {
                            return;
                        }
                        newLocal = localA;
                    }
                    MoveParamCombiner.this.ssaMeth.getDefinitionForRegister(specA.getReg()).setResultLocal(newLocal);
                    final RegisterMapper mapper = new RegisterMapper() {
                        @Override
                        public int getNewRegisterCount() {
                            return MoveParamCombiner.this.ssaMeth.getRegCount();
                        }
                        
                        @Override
                        public RegisterSpec map(final RegisterSpec registerSpec) {
                            if (registerSpec.getReg() == specB.getReg()) {
                                return specA;
                            }
                            return registerSpec;
                        }
                    };
                    final List<SsaInsn> uses = MoveParamCombiner.this.ssaMeth.getUseListForRegister(specB.getReg());
                    for (int i = uses.size() - 1; i >= 0; --i) {
                        final SsaInsn use = uses.get(i);
                        use.mapSourceRegisters(mapper);
                    }
                    deletedInsns.add(insn);
                }
            }
        });
        this.ssaMeth.deleteInsns(deletedInsns);
    }
    
    private int getParamIndex(final NormalSsaInsn insn) {
        final CstInsn cstInsn = (CstInsn)insn.getOriginalRopInsn();
        final int param = ((CstInteger)cstInsn.getConstant()).getValue();
        return param;
    }
}

package embedded.com.android.dx.ssa;

import embedded.com.android.dx.rop.cst.*;
import embedded.com.android.dx.rop.type.*;
import embedded.com.android.dx.rop.code.*;
import java.util.*;

public class LiteralOpUpgrader
{
    private final SsaMethod ssaMeth;
    
    public static void process(final SsaMethod ssaMethod) {
        final LiteralOpUpgrader dc = new LiteralOpUpgrader(ssaMethod);
        dc.run();
    }
    
    private LiteralOpUpgrader(final SsaMethod ssaMethod) {
        this.ssaMeth = ssaMethod;
    }
    
    private static boolean isConstIntZeroOrKnownNull(final RegisterSpec spec) {
        final TypeBearer tb = spec.getTypeBearer();
        if (tb instanceof CstLiteralBits) {
            final CstLiteralBits clb = (CstLiteralBits)tb;
            return clb.getLongBits() == 0L;
        }
        return false;
    }
    
    private void run() {
        final TranslationAdvice advice = Optimizer.getAdvice();
        this.ssaMeth.forEachInsn(new SsaInsn.Visitor() {
            @Override
            public void visitMoveInsn(final NormalSsaInsn insn) {
            }
            
            @Override
            public void visitPhiInsn(final PhiInsn insn) {
            }
            
            @Override
            public void visitNonMoveInsn(final NormalSsaInsn insn) {
                final Insn originalRopInsn = insn.getOriginalRopInsn();
                final Rop opcode = originalRopInsn.getOpcode();
                final RegisterSpecList sources = insn.getSources();
                if (LiteralOpUpgrader.this.tryReplacingWithConstant(insn)) {
                    return;
                }
                if (sources.size() != 2) {
                    return;
                }
                if (opcode.getBranchingness() == 4) {
                    if (isConstIntZeroOrKnownNull(sources.get(0))) {
                        LiteralOpUpgrader.this.replacePlainInsn(insn, sources.withoutFirst(), RegOps.flippedIfOpcode(opcode.getOpcode()), null);
                    }
                    else if (isConstIntZeroOrKnownNull(sources.get(1))) {
                        LiteralOpUpgrader.this.replacePlainInsn(insn, sources.withoutLast(), opcode.getOpcode(), null);
                    }
                }
                else if (advice.hasConstantOperation(opcode, sources.get(0), sources.get(1))) {
                    insn.upgradeToLiteral();
                }
                else if (opcode.isCommutative() && advice.hasConstantOperation(opcode, sources.get(1), sources.get(0))) {
                    insn.setNewSources(RegisterSpecList.make(sources.get(1), sources.get(0)));
                    insn.upgradeToLiteral();
                }
            }
        });
    }
    
    private boolean tryReplacingWithConstant(final NormalSsaInsn insn) {
        final Insn originalRopInsn = insn.getOriginalRopInsn();
        final Rop opcode = originalRopInsn.getOpcode();
        final RegisterSpec result = insn.getResult();
        if (result != null && !this.ssaMeth.isRegALocal(result) && opcode.getOpcode() != 5) {
            final TypeBearer type = insn.getResult().getTypeBearer();
            if (type.isConstant() && type.getBasicType() == 6) {
                this.replacePlainInsn(insn, RegisterSpecList.EMPTY, 5, (Constant)type);
                if (opcode.getOpcode() == 56) {
                    final int pred = insn.getBlock().getPredecessors().nextSetBit(0);
                    final ArrayList<SsaInsn> predInsns = this.ssaMeth.getBlocks().get(pred).getInsns();
                    final NormalSsaInsn sourceInsn = (NormalSsaInsn) predInsns.get(predInsns.size() - 1);
                    this.replacePlainInsn(sourceInsn, RegisterSpecList.EMPTY, 6, null);
                }
                return true;
            }
        }
        return false;
    }
    
    private void replacePlainInsn(final NormalSsaInsn insn, final RegisterSpecList newSources, final int newOpcode, final Constant cst) {
        final Insn originalRopInsn = insn.getOriginalRopInsn();
        final Rop newRop = Rops.ropFor(newOpcode, insn.getResult(), newSources, cst);
        Insn newRopInsn;
        if (cst == null) {
            newRopInsn = new PlainInsn(newRop, originalRopInsn.getPosition(), insn.getResult(), newSources);
        }
        else {
            newRopInsn = new PlainCstInsn(newRop, originalRopInsn.getPosition(), insn.getResult(), newSources, cst);
        }
        final NormalSsaInsn newInsn = new NormalSsaInsn(newRopInsn, insn.getBlock());
        final List<SsaInsn> insns = insn.getBlock().getInsns();
        this.ssaMeth.onInsnRemoved(insn);
        insns.set(insns.lastIndexOf(insn), newInsn);
        this.ssaMeth.onInsnAdded(newInsn);
    }
}

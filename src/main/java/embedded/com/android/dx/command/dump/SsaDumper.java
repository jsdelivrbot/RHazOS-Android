package embedded.com.android.dx.command.dump;

import java.io.*;
import embedded.com.android.dx.cf.iface.*;
import embedded.com.android.dx.cf.code.*;
import embedded.com.android.dx.rop.code.*;
import embedded.com.android.dx.ssa.*;
import java.util.*;
import embedded.com.android.dx.util.*;

public class SsaDumper extends BlockDumper
{
    public static void dump(final byte[] bytes, final PrintStream out, final String filePath, final Args args) {
        final SsaDumper sd = new SsaDumper(bytes, out, filePath, args);
        sd.dump();
    }
    
    private SsaDumper(final byte[] bytes, final PrintStream out, final String filePath, final Args args) {
        super(bytes, out, filePath, true, args);
    }
    
    @Override
    public void endParsingMember(final ByteArray bytes, final int offset, final String name, final String descriptor, final Member member) {
        if (!(member instanceof Method)) {
            return;
        }
        if (!this.shouldDumpMethod(name)) {
            return;
        }
        if ((member.getAccessFlags() & 0x500) != 0x0) {
            return;
        }
        final ConcreteMethod meth = new ConcreteMethod((Method)member, this.classFile, true, true);
        final TranslationAdvice advice = DexTranslationAdvice.THE_ONE;
        final RopMethod rmeth = Ropper.convert(meth, advice, this.classFile.getMethods(), this.dexOptions);
        SsaMethod ssaMeth = null;
        final boolean isStatic = AccessFlags.isStatic(meth.getAccessFlags());
        final int paramWidth = BaseDumper.computeParamWidth(meth, isStatic);
        if (this.args.ssaStep == null) {
            ssaMeth = Optimizer.debugNoRegisterAllocation(rmeth, paramWidth, isStatic, true, advice, EnumSet.allOf(Optimizer.OptionalStep.class));
        }
        else if ("edge-split".equals(this.args.ssaStep)) {
            ssaMeth = Optimizer.debugEdgeSplit(rmeth, paramWidth, isStatic, true, advice);
        }
        else if ("phi-placement".equals(this.args.ssaStep)) {
            ssaMeth = Optimizer.debugPhiPlacement(rmeth, paramWidth, isStatic, true, advice);
        }
        else if ("renaming".equals(this.args.ssaStep)) {
            ssaMeth = Optimizer.debugRenaming(rmeth, paramWidth, isStatic, true, advice);
        }
        else if ("dead-code".equals(this.args.ssaStep)) {
            ssaMeth = Optimizer.debugDeadCodeRemover(rmeth, paramWidth, isStatic, true, advice);
        }
        final StringBuffer sb = new StringBuffer(2000);
        sb.append("first ");
        sb.append(Hex.u2(ssaMeth.blockIndexToRopLabel(ssaMeth.getEntryBlockIndex())));
        sb.append('\n');
        final ArrayList<SsaBasicBlock> blocks = ssaMeth.getBlocks();
        final ArrayList<SsaBasicBlock> sortedBlocks = (ArrayList<SsaBasicBlock>)blocks.clone();
        Collections.sort(sortedBlocks, SsaBasicBlock.LABEL_COMPARATOR);
        for (final SsaBasicBlock block : sortedBlocks) {
            sb.append("block ").append(Hex.u2(block.getRopLabel())).append('\n');
            final BitSet preds = block.getPredecessors();
            for (int i = preds.nextSetBit(0); i >= 0; i = preds.nextSetBit(i + 1)) {
                sb.append("  pred ");
                sb.append(Hex.u2(ssaMeth.blockIndexToRopLabel(i)));
                sb.append('\n');
            }
            sb.append("  live in:" + block.getLiveInRegs());
            sb.append("\n");
            for (final SsaInsn insn : block.getInsns()) {
                sb.append("  ");
                sb.append(insn.toHuman());
                sb.append('\n');
            }
            if (block.getSuccessors().cardinality() == 0) {
                sb.append("  returns\n");
            }
            else {
                final int primary = block.getPrimarySuccessorRopLabel();
                final IntList succLabelList = block.getRopLabelSuccessorList();
                for (int szSuccLabels = succLabelList.size(), j = 0; j < szSuccLabels; ++j) {
                    sb.append("  next ");
                    sb.append(Hex.u2(succLabelList.get(j)));
                    if (szSuccLabels != 1 && primary == succLabelList.get(j)) {
                        sb.append(" *");
                    }
                    sb.append('\n');
                }
            }
            sb.append("  live out:" + block.getLiveOutRegs());
            sb.append("\n");
        }
        this.suppressDump = false;
        this.setAt(bytes, 0);
        this.parsed(bytes, 0, bytes.size(), sb.toString());
        this.suppressDump = true;
    }
}

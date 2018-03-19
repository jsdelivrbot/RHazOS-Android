package embedded.com.android.dx.command.dump;

import java.io.*;
import embedded.com.android.dx.cf.iface.*;
import embedded.com.android.dx.cf.direct.*;
import embedded.com.android.dx.rop.cst.*;
import embedded.com.android.dx.util.*;
import embedded.com.android.dx.cf.code.*;
import embedded.com.android.dx.ssa.*;
import embedded.com.android.dx.rop.code.*;

public class BlockDumper extends BaseDumper
{
    private boolean rop;
    protected DirectClassFile classFile;
    protected boolean suppressDump;
    private boolean first;
    private boolean optimize;
    
    public static void dump(final byte[] bytes, final PrintStream out, final String filePath, final boolean rop, final Args args) {
        final BlockDumper bd = new BlockDumper(bytes, out, filePath, rop, args);
        bd.dump();
    }
    
    BlockDumper(final byte[] bytes, final PrintStream out, final String filePath, final boolean rop, final Args args) {
        super(bytes, out, filePath, args);
        this.rop = rop;
        this.classFile = null;
        this.suppressDump = true;
        this.first = true;
        this.optimize = args.optimize;
    }
    
    public void dump() {
        final byte[] bytes = this.getBytes();
        final ByteArray ba = new ByteArray(bytes);
        (this.classFile = new DirectClassFile(ba, this.getFilePath(), this.getStrictParse())).setAttributeFactory(StdAttributeFactory.THE_ONE);
        this.classFile.getMagic();
        final DirectClassFile liveCf = new DirectClassFile(ba, this.getFilePath(), this.getStrictParse());
        liveCf.setAttributeFactory(StdAttributeFactory.THE_ONE);
        liveCf.setObserver(this);
        liveCf.getMagic();
    }
    
    @Override
    public void changeIndent(final int indentDelta) {
        if (!this.suppressDump) {
            super.changeIndent(indentDelta);
        }
    }
    
    @Override
    public void parsed(final ByteArray bytes, final int offset, final int len, final String human) {
        if (!this.suppressDump) {
            super.parsed(bytes, offset, len, human);
        }
    }
    
    protected boolean shouldDumpMethod(final String name) {
        return this.args.method == null || this.args.method.equals(name);
    }
    
    @Override
    public void startParsingMember(final ByteArray bytes, final int offset, final String name, final String descriptor) {
        if (descriptor.indexOf(40) < 0) {
            return;
        }
        if (!this.shouldDumpMethod(name)) {
            return;
        }
        this.setAt(bytes, offset);
        this.suppressDump = false;
        if (this.first) {
            this.first = false;
        }
        else {
            this.parsed(bytes, offset, 0, "\n");
        }
        this.parsed(bytes, offset, 0, "method " + name + " " + descriptor);
        this.suppressDump = true;
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
        if (this.rop) {
            this.ropDump(meth);
        }
        else {
            this.regularDump(meth);
        }
    }
    
    private void regularDump(final ConcreteMethod meth) {
        final BytecodeArray code = meth.getCode();
        final ByteArray bytes = code.getBytes();
        final ByteBlockList list = BasicBlocker.identifyBlocks(meth);
        final int sz = list.size();
        final CodeObserver codeObserver = new CodeObserver(bytes, this);
        this.setAt(bytes, 0);
        this.suppressDump = false;
        int byteAt = 0;
        for (int i = 0; i < sz; ++i) {
            final ByteBlock bb = list.get(i);
            final int start = bb.getStart();
            final int end = bb.getEnd();
            if (byteAt < start) {
                this.parsed(bytes, byteAt, start - byteAt, "dead code " + Hex.u2(byteAt) + ".." + Hex.u2(start));
            }
            this.parsed(bytes, start, 0, "block " + Hex.u2(bb.getLabel()) + ": " + Hex.u2(start) + ".." + Hex.u2(end));
            this.changeIndent(1);
            int len;
            for (int j = start; j < end; j += len) {
                len = code.parseInstruction(j, codeObserver);
                codeObserver.setPreviousOffset(j);
            }
            final IntList successors = bb.getSuccessors();
            final int ssz = successors.size();
            if (ssz == 0) {
                this.parsed(bytes, end, 0, "returns");
            }
            else {
                for (int k = 0; k < ssz; ++k) {
                    final int succ = successors.get(k);
                    this.parsed(bytes, end, 0, "next " + Hex.u2(succ));
                }
            }
            final ByteCatchList catches = bb.getCatches();
            for (int csz = catches.size(), l = 0; l < csz; ++l) {
                final ByteCatchList.Item one = catches.get(l);
                final CstType exceptionClass = one.getExceptionClass();
                this.parsed(bytes, end, 0, "catch " + ((exceptionClass == CstType.OBJECT) ? "<any>" : exceptionClass.toHuman()) + " -> " + Hex.u2(one.getHandlerPc()));
            }
            this.changeIndent(-1);
            byteAt = end;
        }
        final int end2 = bytes.size();
        if (byteAt < end2) {
            this.parsed(bytes, byteAt, end2 - byteAt, "dead code " + Hex.u2(byteAt) + ".." + Hex.u2(end2));
        }
        this.suppressDump = true;
    }
    
    private void ropDump(final ConcreteMethod meth) {
        final TranslationAdvice advice = DexTranslationAdvice.THE_ONE;
        final BytecodeArray code = meth.getCode();
        final ByteArray bytes = code.getBytes();
        RopMethod rmeth = Ropper.convert(meth, advice, this.classFile.getMethods(), this.dexOptions);
        final StringBuffer sb = new StringBuffer(2000);
        if (this.optimize) {
            final boolean isStatic = AccessFlags.isStatic(meth.getAccessFlags());
            final int paramWidth = BaseDumper.computeParamWidth(meth, isStatic);
            rmeth = Optimizer.optimize(rmeth, paramWidth, isStatic, true, advice);
        }
        final BasicBlockList blocks = rmeth.getBlocks();
        final int[] order = blocks.getLabelsInOrder();
        sb.append("first " + Hex.u2(rmeth.getFirstLabel()) + "\n");
        for (final int label : order) {
            final BasicBlock bb = blocks.get(blocks.indexOfLabel(label));
            sb.append("block ");
            sb.append(Hex.u2(label));
            sb.append("\n");
            final IntList preds = rmeth.labelToPredecessors(label);
            for (int psz = preds.size(), i = 0; i < psz; ++i) {
                sb.append("  pred ");
                sb.append(Hex.u2(preds.get(i)));
                sb.append("\n");
            }
            final InsnList il = bb.getInsns();
            for (int ilsz = il.size(), j = 0; j < ilsz; ++j) {
                final Insn one = il.get(j);
                sb.append("  ");
                sb.append(il.get(j).toHuman());
                sb.append("\n");
            }
            final IntList successors = bb.getSuccessors();
            final int ssz = successors.size();
            if (ssz == 0) {
                sb.append("  returns\n");
            }
            else {
                final int primary = bb.getPrimarySuccessor();
                for (int k = 0; k < ssz; ++k) {
                    final int succ = successors.get(k);
                    sb.append("  next ");
                    sb.append(Hex.u2(succ));
                    if (ssz != 1 && succ == primary) {
                        sb.append(" *");
                    }
                    sb.append("\n");
                }
            }
        }
        this.suppressDump = false;
        this.setAt(bytes, 0);
        this.parsed(bytes, 0, bytes.size(), sb.toString());
        this.suppressDump = true;
    }
}

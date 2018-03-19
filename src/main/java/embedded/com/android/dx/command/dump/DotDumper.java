package embedded.com.android.dx.command.dump;

import embedded.com.android.dx.dex.*;
import embedded.com.android.dx.cf.direct.*;
import embedded.com.android.dx.cf.iface.*;
import embedded.com.android.dx.cf.code.*;
import embedded.com.android.dx.ssa.*;
import embedded.com.android.dx.rop.code.*;
import embedded.com.android.dx.util.*;

public class DotDumper implements ParseObserver
{
    private DirectClassFile classFile;
    private final byte[] bytes;
    private final String filePath;
    private final boolean strictParse;
    private final boolean optimize;
    private final Args args;
    private final DexOptions dexOptions;
    
    static void dump(final byte[] bytes, final String filePath, final Args args) {
        new DotDumper(bytes, filePath, args).run();
    }
    
    DotDumper(final byte[] bytes, final String filePath, final Args args) {
        this.bytes = bytes;
        this.filePath = filePath;
        this.strictParse = args.strictParse;
        this.optimize = args.optimize;
        this.args = args;
        this.dexOptions = new DexOptions();
    }
    
    private void run() {
        final ByteArray ba = new ByteArray(this.bytes);
        (this.classFile = new DirectClassFile(ba, this.filePath, this.strictParse)).setAttributeFactory(StdAttributeFactory.THE_ONE);
        this.classFile.getMagic();
        final DirectClassFile liveCf = new DirectClassFile(ba, this.filePath, this.strictParse);
        liveCf.setAttributeFactory(StdAttributeFactory.THE_ONE);
        liveCf.setObserver(this);
        liveCf.getMagic();
    }
    
    protected boolean shouldDumpMethod(final String name) {
        return this.args.method == null || this.args.method.equals(name);
    }
    
    @Override
    public void changeIndent(final int indentDelta) {
    }
    
    @Override
    public void parsed(final ByteArray bytes, final int offset, final int len, final String human) {
    }
    
    @Override
    public void startParsingMember(final ByteArray bytes, final int offset, final String name, final String descriptor) {
    }
    
    @Override
    public void endParsingMember(final ByteArray bytes, final int offset, final String name, final String descriptor, final Member member) {
        if (!(member instanceof Method)) {
            return;
        }
        if (!this.shouldDumpMethod(name)) {
            return;
        }
        final ConcreteMethod meth = new ConcreteMethod((Method)member, this.classFile, true, true);
        final TranslationAdvice advice = DexTranslationAdvice.THE_ONE;
        RopMethod rmeth = Ropper.convert(meth, advice, this.classFile.getMethods(), this.dexOptions);
        if (this.optimize) {
            final boolean isStatic = AccessFlags.isStatic(meth.getAccessFlags());
            rmeth = Optimizer.optimize(rmeth, BaseDumper.computeParamWidth(meth, isStatic), isStatic, true, advice);
        }
        System.out.println("digraph " + name + "{");
        System.out.println("\tfirst -> n" + Hex.u2(rmeth.getFirstLabel()) + ";");
        final BasicBlockList blocks = rmeth.getBlocks();
        for (int sz = blocks.size(), i = 0; i < sz; ++i) {
            final BasicBlock bb = blocks.get(i);
            final int label = bb.getLabel();
            final IntList successors = bb.getSuccessors();
            if (successors.size() == 0) {
                System.out.println("\tn" + Hex.u2(label) + " -> returns;");
            }
            else if (successors.size() == 1) {
                System.out.println("\tn" + Hex.u2(label) + " -> n" + Hex.u2(successors.get(0)) + ";");
            }
            else {
                System.out.print("\tn" + Hex.u2(label) + " -> {");
                for (int j = 0; j < successors.size(); ++j) {
                    final int successor = successors.get(j);
                    if (successor != bb.getPrimarySuccessor()) {
                        System.out.print(" n" + Hex.u2(successor) + " ");
                    }
                }
                System.out.println("};");
                System.out.println("\tn" + Hex.u2(label) + " -> n" + Hex.u2(bb.getPrimarySuccessor()) + " [label=\"primary\"];");
            }
        }
        System.out.println("}");
    }
}

package embedded.com.android.dx.ssa;

import java.util.*;
import embedded.com.android.dx.util.*;

public class DomFront
{
    private static final boolean DEBUG = false;
    private final SsaMethod meth;
    private final ArrayList<SsaBasicBlock> nodes;
    private final DomInfo[] domInfos;
    
    public DomFront(final SsaMethod meth) {
        this.meth = meth;
        this.nodes = meth.getBlocks();
        final int szNodes = this.nodes.size();
        this.domInfos = new DomInfo[szNodes];
        for (int i = 0; i < szNodes; ++i) {
            this.domInfos[i] = new DomInfo();
        }
    }
    
    public DomInfo[] run() {
        final int szNodes = this.nodes.size();
        final Dominators methDom = Dominators.make(this.meth, this.domInfos, false);
        this.buildDomTree();
        for (int i = 0; i < szNodes; ++i) {
            this.domInfos[i].dominanceFrontiers = SetFactory.makeDomFrontSet(szNodes);
        }
        this.calcDomFronts();
        return this.domInfos;
    }
    
    private void debugPrintDomChildren() {
        for (int szNodes = this.nodes.size(), i = 0; i < szNodes; ++i) {
            final SsaBasicBlock node = this.nodes.get(i);
            final StringBuffer sb = new StringBuffer();
            sb.append('{');
            boolean comma = false;
            for (final SsaBasicBlock child : node.getDomChildren()) {
                if (comma) {
                    sb.append(',');
                }
                sb.append(child);
                comma = true;
            }
            sb.append('}');
            System.out.println("domChildren[" + node + "]: " + (Object)sb);
        }
    }
    
    private void buildDomTree() {
        for (int szNodes = this.nodes.size(), i = 0; i < szNodes; ++i) {
            final DomInfo info = this.domInfos[i];
            if (info.idom != -1) {
                final SsaBasicBlock domParent = this.nodes.get(info.idom);
                domParent.addDomChild(this.nodes.get(i));
            }
        }
    }
    
    private void calcDomFronts() {
        for (int szNodes = this.nodes.size(), b = 0; b < szNodes; ++b) {
            final SsaBasicBlock nb = this.nodes.get(b);
            final DomInfo nbInfo = this.domInfos[b];
            final BitSet pred = nb.getPredecessors();
            if (pred.cardinality() > 1) {
                for (int i = pred.nextSetBit(0); i >= 0; i = pred.nextSetBit(i + 1)) {
                    DomInfo runnerInfo;
                    for (int runnerIndex = i; runnerIndex != nbInfo.idom; runnerIndex = runnerInfo.idom) {
                        if (runnerIndex == -1) {
                            break;
                        }
                        runnerInfo = this.domInfos[runnerIndex];
                        if (runnerInfo.dominanceFrontiers.has(b)) {
                            break;
                        }
                        runnerInfo.dominanceFrontiers.add(b);
                    }
                }
            }
        }
    }
    
    public static class DomInfo
    {
        public IntSet dominanceFrontiers;
        public int idom;
        
        public DomInfo() {
            this.idom = -1;
        }
    }
}

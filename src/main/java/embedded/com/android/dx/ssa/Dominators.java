package embedded.com.android.dx.ssa;

import java.util.*;

public final class Dominators
{
    private final boolean postdom;
    private final SsaMethod meth;
    private final ArrayList<SsaBasicBlock> blocks;
    private final DFSInfo[] info;
    private final ArrayList<SsaBasicBlock> vertex;
    private final DomFront.DomInfo[] domInfos;
    
    private Dominators(final SsaMethod meth, final DomFront.DomInfo[] domInfos, final boolean postdom) {
        this.meth = meth;
        this.domInfos = domInfos;
        this.postdom = postdom;
        this.blocks = meth.getBlocks();
        this.info = new DFSInfo[this.blocks.size() + 2];
        this.vertex = new ArrayList<SsaBasicBlock>();
    }
    
    public static Dominators make(final SsaMethod meth, final DomFront.DomInfo[] domInfos, final boolean postdom) {
        final Dominators result = new Dominators(meth, domInfos, postdom);
        result.run();
        return result;
    }
    
    private BitSet getSuccs(final SsaBasicBlock block) {
        if (this.postdom) {
            return block.getPredecessors();
        }
        return block.getSuccessors();
    }
    
    private BitSet getPreds(final SsaBasicBlock block) {
        if (this.postdom) {
            return block.getSuccessors();
        }
        return block.getPredecessors();
    }
    
    private void compress(final SsaBasicBlock in) {
        final DFSInfo bbInfo = this.info[in.getIndex()];
        final DFSInfo ancestorbbInfo = this.info[bbInfo.ancestor.getIndex()];
        if (ancestorbbInfo.ancestor != null) {
            final ArrayList<SsaBasicBlock> worklist = new ArrayList<SsaBasicBlock>();
            final HashSet<SsaBasicBlock> visited = new HashSet<SsaBasicBlock>();
            worklist.add(in);
            while (!worklist.isEmpty()) {
                final int wsize = worklist.size();
                final SsaBasicBlock v = worklist.get(wsize - 1);
                final DFSInfo vbbInfo = this.info[v.getIndex()];
                final SsaBasicBlock vAncestor = vbbInfo.ancestor;
                final DFSInfo vabbInfo = this.info[vAncestor.getIndex()];
                if (visited.add(vAncestor) && vabbInfo.ancestor != null) {
                    worklist.add(vAncestor);
                }
                else {
                    worklist.remove(wsize - 1);
                    if (vabbInfo.ancestor == null) {
                        continue;
                    }
                    final SsaBasicBlock vAncestorRep = vabbInfo.rep;
                    final SsaBasicBlock vRep = vbbInfo.rep;
                    if (this.info[vAncestorRep.getIndex()].semidom < this.info[vRep.getIndex()].semidom) {
                        vbbInfo.rep = vAncestorRep;
                    }
                    vbbInfo.ancestor = vabbInfo.ancestor;
                }
            }
        }
    }
    
    private SsaBasicBlock eval(final SsaBasicBlock v) {
        final DFSInfo bbInfo = this.info[v.getIndex()];
        if (bbInfo.ancestor == null) {
            return v;
        }
        this.compress(v);
        return bbInfo.rep;
    }
    
    private void run() {
        final SsaBasicBlock root = this.postdom ? this.meth.getExitBlock() : this.meth.getEntryBlock();
        if (root != null) {
            this.vertex.add(root);
            this.domInfos[root.getIndex()].idom = root.getIndex();
        }
        final DfsWalker walker = new DfsWalker();
        this.meth.forEachBlockDepthFirst(this.postdom, walker);
        int i;
        int dfsMax;
        for (dfsMax = (i = this.vertex.size() - 1); i >= 2; --i) {
            final SsaBasicBlock w = this.vertex.get(i);
            final DFSInfo wInfo = this.info[w.getIndex()];
            final BitSet preds = this.getPreds(w);
            for (int j = preds.nextSetBit(0); j >= 0; j = preds.nextSetBit(j + 1)) {
                final SsaBasicBlock predBlock = this.blocks.get(j);
                final DFSInfo predInfo = this.info[predBlock.getIndex()];
                if (predInfo != null) {
                    final int predSemidom = this.info[this.eval(predBlock).getIndex()].semidom;
                    if (predSemidom < wInfo.semidom) {
                        wInfo.semidom = predSemidom;
                    }
                }
            }
            this.info[this.vertex.get(wInfo.semidom).getIndex()].bucket.add(w);
            wInfo.ancestor = wInfo.parent;
            final ArrayList<SsaBasicBlock> wParentBucket = this.info[wInfo.parent.getIndex()].bucket;
            while (!wParentBucket.isEmpty()) {
                final int lastItem = wParentBucket.size() - 1;
                final SsaBasicBlock last = wParentBucket.remove(lastItem);
                final SsaBasicBlock U = this.eval(last);
                if (this.info[U.getIndex()].semidom < this.info[last.getIndex()].semidom) {
                    this.domInfos[last.getIndex()].idom = U.getIndex();
                }
                else {
                    this.domInfos[last.getIndex()].idom = wInfo.parent.getIndex();
                }
            }
        }
        for (i = 2; i <= dfsMax; ++i) {
            final SsaBasicBlock w = this.vertex.get(i);
            if (this.domInfos[w.getIndex()].idom != this.vertex.get(this.info[w.getIndex()].semidom).getIndex()) {
                this.domInfos[w.getIndex()].idom = this.domInfos[this.domInfos[w.getIndex()].idom].idom;
            }
        }
    }
    
    private class DfsWalker implements SsaBasicBlock.Visitor
    {
        private int dfsNum;
        
        private DfsWalker() {
            this.dfsNum = 0;
        }
        
        @Override
        public void visitBlock(final SsaBasicBlock v, final SsaBasicBlock parent) {
            final DFSInfo bbInfo = new DFSInfo();
            bbInfo.semidom = ++this.dfsNum;
            bbInfo.rep = v;
            bbInfo.parent = parent;
            Dominators.this.vertex.add(v);
            Dominators.this.info[v.getIndex()] = bbInfo;
        }
    }
    
    private static final class DFSInfo
    {
        public int semidom;
        public SsaBasicBlock parent;
        public SsaBasicBlock rep;
        public SsaBasicBlock ancestor;
        public ArrayList<SsaBasicBlock> bucket;
        
        public DFSInfo() {
            this.bucket = new ArrayList<SsaBasicBlock>();
        }
    }
}

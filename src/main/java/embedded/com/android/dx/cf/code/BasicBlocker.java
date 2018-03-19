package embedded.com.android.dx.cf.code;

import embedded.com.android.dx.util.*;
import embedded.com.android.dx.rop.type.*;
import embedded.com.android.dx.rop.cst.*;
import java.util.*;

public final class BasicBlocker implements BytecodeArray.Visitor
{
    private final ConcreteMethod method;
    private final int[] workSet;
    private final int[] liveSet;
    private final int[] blockSet;
    private final IntList[] targetLists;
    private final ByteCatchList[] catchLists;
    private int previousOffset;
    
    public static ByteBlockList identifyBlocks(final ConcreteMethod method) {
        final BasicBlocker bb = new BasicBlocker(method);
        bb.doit();
        return bb.getBlockList();
    }
    
    private BasicBlocker(final ConcreteMethod method) {
        if (method == null) {
            throw new NullPointerException("method == null");
        }
        this.method = method;
        final int sz = method.getCode().size() + 1;
        this.workSet = Bits.makeBitSet(sz);
        this.liveSet = Bits.makeBitSet(sz);
        this.blockSet = Bits.makeBitSet(sz);
        this.targetLists = new IntList[sz];
        this.catchLists = new ByteCatchList[sz];
        this.previousOffset = -1;
    }
    
    @Override
    public void visitInvalid(final int opcode, final int offset, final int length) {
        this.visitCommon(offset, length, true);
    }
    
    @Override
    public void visitNoArgs(final int opcode, final int offset, final int length, final Type type) {
        switch (opcode) {
            case 172:
            case 177: {
                this.visitCommon(offset, length, false);
                this.targetLists[offset] = IntList.EMPTY;
                break;
            }
            case 191: {
                this.visitCommon(offset, length, false);
                this.visitThrowing(offset, length, false);
                break;
            }
            case 46:
            case 47:
            case 48:
            case 49:
            case 50:
            case 51:
            case 52:
            case 53:
            case 79:
            case 80:
            case 81:
            case 82:
            case 83:
            case 84:
            case 85:
            case 86:
            case 190:
            case 194:
            case 195: {
                this.visitCommon(offset, length, true);
                this.visitThrowing(offset, length, true);
                break;
            }
            case 108:
            case 112: {
                this.visitCommon(offset, length, true);
                if (type == Type.INT || type == Type.LONG) {
                    this.visitThrowing(offset, length, true);
                    break;
                }
                break;
            }
            default: {
                this.visitCommon(offset, length, true);
                break;
            }
        }
    }
    
    @Override
    public void visitLocal(final int opcode, final int offset, final int length, final int idx, final Type type, final int value) {
        if (opcode == 169) {
            this.visitCommon(offset, length, false);
            this.targetLists[offset] = IntList.EMPTY;
        }
        else {
            this.visitCommon(offset, length, true);
        }
    }
    
    @Override
    public void visitConstant(final int opcode, final int offset, final int length, final Constant cst, final int value) {
        this.visitCommon(offset, length, true);
        if (cst instanceof CstMemberRef || cst instanceof CstType || cst instanceof CstString || cst instanceof CstInvokeDynamic) {
            this.visitThrowing(offset, length, true);
        }
    }
    
    @Override
    public void visitBranch(final int opcode, final int offset, final int length, final int target) {
        Label_0090: {
            switch (opcode) {
                case 167: {
                    this.visitCommon(offset, length, false);
                    this.targetLists[offset] = IntList.makeImmutable(target);
                    break Label_0090;
                }
                case 168: {
                    this.addWorkIfNecessary(offset, true);
                    break;
                }
            }
            final int next = offset + length;
            this.visitCommon(offset, length, true);
            this.addWorkIfNecessary(next, true);
            this.targetLists[offset] = IntList.makeImmutable(next, target);
        }
        this.addWorkIfNecessary(target, true);
    }
    
    @Override
    public void visitSwitch(final int opcode, final int offset, final int length, final SwitchList cases, final int padding) {
        this.visitCommon(offset, length, false);
        this.addWorkIfNecessary(cases.getDefaultTarget(), true);
        for (int sz = cases.size(), i = 0; i < sz; ++i) {
            this.addWorkIfNecessary(cases.getTarget(i), true);
        }
        this.targetLists[offset] = cases.getTargets();
    }
    
    @Override
    public void visitNewarray(final int offset, final int length, final CstType type, final ArrayList<Constant> intVals) {
        this.visitCommon(offset, length, true);
        this.visitThrowing(offset, length, true);
    }
    
    private ByteBlockList getBlockList() {
        final BytecodeArray bytes = this.method.getCode();
        final ByteBlock[] bbs = new ByteBlock[bytes.size()];
        int count = 0;
        int at = 0;
        while (true) {
            final int next = Bits.findFirst(this.blockSet, at + 1);
            if (next < 0) {
                break;
            }
            if (Bits.get(this.liveSet, at)) {
                IntList targets = null;
                int targetsAt = -1;
                for (int i = next - 1; i >= at; --i) {
                    targets = this.targetLists[i];
                    if (targets != null) {
                        targetsAt = i;
                        break;
                    }
                }
                ByteCatchList blockCatches;
                if (targets == null) {
                    targets = IntList.makeImmutable(next);
                    blockCatches = ByteCatchList.EMPTY;
                }
                else {
                    blockCatches = this.catchLists[targetsAt];
                    if (blockCatches == null) {
                        blockCatches = ByteCatchList.EMPTY;
                    }
                }
                bbs[count] = new ByteBlock(at, at, next, targets, blockCatches);
                ++count;
            }
            at = next;
        }
        final ByteBlockList result = new ByteBlockList(count);
        for (int j = 0; j < count; ++j) {
            result.set(j, bbs[j]);
        }
        return result;
    }
    
    private void doit() {
        final BytecodeArray bytes = this.method.getCode();
        final ByteCatchList catches = this.method.getCatches();
        final int catchSz = catches.size();
        Bits.set(this.workSet, 0);
        Bits.set(this.blockSet, 0);
        while (!Bits.isEmpty(this.workSet)) {
            try {
                bytes.processWorkSet(this.workSet, this);
            }
            catch (IllegalArgumentException ex) {
                throw new SimException("flow of control falls off end of method", ex);
            }
            for (int i = 0; i < catchSz; ++i) {
                final ByteCatchList.Item item = catches.get(i);
                final int start = item.getStartPc();
                final int end = item.getEndPc();
                if (Bits.anyInRange(this.liveSet, start, end)) {
                    Bits.set(this.blockSet, start);
                    Bits.set(this.blockSet, end);
                    this.addWorkIfNecessary(item.getHandlerPc(), true);
                }
            }
        }
    }
    
    private void addWorkIfNecessary(final int offset, final boolean blockStart) {
        if (!Bits.get(this.liveSet, offset)) {
            Bits.set(this.workSet, offset);
        }
        if (blockStart) {
            Bits.set(this.blockSet, offset);
        }
    }
    
    private void visitCommon(final int offset, final int length, final boolean nextIsLive) {
        Bits.set(this.liveSet, offset);
        if (nextIsLive) {
            this.addWorkIfNecessary(offset + length, false);
        }
        else {
            Bits.set(this.blockSet, offset + length);
        }
    }
    
    private void visitThrowing(final int offset, final int length, final boolean nextIsLive) {
        final int next = offset + length;
        if (nextIsLive) {
            this.addWorkIfNecessary(next, true);
        }
        final ByteCatchList catches = this.method.getCatches().listFor(offset);
        this.catchLists[offset] = catches;
        this.targetLists[offset] = catches.toTargetList(nextIsLive ? next : -1);
    }
    
    @Override
    public void setPreviousOffset(final int offset) {
        this.previousOffset = offset;
    }
    
    @Override
    public int getPreviousOffset() {
        return this.previousOffset;
    }
}

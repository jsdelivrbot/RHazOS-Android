package embedded.com.android.dx.dex.code;

import embedded.com.android.dx.rop.code.*;
import embedded.com.android.dx.rop.type.*;
import java.util.*;
import embedded.com.android.dx.rop.cst.*;
import embedded.com.android.dx.util.*;

public final class StdCatchBuilder implements CatchBuilder
{
    private static final int MAX_CATCH_RANGE = 65535;
    private final RopMethod method;
    private final int[] order;
    private final BlockAddresses addresses;
    
    public StdCatchBuilder(final RopMethod method, final int[] order, final BlockAddresses addresses) {
        if (method == null) {
            throw new NullPointerException("method == null");
        }
        if (order == null) {
            throw new NullPointerException("order == null");
        }
        if (addresses == null) {
            throw new NullPointerException("addresses == null");
        }
        this.method = method;
        this.order = order;
        this.addresses = addresses;
    }
    
    @Override
    public CatchTable build() {
        return build(this.method, this.order, this.addresses);
    }
    
    @Override
    public boolean hasAnyCatches() {
        final BasicBlockList blocks = this.method.getBlocks();
        for (int size = blocks.size(), i = 0; i < size; ++i) {
            final BasicBlock block = blocks.get(i);
            final TypeList catches = block.getLastInsn().getCatches();
            if (catches.size() != 0) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public HashSet<Type> getCatchTypes() {
        final HashSet<Type> result = new HashSet<Type>(20);
        final BasicBlockList blocks = this.method.getBlocks();
        for (int size = blocks.size(), i = 0; i < size; ++i) {
            final BasicBlock block = blocks.get(i);
            final TypeList catches = block.getLastInsn().getCatches();
            for (int catchSize = catches.size(), j = 0; j < catchSize; ++j) {
                result.add(catches.getType(j));
            }
        }
        return result;
    }
    
    public static CatchTable build(final RopMethod method, final int[] order, final BlockAddresses addresses) {
        final int len = order.length;
        final BasicBlockList blocks = method.getBlocks();
        final ArrayList<CatchTable.Entry> resultList = new ArrayList<CatchTable.Entry>(len);
        CatchHandlerList currentHandlers = CatchHandlerList.EMPTY;
        BasicBlock currentStartBlock = null;
        BasicBlock currentEndBlock = null;
        for (int i = 0; i < len; ++i) {
            final BasicBlock block = blocks.labelToBlock(order[i]);
            if (block.canThrow()) {
                final CatchHandlerList handlers = handlersFor(block, addresses);
                if (currentHandlers.size() == 0) {
                    currentStartBlock = block;
                    currentEndBlock = block;
                    currentHandlers = handlers;
                }
                else if (currentHandlers.equals(handlers) && rangeIsValid(currentStartBlock, block, addresses)) {
                    currentEndBlock = block;
                }
                else {
                    if (currentHandlers.size() != 0) {
                        final CatchTable.Entry entry = makeEntry(currentStartBlock, currentEndBlock, currentHandlers, addresses);
                        resultList.add(entry);
                    }
                    currentStartBlock = block;
                    currentEndBlock = block;
                    currentHandlers = handlers;
                }
            }
        }
        if (currentHandlers.size() != 0) {
            final CatchTable.Entry entry2 = makeEntry(currentStartBlock, currentEndBlock, currentHandlers, addresses);
            resultList.add(entry2);
        }
        final int resultSz = resultList.size();
        if (resultSz == 0) {
            return CatchTable.EMPTY;
        }
        final CatchTable result = new CatchTable(resultSz);
        for (int j = 0; j < resultSz; ++j) {
            result.set(j, resultList.get(j));
        }
        result.setImmutable();
        return result;
    }
    
    private static CatchHandlerList handlersFor(final BasicBlock block, final BlockAddresses addresses) {
        final IntList successors = block.getSuccessors();
        final int succSize = successors.size();
        final int primary = block.getPrimarySuccessor();
        final TypeList catches = block.getLastInsn().getCatches();
        int catchSize = catches.size();
        if (catchSize == 0) {
            return CatchHandlerList.EMPTY;
        }
        if ((primary == -1 && succSize != catchSize) || (primary != -1 && (succSize != catchSize + 1 || primary != successors.get(catchSize)))) {
            throw new RuntimeException("shouldn't happen: weird successors list");
        }
        for (int i = 0; i < catchSize; ++i) {
            final Type type = catches.getType(i);
            if (type.equals(Type.OBJECT)) {
                catchSize = i + 1;
                break;
            }
        }
        final CatchHandlerList result = new CatchHandlerList(catchSize);
        for (int j = 0; j < catchSize; ++j) {
            final CstType oneType = new CstType(catches.getType(j));
            final CodeAddress oneHandler = addresses.getStart(successors.get(j));
            result.set(j, oneType, oneHandler.getAddress());
        }
        result.setImmutable();
        return result;
    }
    
    private static CatchTable.Entry makeEntry(final BasicBlock start, final BasicBlock end, final CatchHandlerList handlers, final BlockAddresses addresses) {
        final CodeAddress startAddress = addresses.getLast(start);
        final CodeAddress endAddress = addresses.getEnd(end);
        return new CatchTable.Entry(startAddress.getAddress(), endAddress.getAddress(), handlers);
    }
    
    private static boolean rangeIsValid(final BasicBlock start, final BasicBlock end, final BlockAddresses addresses) {
        if (start == null) {
            throw new NullPointerException("start == null");
        }
        if (end == null) {
            throw new NullPointerException("end == null");
        }
        final int startAddress = addresses.getLast(start).getAddress();
        final int endAddress = addresses.getEnd(end).getAddress();
        return endAddress - startAddress <= 65535;
    }
}

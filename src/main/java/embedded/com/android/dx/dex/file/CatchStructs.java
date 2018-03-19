package embedded.com.android.dx.dex.file;

import embedded.com.android.dx.dex.code.*;
import java.io.*;
import java.util.*;
import embedded.com.android.dx.util.*;

public final class CatchStructs
{
    private static final int TRY_ITEM_WRITE_SIZE = 8;
    private final DalvCode code;
    private CatchTable table;
    private byte[] encodedHandlers;
    private int encodedHandlerHeaderSize;
    private TreeMap<CatchHandlerList, Integer> handlerOffsets;
    
    public CatchStructs(final DalvCode code) {
        this.code = code;
        this.table = null;
        this.encodedHandlers = null;
        this.encodedHandlerHeaderSize = 0;
        this.handlerOffsets = null;
    }
    
    private void finishProcessingIfNecessary() {
        if (this.table == null) {
            this.table = this.code.getCatches();
        }
    }
    
    public int triesSize() {
        this.finishProcessingIfNecessary();
        return this.table.size();
    }
    
    public void debugPrint(final PrintWriter out, final String prefix) {
        this.annotateEntries(prefix, out, null);
    }
    
    public void encode(final DexFile file) {
        this.finishProcessingIfNecessary();
        final TypeIdsSection typeIds = file.getTypeIds();
        final int size = this.table.size();
        this.handlerOffsets = new TreeMap<CatchHandlerList, Integer>();
        for (int i = 0; i < size; ++i) {
            this.handlerOffsets.put(this.table.get(i).getHandlers(), null);
        }
        if (this.handlerOffsets.size() > 65535) {
            throw new UnsupportedOperationException("too many catch handlers");
        }
        final ByteArrayAnnotatedOutput out = new ByteArrayAnnotatedOutput();
        this.encodedHandlerHeaderSize = out.writeUleb128(this.handlerOffsets.size());
        for (final Map.Entry<CatchHandlerList, Integer> mapping : this.handlerOffsets.entrySet()) {
            final CatchHandlerList list = mapping.getKey();
            int listSize = list.size();
            final boolean catchesAll = list.catchesAll();
            mapping.setValue(out.getCursor());
            if (catchesAll) {
                out.writeSleb128(-(listSize - 1));
                --listSize;
            }
            else {
                out.writeSleb128(listSize);
            }
            for (int j = 0; j < listSize; ++j) {
                final CatchHandlerList.Entry entry = list.get(j);
                out.writeUleb128(typeIds.indexOf(entry.getExceptionType()));
                out.writeUleb128(entry.getHandler());
            }
            if (catchesAll) {
                out.writeUleb128(list.get(listSize).getHandler());
            }
        }
        this.encodedHandlers = out.toByteArray();
    }
    
    public int writeSize() {
        return this.triesSize() * 8 + this.encodedHandlers.length;
    }
    
    public void writeTo(final DexFile file, final AnnotatedOutput out) {
        this.finishProcessingIfNecessary();
        if (out.annotates()) {
            this.annotateEntries("  ", null, out);
        }
        for (int tableSize = this.table.size(), i = 0; i < tableSize; ++i) {
            final CatchTable.Entry one = this.table.get(i);
            final int start = one.getStart();
            final int end = one.getEnd();
            final int insnCount = end - start;
            if (insnCount >= 65536) {
                throw new UnsupportedOperationException("bogus exception range: " + Hex.u4(start) + ".." + Hex.u4(end));
            }
            out.writeInt(start);
            out.writeShort(insnCount);
            out.writeShort(this.handlerOffsets.get(one.getHandlers()));
        }
        out.write(this.encodedHandlers);
    }
    
    private void annotateEntries(final String prefix, final PrintWriter printTo, final AnnotatedOutput annotateTo) {
        this.finishProcessingIfNecessary();
        final boolean consume = annotateTo != null;
        final int amt1 = consume ? 6 : 0;
        final int amt2 = consume ? 2 : 0;
        final int size = this.table.size();
        final String subPrefix = prefix + "  ";
        if (consume) {
            annotateTo.annotate(0, prefix + "tries:");
        }
        else {
            printTo.println(prefix + "tries:");
        }
        for (int i = 0; i < size; ++i) {
            final CatchTable.Entry entry = this.table.get(i);
            final CatchHandlerList handlers = entry.getHandlers();
            final String s1 = subPrefix + "try " + Hex.u2or4(entry.getStart()) + ".." + Hex.u2or4(entry.getEnd());
            final String s2 = handlers.toHuman(subPrefix, "");
            if (consume) {
                annotateTo.annotate(amt1, s1);
                annotateTo.annotate(amt2, s2);
            }
            else {
                printTo.println(s1);
                printTo.println(s2);
            }
        }
        if (!consume) {
            return;
        }
        annotateTo.annotate(0, prefix + "handlers:");
        annotateTo.annotate(this.encodedHandlerHeaderSize, subPrefix + "size: " + Hex.u2(this.handlerOffsets.size()));
        int lastOffset = 0;
        CatchHandlerList lastList = null;
        for (final Map.Entry<CatchHandlerList, Integer> mapping : this.handlerOffsets.entrySet()) {
            final CatchHandlerList list = mapping.getKey();
            final int offset = mapping.getValue();
            if (lastList != null) {
                annotateAndConsumeHandlers(lastList, lastOffset, offset - lastOffset, subPrefix, printTo, annotateTo);
            }
            lastList = list;
            lastOffset = offset;
        }
        annotateAndConsumeHandlers(lastList, lastOffset, this.encodedHandlers.length - lastOffset, subPrefix, printTo, annotateTo);
    }
    
    private static void annotateAndConsumeHandlers(final CatchHandlerList handlers, final int offset, final int size, final String prefix, final PrintWriter printTo, final AnnotatedOutput annotateTo) {
        final String s = handlers.toHuman(prefix, Hex.u2(offset) + ": ");
        if (printTo != null) {
            printTo.println(s);
        }
        annotateTo.annotate(size, s);
    }
}

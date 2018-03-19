package embedded.com.android.dx.dex.file;

import embedded.com.android.dx.rop.cst.*;
import java.util.*;

public final class MethodHandlesSection extends UniformItemSection
{
    private final TreeMap<CstMethodHandle, MethodHandleItem> methodHandles;
    
    public MethodHandlesSection(final DexFile dexFile) {
        super("method_handles", dexFile, 8);
        this.methodHandles = new TreeMap<CstMethodHandle, MethodHandleItem>();
    }
    
    @Override
    public IndexedItem get(final Constant cst) {
        if (cst == null) {
            throw new NullPointerException("cst == null");
        }
        this.throwIfNotPrepared();
        final IndexedItem result = this.methodHandles.get(cst);
        if (result == null) {
            throw new IllegalArgumentException("not found");
        }
        return result;
    }
    
    @Override
    protected void orderItems() {
        int index = 0;
        for (final MethodHandleItem item : this.methodHandles.values()) {
            item.setIndex(index++);
        }
    }
    
    @Override
    public Collection<? extends Item> items() {
        return this.methodHandles.values();
    }
    
    public void intern(final CstMethodHandle methodHandle) {
        if (methodHandle == null) {
            throw new NullPointerException("methodHandle == null");
        }
        this.throwIfPrepared();
        MethodHandleItem result = this.methodHandles.get(methodHandle);
        if (result == null) {
            result = new MethodHandleItem(methodHandle);
            this.methodHandles.put(methodHandle, result);
        }
    }
    
    int indexOf(final CstMethodHandle cstMethodHandle) {
        return this.methodHandles.get(cstMethodHandle).getIndex();
    }
}

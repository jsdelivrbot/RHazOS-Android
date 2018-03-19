package embedded.com.android.dx.dex.file;

import embedded.com.android.dx.util.*;
import embedded.com.android.dx.rop.cst.*;

public final class MethodHandleItem extends IndexedItem
{
    private final int ITEM_SIZE = 8;
    private final CstMethodHandle methodHandle;
    
    public MethodHandleItem(final CstMethodHandle methodHandle) {
        this.methodHandle = methodHandle;
    }
    
    @Override
    public ItemType itemType() {
        return ItemType.TYPE_METHOD_HANDLE_ITEM;
    }
    
    @Override
    public int writeSize() {
        return 8;
    }
    
    @Override
    public void addContents(final DexFile file) {
        final MethodHandlesSection methodHandles = file.getMethodHandles();
        methodHandles.intern(this.methodHandle);
    }
    
    @Override
    public void writeTo(final DexFile file, final AnnotatedOutput out) {
        final int targetIndex = this.getTargetIndex(file);
        if (out.annotates()) {
            out.annotate(2, "kind: " + Hex.u2(this.methodHandle.getType()));
            out.annotate(2, "reserved:" + Hex.u2(0));
            if (this.methodHandle.isAccessor()) {
                out.annotate(2, "fieldId: " + targetIndex);
            }
            else {
                out.annotate(2, "methodId: " + targetIndex);
            }
            out.annotate(2, "reserved:" + Hex.u2(0));
        }
        out.writeShort(this.methodHandle.getType());
        out.writeShort(0);
        out.writeShort(this.getTargetIndex(file));
        out.writeShort(0);
    }
    
    private int getTargetIndex(final DexFile file) {
        Constant ref = this.methodHandle.getRef();
        if (this.methodHandle.isAccessor()) {
            final FieldIdsSection fieldIds = file.getFieldIds();
            return fieldIds.indexOf((CstFieldRef)ref);
        }
        if (this.methodHandle.isInvocation()) {
            if (ref instanceof CstInterfaceMethodRef) {
                ref = ((CstInterfaceMethodRef)ref).toMethodRef();
            }
            final MethodIdsSection methodIds = file.getMethodIds();
            return methodIds.indexOf((CstBaseMethodRef)ref);
        }
        throw new IllegalStateException("Unhandled invocation type");
    }
}

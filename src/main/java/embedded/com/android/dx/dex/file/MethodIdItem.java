package embedded.com.android.dx.dex.file;

import embedded.com.android.dx.rop.cst.*;

public final class MethodIdItem extends MemberIdItem
{
    public MethodIdItem(final CstBaseMethodRef method) {
        super(method);
    }
    
    @Override
    public ItemType itemType() {
        return ItemType.TYPE_METHOD_ID_ITEM;
    }
    
    @Override
    public void addContents(final DexFile file) {
        super.addContents(file);
        final ProtoIdsSection protoIds = file.getProtoIds();
        protoIds.intern(this.getMethodRef().getPrototype());
    }
    
    public CstBaseMethodRef getMethodRef() {
        return (CstBaseMethodRef)this.getRef();
    }
    
    @Override
    protected int getTypoidIdx(final DexFile file) {
        final ProtoIdsSection protoIds = file.getProtoIds();
        return protoIds.indexOf(this.getMethodRef().getPrototype());
    }
    
    @Override
    protected String getTypoidName() {
        return "proto_idx";
    }
}

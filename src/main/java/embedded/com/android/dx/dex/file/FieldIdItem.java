package embedded.com.android.dx.dex.file;

import embedded.com.android.dx.rop.cst.*;

public final class FieldIdItem extends MemberIdItem
{
    public FieldIdItem(final CstFieldRef field) {
        super(field);
    }
    
    @Override
    public ItemType itemType() {
        return ItemType.TYPE_FIELD_ID_ITEM;
    }
    
    @Override
    public void addContents(final DexFile file) {
        super.addContents(file);
        final TypeIdsSection typeIds = file.getTypeIds();
        typeIds.intern(this.getFieldRef().getType());
    }
    
    public CstFieldRef getFieldRef() {
        return (CstFieldRef)this.getRef();
    }
    
    @Override
    protected int getTypoidIdx(final DexFile file) {
        final TypeIdsSection typeIds = file.getTypeIds();
        return typeIds.indexOf(this.getFieldRef().getType());
    }
    
    @Override
    protected String getTypoidName() {
        return "type_idx";
    }
}

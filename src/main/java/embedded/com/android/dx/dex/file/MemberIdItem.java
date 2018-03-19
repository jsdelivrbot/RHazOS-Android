package embedded.com.android.dx.dex.file;

import embedded.com.android.dx.util.*;
import embedded.com.android.dx.rop.cst.*;

public abstract class MemberIdItem extends IdItem
{
    private final CstMemberRef cst;
    
    public MemberIdItem(final CstMemberRef cst) {
        super(cst.getDefiningClass());
        this.cst = cst;
    }
    
    @Override
    public int writeSize() {
        return 8;
    }
    
    @Override
    public void addContents(final DexFile file) {
        super.addContents(file);
        final StringIdsSection stringIds = file.getStringIds();
        stringIds.intern(this.getRef().getNat().getName());
    }
    
    @Override
    public final void writeTo(final DexFile file, final AnnotatedOutput out) {
        final TypeIdsSection typeIds = file.getTypeIds();
        final StringIdsSection stringIds = file.getStringIds();
        final CstNat nat = this.cst.getNat();
        final int classIdx = typeIds.indexOf(this.getDefiningClass());
        final int nameIdx = stringIds.indexOf(nat.getName());
        final int typoidIdx = this.getTypoidIdx(file);
        if (out.annotates()) {
            out.annotate(0, this.indexString() + ' ' + this.cst.toHuman());
            out.annotate(2, "  class_idx: " + Hex.u2(classIdx));
            out.annotate(2, String.format("  %-10s %s", this.getTypoidName() + ':', Hex.u2(typoidIdx)));
            out.annotate(4, "  name_idx:  " + Hex.u4(nameIdx));
        }
        out.writeShort(classIdx);
        out.writeShort(typoidIdx);
        out.writeInt(nameIdx);
    }
    
    protected abstract int getTypoidIdx(final DexFile p0);
    
    protected abstract String getTypoidName();
    
    public final CstMemberRef getRef() {
        return this.cst;
    }
}

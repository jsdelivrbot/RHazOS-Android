package embedded.com.android.dx.dex.file;

import embedded.com.android.dx.rop.cst.*;
import embedded.com.android.dx.rop.type.*;
import embedded.com.android.dx.util.*;

public final class ProtoIdItem extends IndexedItem
{
    private final Prototype prototype;
    private final CstString shortForm;
    private TypeListItem parameterTypes;
    
    public ProtoIdItem(final Prototype prototype) {
        if (prototype == null) {
            throw new NullPointerException("prototype == null");
        }
        this.prototype = prototype;
        this.shortForm = makeShortForm(prototype);
        final StdTypeList parameters = prototype.getParameterTypes();
        this.parameterTypes = ((parameters.size() == 0) ? null : new TypeListItem(parameters));
    }
    
    private static CstString makeShortForm(final Prototype prototype) {
        final StdTypeList parameters = prototype.getParameterTypes();
        final int size = parameters.size();
        final StringBuilder sb = new StringBuilder(size + 1);
        sb.append(shortFormCharFor(prototype.getReturnType()));
        for (int i = 0; i < size; ++i) {
            sb.append(shortFormCharFor(parameters.getType(i)));
        }
        return new CstString(sb.toString());
    }
    
    private static char shortFormCharFor(final Type type) {
        final char descriptorChar = type.getDescriptor().charAt(0);
        if (descriptorChar == '[') {
            return 'L';
        }
        return descriptorChar;
    }
    
    @Override
    public ItemType itemType() {
        return ItemType.TYPE_PROTO_ID_ITEM;
    }
    
    @Override
    public int writeSize() {
        return 12;
    }
    
    @Override
    public void addContents(final DexFile file) {
        final StringIdsSection stringIds = file.getStringIds();
        final TypeIdsSection typeIds = file.getTypeIds();
        final MixedItemSection typeLists = file.getTypeLists();
        typeIds.intern(this.prototype.getReturnType());
        stringIds.intern(this.shortForm);
        if (this.parameterTypes != null) {
            this.parameterTypes = typeLists.intern(this.parameterTypes);
        }
    }
    
    @Override
    public void writeTo(final DexFile file, final AnnotatedOutput out) {
        final int shortyIdx = file.getStringIds().indexOf(this.shortForm);
        final int returnIdx = file.getTypeIds().indexOf(this.prototype.getReturnType());
        final int paramsOff = OffsettedItem.getAbsoluteOffsetOr0(this.parameterTypes);
        if (out.annotates()) {
            final StringBuilder sb = new StringBuilder();
            sb.append(this.prototype.getReturnType().toHuman());
            sb.append(" proto(");
            final StdTypeList params = this.prototype.getParameterTypes();
            for (int size = params.size(), i = 0; i < size; ++i) {
                if (i != 0) {
                    sb.append(", ");
                }
                sb.append(params.getType(i).toHuman());
            }
            sb.append(")");
            out.annotate(0, this.indexString() + ' ' + sb.toString());
            out.annotate(4, "  shorty_idx:      " + Hex.u4(shortyIdx) + " // " + this.shortForm.toQuoted());
            out.annotate(4, "  return_type_idx: " + Hex.u4(returnIdx) + " // " + this.prototype.getReturnType().toHuman());
            out.annotate(4, "  parameters_off:  " + Hex.u4(paramsOff));
        }
        out.writeInt(shortyIdx);
        out.writeInt(returnIdx);
        out.writeInt(paramsOff);
    }
}

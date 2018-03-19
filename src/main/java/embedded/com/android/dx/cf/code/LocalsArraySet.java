package embedded.com.android.dx.cf.code;

import java.util.*;
import embedded.com.android.dex.util.*;
import embedded.com.android.dx.util.*;
import embedded.com.android.dx.rop.type.*;
import embedded.com.android.dx.rop.code.*;

public class LocalsArraySet extends LocalsArray
{
    private final OneLocalsArray primary;
    private final ArrayList<LocalsArray> secondaries;
    
    public LocalsArraySet(final int maxLocals) {
        super(maxLocals != 0);
        this.primary = new OneLocalsArray(maxLocals);
        this.secondaries = new ArrayList<LocalsArray>();
    }
    
    public LocalsArraySet(final OneLocalsArray primary, final ArrayList<LocalsArray> secondaries) {
        super(primary.getMaxLocals() > 0);
        this.primary = primary;
        this.secondaries = secondaries;
    }
    
    private LocalsArraySet(final LocalsArraySet toCopy) {
        super(toCopy.getMaxLocals() > 0);
        this.primary = toCopy.primary.copy();
        this.secondaries = new ArrayList<LocalsArray>(toCopy.secondaries.size());
        for (int sz = toCopy.secondaries.size(), i = 0; i < sz; ++i) {
            final LocalsArray la = toCopy.secondaries.get(i);
            if (la == null) {
                this.secondaries.add(null);
            }
            else {
                this.secondaries.add(la.copy());
            }
        }
    }
    
    @Override
    public void setImmutable() {
        this.primary.setImmutable();
        for (final LocalsArray la : this.secondaries) {
            if (la != null) {
                la.setImmutable();
            }
        }
        super.setImmutable();
    }
    
    @Override
    public LocalsArray copy() {
        return new LocalsArraySet(this);
    }
    
    @Override
    public void annotate(final ExceptionWithContext ex) {
        ex.addContext("(locals array set; primary)");
        this.primary.annotate(ex);
        for (int sz = this.secondaries.size(), label = 0; label < sz; ++label) {
            final LocalsArray la = this.secondaries.get(label);
            if (la != null) {
                ex.addContext("(locals array set: primary for caller " + Hex.u2(label) + ')');
                la.getPrimary().annotate(ex);
            }
        }
    }
    
    @Override
    public String toHuman() {
        final StringBuilder sb = new StringBuilder();
        sb.append("(locals array set; primary)\n");
        sb.append(this.getPrimary().toHuman());
        sb.append('\n');
        for (int sz = this.secondaries.size(), label = 0; label < sz; ++label) {
            final LocalsArray la = this.secondaries.get(label);
            if (la != null) {
                sb.append("(locals array set: primary for caller " + Hex.u2(label) + ")\n");
                sb.append(la.getPrimary().toHuman());
                sb.append('\n');
            }
        }
        return sb.toString();
    }
    
    @Override
    public void makeInitialized(final Type type) {
        final int len = this.primary.getMaxLocals();
        if (len == 0) {
            return;
        }
        this.throwIfImmutable();
        this.primary.makeInitialized(type);
        for (final LocalsArray la : this.secondaries) {
            if (la != null) {
                la.makeInitialized(type);
            }
        }
    }
    
    @Override
    public int getMaxLocals() {
        return this.primary.getMaxLocals();
    }
    
    @Override
    public void set(final int idx, final TypeBearer type) {
        this.throwIfImmutable();
        this.primary.set(idx, type);
        for (final LocalsArray la : this.secondaries) {
            if (la != null) {
                la.set(idx, type);
            }
        }
    }
    
    @Override
    public void set(final RegisterSpec spec) {
        this.set(spec.getReg(), spec);
    }
    
    @Override
    public void invalidate(final int idx) {
        this.throwIfImmutable();
        this.primary.invalidate(idx);
        for (final LocalsArray la : this.secondaries) {
            if (la != null) {
                la.invalidate(idx);
            }
        }
    }
    
    @Override
    public TypeBearer getOrNull(final int idx) {
        return this.primary.getOrNull(idx);
    }
    
    @Override
    public TypeBearer get(final int idx) {
        return this.primary.get(idx);
    }
    
    @Override
    public TypeBearer getCategory1(final int idx) {
        return this.primary.getCategory1(idx);
    }
    
    @Override
    public TypeBearer getCategory2(final int idx) {
        return this.primary.getCategory2(idx);
    }
    
    private LocalsArraySet mergeWithSet(final LocalsArraySet other) {
        boolean secondariesChanged = false;
        final OneLocalsArray newPrimary = this.primary.merge(other.getPrimary());
        final int sz1 = this.secondaries.size();
        final int sz2 = other.secondaries.size();
        final int sz3 = Math.max(sz1, sz2);
        final ArrayList<LocalsArray> newSecondaries = new ArrayList<LocalsArray>(sz3);
        for (int i = 0; i < sz3; ++i) {
            final LocalsArray la1 = (i < sz1) ? this.secondaries.get(i) : null;
            final LocalsArray la2 = (i < sz2) ? other.secondaries.get(i) : null;
            LocalsArray resultla = null;
            if (la1 == la2) {
                resultla = la1;
            }
            else if (la1 == null) {
                resultla = la2;
            }
            else if (la2 == null) {
                resultla = la1;
            }
            else {
                try {
                    resultla = la1.merge(la2);
                }
                catch (SimException ex) {
                    ex.addContext("Merging locals set for caller block " + Hex.u2(i));
                }
            }
            secondariesChanged = (secondariesChanged || la1 != resultla);
            newSecondaries.add(resultla);
        }
        if (this.primary == newPrimary && !secondariesChanged) {
            return this;
        }
        return new LocalsArraySet(newPrimary, newSecondaries);
    }
    
    private LocalsArraySet mergeWithOne(final OneLocalsArray other) {
        boolean secondariesChanged = false;
        final OneLocalsArray newPrimary = this.primary.merge(other.getPrimary());
        final ArrayList<LocalsArray> newSecondaries = new ArrayList<LocalsArray>(this.secondaries.size());
        for (int sz = this.secondaries.size(), i = 0; i < sz; ++i) {
            final LocalsArray la = this.secondaries.get(i);
            LocalsArray resultla = null;
            if (la != null) {
                try {
                    resultla = la.merge(other);
                }
                catch (SimException ex) {
                    ex.addContext("Merging one locals against caller block " + Hex.u2(i));
                }
            }
            secondariesChanged = (secondariesChanged || la != resultla);
            newSecondaries.add(resultla);
        }
        if (this.primary == newPrimary && !secondariesChanged) {
            return this;
        }
        return new LocalsArraySet(newPrimary, newSecondaries);
    }
    
    @Override
    public LocalsArraySet merge(final LocalsArray other) {
        LocalsArraySet result;
        try {
            if (other instanceof LocalsArraySet) {
                result = this.mergeWithSet((LocalsArraySet)other);
            }
            else {
                result = this.mergeWithOne((OneLocalsArray)other);
            }
        }
        catch (SimException ex) {
            ex.addContext("underlay locals:");
            this.annotate(ex);
            ex.addContext("overlay locals:");
            other.annotate(ex);
            throw ex;
        }
        result.setImmutable();
        return result;
    }
    
    private LocalsArray getSecondaryForLabel(final int label) {
        if (label >= this.secondaries.size()) {
            return null;
        }
        return this.secondaries.get(label);
    }
    
    @Override
    public LocalsArraySet mergeWithSubroutineCaller(final LocalsArray other, final int predLabel) {
        final LocalsArray mine = this.getSecondaryForLabel(predLabel);
        OneLocalsArray newPrimary = this.primary.merge(other.getPrimary());
        LocalsArray newSecondary;
        if (mine == other) {
            newSecondary = mine;
        }
        else if (mine == null) {
            newSecondary = other;
        }
        else {
            newSecondary = mine.merge(other);
        }
        if (newSecondary == mine && newPrimary == this.primary) {
            return this;
        }
        newPrimary = null;
        final int szSecondaries = this.secondaries.size();
        final int sz = Math.max(predLabel + 1, szSecondaries);
        final ArrayList<LocalsArray> newSecondaries = new ArrayList<LocalsArray>(sz);
        for (int i = 0; i < sz; ++i) {
            LocalsArray la = null;
            if (i == predLabel) {
                la = newSecondary;
            }
            else if (i < szSecondaries) {
                la = this.secondaries.get(i);
            }
            if (la != null) {
                if (newPrimary == null) {
                    newPrimary = la.getPrimary();
                }
                else {
                    newPrimary = newPrimary.merge(la.getPrimary());
                }
            }
            newSecondaries.add(la);
        }
        final LocalsArraySet result = new LocalsArraySet(newPrimary, newSecondaries);
        result.setImmutable();
        return result;
    }
    
    public LocalsArray subArrayForLabel(final int subLabel) {
        final LocalsArray result = this.getSecondaryForLabel(subLabel);
        return result;
    }
    
    @Override
    protected OneLocalsArray getPrimary() {
        return this.primary;
    }
}

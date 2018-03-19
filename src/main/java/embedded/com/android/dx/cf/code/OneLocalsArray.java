package embedded.com.android.dx.cf.code;

import embedded.com.android.dex.util.*;
import embedded.com.android.dx.util.*;
import embedded.com.android.dx.rop.type.*;
import embedded.com.android.dx.rop.code.*;

public class OneLocalsArray extends LocalsArray
{
    private final TypeBearer[] locals;
    
    public OneLocalsArray(final int maxLocals) {
        super(maxLocals != 0);
        this.locals = new TypeBearer[maxLocals];
    }
    
    @Override
    public OneLocalsArray copy() {
        final OneLocalsArray result = new OneLocalsArray(this.locals.length);
        System.arraycopy(this.locals, 0, result.locals, 0, this.locals.length);
        return result;
    }
    
    @Override
    public void annotate(final ExceptionWithContext ex) {
        for (int i = 0; i < this.locals.length; ++i) {
            final TypeBearer type = this.locals[i];
            final String s = (type == null) ? "<invalid>" : type.toString();
            ex.addContext("locals[" + Hex.u2(i) + "]: " + s);
        }
    }
    
    @Override
    public String toHuman() {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < this.locals.length; ++i) {
            final TypeBearer type = this.locals[i];
            final String s = (type == null) ? "<invalid>" : type.toString();
            sb.append("locals[" + Hex.u2(i) + "]: " + s + "\n");
        }
        return sb.toString();
    }
    
    @Override
    public void makeInitialized(final Type type) {
        final int len = this.locals.length;
        if (len == 0) {
            return;
        }
        this.throwIfImmutable();
        final Type initializedType = type.getInitializedType();
        for (int i = 0; i < len; ++i) {
            if (this.locals[i] == type) {
                this.locals[i] = initializedType;
            }
        }
    }
    
    @Override
    public int getMaxLocals() {
        return this.locals.length;
    }
    
    @Override
    public void set(final int idx, TypeBearer type) {
        this.throwIfImmutable();
        try {
            type = type.getFrameType();
        }
        catch (NullPointerException ex) {
            throw new NullPointerException("type == null");
        }
        if (idx < 0) {
            throw new IndexOutOfBoundsException("idx < 0");
        }
        if (type.getType().isCategory2()) {
            this.locals[idx + 1] = null;
        }
        this.locals[idx] = type;
        if (idx != 0) {
            final TypeBearer prev = this.locals[idx - 1];
            if (prev != null && prev.getType().isCategory2()) {
                this.locals[idx - 1] = null;
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
        this.locals[idx] = null;
    }
    
    @Override
    public TypeBearer getOrNull(final int idx) {
        return this.locals[idx];
    }
    
    @Override
    public TypeBearer get(final int idx) {
        final TypeBearer result = this.locals[idx];
        if (result == null) {
            return throwSimException(idx, "invalid");
        }
        return result;
    }
    
    @Override
    public TypeBearer getCategory1(final int idx) {
        final TypeBearer result = this.get(idx);
        final Type type = result.getType();
        if (type.isUninitialized()) {
            return throwSimException(idx, "uninitialized instance");
        }
        if (type.isCategory2()) {
            return throwSimException(idx, "category-2");
        }
        return result;
    }
    
    @Override
    public TypeBearer getCategory2(final int idx) {
        final TypeBearer result = this.get(idx);
        if (result.getType().isCategory1()) {
            return throwSimException(idx, "category-1");
        }
        return result;
    }
    
    @Override
    public LocalsArray merge(final LocalsArray other) {
        if (other instanceof OneLocalsArray) {
            return this.merge((OneLocalsArray)other);
        }
        return other.merge(this);
    }
    
    public OneLocalsArray merge(final OneLocalsArray other) {
        try {
            return Merger.mergeLocals(this, other);
        }
        catch (SimException ex) {
            ex.addContext("underlay locals:");
            this.annotate(ex);
            ex.addContext("overlay locals:");
            other.annotate(ex);
            throw ex;
        }
    }
    
    @Override
    public LocalsArraySet mergeWithSubroutineCaller(final LocalsArray other, final int predLabel) {
        final LocalsArraySet result = new LocalsArraySet(this.getMaxLocals());
        return result.mergeWithSubroutineCaller(other, predLabel);
    }
    
    @Override
    protected OneLocalsArray getPrimary() {
        return this;
    }
    
    private static TypeBearer throwSimException(final int idx, final String msg) {
        throw new SimException("local " + Hex.u2(idx) + ": " + msg);
    }
}

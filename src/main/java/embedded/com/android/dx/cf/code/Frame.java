package embedded.com.android.dx.cf.code;

import embedded.com.android.dx.util.*;
import embedded.com.android.dx.rop.type.*;
import embedded.com.android.dx.rop.cst.*;
import embedded.com.android.dex.util.*;

public final class Frame
{
    private final LocalsArray locals;
    private final ExecutionStack stack;
    private final IntList subroutines;
    
    private Frame(final LocalsArray locals, final ExecutionStack stack) {
        this(locals, stack, IntList.EMPTY);
    }
    
    private Frame(final LocalsArray locals, final ExecutionStack stack, final IntList subroutines) {
        if (locals == null) {
            throw new NullPointerException("locals == null");
        }
        if (stack == null) {
            throw new NullPointerException("stack == null");
        }
        subroutines.throwIfMutable();
        this.locals = locals;
        this.stack = stack;
        this.subroutines = subroutines;
    }
    
    public Frame(final int maxLocals, final int maxStack) {
        this(new OneLocalsArray(maxLocals), new ExecutionStack(maxStack));
    }
    
    public Frame copy() {
        return new Frame(this.locals.copy(), this.stack.copy(), this.subroutines);
    }
    
    public void setImmutable() {
        this.locals.setImmutable();
        this.stack.setImmutable();
    }
    
    public void makeInitialized(final Type type) {
        this.locals.makeInitialized(type);
        this.stack.makeInitialized(type);
    }
    
    public LocalsArray getLocals() {
        return this.locals;
    }
    
    public ExecutionStack getStack() {
        return this.stack;
    }
    
    public IntList getSubroutines() {
        return this.subroutines;
    }
    
    public void initializeWithParameters(final StdTypeList params) {
        int at = 0;
        for (int sz = params.size(), i = 0; i < sz; ++i) {
            final Type one = params.get(i);
            this.locals.set(at, one);
            at += one.getCategory();
        }
    }
    
    public Frame subFrameForLabel(final int startLabel, final int subLabel) {
        LocalsArray subLocals = null;
        if (this.locals instanceof LocalsArraySet) {
            subLocals = ((LocalsArraySet)this.locals).subArrayForLabel(subLabel);
        }
        try {
            final IntList newSubroutines = this.subroutines.mutableCopy();
            if (newSubroutines.pop() != startLabel) {
                throw new RuntimeException("returning from invalid subroutine");
            }
            newSubroutines.setImmutable();
        }
        catch (IndexOutOfBoundsException ex) {
            throw new RuntimeException("returning from invalid subroutine");
        }
        catch (NullPointerException ex2) {
            throw new NullPointerException("can't return from non-subroutine");
        }
        IntList newSubroutines = null;
        return (subLocals == null) ? null : new Frame(subLocals, this.stack, newSubroutines);
    }
    
    public Frame mergeWith(final Frame other) {
        LocalsArray resultLocals = this.getLocals().merge(other.getLocals());
        final ExecutionStack resultStack = this.getStack().merge(other.getStack());
        final IntList resultSubroutines = this.mergeSubroutineLists(other.subroutines);
        resultLocals = adjustLocalsForSubroutines(resultLocals, resultSubroutines);
        if (resultLocals == this.getLocals() && resultStack == this.getStack() && this.subroutines == resultSubroutines) {
            return this;
        }
        return new Frame(resultLocals, resultStack, resultSubroutines);
    }
    
    private IntList mergeSubroutineLists(final IntList otherSubroutines) {
        if (this.subroutines.equals(otherSubroutines)) {
            return this.subroutines;
        }
        final IntList resultSubroutines = new IntList();
        for (int szSubroutines = this.subroutines.size(), szOthers = otherSubroutines.size(), i = 0; i < szSubroutines && i < szOthers && this.subroutines.get(i) == otherSubroutines.get(i); ++i) {
            resultSubroutines.add(i);
        }
        resultSubroutines.setImmutable();
        return resultSubroutines;
    }
    
    private static LocalsArray adjustLocalsForSubroutines(final LocalsArray locals, final IntList subroutines) {
        if (!(locals instanceof LocalsArraySet)) {
            return locals;
        }
        final LocalsArraySet laSet = (LocalsArraySet)locals;
        if (subroutines.size() == 0) {
            return laSet.getPrimary();
        }
        return laSet;
    }
    
    public Frame mergeWithSubroutineCaller(final Frame other, final int subLabel, final int predLabel) {
        final LocalsArray resultLocals = this.getLocals().mergeWithSubroutineCaller(other.getLocals(), predLabel);
        final ExecutionStack resultStack = this.getStack().merge(other.getStack());
        final IntList newOtherSubroutines = other.subroutines.mutableCopy();
        newOtherSubroutines.add(subLabel);
        newOtherSubroutines.setImmutable();
        if (resultLocals == this.getLocals() && resultStack == this.getStack() && this.subroutines.equals(newOtherSubroutines)) {
            return this;
        }
        IntList resultSubroutines;
        if (this.subroutines.equals(newOtherSubroutines)) {
            resultSubroutines = this.subroutines;
        }
        else {
            IntList nonResultSubroutines;
            if (this.subroutines.size() > newOtherSubroutines.size()) {
                resultSubroutines = this.subroutines;
                nonResultSubroutines = newOtherSubroutines;
            }
            else {
                resultSubroutines = newOtherSubroutines;
                nonResultSubroutines = this.subroutines;
            }
            final int szResult = resultSubroutines.size();
            final int szNonResult = nonResultSubroutines.size();
            for (int i = szNonResult - 1; i >= 0; --i) {
                if (nonResultSubroutines.get(i) != resultSubroutines.get(i + (szResult - szNonResult))) {
                    throw new RuntimeException("Incompatible merged subroutines");
                }
            }
        }
        return new Frame(resultLocals, resultStack, resultSubroutines);
    }
    
    public Frame makeNewSubroutineStartFrame(final int subLabel, final int callerLabel) {
        final IntList newSubroutines = this.subroutines.mutableCopy();
        newSubroutines.add(subLabel);
        final Frame newFrame = new Frame(this.locals.getPrimary(), this.stack, IntList.makeImmutable(subLabel));
        return newFrame.mergeWithSubroutineCaller(this, subLabel, callerLabel);
    }
    
    public Frame makeExceptionHandlerStartFrame(final CstType exceptionClass) {
        final ExecutionStack newStack = this.getStack().copy();
        newStack.clear();
        newStack.push(exceptionClass);
        return new Frame(this.getLocals(), newStack, this.subroutines);
    }
    
    public void annotate(final ExceptionWithContext ex) {
        this.locals.annotate(ex);
        this.stack.annotate(ex);
    }
}

package embedded.com.android.dx.cf.code;

import embedded.com.android.dx.util.*;

public final class SwitchList extends MutabilityControl
{
    private final IntList values;
    private final IntList targets;
    private int size;
    
    public SwitchList(final int size) {
        super(true);
        this.values = new IntList(size);
        this.targets = new IntList(size + 1);
        this.size = size;
    }
    
    @Override
    public void setImmutable() {
        this.values.setImmutable();
        this.targets.setImmutable();
        super.setImmutable();
    }
    
    public int size() {
        return this.size;
    }
    
    public int getValue(final int n) {
        return this.values.get(n);
    }
    
    public int getTarget(final int n) {
        return this.targets.get(n);
    }
    
    public int getDefaultTarget() {
        return this.targets.get(this.size);
    }
    
    public IntList getTargets() {
        return this.targets;
    }
    
    public IntList getValues() {
        return this.values;
    }
    
    public void setDefaultTarget(final int target) {
        this.throwIfImmutable();
        if (target < 0) {
            throw new IllegalArgumentException("target < 0");
        }
        if (this.targets.size() != this.size) {
            throw new RuntimeException("non-default elements not all set");
        }
        this.targets.add(target);
    }
    
    public void add(final int value, final int target) {
        this.throwIfImmutable();
        if (target < 0) {
            throw new IllegalArgumentException("target < 0");
        }
        this.values.add(value);
        this.targets.add(target);
    }
    
    public void removeSuperfluousDefaults() {
        this.throwIfImmutable();
        final int sz = this.size;
        if (sz != this.targets.size() - 1) {
            throw new IllegalArgumentException("incomplete instance");
        }
        final int defaultTarget = this.targets.get(sz);
        int at = 0;
        for (int i = 0; i < sz; ++i) {
            final int target = this.targets.get(i);
            if (target != defaultTarget) {
                if (i != at) {
                    this.targets.set(at, target);
                    this.values.set(at, this.values.get(i));
                }
                ++at;
            }
        }
        if (at != sz) {
            this.values.shrink(at);
            this.targets.set(at, defaultTarget);
            this.targets.shrink(at + 1);
            this.size = at;
        }
    }
}
